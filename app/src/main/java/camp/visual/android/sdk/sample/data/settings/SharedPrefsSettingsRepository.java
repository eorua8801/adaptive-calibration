// SharedPrefsSettingsRepository.java - 안전한 기본값 설정 (패키지명 수정)
package camp.visual.android.sdk.sample.data.settings;

import android.content.Context;
import android.content.SharedPreferences;
import camp.visual.android.sdk.sample.domain.model.OneEuroFilterPreset;
import camp.visual.android.sdk.sample.domain.model.UserSettings;

public class SharedPrefsSettingsRepository implements SettingsRepository {

    private static final String PREF_NAME = "EyedidSampleAppSettings";

    // 기존 키들...
    private static final String KEY_FIXATION_DURATION = "fixation_duration";
    private static final String KEY_AOI_RADIUS = "aoi_radius";
    private static final String KEY_SCROLL_ENABLED = "scroll_enabled";
    private static final String KEY_EDGE_MARGIN_RATIO = "edge_margin_ratio";
    private static final String KEY_EDGE_TRIGGER_MS = "edge_trigger_ms";
    private static final String KEY_CONTINUOUS_SCROLL_COUNT = "continuous_scroll_count";
    private static final String KEY_CLICK_ENABLED = "click_enabled";
    private static final String KEY_EDGE_SCROLL_ENABLED = "edge_scroll_enabled";
    private static final String KEY_BLINK_DETECTION_ENABLED = "blink_detection_enabled";
    private static final String KEY_AUTO_ONE_POINT_CALIBRATION = "auto_one_point_calibration";
    private static final String KEY_CURSOR_OFFSET_X = "cursor_offset_x";
    private static final String KEY_CURSOR_OFFSET_Y = "cursor_offset_y";
    private static final String KEY_ONE_EURO_PRESET = "one_euro_preset";
    private static final String KEY_ONE_EURO_FREQ = "one_euro_freq";
    private static final String KEY_ONE_EURO_MIN_CUTOFF = "one_euro_min_cutoff";
    private static final String KEY_ONE_EURO_BETA = "one_euro_beta";
    private static final String KEY_ONE_EURO_D_CUTOFF = "one_euro_d_cutoff";

    // 새로운 키들 추가
    private static final String KEY_CALIBRATION_STRATEGY = "calibration_strategy";
    private static final String KEY_BACKGROUND_LEARNING = "background_learning";
    private static final String KEY_CLICK_TIMING = "click_timing";

    private final SharedPreferences prefs;

    public SharedPrefsSettingsRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public UserSettings getUserSettings() {
        // 🎯 캘리브레이션 전략 로드 (기본값: PRECISION으로 변경)
        String strategyName = prefs.getString(KEY_CALIBRATION_STRATEGY,
                UserSettings.CalibrationStrategy.PRECISION.name()); // QUICK_START에서 변경
        UserSettings.CalibrationStrategy strategy;
        try {
            strategy = UserSettings.CalibrationStrategy.valueOf(strategyName);
        } catch (IllegalArgumentException e) {
            strategy = UserSettings.CalibrationStrategy.PRECISION; // 안전한 기본값으로 변경
        }

        // OneEuroFilter 프리셋 로드 - BALANCED_STABILITY를 기본값으로 사용
        String presetName = prefs.getString(KEY_ONE_EURO_PRESET, OneEuroFilterPreset.BALANCED_STABILITY.name());
        OneEuroFilterPreset preset = OneEuroFilterPreset.fromName(presetName);

        // 클릭 타이밍 로드
        String clickTimingName = prefs.getString(KEY_CLICK_TIMING, UserSettings.ClickTiming.NORMAL.name());
        UserSettings.ClickTiming clickTiming;
        try {
            clickTiming = UserSettings.ClickTiming.valueOf(clickTimingName);
        } catch (IllegalArgumentException e) {
            clickTiming = UserSettings.ClickTiming.NORMAL;
        }

        return new UserSettings.Builder()
                .fixationDurationMs(prefs.getFloat(KEY_FIXATION_DURATION, 1000f))
                .aoiRadius(prefs.getFloat(KEY_AOI_RADIUS, 40f))
                .scrollEnabled(prefs.getBoolean(KEY_SCROLL_ENABLED, true))
                .edgeMarginRatio(prefs.getFloat(KEY_EDGE_MARGIN_RATIO, 0.01f))
                .edgeTriggerMs(prefs.getLong(KEY_EDGE_TRIGGER_MS, 3000))
                .continuousScrollCount(prefs.getInt(KEY_CONTINUOUS_SCROLL_COUNT, 2))
                .clickEnabled(prefs.getBoolean(KEY_CLICK_ENABLED, true))
                .edgeScrollEnabled(prefs.getBoolean(KEY_EDGE_SCROLL_ENABLED, true))
                .blinkDetectionEnabled(prefs.getBoolean(KEY_BLINK_DETECTION_ENABLED, false))
                .autoOnePointCalibrationEnabled(prefs.getBoolean(KEY_AUTO_ONE_POINT_CALIBRATION, true))
                .cursorOffsetX(prefs.getFloat(KEY_CURSOR_OFFSET_X, 0f))
                .cursorOffsetY(prefs.getFloat(KEY_CURSOR_OFFSET_Y, 0f))
                .oneEuroFilterPreset(preset)
                .oneEuroFreq(prefs.getFloat(KEY_ONE_EURO_FREQ, 30.0f))
                .oneEuroMinCutoff(prefs.getFloat(KEY_ONE_EURO_MIN_CUTOFF, 1.0f))
                .oneEuroBeta(prefs.getFloat(KEY_ONE_EURO_BETA, 0.007f))
                .oneEuroDCutoff(prefs.getFloat(KEY_ONE_EURO_D_CUTOFF, 1.0f))
                // 🎯 새 설정들 추가 (안전한 기본값)
                .calibrationStrategy(strategy)
                .backgroundLearningEnabled(prefs.getBoolean(KEY_BACKGROUND_LEARNING, false)) // 기본값 false로 변경
                .clickTiming(clickTiming)
                .build();
    }

