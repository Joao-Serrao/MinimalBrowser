package com.example.minimalbrowser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity {

    private WebView mainWebView;
    private WebView secondaryWebView;
    private WebView activeWebView;

    private EditText searchInput;
    private FrameLayout leftContainer;
    private FrameLayout rightContainer;
    private LinearLayout splitContainer;

    private boolean isSplit = false;
    private boolean isResizing = false;
    private View resizeOverlay;

    private long lastClickTime = 0;
    private WebView lastClickedWebView;

    private float savedLeftWeight = 0.5f;
    private float savedRightWeight = 0.5f;

    private ImageButton toggleSearchButton;
    private LinearLayout searchContainer;



    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_search);

        enableImmersiveMode();
        applyKeyboardResizeWorkaround();

        searchInput = findViewById(R.id.search_input);
        ImageButton searchButton = findViewById(R.id.search_button);
        ImageButton homeButton = findViewById(R.id.home_button);
        ImageButton toggleSplitButton = findViewById(R.id.toggle_split_button);
        ImageButton toggleSearchButton = findViewById(R.id.toggle_search_button); // arrow button

        splitContainer = findViewById(R.id.web_split_container);
        leftContainer = findViewById(R.id.web_container_left);
        rightContainer = findViewById(R.id.web_container_right);

        // Main WebView
        mainWebView = WebViewHolder.getWebView(this);
        attachWebView(leftContainer, mainWebView);
        activeWebView = mainWebView;

        setupWebViewClickListener(mainWebView);
        loadHomePage();

        // Widget launch: focus input
        boolean fromWidget = getIntent().getBooleanExtra("fromWidget", false);
        if (fromWidget) {
            searchInput.post(() -> {
                searchInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }

        // Search
        Runnable performSearch = () -> {
            if (activeWebView != null) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    String searchUrl = "https://duckduckgo.com/?q=" + query.replace(" ", "+") + "&kp=-2&ka=-1";
                    activeWebView.loadUrl(searchUrl);
                }
            }
            hideKeyboard();
        };
        searchButton.setOnClickListener(v -> performSearch.run());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            performSearch.run();
            return true;
        });

        // Home button
        homeButton.setOnClickListener(v -> {
            hideKeyboard();
            if (activeWebView != null)
                activeWebView.loadUrl("https://duckduckgo.com/?kp=-2&ka=-1");
        });

        searchContainer = findViewById(R.id.searchContainer);

        // Toggle search bar with smooth animation
        toggleSearchButton.setOnClickListener(v -> {
            boolean isVisible = searchContainer.getVisibility() == View.VISIBLE;

            if (isVisible) {
                // Hide search container
                int startHeight = searchContainer.getHeight();
                ValueAnimator animator = ValueAnimator.ofInt(startHeight, 0);
                animator.setDuration(250);
                animator.addUpdateListener(animation -> {
                    int val = (int) animation.getAnimatedValue();
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) searchContainer.getLayoutParams();
                    lp.height = val;
                    searchContainer.setLayoutParams(lp);
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        searchContainer.setVisibility(View.GONE);
                        toggleSearchButton.setImageResource(R.drawable.ic_arrow_down);
                    }
                });
                animator.start();
            } else {
                // Show search container
                searchContainer.setVisibility(View.VISIBLE);
                toggleSearchButton.setImageResource(R.drawable.ic_arrow_up);

                int targetHeight = dpToPx(48); // desired height
                ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
                animator.setDuration(250);
                animator.addUpdateListener(animation -> {
                    int val = (int) animation.getAnimatedValue();
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) searchContainer.getLayoutParams();
                    lp.height = val;
                    searchContainer.setLayoutParams(lp);
                });
                animator.start();

                // Show keyboard
                searchInput.post(() -> {
                    searchInput.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                });
            }
        });




        // Toggle split button
        toggleSplitButton.setOnClickListener(v -> {
            hideKeyboard();
            toggleSplit();
        });

        toggleSplitButton.setOnLongClickListener(v -> {
            hideKeyboard();
            if (!isSplit) {
                secondaryWebView = new WebView(this);
                WebViewHolder.applySettings(secondaryWebView);
                attachWebView(rightContainer, secondaryWebView);
                rightContainer.setVisibility(View.VISIBLE);
                secondaryWebView.loadUrl("https://chat.openai.com/");
                isSplit = true;
                setupWebViewClickListener(secondaryWebView);
                savedLeftWeight = 0.5f;
                savedRightWeight = 0.5f;
                adjustSplitLayoutWithSavedWeights(getResources().getConfiguration().orientation);
                activeWebView = secondaryWebView;
                lastClickedWebView = secondaryWebView;
            } else {
                swapWebViews();
            }
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activeWebView != null && activeWebView.canGoBack()) {
                    activeWebView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    // Helper dp → px
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }




    // ------------------- KEYBOARD HANDLING -------------------
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view instanceof EditText) {
            int[] scrcoords = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];

            if (ev.getAction() == MotionEvent.ACTION_DOWN
                    && (x < view.getLeft() || x > view.getRight()
                    || y < view.getTop() || y > view.getBottom())) {

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
    // -----------------------------------------------------------

    // ------------------- SPLIT WEBVIEW HANDLING -------------------
    private void toggleSplit() {
        int orientation = getResources().getConfiguration().orientation;

        if (!isSplit) {
            if (secondaryWebView == null) {
                secondaryWebView = new WebView(this);
                WebViewHolder.applySettings(secondaryWebView);
                secondaryWebView.loadUrl("https://chat.openai.com/");
                setupWebViewClickListener(secondaryWebView);
                savedLeftWeight = 0.5f;
                savedRightWeight = 0.5f;
            }

            attachWebView(rightContainer, secondaryWebView);
            rightContainer.setVisibility(View.VISIBLE);
            isSplit = true;

            adjustSplitLayoutWithSavedWeights(orientation);

            if (lastClickedWebView != null &&
                    (lastClickedWebView == mainWebView || lastClickedWebView == secondaryWebView)) {
                activeWebView = lastClickedWebView;
            } else {
                activeWebView = secondaryWebView;
            }
        } else {
            LinearLayout.LayoutParams lpLeft = (LinearLayout.LayoutParams) leftContainer.getLayoutParams();
            LinearLayout.LayoutParams lpRight = (LinearLayout.LayoutParams) rightContainer.getLayoutParams();
            savedLeftWeight = lpLeft.weight;
            savedRightWeight = lpRight.weight;

            exitResizeMode();
            rightContainer.removeAllViews();
            rightContainer.setVisibility(View.GONE);

            isSplit = false;
            leftContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            splitContainer.setOrientation(LinearLayout.HORIZONTAL);
            activeWebView = mainWebView;
        }
    }

    private void setupWebViewClickListener(WebView webView) {
        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                activeWebView = webView;
                long now = System.currentTimeMillis();
                if (lastClickedWebView == webView && now - lastClickTime < 300) {
                    if (isResizing) exitResizeMode();
                    else enterResizeMode();
                    lastClickTime = 0;
                } else {
                    lastClickTime = now;
                    lastClickedWebView = webView;
                }
            }
            return false;
        });
    }

    private void enterResizeMode() {
        if (!isSplit || resizeOverlay != null) return;

        resizeOverlay = new View(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        ((FrameLayout) findViewById(R.id.root_layout)).addView(resizeOverlay, overlayParams);
        resizeOverlay.setBackgroundColor(0x00000000);
        resizeOverlay.setOnTouchListener(resizeTouchListener);
        isResizing = true;
    }

    private void exitResizeMode() {
        if (resizeOverlay != null) {
            ((ViewGroup) findViewById(R.id.root_layout)).removeView(resizeOverlay);
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
            LinearLayout.LayoutParams lpLeft = (LinearLayout.LayoutParams) leftContainer.getLayoutParams();
            LinearLayout.LayoutParams lpRight = (LinearLayout.LayoutParams) rightContainer.getLayoutParams();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    long now = System.currentTimeMillis();
                    if (now - lastTapTime < 300) {
                        exitResizeMode();
                        return true;
                    }
                    lastTapTime = now;

                    startPos = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? event.getX() : event.getY();
                    startLeftWeight = lpLeft.weight;
                    startRightWeight = lpRight.weight;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float currentPos = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? event.getX() : event.getY();
                    float delta = currentPos - startPos;
                    float total = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? splitContainer.getWidth() : splitContainer.getHeight();
                    float leftFrac = startLeftWeight / (startLeftWeight + startRightWeight) + delta / total;
                    leftFrac = Math.max(0.1f, Math.min(0.9f, leftFrac));
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
            leftContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, savedLeftWeight));
            rightContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, savedRightWeight));
        } else {
            splitContainer.setOrientation(LinearLayout.VERTICAL);
            leftContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, savedLeftWeight));
            rightContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, savedRightWeight));
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

        if (activeWebView == mainWebView) activeWebView = secondaryWebView;
        else if (activeWebView == secondaryWebView) activeWebView = mainWebView;

        if (lastClickedWebView == mainWebView) lastClickedWebView = secondaryWebView;
        else if (lastClickedWebView == secondaryWebView) lastClickedWebView = mainWebView;
    }

    private void attachWebView(ViewGroup container, WebView wv) {
        if (wv.getParent() != null) ((ViewGroup) wv.getParent()).removeView(wv);
        container.addView(wv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void loadHomePage() {
        if (activeWebView != null)
            activeWebView.loadUrl("https://duckduckgo.com/?kp=-2&ka=-1");
    }

    private void enableImmersiveMode() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void applyKeyboardResizeWorkaround() {
        final View rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > screenHeight * 0.15) {
                rootView.setPadding(0, 0, 0, keypadHeight);
            } else {
                rootView.setPadding(0, 0, 0, 0);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustSplitLayoutWithSavedWeights(newConfig.orientation);
        enableImmersiveMode();
    }
}
