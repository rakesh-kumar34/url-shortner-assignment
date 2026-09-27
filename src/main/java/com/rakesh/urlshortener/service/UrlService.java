package com.rakesh.urlshortener.service;

import com.rakesh.urlshortener.api.ApiException;
import com.rakesh.urlshortener.api.CreateUrlRequest;
import com.rakesh.urlshortener.api.UrlResponse;
import com.rakesh.urlshortener.config.AppProperties;
import com.rakesh.urlshortener.persistence.ShortUrl;
import com.rakesh.urlshortener.persistence.UrlRepository;
import com.rakesh.urlshortener.persistence.DailyClick;
import com.rakesh.urlshortener.persistence.DailyClickId;
import com.rakesh.urlshortener.persistence.DailyClickRepository;
import com.rakesh.urlshortener.persistence.IdempotencyRecord;
import com.rakesh.urlshortener.persistence.IdempotencyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UrlService {
    private final UrlRepository urls;
    private final UrlPolicy urlPolicy;
    private final ShortCodeGenerator codes;
    private final AppProperties config;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final DailyClickRepository daily;
    private final IdempotencyRepository keys;

    public UrlService(UrlRepository urls, UrlPolicy urlPolicy, ShortCodeGenerator codes,
                       AppProperties config, Clock clock, TransactionTemplate transactions,
                       DailyClickRepository daily, IdempotencyRepository keys) {
        this.urls = urls; this.urlPolicy = urlPolicy; this.codes = codes;
        this.config = config; this.clock = clock; this.transactions = transactions;
        this.daily = daily;
        this.keys = keys;
    }

    public record CreateResult(UrlResponse url, boolean replayed) { }
    public record UrlPage(List<UrlResponse> items, long total, int page, int size) { }

    public CreateResult create(CreateUrlRequest request, String key) {
        String destination = urlPolicy.validate(request.url());
        if (request.expiresAt() != null && request.expiresAt().getNano() % 1000 != 0) {
            throw new ApiException(422, "invalid_expiry", "Expiry supports at most six fractional second digits");
        }
        if (key != null && !key.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new ApiException(422, "invalid_idempotency_key", "Use 1-128 URL-safe characters for Idempotency-Key");
        }
        String title = request.title() == null ? "" : request.title().strip();
        String keyHash = key == null ? null : hash(key);
        String fingerprint = fingerprint(destination, title, request.customAlias(),
                request.expiresAt() == null ? null : request.expiresAt().toString());
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = request.customAlias() == null ? codes.next() : request.customAlias();
            try {
                return transactions.execute(status -> {
                    CreateResult replay = replay(keyHash, fingerprint);
                    if (replay != null) { return replay; }
                    if (request.expiresAt() != null && (!request.expiresAt().isAfter(clock.instant())
                            || request.expiresAt().isAfter(clock.instant().plus(Duration.ofDays(365))))) {
                        throw new ApiException(422, "invalid_expiry", "Expiry must be in the future and within 365 days");
                    }
                    ShortUrl url = new ShortUrl(code, destination, title, clock.instant(), request.expiresAt());
                    urls.saveAndFlush(url);
                    if (keyHash != null) { keys.saveAndFlush(new IdempotencyRecord(keyHash, fingerprint, code)); }
                    return new CreateResult(view(url), false);
                });
            } catch (DataIntegrityViolationException collision) {
                if (!isUniqueViolation(collision)) { throw collision; }
                // The failed transaction is over. Check the committed winner using a fresh transaction.
                CreateResult replay = transactions.execute(status -> replay(keyHash, fingerprint));
                if (replay != null) { return replay; }
                if (request.customAlias() != null) {
                    throw new ApiException(409, "alias_conflict", "This alias is already in use");
                }
            }
        }
        throw new ApiException(503, "code_unavailable", "Unable to allocate a code; retry the request");
    }

    private boolean isUniqueViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) { return true; }
        }
        return false;
    }

    private CreateResult replay(String keyHash, String fingerprint) {
        if (keyHash == null) { return null; }
        var previous = keys.findById(keyHash);
        if (previous.isEmpty()) { return null; }
        if (!previous.get().getFingerprint().equals(fingerprint)) {
            throw new ApiException(409, "idempotency_conflict", "This key was already used with a different request");
        }
        return new CreateResult(view(require(previous.get().getCode())), true);
    }

    private String fingerprint(String... values) {
        var encoded = new StringBuilder();
        for (String value : values) {
            // Length prefixes prevent ambiguities even when a title contains delimiter characters.
            encoded.append(value == null ? -1 : value.length()).append(':');
            if (value != null) { encoded.append(value); }
        }
        return hash(encoded.toString());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 is required by Java", impossible); }
    }

    public UrlResponse get(String code) { return view(require(code)); }

    public String resolve(String code, boolean count) {
        return transactions.execute(status -> {
            ShortUrl url = urls.findLocked(code).orElseThrow(UrlService::missing);
            var now = clock.instant();
            if (!url.statusAt(now).equals("ACTIVE")) {
                throw new ApiException(410, "url_inactive", "This URL has expired or been disabled");
            }
            if (count) {
                url.recordClick(now);
                var day = new DailyClickId(code, LocalDate.ofInstant(now, ZoneOffset.UTC));
                var clicks = daily.findById(day).orElseGet(() -> new DailyClick(day));
                clicks.increment();
                daily.saveAndFlush(clicks);
            }
            return url.getDestination();
        });
    }

    public void disable(String code) {
        transactions.executeWithoutResult(status -> urls.findLocked(code).orElseThrow(UrlService::missing).disable(clock.instant()));
    }

    public record DayClicks(LocalDate date, long clicks) { }
    public record Stats(String code, long totalClicks, List<DayClicks> daily, String definition) { }

    public Stats stats(String code) {
        return transactions.execute(status -> {
            ShortUrl url = urls.findLocked(code).orElseThrow(UrlService::missing);
            LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
            var counts = new HashMap<LocalDate, Long>();
            daily.recent(code, today.minusDays(29)).forEach(row -> counts.put(row.getId().getDay(), row.getClicks()));
            var buckets = new ArrayList<DayClicks>();
            for (int i = 29; i >= 0; i--) {
                LocalDate day = today.minusDays(i);
                buckets.add(new DayClicks(day, counts.getOrDefault(day, 0L)));
            }
            return new Stats(code, url.getTotalClicks(), buckets, "Committed GET resolutions; includes bots and repeat requests; UTC days");
        });
    }

    public UrlPage list(int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 100) {
            throw new ApiException(422, "invalid_page", "Use page 0-10000 and size 1-100");
        }
        var result = urls.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("code"))));
        return new UrlPage(result.stream().map(this::view).toList(), result.getTotalElements(), page, size);
    }

    private ShortUrl require(String code) { return urls.findById(code).orElseThrow(UrlService::missing); }
    private static ApiException missing() { return new ApiException(404, "not_found", "URL not found"); }

    private UrlResponse view(ShortUrl url) {
        return new UrlResponse(url.getCode(), config.publicOrigin() + "/s/" + url.getCode(),
                url.getDestination(), url.getTitle(), url.getCreatedAt(), url.getTotalClicks(), url.getLastClickedAt(),
                url.getExpiresAt(), url.getDisabledAt(), url.statusAt(clock.instant()));
    }
}
