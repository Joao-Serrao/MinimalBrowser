package io.github.joaoserrao.minimalbrowser;

import android.content.Context;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * A WebView that can pretend the page starts lower than it really does.
 *
 * On DuckDuckGo results the app's own search bar sits directly above the page,
 * so their search header is redundant. Rather than hiding it (which would take
 * their settings with it), a "floor" is set: normal scrolling treats that
 * offset as the top of the page. Pulling down again while already resting on
 * the floor unlocks it for that gesture, revealing the real top. Scrolling back
 * down past the floor re-locks it.
 */
public class FloorWebView extends WebView {

    /** Finger travel needed to count as a deliberate second pull. */
    private static final int UNLOCK_TRAVEL_DP = 48;

    private int scrollFloor = 0;
    private boolean unlocked = false;

    private float downY;
    private boolean gestureStartedOnFloor;
    private final int unlockTravelPx;

    public FloorWebView(Context context) {
        super(context);
        unlockTravelPx = Math.round(
                UNLOCK_TRAVEL_DP * context.getResources().getDisplayMetrics().density);
    }

    /** Sets the floor and snaps to it if the page is currently above it. */
    void setScrollFloor(int floorPx) {
        scrollFloor = Math.max(0, floorPx);
        unlocked = false;
        if (scrollFloor > 0 && getScrollY() < scrollFloor) {
            scrollTo(getScrollX(), scrollFloor);
        }
    }

    void clearScrollFloor() {
        scrollFloor = 0;
        unlocked = false;
    }

    boolean hasScrollFloor() {
        return scrollFloor > 0;
    }

    /** True if the content is tall enough to scroll past the given offset. */
    boolean hasRoomToScroll(int px) {
        return computeVerticalScrollRange() > getHeight() + px;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (scrollFloor <= 0) return;

        if (!unlocked && t < scrollFloor) {
            // Clamp: an upward scroll or fling stops here instead of the top.
            scrollTo(l, scrollFloor);
        } else if (unlocked && t > scrollFloor) {
            // Scrolled back down past the floor, so arm it again.
            unlocked = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scrollFloor > 0) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY = event.getY();
                    // Only a gesture that begins already resting on the floor
                    // can unlock it, so a long scroll up from further down
                    // stops at the floor rather than continuing to the top.
                    gestureStartedOnFloor = getScrollY() <= scrollFloor;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!unlocked && gestureStartedOnFloor
                            && event.getY() - downY > unlockTravelPx) {
                        unlocked = true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    gestureStartedOnFloor = false;
                    break;
            }
        }
        return super.onTouchEvent(event);
    }
}
