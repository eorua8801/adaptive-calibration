package camp.visual.android.sdk.sample.domain.filter;

import android.graphics.PointF;
import android.util.Log;

import camp.visual.eyedid.gazetracker.filter.OneEuroFilterManager;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

/**
 * 향상된 OneEuroFilter 관리자
 * - fixationX/Y 데이터를 활용한 안경 굴절 보정
 * - TrackingState 기반 동적 필터링 (단순화된 버전)
 * - 안경 착용자 최적화
 */
public class EnhancedOneEuroFilterManager {
    private static final String TAG = "EnhancedOneEuroFilter";

    // 기본 필터 (gazeX/Y용)
    private OneEuroFilterManager gazeFilter;

    // 보조 필터 (fixationX/Y용)
    private OneEuroFilterManager fixationFilter;

    // 동적 필터 설정
    private OneEuroFilterManager normalFilter;
    private OneEuroFilterManager confidenceFilter;
    private OneEuroFilterManager activeFilter;

    // 필터 파라미터 저장 (reset용)
    private final float freq;
    private final float minCutoff;
    private final float beta;
    private final float dCutoff;

    // 시선 안정화 설정 (수정된 설명)
    private boolean glassesCompensationEnabled = true;
    private float refractionCorrectionFactor = 0.15f; // gaze-fixation 블렌딩 비율 (기본 15%)
    private int consecutiveLowConfidenceCount = 0;
    private static final int LOW_CONFIDENCE_THRESHOLD = 2; // 더 빠른 반응 (기존 3 → 2)

    // 필터 성능 모니터링
    private long lastFilterTime = 0;
    private float[] lastFilteredValues = new float[2];
    private boolean isInitialized = false;
    private String currentFilterType = "정상신뢰도";

    public EnhancedOneEuroFilterManager(double freq, double minCutoff, double beta, double dCutoff) {
        this.freq = (float) freq;
        this.minCutoff = (float) minCutoff;
        this.beta = (float) beta;
        this.dCutoff = (float) dCutoff;

        initializeFilters();
    }

    private void initializeFilters() {
        // 🆕 기본 gaze 필터
        gazeFilter = new OneEuroFilterManager(2, freq, minCutoff, beta, dCutoff);

        // 🆕 fixation 데이터용 필터 (더 부드러운 설정 - 개선됨)
        fixationFilter = new OneEuroFilterManager(2, freq, minCutoff * 0.6f, beta * 0.7f, dCutoff);

        // 🆕 TrackingState별 동적 필터 (부드러움 중심으로 개선)
        // 정상 상태: 부드러움과 반응성 균형
        normalFilter = new OneEuroFilterManager(2, freq, minCutoff, beta * 0.9f, dCutoff);

        // 저신뢰도 상태: 안정성 극대화
        confidenceFilter = new OneEuroFilterManager(2, freq, minCutoff * 0.4f, beta * 0.6f, dCutoff);

        // 기본적으로 정상 필터 사용
        activeFilter = normalFilter;

        Log.d(TAG, "Enhanced OneEuro 필터 초기화 완료");
        Log.d(TAG, "안경 보정: " + (glassesCompensationEnabled ? "활성화" : "비활성화"));
    }

    /**
     * 🆕 향상된 필터링 메인 메서드
     * gaze와 fixation 데이터를 모두 활용
     */
    public boolean filterValues(long timestamp, float gazeX, float gazeY, float fixationX, float fixationY, TrackingState trackingState) {
        // TrackingState에 따른 동적 필터 선택
        selectFilterBasedOnTrackingState(trackingState);

        // 기본 gaze 필터링
        boolean gazeFiltered = activeFilter.filterValues(timestamp, gazeX, gazeY);

        // fixation 데이터 필터링 (안경 보정용)
        boolean fixationFiltered = fixationFilter.filterValues(timestamp, fixationX, fixationY);

        if (gazeFiltered && fixationFiltered) {
            // 필터링된 값들 가져오기
            float[] gazeValues = activeFilter.getFilteredValues();
            float[] fixationValues = fixationFilter.getFilteredValues();

            // 🆕 안경 굴절 보정 적용
            if (glassesCompensationEnabled) {
                PointF corrected = applyGlassesCorrection(
                        gazeValues[0], gazeValues[1],
                        fixationValues[0], fixationValues[1]
                );
                lastFilteredValues[0] = corrected.x;
                lastFilteredValues[1] = corrected.y;
            } else {
                lastFilteredValues[0] = gazeValues[0];
                lastFilteredValues[1] = gazeValues[1];
            }

            lastFilterTime = timestamp;
            isInitialized = true;

            return true;
        }

        return false;
    }

