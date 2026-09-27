package com.rakesh.shortline.link;

import com.rakesh.shortline.api.ApiException;
import com.rakesh.shortline.config.AppProperties;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UrlPolicy {
    private final URI publicOrigin;

    public UrlPolicy(AppProperties config) { publicOrigin = URI.create(config.publicOrigin()); }

    public String validate(String value) {
        try {
            if (value == null || value.length() > 2048 || value.matches(".*[\\s\\p{Cntrl}].*")
                    || value.toLowerCase(Locale.ROOT).matches(".*%(0a|0d|00).*")) {
                throw new IllegalArgumentException();
            }
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("https") || scheme.equals("http")) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw new IllegalArgumentException();
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT).replaceAll("\\.$", "");
            if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")
                    || host.endsWith(".internal") || host.endsWith(".lan")
                    || (!host.contains(".") && !host.contains(":"))) {
                throw new IllegalArgumentException();
            }
            // Numeric literals only: never resolve an arbitrary hostname or fetch its destination.
            if (host.matches("[0-9.]+") || host.contains(":")) {
                InetAddress ip = InetAddress.getByName(host);
                byte[] bytes = ip.getAddress();
                if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress()
                        || ip.isSiteLocalAddress() || ip.isMulticastAddress()
                        || (bytes.length == 16 && (bytes[0] & 0xe0) != 0x20)
                        || (bytes.length == 4 && ((bytes[0] & 255) == 0
                        || ((bytes[0] & 255) == 100 && (bytes[1] & 255) >= 64 && (bytes[1] & 255) <= 127)))) {
                    throw new IllegalArgumentException();
                }
            }
            if (host.equalsIgnoreCase(publicOrigin.getHost()) && port(uri) == port(publicOrigin)) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (Exception e) {
            throw new ApiException(422, "invalid_url", "Use a public HTTP(S) URL without credentials or control characters");
        }
    }

    private int port(URI uri) { return uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort(); }
}
