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

    // 클릭 타이밍 enum (기존 유지)
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

    // 🆕 성능 최적화 모드 enum 추가
    public enum PerformanceMode {
        POWER_SAVING("절전 모드", "배터리 수명 우선 (FPS 자동 조정)"),
        BALANCED("균형 모드", "성능과 배터리의 균형"),
        PERFORMANCE("성능 모드", "최고 성능 우선 (배터리 소모 증가)");

        private final String displayName;
        private final String description;

        PerformanceMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // 기존 필드들
    private final CalibrationStrategy calibrationStrategy;
    private final boolean backgroundLearningEnabled;
    private final boolean autoOnePointCalibrationEnabled;
    private final float cursorOffsetX;
    private final float cursorOffsetY;
    private final OneEuroFilterPreset oneEuroFilterPreset;
    private final ClickTiming clickTiming;

    // 🆕 1단계 개선 관련 필드들
    private final boolean performanceOptimizationEnabled;
    private final PerformanceMode performanceMode;
    private final boolean glassesCompensationEnabled;
    private final float refractionCorrectionFactor;
    private final boolean dynamicFilteringEnabled;
    private final int targetFPS;

    // 고정된 값들 (상수로 처리)
    private static final float FIXED_EDGE_MARGIN_RATIO = 0.01f;
    private static final long FIXED_EDGE_TRIGGER_MS = 2000L;
    private static final int FIXED_CONTINUOUS_SCROLL_COUNT = 2;
    private static final float FIXED_AOI_RADIUS = 40f;

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

        // 🆕 1단계 개선 필드 초기화
        this.performanceOptimizationEnabled = builder.performanceOptimizationEnabled;
        this.performanceMode = builder.performanceMode;
        this.glassesCompensationEnabled = builder.glassesCompensationEnabled;
        this.refractionCorrectionFactor = builder.refractionCorrectionFactor;
        this.dynamicFilteringEnabled = builder.dynamicFilteringEnabled;
        this.targetFPS = builder.targetFPS;

        // OneEuroFilter 값들을 프리셋에서 가져옴
        this.oneEuroFreq = this.oneEuroFilterPreset.getFreq();
        this.oneEuroMinCutoff = this.oneEuroFilterPreset.getMinCutoff();
        this.oneEuroBeta = this.oneEuroFilterPreset.getBeta();
        this.oneEuroDCutoff = this.oneEuroFilterPreset.getDCutoff();
    }

    // 기존 Getter 메서드들
    public CalibrationStrategy getCalibrationStrategy() { return calibrationStrategy; }
    public boolean isBackgroundLearningEnabled() { return backgroundLearningEnabled; }
    public boolean isAutoOnePointCalibrationEnabled() { return autoOnePointCalibrationEnabled; }
    public float getCursorOffsetX() { return cursorOffsetX; }
    public float getCursorOffsetY() { return cursorOffsetY; }
    public OneEuroFilterPreset getOneEuroFilterPreset() { return oneEuroFilterPreset; }
    public ClickTiming getClickTiming() { return clickTiming; }

    // 🆕 1단계 개선 관련 Getter 메서드들
    public boolean isPerformanceOptimizationEnabled() { return performanceOptimizationEnabled; }
    public PerformanceMode getPerformanceMode() { return performanceMode; }
    public boolean isGlassesCompensationEnabled() { return glassesCompensationEnabled; }
    public float getRefractionCorrectionFactor() { return refractionCorrectionFactor; }
    public boolean isDynamicFilteringEnabled() { return dynamicFilteringEnabled; }
    public int getTargetFPS() { return targetFPS; }

    // 클릭 관련 메서드
    public float getFixationDurationMs() { return clickTiming.getDurationMs(); }
    public float getAoiRadius() { return FIXED_AOI_RADIUS; }

    // 고정값 반환 메서드들
    public boolean isScrollEnabled() { return true; }
    public float getEdgeMarginRatio() { return FIXED_EDGE_MARGIN_RATIO; }
    public long getEdgeTriggerMs() { return FIXED_EDGE_TRIGGER_MS; }
    public int getContinuousScrollCount() { return FIXED_CONTINUOUS_SCROLL_COUNT; }
    public boolean isClickEnabled() { return true; }
    public boolean isEdgeScrollEnabled() { return true; }
    public boolean isBlinkDetectionEnabled() { return false; } // 기능 제거

    // OneEuroFilter 관련 getter들
    public double getOneEuroFreq() { return oneEuroFreq; }
    public double getOneEuroMinCutoff() { return oneEuroMinCutoff; }
    public double getOneEuroBeta() { return oneEuroBeta; }
    public double getOneEuroDCutoff() { return oneEuroDCutoff; }

    // 🆕 성능 모드별 권장 FPS 반환
    public int getRecommendedFPSForPerformanceMode() {
        switch (performanceMode) {
            case POWER_SAVING:
                return 15; // 절전 모드: 낮은 FPS
            case BALANCED:
                return 25; // 균형 모드: 중간 FPS
            case PERFORMANCE:
                return 30; // 성능 모드: 최고 FPS
            default:
                return targetFPS;
        }
    }

    // 🆕 안경 보정 강도 레벨 반환
    public String getGlassesCompensationLevelDescription() {
        if (!glassesCompensationEnabled) {
            return "비활성화";
        }

        if (refractionCorrectionFactor <= 0.05f) {
            return "약함";
        } else if (refractionCorrectionFactor <= 0.15f) {
            return "보통";
        } else {
            return "강함";
        }
    }

    public static class Builder {
        // 기존 기본값들
        private CalibrationStrategy calibrationStrategy = CalibrationStrategy.PRECISION;
        private boolean backgroundLearningEnabled = false;
        private boolean autoOnePointCalibrationEnabled = true;
        private float cursorOffsetX = 0f;
        private float cursorOffsetY = 0f;
        private OneEuroFilterPreset oneEuroFilterPreset = OneEuroFilterPreset.BALANCED_STABILITY;
        private ClickTiming clickTiming = ClickTiming.NORMAL;

        // 🆕 1단계 개선 관련 기본값들 (개선됨)
        private boolean performanceOptimizationEnabled = true; // 기본적으로 활성화
        private PerformanceMode performanceMode = PerformanceMode.BALANCED; // 균형 모드가 기본
        private boolean glassesCompensationEnabled = true; // 안경 보정 기본 활성화
        private float refractionCorrectionFactor = 0.15f; // 개선된 보정 강도 (기존 0.1f → 0.15f)
        private boolean dynamicFilteringEnabled = true; // 동적 필터링 기본 활성화
        private int targetFPS = 25; // 부드러움 중심으로 조정 (기존 30 → 25)

        // 기존 빌더 메서드들
        public Builder calibrationStrategy(CalibrationStrategy val) { calibrationStrategy = val; return this; }
        public Builder backgroundLearningEnabled(boolean val) { backgroundLearningEnabled = val; return this; }
        public Builder autoOnePointCalibrationEnabled(boolean val) { autoOnePointCalibrationEnabled = val; return this; }
        public Builder cursorOffsetX(float val) { cursorOffsetX = val; return this; }
        public Builder cursorOffsetY(float val) { cursorOffsetY = val; return this; }
        public Builder oneEuroFilterPreset(OneEuroFilterPreset val) { oneEuroFilterPreset = val; return this; }
        public Builder clickTiming(ClickTiming val) { clickTiming = val; return this; }

        // 🆕 1단계 개선 관련 빌더 메서드들
        public Builder performanceOptimizationEnabled(boolean val) { performanceOptimizationEnabled = val; return this; }
        public Builder performanceMode(PerformanceMode val) { performanceMode = val; return this; }
        public Builder glassesCompensationEnabled(boolean val) { glassesCompensationEnabled = val; return this; }
        public Builder refractionCorrectionFactor(float val) {
            refractionCorrectionFactor = Math.max(0f, Math.min(1f, val)); // 0~1 범위로 제한
            return this;
        }
        public Builder dynamicFilteringEnabled(boolean val) { dynamicFilteringEnabled = val; return this; }
        public Builder targetFPS(int val) {
            targetFPS = Math.max(10, Math.min(30, val)); // 10~30 범위로 제한
            return this;
        }

        // 제거된 기능들을 위한 호환성 메서드들 (no-op으로 유지)
        public Builder fixationDurationMs(float val) { return this; }
        public Builder aoiRadius(float val) { return this; }
        public Builder scrollEnabled(boolean val) { return this; }
        public Builder edgeMarginRatio(float val) { return this; }
        public Builder edgeTriggerMs(long val) { return this; }
        public Builder continuousScrollCount(int val) { return this; }
        public Builder clickEnabled(boolean val) { return this; }
        public Builder edgeScrollEnabled(boolean val) { return this; }
        public Builder blinkDetectionEnabled(boolean val) { return this; }
        public Builder oneEuroFreq(double val) { return this; }
        public Builder oneEuroMinCutoff(double val) { return this; }
        public Builder oneEuroBeta(double val) { return this; }
        public Builder oneEuroDCutoff(double val) { return this; }

        public UserSettings build() {
            return new UserSettings(this);
        }
    }
}