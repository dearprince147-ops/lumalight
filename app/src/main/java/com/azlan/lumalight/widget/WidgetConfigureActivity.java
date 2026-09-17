package com.azlan.lumalight.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.azlan.lumalight.R;

public class WidgetConfigureActivity extends AppCompatActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private View swatch1, swatch2;
    private SeekBar hue1, brightness1, hue2, brightness2;
    private TextView hueText1, brightnessText1, hueText2, brightnessText2;
    private LinearLayout presets1, presets2, color2Section;
    private int color1 = Color.WHITE, colorTwo = Color.BLACK;
    private float currentHue1, currentHue2;
    private int currentBrightness1 = 100, currentBrightness2;
    private boolean strobeWidget;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        setContentView(R.layout.activity_widget_configure);

        swatch1 = findViewById(R.id.configureSwatch);
        swatch2 = findViewById(R.id.configureSwatch2);
        hue1 = findViewById(R.id.configureHueSlider);
        brightness1 = findViewById(R.id.configureBrightnessSlider);
        hue2 = findViewById(R.id.configureHueSlider2);
        brightness2 = findViewById(R.id.configureBrightnessSlider2);
        hueText1 = findViewById(R.id.tvConfigureHue);
        brightnessText1 = findViewById(R.id.tvConfigureBrightness);
        hueText2 = findViewById(R.id.tvConfigureHue2);
        brightnessText2 = findViewById(R.id.tvConfigureBrightness2);
        presets1 = findViewById(R.id.configurePresetContainer);
        presets2 = findViewById(R.id.configurePresetContainer2);
        color2Section = findViewById(R.id.configureColor2Section);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName provider = manager.getAppWidgetInfo(appWidgetId).provider;
        strobeWidget = provider != null
                && StrobeWidgetProvider.class.getName().equals(provider.getClassName());
        color2Section.setVisibility(strobeWidget ? View.VISIBLE : View.GONE);

        setupTracks();
        setupPresets();
        setupListeners();
        applyColor(1, color1);
        applyColor(2, colorTwo);
        findViewById(R.id.btnAddWidget).setOnClickListener(v -> confirmAndFinish());
    }

    private Drawable buildTrack(ShapeDrawable.ShaderFactory factory, float radius, float density) {
        ShapeDrawable shape = new ShapeDrawable(new RoundRectShape(
                new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
        shape.setShaderFactory(factory);
        shape.setIntrinsicHeight((int) (12 * density));
        LayerDrawable result = new LayerDrawable(new Drawable[]{shape, new ColorDrawable(Color.TRANSPARENT)});
        result.setId(0, android.R.id.background);
        result.setId(1, android.R.id.progress);
        return result;
    }

    private void setupTracks() {
        final float density = getResources().getDisplayMetrics().density;
        final float radius = 8 * density;
        ShapeDrawable.ShaderFactory hueFactory = new ShapeDrawable.ShaderFactory() {
            @Override public Shader resize(int width, int height) {
                return new LinearGradient(0, height / 2f, width, height / 2f,
                        new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
                                Color.BLUE, Color.MAGENTA, Color.RED}, null, Shader.TileMode.CLAMP);
            }
        };
        ShapeDrawable.ShaderFactory brightnessFactory = new ShapeDrawable.ShaderFactory() {
            @Override public Shader resize(int width, int height) {
                return new LinearGradient(0, height / 2f, width, height / 2f,
                        Color.BLACK, Color.WHITE, Shader.TileMode.CLAMP);
            }
        };
        hue1.setProgressDrawable(buildTrack(hueFactory, radius, density));
        hue2.setProgressDrawable(buildTrack(hueFactory, radius, density));
        brightness1.setProgressDrawable(buildTrack(brightnessFactory, radius, density));
        brightness2.setProgressDrawable(buildTrack(brightnessFactory, radius, density));
    }

    private void setupPresets() {
        int[] colors = {Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN, Color.MAGENTA, Color.parseColor("#FFA500")};
        addPresets(presets1, colors, 1);
        addPresets(presets2, colors, 2);
    }

    private void addPresets(LinearLayout container, int[] colors, final int slot) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (48 * density), margin = (int) (12 * density);
        for (final int color : colors) {
            View preset = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, 0, margin, 0);
            preset.setLayoutParams(params);
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(color);
            preset.setBackground(background);
            preset.setOnClickListener(v -> applyColor(slot, color));
            container.addView(preset);
        }
    }

    private void setupListeners() {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (bar == hue1) applyHueBrightness(1, progress, currentBrightness1);
                else if (bar == brightness1) applyHueBrightness(1, currentHue1, progress);
                else if (bar == hue2) applyHueBrightness(2, progress, currentBrightness2);
                else applyHueBrightness(2, currentHue2, progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        };
        hue1.setOnSeekBarChangeListener(listener);
        brightness1.setOnSeekBarChangeListener(listener);
        hue2.setOnSeekBarChangeListener(listener);
        brightness2.setOnSeekBarChangeListener(listener);
    }

    private void applyHueBrightness(int slot, float hue, int brightness) {
        applyColor(slot, Color.HSVToColor(new float[]{hue, 1f, brightness / 100f}));
    }

    private void applyColor(int slot, int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (slot == 1) {
            color1 = color; currentHue1 = hsv[0]; currentBrightness1 = Math.round(hsv[2] * 100);
            swatch1.setBackgroundColor(color); hue1.setProgress((int) currentHue1);
            brightness1.setProgress(currentBrightness1); hueText1.setText((int) currentHue1 + "°");
            brightnessText1.setText(currentBrightness1 + "%");
        } else {
            colorTwo = color; currentHue2 = hsv[0]; currentBrightness2 = Math.round(hsv[2] * 100);
            swatch2.setBackgroundColor(color); hue2.setProgress((int) currentHue2);
            brightness2.setProgress(currentBrightness2); hueText2.setText((int) currentHue2 + "°");
            brightnessText2.setText(currentBrightness2 + "%");
        }
    }

    private void confirmAndFinish() {
        WidgetColorPrefs.setColor1(this, appWidgetId, color1);
        if (strobeWidget) WidgetColorPrefs.setColor2(this, appWidgetId, colorTwo);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (strobeWidget) StrobeWidgetProvider.updateWidget(this, manager, appWidgetId);
        else FlashlightWidgetProvider.updateWidget(this, manager, appWidgetId);
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }
}
