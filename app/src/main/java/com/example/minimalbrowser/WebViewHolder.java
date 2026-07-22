package com.example.minimalbrowser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Creates and configures the WebViews used by {@link SearchActivity}. */
public class WebViewHolder {

    /** Callbacks the activity provides so the WebViews can drive its chrome. */
    interface Host {
        /** Loading progress (0-100) for the given view. */
        void onProgress(WebView view, int progress);

        /** The given view navigated to a new URL. */
        void onUrlChanged(WebView view, String url);

        /**
         * A page requested a file picker. Returns true if a chooser was
         * launched (the callback will be invoked later), false to cancel.
         */
        boolean onFileChooser(ValueCallback<Uri[]> callback,
                              WebChromeClient.FileChooserParams params);
    }

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

    public static WebView create(Activity activity, Host host) {
        if (activity == null) throw new IllegalArgumentException("Activity context required");

        WebView wv = new WebView(activity);
        applySettings(activity, wv, host);
        return wv;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void applySettings(Activity activity, WebView wv, Host host) {
        WebSettings settings = wv.getSettings();
        // Append to the stock user agent rather than pinning a fixed Chrome
        // version, which goes stale and makes sites serve degraded pages.
        settings.setUserAgentString(settings.getUserAgentString() + " MinimalBrowser");
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        // Leave multiple-window support OFF. With it on, ad scripts' window.open
        // and popup links spawn new windows; off, target="_blank" simply loads
        // in the current view and popups are swallowed.
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(wv, true);

        wv.setWebViewClient(createWebViewClient(activity, host));
        wv.setWebChromeClient(createChromeClient(activity, host));
        wv.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength)
                -> openExternally(activity, url));
    }

    private static WebViewClient createWebViewClient(Activity activity, Host host) {
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
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                notifyUrl(host, view, url);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                // Fires on in-page (history.pushState) navigations too.
                notifyUrl(host, view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                notifyUrl(host, view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                // Only replace the whole page for main-frame failures; a failed
                // image or tracker must not blow away the document.
                if (request.isForMainFrame()) {
                    showErrorPage(view, request.getUrl().toString(), error.getDescription());
                }
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

    private static void notifyUrl(Host host, WebView view, String url) {
        // The error page is a data: document; showing that in the address bar
        // would be noise, so it is filtered here.
        if (host != null && url != null && !url.startsWith("data:")) {
            host.onUrlChanged(view, url);
        }
    }

    private static void showErrorPage(WebView view, String failedUrl, CharSequence description) {
        String safeUrl = TextUtils.htmlEncode(failedUrl == null ? "" : failedUrl);
        String safeDesc = TextUtils.htmlEncode(description == null ? "" : description.toString());
        String html = "<!DOCTYPE html><html><head><meta name='viewport' "
                + "content='width=device-width, initial-scale=1'>"
                + "<style>body{background:#121212;color:#e0e0e0;font-family:sans-serif;"
                + "margin:0;display:flex;min-height:100vh;align-items:center;"
                + "justify-content:center;text-align:center;padding:24px}"
                + "h1{font-size:20px;font-weight:600}p{color:#9e9e9e;font-size:14px;"
                + "word-break:break-all}a{display:inline-block;margin-top:16px;padding:10px 20px;"
                + "background:#3DDC84;color:#00210f;border-radius:24px;text-decoration:none;"
                + "font-weight:600}</style></head><body><div>"
                + "<h1>Can’t reach this page</h1><p>" + safeDesc + "</p>"
                + "<p>" + safeUrl + "</p>"
                + "<a href='" + safeUrl + "'>Retry</a>"
                + "</div></body></html>";
        view.loadDataWithBaseURL(failedUrl, html, "text/html", "utf-8", failedUrl);
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

    private static WebChromeClient createChromeClient(Activity activity, Host host) {
        return new WebChromeClient() {
            private View customView;
            private CustomViewCallback customViewCallback;
            private int originalOrientation;

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (host != null) host.onProgress(view, newProgress);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                // Without this, <input type="file"> silently does nothing.
                return host != null && host.onFileChooser(callback, params);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(activity)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (d, w) -> result.confirm())
                        .setOnCancelListener(d -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(activity)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (d, w) -> result.confirm())
                        .setNegativeButton(android.R.string.cancel, (d, w) -> result.cancel())
                        .setOnCancelListener(d -> result.cancel())
                        .show();
                return true;
            }

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
