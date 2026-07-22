package com.example.minimalbrowser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Covers the host matcher behind {@link WebViewHolder#isBlocked}. */
public class AdBlockTest {

    @Test
    public void blocksKnownAdHosts() {
        assertTrue(WebViewHolder.isBlockedHost("doubleclick.net"));
        assertTrue(WebViewHolder.isBlockedHost("googlesyndication.com"));
        assertTrue(WebViewHolder.isBlockedHost("ads.example.com"));
    }

    @Test
    public void blocksSubdomainsOfKnownAdHosts() {
        assertTrue(WebViewHolder.isBlockedHost("static.doubleclick.net"));
        assertTrue(WebViewHolder.isBlockedHost("pagead2.googlesyndication.com"));
    }

    @Test
    public void isCaseInsensitive() {
        assertTrue(WebViewHolder.isBlockedHost("DoubleClick.NET"));
    }

    /**
     * Regression: the previous check was {@code url.contains("ads.")}, and
     * "downloads." contains "ads." — so these were all silently blocked.
     */
    @Test
    public void allowsHostsThatMerelyContainAdSubstrings() {
        assertFalse(WebViewHolder.isBlockedHost("downloads.mozilla.org"));
        assertFalse(WebViewHolder.isBlockedHost("uploads.github.com"));
        assertFalse(WebViewHolder.isBlockedHost("threads.net"));
        assertFalse(WebViewHolder.isBlockedHost("gradle.org"));
    }

    /** A blocked name must not match a different registrable domain. */
    @Test
    public void doesNotBlockLookalikeDomains() {
        assertFalse(WebViewHolder.isBlockedHost("notdoubleclick.net"));
        assertFalse(WebViewHolder.isBlockedHost("doubleclick.net.evil.com"));
    }

    @Test
    public void allowsOrdinaryHosts() {
        assertFalse(WebViewHolder.isBlockedHost("duckduckgo.com"));
        assertFalse(WebViewHolder.isBlockedHost("en.wikipedia.org"));
        assertFalse(WebViewHolder.isBlockedHost(null));
    }
}
