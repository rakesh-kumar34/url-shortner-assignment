package com.rakesh.shortline.link;

import com.rakesh.shortline.api.ApiException;
import com.rakesh.shortline.api.CreateLinkRequest;
import com.rakesh.shortline.api.LinkResponse;
import com.rakesh.shortline.config.AppProperties;
import com.rakesh.shortline.persistence.Link;
import com.rakesh.shortline.persistence.LinkRepository;
import com.rakesh.shortline.persistence.DailyClick;
import com.rakesh.shortline.persistence.DailyClickId;
import com.rakesh.shortline.persistence.DailyClickRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LinkService {
    private final LinkRepository links;
    private final UrlPolicy urls;
    private final CodeGenerator codes;
    private final AppProperties config;
    private final Clock clock;
    private final TransactionTemplate transactions;
    private final DailyClickRepository daily;

    public LinkService(LinkRepository links, UrlPolicy urls, CodeGenerator codes,
                       AppProperties config, Clock clock, TransactionTemplate transactions, DailyClickRepository daily) {
        this.links = links; this.urls = urls; this.codes = codes;
        this.config = config; this.clock = clock; this.transactions = transactions;
        this.daily = daily;
    }

    public record CreateResult(LinkResponse link, boolean replayed) { }
    public record LinkPage(List<LinkResponse> items, long total, int page, int size) { }

    public CreateResult create(CreateLinkRequest request, String key) {
        String destination = urls.validate(request.url());
        if (request.expiresAt() != null && (!request.expiresAt().isAfter(clock.instant())
                || request.expiresAt().isAfter(clock.instant().plus(Duration.ofDays(365))))) {
            throw new ApiException(422, "invalid_expiry", "Expiry must be in the future and within 365 days");
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = request.customAlias() == null ? codes.next() : request.customAlias();
            try {
                return transactions.execute(status -> {
                    if (links.existsById(code)) {
                        throw new DataIntegrityViolationException("Code already exists");
                    }
                    Link link = new Link(code, destination, request.title() == null ? "" : request.title().strip(), clock.instant(), request.expiresAt());
                    links.saveAndFlush(link);
                    return new CreateResult(view(link), false);
                });
            } catch (DataIntegrityViolationException collision) {
                if (request.customAlias() != null) {
                    throw new ApiException(409, "alias_conflict", "This alias is already in use");
                }
            }
        }
        throw new ApiException(503, "code_unavailable", "Unable to allocate a code; retry the request");
    }

    public LinkResponse get(String code) { return view(require(code)); }

    public String resolve(String code, boolean count) {
        return transactions.execute(status -> {
            Link link = links.findLocked(code).orElseThrow(LinkService::missing);
            var now = clock.instant();
            if (!link.statusAt(now).equals("ACTIVE")) {
                throw new ApiException(410, "link_inactive", "This link has expired or been disabled");
            }
            if (count) {
                link.recordClick(now);
                var day = new DailyClickId(code, LocalDate.ofInstant(now, ZoneOffset.UTC));
                var clicks = daily.findById(day).orElseGet(() -> new DailyClick(day));
                clicks.increment();
                daily.saveAndFlush(clicks);
            }
            return link.getDestination();
        });
    }

    public void disable(String code) {
        transactions.executeWithoutResult(status -> links.findLocked(code).orElseThrow(LinkService::missing).disable(clock.instant()));
    }

    public record DayClicks(LocalDate date, long clicks) { }
    public record Stats(String code, long totalClicks, List<DayClicks> daily, String definition) { }

    public Stats stats(String code) {
        return transactions.execute(status -> {
            Link link = links.findLocked(code).orElseThrow(LinkService::missing);
            LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
            var counts = new HashMap<LocalDate, Long>();
            daily.recent(code, today.minusDays(29)).forEach(row -> counts.put(row.getId().getDay(), row.getClicks()));
            var buckets = new ArrayList<DayClicks>();
            for (int i = 29; i >= 0; i--) {
                LocalDate day = today.minusDays(i);
                buckets.add(new DayClicks(day, counts.getOrDefault(day, 0L)));
            }
            return new Stats(code, link.getTotalClicks(), buckets, "Committed GET resolutions; includes bots and repeat requests; UTC days");
        });
    }

    public LinkPage list(int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 100) {
            throw new ApiException(422, "invalid_page", "Use page 0-10000 and size 1-100");
        }
        var result = links.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("code"))));
        return new LinkPage(result.stream().map(this::view).toList(), result.getTotalElements(), page, size);
    }

    private Link require(String code) { return links.findById(code).orElseThrow(LinkService::missing); }
    private static ApiException missing() { return new ApiException(404, "not_found", "Link not found"); }

    private LinkResponse view(Link link) {
        return new LinkResponse(link.getCode(), config.publicOrigin() + "/s/" + link.getCode(),
                link.getDestination(), link.getTitle(), link.getCreatedAt(), link.getTotalClicks(), link.getLastClickedAt(),
                link.getExpiresAt(), link.getDisabledAt(), link.statusAt(clock.instant()));
    }
}
