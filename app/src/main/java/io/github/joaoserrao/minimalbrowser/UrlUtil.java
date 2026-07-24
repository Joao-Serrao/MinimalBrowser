package io.github.joaoserrao.minimalbrowser;

import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * Pure URL/query helpers, kept free of Android APIs so they can be unit-tested
 * on a plain JVM.
 */
final class UrlUtil {

    private static final Pattern HAS_SCHEME = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://.*");
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}([:/].*)?$");
    private static final Pattern TLD = Pattern.compile("[a-zA-Z]{2,}");

    private UrlUtil() {
    }

    /**
     * Returns a URL to navigate to if the input clearly names a page, or null
     * if it should be treated as a search query.
     */
    static String normalizeToUrl(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        if (HAS_SCHEME.matcher(s).matches()) return s;
        if (s.contains(" ")) return null;                       // has a space -> search

        if (s.equals("localhost") || s.startsWith("localhost:") || s.startsWith("localhost/")) {
            return "http://" + s;
        }
        if (IPV4.matcher(s).matches()) return "http://" + s;

        // Strip path and port, then require a plausible letter TLD so that
        // "3.14" or "version1.2" fall through to search.
        String host = s;
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);

        int dot = host.lastIndexOf('.');
        if (dot > 0 && dot < host.length() - 1) {
            if (TLD.matcher(host.substring(dot + 1)).matches()) return "https://" + s;
        }
        return null;
    }

    /**
     * Turns a loaded URL into address-bar text: a DuckDuckGo results page shows
     * just its query, the bare homepage shows nothing (so Home leaves the field
     * empty), and everything else shows the full URL.
     */
    static String displayTextForUrl(String url) {
        if (url == null) return "";
        if (isDuckDuckGo(url)) {
            String query = queryParam(url, "q");
            return query == null ? "" : query;
        }
        return url;
    }

    /** True for pages served by DuckDuckGo itself. */
    static boolean isDuckDuckGo(String url) {
        return url != null
                && (url.startsWith("https://duckduckgo.com/")
                || url.startsWith("https://www.duckduckgo.com/"));
    }

    static String queryParam(String url, String key) {
        int mark = url.indexOf('?');
        if (mark < 0) return null;
        for (String pair : url.substring(mark + 1).split("&")) {
            int eq = pair.indexOf('=');
            String k = eq >= 0 ? pair.substring(0, eq) : pair;
            if (k.equals(key)) {
                String v = eq >= 0 ? pair.substring(eq + 1) : "";
                try {
                    return URLDecoder.decode(v, "UTF-8");
                } catch (Exception e) {
                    return v;
                }
            }
        }
        return null;
    }
}