    @Override
    public void saveUserSettings(UserSettings settings) {
        SharedPreferences.Editor editor = prefs.edit();

        // 기존 설정들 저장...
        editor.putFloat(KEY_FIXATION_DURATION, settings.getFixationDurationMs());
        editor.putFloat(KEY_AOI_RADIUS, settings.getAoiRadius());
        editor.putBoolean(KEY_SCROLL_ENABLED, settings.isScrollEnabled());
        editor.putFloat(KEY_EDGE_MARGIN_RATIO, settings.getEdgeMarginRatio());
        editor.putLong(KEY_EDGE_TRIGGER_MS, settings.getEdgeTriggerMs());
        editor.putInt(KEY_CONTINUOUS_SCROLL_COUNT, settings.getContinuousScrollCount());
        editor.putBoolean(KEY_CLICK_ENABLED, settings.isClickEnabled());
        editor.putBoolean(KEY_EDGE_SCROLL_ENABLED, settings.isEdgeScrollEnabled());
        editor.putBoolean(KEY_BLINK_DETECTION_ENABLED, settings.isBlinkDetectionEnabled());
        editor.putBoolean(KEY_AUTO_ONE_POINT_CALIBRATION, settings.isAutoOnePointCalibrationEnabled());
        editor.putFloat(KEY_CURSOR_OFFSET_X, settings.getCursorOffsetX());
        editor.putFloat(KEY_CURSOR_OFFSET_Y, settings.getCursorOffsetY());

        // OneEuroFilter 설정 저장
        editor.putString(KEY_ONE_EURO_PRESET, settings.getOneEuroFilterPreset().name());
        editor.putFloat(KEY_ONE_EURO_FREQ, (float) settings.getOneEuroFreq());
        editor.putFloat(KEY_ONE_EURO_MIN_CUTOFF, (float) settings.getOneEuroMinCutoff());
        editor.putFloat(KEY_ONE_EURO_BETA, (float) settings.getOneEuroBeta());
        editor.putFloat(KEY_ONE_EURO_D_CUTOFF, (float) settings.getOneEuroDCutoff());

        // 새 설정들 저장
        editor.putString(KEY_CALIBRATION_STRATEGY, settings.getCalibrationStrategy().name());
        editor.putBoolean(KEY_BACKGROUND_LEARNING, settings.isBackgroundLearningEnabled());
        editor.putString(KEY_CLICK_TIMING, settings.getClickTiming().name());

        editor.apply();
    }

    @Override
    public void setDefaultSettings() {
        // 🎯 기본 설정도 정밀 보정 우선으로 변경
        saveUserSettings(new UserSettings.Builder()
                .calibrationStrategy(UserSettings.CalibrationStrategy.PRECISION)
                .backgroundLearningEnabled(false)
                .clickTiming(UserSettings.ClickTiming.NORMAL)
                .oneEuroFilterPreset(OneEuroFilterPreset.BALANCED_STABILITY)
                .build());
    }

    // 기존 메서드들 유지...
    public void saveIntegratedCursorOffset(float offsetX, float offsetY) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_CURSOR_OFFSET_X, offsetX);
        editor.putFloat(KEY_CURSOR_OFFSET_Y, offsetY);
        editor.apply();
    }
}