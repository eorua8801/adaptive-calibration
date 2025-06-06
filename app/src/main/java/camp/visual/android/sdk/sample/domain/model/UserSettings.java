package camp.visual.android.sdk.sample.domain.model;

public class UserSettings {

    // 캘리브레이션 전략 enum (기존 유지)
    public enum CalibrationStrategy {
        QUICK_START("빠른 시작", "2초 간단 보정 + 자동 학습 (정확도 주의)"),
        BALANCED("균형", "간단 보정 + 선택적 정밀 보정 (표준)"),
        PRECISION("정밀", "정확한 초기 보정 (적극 권장)");

        private final String displayName;
        private final String description;

        CalibrationStrategy(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // 🆕 클릭 타이밍 enum 추가
    public enum ClickTiming {
        NORMAL("표준 (1.0초)", 1000f),
        SLOW("느림 (1.5초)", 1500f);

        private final String displayName;
        private final float durationMs;

        ClickTiming(String displayName, float durationMs) {
            this.displayName = displayName;
            this.durationMs = durationMs;
        }

        public String getDisplayName() { return displayName; }
        public float getDurationMs() { return durationMs; }
    }

    // 기존 필드들 (간소화됨)
    private final CalibrationStrategy calibrationStrategy;
    private final boolean backgroundLearningEnabled;
    private final boolean autoOnePointCalibrationEnabled;
    private final float cursorOffsetX;
    private final float cursorOffsetY;
    private final OneEuroFilterPreset oneEuroFilterPreset;

    // 🆕 클릭 타이밍 필드 추가
    private final ClickTiming clickTiming;

    // 🔧 고정된 값들 (상수로 처리)
    private static final float FIXED_EDGE_MARGIN_RATIO = 0.01f;
    private static final long FIXED_EDGE_TRIGGER_MS = 2000L; // 2초로 고정
    private static final int FIXED_CONTINUOUS_SCROLL_COUNT = 2; // 2회로 고정
    private static final float FIXED_AOI_RADIUS = 40f; // 40px로 고정

    // OneEuroFilter 값들 (프리셋에서 가져옴)
    private final double oneEuroFreq;
    private final double oneEuroMinCutoff;
    private final double oneEuroBeta;
    private final double oneEuroDCutoff;

    private UserSettings(Builder builder) {
        this.calibrationStrategy = builder.calibrationStrategy;
        this.backgroundLearningEnabled = builder.backgroundLearningEnabled;
        this.autoOnePointCalibrationEnabled = builder.autoOnePointCalibrationEnabled;
        this.cursorOffsetX = builder.cursorOffsetX;
        this.cursorOffsetY = builder.cursorOffsetY;
        this.oneEuroFilterPreset = builder.oneEuroFilterPreset;
        this.clickTiming = builder.clickTiming;

        // OneEuroFilter 값들을 프리셋에서 가져옴
        this.oneEuroFreq = this.oneEuroFilterPreset.getFreq();
        this.oneEuroMinCutoff = this.oneEuroFilterPreset.getMinCutoff();
        this.oneEuroBeta = this.oneEuroFilterPreset.getBeta();
        this.oneEuroDCutoff = this.oneEuroFilterPreset.getDCutoff();
    }

    // Getter 메서드들
    public CalibrationStrategy getCalibrationStrategy() { return calibrationStrategy; }
    public boolean isBackgroundLearningEnabled() { return backgroundLearningEnabled; }
    public boolean isAutoOnePointCalibrationEnabled() { return autoOnePointCalibrationEnabled; }
    public float getCursorOffsetX() { return cursorOffsetX; }
    public float getCursorOffsetY() { return cursorOffsetY; }
    public OneEuroFilterPreset getOneEuroFilterPreset() { return oneEuroFilterPreset; }
    public ClickTiming getClickTiming() { return clickTiming; }

    // 🆕 클릭 관련 메서드
    public float getFixationDurationMs() { return clickTiming.getDurationMs(); }
    public float getAoiRadius() { return FIXED_AOI_RADIUS; }

    // 🔧 고정값 반환 메서드들
    public boolean isScrollEnabled() { return true; } // 항상 활성화
    public float getEdgeMarginRatio() { return FIXED_EDGE_MARGIN_RATIO; }
    public long getEdgeTriggerMs() { return FIXED_EDGE_TRIGGER_MS; }
    public int getContinuousScrollCount() { return FIXED_CONTINUOUS_SCROLL_COUNT; }
    public boolean isClickEnabled() { return true; } // 항상 활성화
    public boolean isEdgeScrollEnabled() { return true; } // 항상 활성화

    // 🚫 제거된 기능들 (false 반환)
    public boolean isBlinkDetectionEnabled() { return false; } // 기능 제거

    // OneEuroFilter 관련 getter들
    public double getOneEuroFreq() { return oneEuroFreq; }
    public double getOneEuroMinCutoff() { return oneEuroMinCutoff; }
    public double getOneEuroBeta() { return oneEuroBeta; }
    public double getOneEuroDCutoff() { return oneEuroDCutoff; }

    public static class Builder {
        // 🎯 새로운 기본값들 (요구사항 반영)
        private CalibrationStrategy calibrationStrategy = CalibrationStrategy.PRECISION;
        private boolean backgroundLearningEnabled = false;
        private boolean autoOnePointCalibrationEnabled = true;
        private float cursorOffsetX = 0f;
        private float cursorOffsetY = 0f;
        private OneEuroFilterPreset oneEuroFilterPreset = OneEuroFilterPreset.BALANCED_STABILITY; // 균형-안정성을 기본값으로
        private ClickTiming clickTiming = ClickTiming.NORMAL; // 1초 기본값

        public Builder calibrationStrategy(CalibrationStrategy val) { calibrationStrategy = val; return this; }
        public Builder backgroundLearningEnabled(boolean val) { backgroundLearningEnabled = val; return this; }
        public Builder autoOnePointCalibrationEnabled(boolean val) { autoOnePointCalibrationEnabled = val; return this; }
        public Builder cursorOffsetX(float val) { cursorOffsetX = val; return this; }
        public Builder cursorOffsetY(float val) { cursorOffsetY = val; return this; }
        public Builder oneEuroFilterPreset(OneEuroFilterPreset val) { oneEuroFilterPreset = val; return this; }
        public Builder clickTiming(ClickTiming val) { clickTiming = val; return this; }

        // 🚫 제거된 빌더 메서드들 (호환성을 위해 no-op으로 유지)
        public Builder fixationDurationMs(float val) { return this; } // 무시됨 (clickTiming 사용)
        public Builder aoiRadius(float val) { return this; } // 무시됨 (고정값 사용)
        public Builder scrollEnabled(boolean val) { return this; } // 무시됨 (항상 true)
        public Builder edgeMarginRatio(float val) { return this; } // 무시됨 (고정값 사용)
        public Builder edgeTriggerMs(long val) { return this; } // 무시됨 (고정값 사용)
        public Builder continuousScrollCount(int val) { return this; } // 무시됨 (고정값 사용)
        public Builder clickEnabled(boolean val) { return this; } // 무시됨 (항상 true)
        public Builder edgeScrollEnabled(boolean val) { return this; } // 무시됨 (항상 true)
        public Builder blinkDetectionEnabled(boolean val) { return this; } // 무시됨 (항상 false)
        public Builder oneEuroFreq(double val) { return this; } // 무시됨 (프리셋 사용)
        public Builder oneEuroMinCutoff(double val) { return this; } // 무시됨 (프리셋 사용)
        public Builder oneEuroBeta(double val) { return this; } // 무시됨 (프리셋 사용)
        public Builder oneEuroDCutoff(double val) { return this; } // 무시됨 (프리셋 사용)

        public UserSettings build() {
            return new UserSettings(this);
        }
    }
}