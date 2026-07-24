package io.github.joaoserrao.minimalbrowser;

import android.content.Context;
import android.webkit.WebView;

/**
 * A WebView that can pretend the page starts lower than it really does.
 *
 * On DuckDuckGo results the app's own search bar sits directly above the page,
 * so DuckDuckGo's own search box is redundant. A "floor" is set just below it:
 * the page cannot scroll above that offset, so the results start with the
 * type tabs (All / Images / Videos ...) and their settings gear, and the
 * duplicate search box is never shown. The page is only clamped, never
 * modified, so nothing about the results changes.
 */
public class FloorWebView extends WebView {

    private int scrollFloor = 0;

    public FloorWebView(Context context) {
        super(context);
    }

    /** Sets the floor and snaps to it if the page is currently above it. */
    void setScrollFloor(int floorPx) {
        scrollFloor = Math.max(0, floorPx);
        if (scrollFloor > 0 && getScrollY() < scrollFloor) {
            scrollTo(getScrollX(), scrollFloor);
        }
    }

    void clearScrollFloor() {
        scrollFloor = 0;
    }

    /** True if the content is tall enough to scroll past the given offset. */
    boolean hasRoomToScroll(int px) {
        return computeVerticalScrollRange() > getHeight() + px;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // Hard clamp: any scroll or fling above the floor stops at it, so
        // DuckDuckGo's search box can never be scrolled into view. Re-clamping
        // sets t == scrollFloor, so this does not recurse.
        if (scrollFloor > 0 && t < scrollFloor) {
            scrollTo(l, scrollFloor);
        }
    }
}