    /**
     * 🆕 TrackingState 기반 동적 필터 선택 (간소화됨)
     */
    private void selectFilterBasedOnTrackingState(TrackingState trackingState) {
        // 🔧 수정: 간소화된 방식으로 TrackingState 처리
        // SUCCESS 외의 모든 상태를 LOW_CONFIDENCE로 간주
        if (trackingState == TrackingState.SUCCESS) {
            if (activeFilter != normalFilter) {
                activeFilter = normalFilter;
                consecutiveLowConfidenceCount = 0;
                currentFilterType = "정상신뢰도";
                Log.d(TAG, "정상 신뢰도 필터로 전환 (반응성 중심)");
            }
        } else {
            // SUCCESS가 아닌 모든 상태 (FACE_MISSING, UNSUPPORTED 등)
            consecutiveLowConfidenceCount++;
            if (consecutiveLowConfidenceCount >= LOW_CONFIDENCE_THRESHOLD && activeFilter != confidenceFilter) {
                activeFilter = confidenceFilter;
                currentFilterType = "저신뢰도";
                Log.d(TAG, "저신뢰도 필터로 전환 (안정성 중심) - 연속 " + consecutiveLowConfidenceCount + "회");
            }
        }
    }

    /**
     * 🆕 시선 안정화를 위한 가중평균 알고리즘 (수정된 설명)
     * gaze(실시간, 흔들림)와 fixation(평균화, 안정)을 적절히 블렌딩
     */
    private PointF applyGlassesCorrection(float gazeX, float gazeY, float fixationX, float fixationY) {
        // gaze와 fixation 간의 차이 계산 (흔들림 정도)
        float deltaX = fixationX - gazeX;
        float deltaY = fixationY - gazeY;

        // 흔들림 정도에 따른 적응형 안정화
        // 흔들림이 클수록 fixation 비율 증가 (안정성 강화)
        float jitterLevel = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        float adaptiveBlending = refractionCorrectionFactor * (1.0f + Math.min(jitterLevel / 100f, 0.3f));
        
        // 부드러운 안정화 (급격한 변화 방지)
        float smoothingFactor = 0.8f; // 전체적인 블렌딩 강도 조절
        float stabilizedX = gazeX + deltaX * adaptiveBlending * smoothingFactor;
        float stabilizedY = gazeY + deltaY * adaptiveBlending * smoothingFactor;
        
        // 결과: gaze(반응성) + fixation(안정성)의 최적 블렌드
        return new PointF(stabilizedX, stabilizedY);
    }

    /**
     * 기존 OneEuroFilterManager 호환성을 위한 메서드
     */
    public boolean filterValues(long timestamp, float x, float y) {
        // fixation 데이터가 없는 경우 기본 필터링만 수행
        return activeFilter.filterValues(timestamp, x, y);
    }

    public float[] getFilteredValues() {
        if (isInitialized) {
            return lastFilteredValues.clone();
        } else {
            return activeFilter.getFilteredValues();
        }
    }

    // 🆕 설정 메서드들
    public void setGlassesCompensationEnabled(boolean enabled) {
        glassesCompensationEnabled = enabled;
        Log.d(TAG, "시선 안정화 " + (enabled ? "활성화" : "비활성화"));
    }

    public void setRefractionCorrectionFactor(float factor) {
        refractionCorrectionFactor = Math.max(0f, Math.min(1f, factor)); // 0~1 범위로 제한
        Log.d(TAG, "gaze-fixation 블렌딩 비율 설정: " + refractionCorrectionFactor);
    }

    public boolean isGlassesCompensationEnabled() {
        return glassesCompensationEnabled;
    }

    public float getRefractionCorrectionFactor() {
        return refractionCorrectionFactor;
    }

    // 🆕 필터 상태 정보
    public String getCurrentFilterInfo() {
        return currentFilterType + " (시선안정화: " + (glassesCompensationEnabled ? "ON" : "OFF") + ")";
    }

    public void reset() {
        // 🔧 수정: 기존 파라미터로 새 필터 생성
        gazeFilter = new OneEuroFilterManager(2, freq, minCutoff, beta, dCutoff);
        fixationFilter = new OneEuroFilterManager(2, freq, minCutoff * 0.6f, beta * 0.7f, dCutoff);
        normalFilter = new OneEuroFilterManager(2, freq, minCutoff, beta * 0.9f, dCutoff);
        confidenceFilter = new OneEuroFilterManager(2, freq, minCutoff * 0.4f, beta * 0.6f, dCutoff);

        activeFilter = normalFilter;
        consecutiveLowConfidenceCount = 0;
        isInitialized = false;
        currentFilterType = "정상신뢰도";

        Log.d(TAG, "필터 상태 초기화");
    }
}