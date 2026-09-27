package com.rakesh.shortline.link;

import com.rakesh.shortline.api.ApiException;
import com.rakesh.shortline.api.CreateLinkRequest;
import com.rakesh.shortline.api.LinkResponse;
import com.rakesh.shortline.config.AppProperties;
import com.rakesh.shortline.persistence.Link;
import com.rakesh.shortline.persistence.LinkRepository;
import java.time.Clock;
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

    public LinkService(LinkRepository links, UrlPolicy urls, CodeGenerator codes,
                       AppProperties config, Clock clock, TransactionTemplate transactions) {
        this.links = links; this.urls = urls; this.codes = codes;
        this.config = config; this.clock = clock; this.transactions = transactions;
    }

    public record CreateResult(LinkResponse link, boolean replayed) { }
    public record LinkPage(List<LinkResponse> items, long total, int page, int size) { }

    public CreateResult create(CreateLinkRequest request, String key) {
        String destination = urls.validate(request.url());
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = request.customAlias() == null ? codes.next() : request.customAlias();
            try {
                return transactions.execute(status -> {
                    if (links.existsById(code)) {
                        throw new DataIntegrityViolationException("Code already exists");
                    }
                    Link link = new Link(code, destination, request.title() == null ? "" : request.title().strip(), clock.instant());
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
            if (count) { link.recordClick(clock.instant()); }
            return link.getDestination();
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
                link.getDestination(), link.getTitle(), link.getCreatedAt(), link.getTotalClicks(), link.getLastClickedAt());
    }
}
