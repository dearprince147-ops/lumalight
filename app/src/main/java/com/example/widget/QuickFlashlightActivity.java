package com.example.widget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;

/**
 * Launched directly from a tap on the Flashlight home screen widget. Fills the
 * screen with the color chosen for that widget instance at maximum brightness,
 * exactly like the main app's "active" mode - tap anywhere, press back, or
 * press a volume key to stop and return to the home screen.
 *
 * Note: this intentionally does not offer to sync the real camera LED torch.
 * The torch is white-only hardware (colored light is only possible via the
 * screen), and syncing it here would require a runtime CAMERA permission
 * prompt to pop up the instant the widget is tapped, which defeats the point
 * of an instant widget. "Sync Camera LED" remains available in the main app.
 */
public class QuickFlashlightActivity extends AppCompatActivity {

    private PowerManager.WakeLock wakeLock;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_light);

        int appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        int color = WidgetColorPrefs.getColor(this, appWidgetId);

        findViewById(R.id.quickLightColor).setBackgroundColor(color);

        TextView hint = findViewById(R.id.quickLightHint);
        hint.postDelayed(() -> hint.animate().alpha(0f).setDuration(500)
                .withEndAction(() -> hint.setVisibility(View.GONE)).start(), 2000);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(layoutParams);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "LumaLight:QuickFlashlight");
            wakeLock.acquire(10 * 60 * 1000L /* 10 min safety cap */);
        }

        findViewById(android.R.id.content).setOnClickListener(v -> finish());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Leaving the activity (home button, app switch, or finish()) always
        // stops the light - never leave the wake lock or a bright screen behind.
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
