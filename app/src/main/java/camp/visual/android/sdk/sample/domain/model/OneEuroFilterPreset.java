package camp.visual.android.sdk.sample.domain.model;

public enum OneEuroFilterPreset {
    STABILITY(
            "부드럽게",
            "약간 느리지만 커서가 매우 부드럽게 움직여요",
            30.0,
            0.5,
            0.003,
            1.0
    ),

    BALANCED_STABILITY(
            "적당히",
            "부드러움과 빠른 반응의 적절한 균형",
            30.0,
            0.75,
            0.005,
            1.0
    ),

    BALANCED(
            "적당히",
            "부드러움과 빠른 반응의 균형",
            30.0,
            0.75,
            0.005,
            1.0
    ),

    RESPONSIVE(
            "빠르게",
            "더 빠르지만 커서가 약간 떨릴 수 있어요",
            30.0,
            1.0,
            0.015,
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