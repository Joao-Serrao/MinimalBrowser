package com.example.minimalbrowser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Creates and configures the WebViews used by {@link SearchActivity}. */
public class WebViewHolder {

    /** Hosts blocked outright, matched exactly or as a parent domain. */
    private static final Set<String> BLOCKED_HOSTS = new HashSet<>(Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "google-analytics.com",
            "adservice.google.com",
            "amazon-adsystem.com",
            "adnxs.com",
            "criteo.com",
            "outbrain.com",
            "taboola.com",
            "scorecardresearch.com"));

    /** Subdomain prefixes that reliably indicate an ad server. */
    private static final String[] BLOCKED_PREFIXES = {"ads.", "ad.", "adserver.", "banner.", "track."};

    private WebViewHolder() {
    }

    public static WebView create(Activity activity) {
        if (activity == null) throw new IllegalArgumentException("Activity context required");

        WebView wv = new WebView(activity);
        applySettings(activity, wv);
        return wv;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void applySettings(Activity activity, WebView wv) {
        WebSettings settings = wv.getSettings();
        // Append to the stock user agent rather than pinning a fixed Chrome
        // version, which goes stale and makes sites serve degraded pages.
        settings.setUserAgentString(settings.getUserAgentString() + " MinimalBrowser");
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(wv, true);

        wv.setWebViewClient(createWebViewClient(activity));
        wv.setWebChromeClient(createChromeClient(activity));
        wv.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength)
                -> openExternally(activity, url));
    }

    private static WebViewClient createWebViewClient(Activity activity) {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) return false;

                // mailto:, tel:, intent:, market: ... hand off to the system.
                openExternally(activity, url);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (isBlocked(request.getUrl())) {
                    // An empty stream, not null: a null stream is undefined
                    // behaviour and can stall the load on some WebView builds.
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }
        };
    }

    /**
     * Host-based match. The old check was {@code url.contains("ads.")}, which
     * also matched any URL containing "loads." — blocking downloads.*, uploads.*
     * and threads.* among others.
     */
    static boolean isBlocked(Uri uri) {
        return isBlockedHost(uri.getHost());
    }

    /** Split out from {@link #isBlocked} so it is unit-testable off-device. */
    static boolean isBlockedHost(String host) {
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);

        for (String prefix : BLOCKED_PREFIXES) {
            if (host.startsWith(prefix)) return true;
        }
        for (String blocked : BLOCKED_HOSTS) {
            if (host.equals(blocked) || host.endsWith("." + blocked)) return true;
        }
        return false;
    }

    private static void openExternally(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
            // No app installed that can handle this scheme.
        }
    }

    private static WebChromeClient createChromeClient(Activity activity) {
        return new WebChromeClient() {
            private View customView;
            private CustomViewCallback customViewCallback;
            private int originalOrientation;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;
                originalOrientation = activity.getRequestedOrientation();
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

                View focusedView = activity.getCurrentFocus();
                if (focusedView != null) {
                    InputMethodManager imm = (InputMethodManager)
                            activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                }

                ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                decor.addView(customView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                SystemUi.hideBars(activity);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;

                ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
                decor.removeView(customView);
                customView = null;

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }

                activity.setRequestedOrientation(originalOrientation);
                SystemUi.hideBars(activity);
            }
        };
    }
}
