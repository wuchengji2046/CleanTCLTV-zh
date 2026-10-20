package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.app.ActivityOptions;
import android.view.KeyEvent;
import android.util.Log;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/** Opt-in bridge for TCL firmware that refuses third-party HOME intent filters.
 * Only observes window changes from the stock home package; no screen content is read.
 */
public class TclHomeRedirectService extends AccessibilityService {
    private long lastRedirect;
    private long burstStart;
    private int burstCount;

    @Override protected void onServiceConnected() { redirect(); }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME && event.getKeyCode() != KeyEvent.KEYCODE_ALL_APPS) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            Log.i("TclHomeRedirect", "Home key intercepted");
            redirect();
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!"com.tcl.cyberui".contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) return;
        if (!"com.tcl.cyberui.MainActivity".contentEquals(event.getClassName() == null ? "" : event.getClassName())) return;
        redirect();
    }

    private void redirect() {
        long now = SystemClock.elapsedRealtime();
        if (lastRedirect != 0 && now - lastRedirect < 180) return;
        // Avoid repeated launches if the app cannot stay open on this firmware.
        if (now - burstStart > 15000) { burstStart = now; burstCount = 0; }
        if (burstCount >= 12) return;
        lastRedirect = now;
        burstCount++;
        try {
            startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION),
                ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle());
            Log.i("TclHomeRedirect", "Redirect dispatched at " + now);
        } catch (RuntimeException ignored) { }
    }

    @Override public void onInterrupt() { }
}
