package com.azlan.lumalight.widget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.azlan.lumalight.R;

/**
 * Launched directly from a tap on the Strobe home screen widget. Immediately
 * strobes the screen between black and the color chosen for that widget
 * instance, at the same default 10Hz speed as the main app - tap anywhere,
 * press back, or press a volume key to stop.
 *
 * As with QuickFlashlightActivity, this does not sync the real camera torch to
 * avoid a surprise permission prompt on tap; use the main app for that.
 */
public class QuickStrobeActivity extends AppCompatActivity {

    private static final int STROBE_HZ = 10;

    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean showingColor = true;
    private boolean running = false;
    private View coloredView;
    private int strobeColor = Color.WHITE;

    private final Runnable strobeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            coloredView.setBackgroundColor(showingColor ? strobeColor : Color.BLACK);
            showingColor = !showingColor;
            int delay = 1000 / (STROBE_HZ * 2);
            handler.postDelayed(this, delay);
        }
    };

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_light);

        int appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        strobeColor = WidgetColorPrefs.getColor(this, appWidgetId);

        coloredView = findViewById(R.id.quickLightColor);
        coloredView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        TextView hint = findViewById(R.id.quickLightHint);
        hint.setText("Tap anywhere to stop strobe");
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
                    "LumaLight:QuickStrobe");
            wakeLock.acquire(10 * 60 * 1000L /* 10 min safety cap */);
        }

        findViewById(android.R.id.content).setOnClickListener(v -> finish());

        running = true;
        showingColor = true;
        handler.post(strobeRunnable);
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
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
        handler.removeCallbacks(strobeRunnable);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
