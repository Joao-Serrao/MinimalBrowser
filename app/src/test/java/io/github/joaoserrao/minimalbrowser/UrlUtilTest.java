package io.github.joaoserrao.minimalbrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UrlUtilTest {

    // ---- normalizeToUrl: treated as a URL ----

    @Test
    public void keepsExplicitSchemes() {
        assertEquals("https://example.com", UrlUtil.normalizeToUrl("https://example.com"));
        assertEquals("http://example.com", UrlUtil.normalizeToUrl("http://example.com"));
        assertEquals("ftp://host/file", UrlUtil.normalizeToUrl("ftp://host/file"));
    }

    @Test
    public void addsHttpsToBareDomains() {
        assertEquals("https://github.com", UrlUtil.normalizeToUrl("github.com"));
        assertEquals("https://en.wikipedia.org/wiki/X",
                UrlUtil.normalizeToUrl("en.wikipedia.org/wiki/X"));
        assertEquals("https://example.co.uk", UrlUtil.normalizeToUrl("example.co.uk"));
    }

    @Test
    public void handlesLocalhostAndIpv4() {
        assertEquals("http://localhost", UrlUtil.normalizeToUrl("localhost"));
        assertEquals("http://localhost:8080", UrlUtil.normalizeToUrl("localhost:8080"));
        assertEquals("http://192.168.1.1", UrlUtil.normalizeToUrl("192.168.1.1"));
        assertEquals("http://127.0.0.1:3000/x", UrlUtil.normalizeToUrl("127.0.0.1:3000/x"));
    }

    // ---- normalizeToUrl: treated as a search ----

    @Test
    public void treatsPlainWordsAsSearch() {
        assertNull(UrlUtil.normalizeToUrl("hello world"));
        assertNull(UrlUtil.normalizeToUrl("android webview tutorial"));
        assertNull(UrlUtil.normalizeToUrl("weather"));
    }

    @Test
    public void doesNotMistakeDecimalsOrVersionsForUrls() {
        assertNull(UrlUtil.normalizeToUrl("3.14"));
        assertNull(UrlUtil.normalizeToUrl("version1.2"));
        assertNull(UrlUtil.normalizeToUrl("what is 2.5 cups"));
    }

    @Test
    public void blankInputIsNull() {
        assertNull(UrlUtil.normalizeToUrl(""));
        assertNull(UrlUtil.normalizeToUrl("   "));
        assertNull(UrlUtil.normalizeToUrl(null));
    }

    // ---- displayTextForUrl ----

    @Test
    public void showsQueryForDuckDuckGoResults() {
        assertEquals("cats", UrlUtil.displayTextForUrl(
                "https://duckduckgo.com/?q=cats&kp=-2&ka=-1"));
        assertEquals("black cats", UrlUtil.displayTextForUrl(
                "https://duckduckgo.com/?q=black+cats"));
        assertEquals("a&b", UrlUtil.displayTextForUrl(
                "https://duckduckgo.com/?q=a%26b"));
    }

    @Test
    public void showsNothingForDuckDuckGoHome() {
        assertEquals("", UrlUtil.displayTextForUrl("https://duckduckgo.com/?kp=-2&ka=-1"));
        assertEquals("", UrlUtil.displayTextForUrl("https://duckduckgo.com/"));
    }

    @Test
    public void showsFullUrlForOtherPages() {
        assertEquals("https://github.com/user/repo",
                UrlUtil.displayTextForUrl("https://github.com/user/repo"));
        assertEquals("", UrlUtil.displayTextForUrl(null));
    }
}
