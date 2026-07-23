package io.github.joaoserrao.minimalbrowser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity implements WebViewHolder.Host {

    private static final String HOME_URL = "https://duckduckgo.com/?kp=-2&ka=-1";
    private static final String SEARCH_URL = "https://duckduckgo.com/?q=%s&kp=-2&ka=-1";
    private static final String SECONDARY_HOME_URL = "https://claude.ai/";

    private static final String EXTRA_FROM_WIDGET = "fromWidget";
    private static final int DOUBLE_TAP_MS = 300;
    private static final long ANIM_MS = 250;
    private static final float MIN_PANE_FRACTION = 0.1f;

    private WebView mainWebView;
    private WebView secondaryWebView;
    private WebView activeWebView;

    private EditText searchInput;
    private ImageButton toggleSearchButton;
    private LinearLayout searchContainer;
    private ProgressBar progressBar;
    private FrameLayout webArea;
    private FrameLayout leftContainer;
    private FrameLayout rightContainer;
    private LinearLayout splitContainer;

    private ValueCallback<Uri[]> pendingFileCallback;
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Uri[] uris = WebChromeClient.FileChooserParams.parseResult(
                        result.getResultCode(), result.getData());
                if (pendingFileCallback != null) {
                    // Must always deliver, even null on cancel, or the page's
                    // file input stays permanently stuck.
                    pendingFileCallback.onReceiveValue(uris);
                    pendingFileCallback = null;
                }
            });

    private boolean isSplit = false;
    private boolean isResizing = false;
    private View resizeOverlay;

    private long lastClickTime = 0;
    private WebView lastClickedWebView;

    private float savedLeftWeight = 0.5f;
    private float savedRightWeight = 0.5f;

    private View rootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private int lastKeyboardInset = -1;

    private GestureDetector searchSwipeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The resize mode is declared in the manifest; setting it here as well
        // used the deprecated SOFT_INPUT_ADJUST_RESIZE constant for no gain.
        setContentView(R.layout.activity_search);

        bindViews();
        SystemUi.hideBars(this);
        applyKeyboardResizeWorkaround();

        mainWebView = WebViewHolder.create(this, this);
        attachWebView(leftContainer, mainWebView);
        activeWebView = mainWebView;
        setupWebViewTouchListener(mainWebView);
        mainWebView.loadUrl(HOME_URL);

        setupSearch();
        setupSearchBarToggle();
        setupSplitControls();
        setupBackHandling();

        handleWidgetLaunch(getIntent());
    }

    private void bindViews() {
        rootView = findViewById(R.id.root_layout);
        searchInput = findViewById(R.id.search_input);
        searchContainer = findViewById(R.id.searchContainer);
        toggleSearchButton = findViewById(R.id.toggle_search_button);
        progressBar = findViewById(R.id.progress_bar);
        webArea = findViewById(R.id.web_area);
        splitContainer = findViewById(R.id.web_split_container);
        leftContainer = findViewById(R.id.web_container_left);
        rightContainer = findViewById(R.id.web_container_right);
    }

    /**
     * The widget uses singleTop, so a tap while the app is already running
     * arrives here rather than in {@link #onCreate}.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWidgetLaunch(intent);
    }

    private void handleWidgetLaunch(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_FROM_WIDGET, false)) return;

        setSearchBarVisible(true, false);
        focusSearchInput();
    }

    // ------------------- SEARCH -------------------

    private void setupSearch() {
        ImageButton searchButton = findViewById(R.id.search_button);
        ImageButton homeButton = findViewById(R.id.home_button);

        searchButton.setOnClickListener(v -> performSearch());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            // Without this guard the listener also fires for
            // IME_ACTION_UNSPECIFIED and for both the down and up of a
            // hardware Enter, running the search two or three times.
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                performSearch();
                return true;
            }
            return false;
        });

        homeButton.setOnClickListener(v -> {
            searchInput.setText("");
            hideKeyboard();
            if (activeWebView != null) activeWebView.loadUrl(HOME_URL);
        });

        setupSearchBarSwipe();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /** A left-to-right swipe across the search bar triggers Back. */
    @SuppressLint("ClickableViewAccessibility")
    private void setupSearchBarSwipe() {
        final int minDistance = dpToPx(64);
        final int minVelocity = dpToPx(200);

        searchSwipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                // Left-to-right, clearly horizontal, and fast enough.
                if (dx > minDistance && Math.abs(dx) > Math.abs(dy) * 2
                        && Math.abs(velocityX) > minVelocity) {
                    goBackOrUnwind();
                    return true;
                }
                return false;
            }
        });

        // Returning false for everything except a detected fling keeps normal
        // tap-to-edit behaviour on the input intact.
        View.OnTouchListener swipeListener = (v, event) -> searchSwipeDetector.onTouchEvent(event);
        searchInput.setOnTouchListener(swipeListener);
        searchContainer.setOnTouchListener(swipeListener);
    }

    private void performSearch() {
        String input = searchInput.getText().toString().trim();
        hideKeyboard();
        if (input.isEmpty() || activeWebView == null) return;

        String url = UrlUtil.normalizeToUrl(input);
        if (url == null) {
            // Uri.encode, not a manual space swap: a query containing &, #, + or
            // % used to produce a broken or truncated URL.
            url = String.format(SEARCH_URL, Uri.encode(input));
        }
        activeWebView.loadUrl(url);
    }

    // ------------------- SEARCH BAR TOGGLE -------------------

    private void setupSearchBarToggle() {
        toggleSearchButton.setOnClickListener(v ->
                setSearchBarVisible(searchContainer.getVisibility() != View.VISIBLE, true));
    }

    private void setSearchBarVisible(boolean visible, boolean animate) {
        boolean alreadyVisible = searchContainer.getVisibility() == View.VISIBLE;
        if (visible == alreadyVisible && searchContainer.getLayoutParams().height
                == ViewGroup.LayoutParams.WRAP_CONTENT) {
            if (visible) focusSearchInput();
            return;
        }

        toggleSearchButton.setImageResource(
                visible ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);

        if (!animate) {
            setSearchBarHeight(visible ? ViewGroup.LayoutParams.WRAP_CONTENT : 0);
            searchContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) focusSearchInput();
            return;
        }

        if (visible) {
            searchContainer.setVisibility(View.VISIBLE);
            animateSearchBar(0, measureSearchBarHeight(), true);
            focusSearchInput();
        } else {
            hideKeyboard();
            animateSearchBar(searchContainer.getHeight(), 0, false);
        }
    }

    /**
     * The old code animated to a hardcoded 48dp, which ignored the container's
     * 8dp padding and clipped the field, and it left the height pinned to that
     * pixel value forever instead of restoring WRAP_CONTENT.
     */
    private int measureSearchBarHeight() {
        int width = ((View) searchContainer.getParent()).getWidth();
        searchContainer.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return searchContainer.getMeasuredHeight();
    }

    private void animateSearchBar(int from, int to, boolean showing) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(ANIM_MS);
        animator.addUpdateListener(a -> setSearchBarHeight((int) a.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (showing) {
                    setSearchBarHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                } else {
                    searchContainer.setVisibility(View.GONE);
                }
            }
        });
        animator.start();
    }

    private void setSearchBarHeight(int height) {
        ViewGroup.LayoutParams lp = searchContainer.getLayoutParams();
        lp.height = height;
        searchContainer.setLayoutParams(lp);
    }

    private void focusSearchInput() {
        searchInput.post(() -> {
            searchInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    // ------------------- KEYBOARD HANDLING -------------------

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view instanceof EditText && ev.getAction() == MotionEvent.ACTION_DOWN) {
            int[] coords = new int[2];
            view.getLocationOnScreen(coords);
            Rect bounds = new Rect(coords[0], coords[1],
                    coords[0] + view.getWidth(), coords[1] + view.getHeight());
            if (!bounds.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                hideKeyboard();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }
    }

    /**
     * Immersive mode and adjustResize do not cooperate, so the keyboard height
     * is applied as bottom padding by hand.
     *
     * The listener is now retained so it can be unregistered, and padding is
     * only written when the value actually changes — setting it unconditionally
     * from inside a layout pass re-triggers layout.
     */
    private void applyKeyboardResizeWorkaround() {
        keyboardListener = () -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            int inset = (keypadHeight > screenHeight * 0.15) ? keypadHeight : 0;

            if (inset != lastKeyboardInset) {
                lastKeyboardInset = inset;
                rootView.setPadding(0, 0, 0, inset);
            }
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
    }

    // ------------------- SPLIT WEBVIEW HANDLING -------------------

    private void setupSplitControls() {
        ImageButton toggleSplitButton = findViewById(R.id.toggle_split_button);

        toggleSplitButton.setOnClickListener(v -> {
            hideKeyboard();
            toggleSplit();
        });

        toggleSplitButton.setOnLongClickListener(v -> {
            hideKeyboard();
            if (!isSplit) {
                // Closed -> open the second pane on Claude, replacing whatever
                // it last showed, and focus it.
                ensureSecondaryWebView();
                openSplit();
                secondaryWebView.loadUrl(SECONDARY_HOME_URL);
                activeWebView = secondaryWebView;
                lastClickedWebView = secondaryWebView;
                refreshOmnibox();
            } else {
                // Already open -> swap the two panes.
                swapWebViews();
            }
            return true;
        });
    }

    private void toggleSplit() {
        if (isSplit) closeSplit();
        else openSplit();
    }

    private void openSplit() {
        ensureSecondaryWebView();

        attachWebView(rightContainer, secondaryWebView);
        rightContainer.setVisibility(View.VISIBLE);
        secondaryWebView.onResume();
        isSplit = true;

        adjustSplitLayoutWithSavedWeights(getResources().getConfiguration().orientation);

        // Keep focus on the window the user was already using. Previously the
        // split stole focus to the new pane, so hardware Back drove the second
        // window's history and only reached the main window once the split was
        // closed. Tapping a pane (or long-press) still switches focus.
        activeWebView = mainWebView;
        lastClickedWebView = mainWebView;
        refreshOmnibox();
    }

    private void closeSplit() {
        LinearLayout.LayoutParams lpLeft = (LinearLayout.LayoutParams) leftContainer.getLayoutParams();
        LinearLayout.LayoutParams lpRight = (LinearLayout.LayoutParams) rightContainer.getLayoutParams();
        savedLeftWeight = lpLeft.weight;
        savedRightWeight = lpRight.weight;

        exitResizeMode();
        rightContainer.removeAllViews();
        rightContainer.setVisibility(View.GONE);

        // Detaching alone does not stop timers or media: without onPause the
        // hidden pane keeps playing audio.
        if (secondaryWebView != null) secondaryWebView.onPause();

        isSplit = false;
        leftContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        splitContainer.setOrientation(LinearLayout.HORIZONTAL);
        activeWebView = mainWebView;
        lastClickedWebView = mainWebView;
        // The address bar was still showing the now-closed pane's URL.
        progressBar.setVisibility(View.GONE);
        refreshOmnibox();
    }

    /**
     * Single creation path. The long-press handler used to build its own
     * WebView unconditionally, overwriting and leaking any existing one.
     */
    private void ensureSecondaryWebView() {
        if (secondaryWebView != null) return;

        secondaryWebView = WebViewHolder.create(this, this);
        setupWebViewTouchListener(secondaryWebView);
        secondaryWebView.loadUrl(SECONDARY_HOME_URL);
        savedLeftWeight = 0.5f;
        savedRightWeight = 0.5f;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupWebViewTouchListener(WebView webView) {
        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (activeWebView != webView) {
                    activeWebView = webView;
                    // Reflect the newly focused pane in the address bar and drop
                    // the other pane's stale progress.
                    updateOmnibox(webView, webView.getUrl());
                    progressBar.setVisibility(View.GONE);
                }
                long now = System.currentTimeMillis();
                if (lastClickedWebView == webView && now - lastClickTime < DOUBLE_TAP_MS) {
                    // Exiting is handled by the overlay's own double tap; while
                    // resizing the overlay swallows these events anyway.
                    if (isSplit && !isResizing) enterResizeMode();
                    lastClickTime = 0;
                } else {
                    lastClickTime = now;
                    lastClickedWebView = webView;
                }
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void enterResizeMode() {
        if (!isSplit || resizeOverlay != null) return;

        resizeOverlay = new View(this);
        // Added to web_area, not root_layout, so the top bar and search field
        // stay usable while resizing.
        webArea.addView(resizeOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        resizeOverlay.setBackgroundColor(0x22FFFFFF);
        resizeOverlay.setOnTouchListener(resizeTouchListener);
        isResizing = true;
    }

    private void exitResizeMode() {
        if (resizeOverlay != null) {
            webArea.removeView(resizeOverlay);
            resizeOverlay = null;
        }
        isResizing = false;
    }

    private final View.OnTouchListener resizeTouchListener = new View.OnTouchListener() {
        private float startPos = 0f;
        private float startLeftWeight = 0f;
        private float startRightWeight = 0f;
        private long lastTapTime = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!isSplit) return false;

            int orientation = getResources().getConfiguration().orientation;
            boolean horizontal = orientation == Configuration.ORIENTATION_LANDSCAPE;
            LinearLayout.LayoutParams lpLeft = (LinearLayout.LayoutParams) leftContainer.getLayoutParams();
            LinearLayout.LayoutParams lpRight = (LinearLayout.LayoutParams) rightContainer.getLayoutParams();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    long now = System.currentTimeMillis();
                    if (now - lastTapTime < DOUBLE_TAP_MS) {
                        exitResizeMode();
                        return true;
                    }
                    lastTapTime = now;

                    startPos = horizontal ? event.getX() : event.getY();
                    startLeftWeight = lpLeft.weight;
                    startRightWeight = lpRight.weight;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float currentPos = horizontal ? event.getX() : event.getY();
                    float delta = currentPos - startPos;
                    float total = horizontal ? splitContainer.getWidth() : splitContainer.getHeight();
                    if (total <= 0) return true;

                    float leftFrac = startLeftWeight / (startLeftWeight + startRightWeight)
                            + delta / total;
                    leftFrac = Math.max(MIN_PANE_FRACTION, Math.min(1 - MIN_PANE_FRACTION, leftFrac));
                    lpLeft.weight = leftFrac;
                    lpRight.weight = 1 - leftFrac;
                    leftContainer.setLayoutParams(lpLeft);
                    rightContainer.setLayoutParams(lpRight);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return false;
        }
    };

    private void adjustSplitLayoutWithSavedWeights(int orientation) {
        if (!isSplit) return;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            splitContainer.setOrientation(LinearLayout.HORIZONTAL);
            leftContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, savedLeftWeight));
            rightContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, savedRightWeight));
        } else {
            splitContainer.setOrientation(LinearLayout.VERTICAL);
            leftContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, savedLeftWeight));
            rightContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, savedRightWeight));
        }
    }

    private void swapWebViews() {
        if (!isSplit || secondaryWebView == null) return;

        WebView temp = mainWebView;
        mainWebView = secondaryWebView;
        secondaryWebView = temp;

        leftContainer.removeAllViews();
        rightContainer.removeAllViews();

        attachWebView(leftContainer, mainWebView);
        attachWebView(rightContainer, secondaryWebView);

        // activeWebView and lastClickedWebView deliberately keep pointing at
        // the same page object, so focus follows the page to its new side
        // rather than jumping to the other pane.
    }

    private void attachWebView(ViewGroup container, WebView wv) {
        if (wv.getParent() != null) ((ViewGroup) wv.getParent()).removeView(wv);
        container.addView(wv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ------------------- WEBVIEW HOST CALLBACKS -------------------

    @Override
    public void onProgress(WebView view, int progress) {
        if (view != activeWebView) return;              // ignore the background pane
        if (progress >= 100) {
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress);
        }
    }

    @Override
    public void onUrlChanged(WebView view, String url) {
        updateOmnibox(view, url);
    }

    @Override
    public boolean onFileChooser(ValueCallback<Uri[]> callback,
                                 WebChromeClient.FileChooserParams params) {
        // Cancel a previous, still-pending chooser before starting a new one.
        if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
        pendingFileCallback = callback;
        try {
            fileChooserLauncher.launch(params.createIntent());
            return true;
        } catch (Exception e) {
            pendingFileCallback = null;
            return false;
        }
    }

    /** Shows the page URL (or a DuckDuckGo query) unless the user is typing. */
    private void updateOmnibox(WebView view, String url) {
        if (view != activeWebView || searchInput.hasFocus()) return;
        searchInput.setText(UrlUtil.displayTextForUrl(url));
    }

    /** Refreshes the address bar to reflect the current active view. */
    private void refreshOmnibox() {
        if (activeWebView != null) updateOmnibox(activeWebView, activeWebView.getUrl());
    }

    // ------------------- NAVIGATION -------------------

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBackOrUnwind();
            }
        });
    }

    /** Shared by the system Back button and the search-bar swipe gesture. */
    private void goBackOrUnwind() {
        // Unwind UI state before touching history or leaving the app.
        if (isResizing) {
            exitResizeMode();
        } else if (searchContainer.getVisibility() == View.VISIBLE
                && searchInput.hasFocus()) {
            setSearchBarVisible(false, true);
        } else if (activeWebView != null && activeWebView.canGoBack()) {
            activeWebView.goBack();
        } else if (isSplit) {
            closeSplit();
        } else {
            finish();
        }
    }

    // ------------------- LIFECYCLE -------------------

    @Override
    protected void onResume() {
        super.onResume();
        if (mainWebView != null) mainWebView.onResume();
        if (isSplit && secondaryWebView != null) secondaryWebView.onResume();
    }

    @Override
    protected void onPause() {
        // Without these the WebViews keep running timers and playing media
        // after the app is backgrounded.
        if (mainWebView != null) mainWebView.onPause();
        if (secondaryWebView != null) secondaryWebView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (rootView != null && keyboardListener != null) {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
            keyboardListener = null;
        }
        destroyWebView(mainWebView);
        destroyWebView(secondaryWebView);
        mainWebView = null;
        secondaryWebView = null;
        activeWebView = null;
        lastClickedWebView = null;
        super.onDestroy();
    }

    private void destroyWebView(WebView wv) {
        if (wv == null) return;
        if (wv.getParent() != null) ((ViewGroup) wv.getParent()).removeView(wv);
        wv.stopLoading();
        wv.setWebChromeClient(null);
        wv.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustSplitLayoutWithSavedWeights(newConfig.orientation);
        SystemUi.hideBars(this);
    }

    /**
     * Immersive mode has to be reasserted here. Previously it was only applied
     * in onCreate and on rotation, so the system bars came back for good after
     * the keyboard, a dialog, or an app switch.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) SystemUi.hideBars(this);
    }
}
