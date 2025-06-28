package camp.visual.android.sdk.sample.domain.model;

public enum OneEuroFilterPreset {
    STABILITY(
            "부드럽게",
            "매우 부드럽고 안정적인 커서 움직임 (시선 흔들림 최소화)",
            30.0,
            0.3,     // 더 낮은 minCutoff로 더 부드럽게
            0.002,   // 더 낮은 beta로 안정성 극대화
            1.0
    ),

    BALANCED_STABILITY(
            "적당히",
            "부드러움과 반응성의 최적 균형 (기존 부드럽게 설정 적용)",
            30.0,
            0.5,     // 기존 '부드럽게' 설정 적용
            0.003,   // 기존 '부드럽게' 설정 적용 
            1.0
    ),

    BALANCED(
            "적당히",
            "부드러움과 반응성의 균형",
            30.0,
            0.5,     // BALANCED_STABILITY와 동일하게 맞춤
            0.003,
            1.0
    ),

    RESPONSIVE(
            "빠르게",
            "빠른 반응성과 정확성 (미세한 떨림은 있지만 반응 빠름)",
            30.0,
            0.8,     // 적당한 반응성 (기존 1.0에서 약간 완화)
            0.01,    // 적당한 beta (기존 0.015에서 약간 완화)
            1.0
    );

    private final String displayName;
    private final String description;
    private final double freq;
    private final double minCutoff;
    private final double beta;
    private final double dCutoff;

    OneEuroFilterPreset(String displayName, String description,
                        double freq, double minCutoff, double beta, double dCutoff) {
        this.displayName = displayName;
        this.description = description;
        this.freq = freq;
        this.minCutoff = minCutoff;
        this.beta = beta;
        this.dCutoff = dCutoff;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getFreq() {
        return freq;
    }

    public double getMinCutoff() {
        return minCutoff;
    }

    public double getBeta() {
        return beta;
    }

    public double getDCutoff() {
        return dCutoff;
    }

    public static OneEuroFilterPreset fromName(String name) {
        if (name == null) {
            return BALANCED_STABILITY;
        }

        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return BALANCED_STABILITY;
        }
    }
}