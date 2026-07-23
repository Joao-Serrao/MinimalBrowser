package io.github.joaoserrao.minimalbrowser;

import android.app.Activity;
import android.view.Window;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Single definition of the app's immersive mode.
 *
 * The activity and the fullscreen-video chrome client each used to set their
 * own {@code setSystemUiVisibility} flag soup, and the two sets disagreed, so
 * leaving a fullscreen video changed which bars were hidden.
 */
final class SystemUi {

    private SystemUi() {
    }

    /**
     * Hides the status and navigation bars, letting a swipe reveal them
     * temporarily.
     *
     * The previous flags omitted {@code SYSTEM_UI_FLAG_HIDE_NAVIGATION}, and
     * {@code IMMERSIVE_STICKY} only modifies that flag — so the navigation bar
     * was never actually hidden.
     */
    static void hideBars(Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }
}
