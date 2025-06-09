package camp.visual.android.sdk.sample.domain.safety;

import android.util.Log;

import camp.visual.android.sdk.sample.domain.calibration.AdaptiveCalibrationManager;

/**
 * 🔒 적응형 캘리브레이션 안전성 래퍼
 * - 누적 드리프트 방지
 * - 과도한 적응 방지
 * - 안전한 적응형 조정 적용
 */
public class AdaptiveCalibrationSafetyWrapper {
    private static final String TAG = "AdaptiveSafety";
    
    private AdaptiveCalibrationManager adaptiveManager;
    private long lastAdaptationTime = 0;
    private static final long MIN_ADAPTATION_INTERVAL = 300000; // 5분 최소 간격
    
    // 누적 드리프트 추적
    private float totalDriftX = 0;
    private float totalDriftY = 0;
    private static final float MAX_CUMULATIVE_DRIFT = 50.0f; // 최대 누적 드리프트
    
    // 적응 이력 추적
    private int adaptationCount = 0;
    private long sessionStartTime;
    private static final int MAX_ADAPTATIONS_PER_SESSION = 10; // 세션당 최대 적응 횟수
    
    // 연속 적응 방지
    private int consecutiveAdaptations = 0;
    private static final int MAX_CONSECUTIVE_ADAPTATIONS = 3;
    
    public AdaptiveCalibrationSafetyWrapper(AdaptiveCalibrationManager manager) {
        this.adaptiveManager = manager;
        this.sessionStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "적응형 캘리브레이션 안전 래퍼 초기화");
    }
    
    /**
     * 🛡️ 안전한 적응형 조정 적용
     */
    public boolean safelyApplyAdaptation(float adjustmentX, float adjustmentY) {
        long currentTime = System.currentTimeMillis();
        
        // 1. 최소 간격 체크
        if (currentTime - lastAdaptationTime < MIN_ADAPTATION_INTERVAL) {
            long remainingTime = (MIN_ADAPTATION_INTERVAL - (currentTime - lastAdaptationTime)) / 1000;
            Log.d(TAG, "적응 간격 부족 - " + remainingTime + "초 후 재시도");
            return false;
        }
        
        // 2. 세션당 최대 적응 횟수 체크
        if (adaptationCount >= MAX_ADAPTATIONS_PER_SESSION) {
            Log.w(TAG, "세션당 최대 적응 횟수 도달 (" + adaptationCount + "/" + MAX_ADAPTATIONS_PER_SESSION + ")");
            return false;
        }
        
        // 3. 연속 적응 방지
        if (consecutiveAdaptations >= MAX_CONSECUTIVE_ADAPTATIONS) {
            Log.w(TAG, "연속 적응 한계 도달 (" + consecutiveAdaptations + "/" + MAX_CONSECUTIVE_ADAPTATIONS + ")");
            consecutiveAdaptations = 0; // 리셋
            return false;
        }
        
        // 4. 조정량 크기 검증
        float adjustmentMagnitude = (float) Math.sqrt(adjustmentX * adjustmentX + adjustmentY * adjustmentY);
        final float MAX_SINGLE_ADJUSTMENT = 20.0f; // 한 번에 최대 20px
        
        if (adjustmentMagnitude > MAX_SINGLE_ADJUSTMENT) {
            // 조정량을 안전한 크기로 스케일링
            float scale = MAX_SINGLE_ADJUSTMENT / adjustmentMagnitude;
            adjustmentX *= scale;
            adjustmentY *= scale;
            
            Log.w(TAG, "과도한 조정량 감지 - 스케일링 적용: " + adjustmentMagnitude + "px -> " + MAX_SINGLE_ADJUSTMENT + "px");
        }
        
        // 5. 누적 드리프트 체크
        float newTotalDriftX = totalDriftX + adjustmentX;
        float newTotalDriftY = totalDriftY + adjustmentY;
        float newDriftMagnitude = (float) Math.sqrt(newTotalDriftX * newTotalDriftX + newTotalDriftY * newTotalDriftY);
        
        if (newDriftMagnitude > MAX_CUMULATIVE_DRIFT) {
            Log.w(TAG, "누적 드리프트 한계 도달 (" + newDriftMagnitude + "/" + MAX_CUMULATIVE_DRIFT + "px) - 재캘리브레이션 필요");
            
            // 누적 드리프트 리셋 및 재캘리브레이션 요청
            requestRecalibration("누적 드리프트 한계 도달");
            resetDrift();
            return false;
        }
        
        // 6. 모든 검증 통과 - 안전한 적응 적용
        totalDriftX = newTotalDriftX;
        totalDriftY = newTotalDriftY;
        lastAdaptationTime = currentTime;
        adaptationCount++;
        consecutiveAdaptations++;
        
        Log.d(TAG, String.format("안전한 적응형 조정 적용: (%.2f, %.2f) - 누적: (%.2f, %.2f)", 
              adjustmentX, adjustmentY, totalDriftX, totalDriftY));
        
        // 실제 적응형 매니저에 적용 (필요시)
        // adaptiveManager.applyAdjustment(adjustmentX, adjustmentY);
        
        return true;
    }
    
    /**
     * 🔄 드리프트 상태 초기화
     */
    public void resetDrift() {
        totalDriftX = 0;
        totalDriftY = 0;
        consecutiveAdaptations = 0;
        Log.d(TAG, "누적 드리프트 초기화");
    }
    
    /**
     * 🔄 전체 상태 초기화 (새로운 세션 시작)
     */
    public void resetSession() {
        resetDrift();
        lastAdaptationTime = 0;
        adaptationCount = 0;
        sessionStartTime = System.currentTimeMillis();
        
        Log.d(TAG, "적응형 안전 래퍼 세션 초기화");
    }
    
    /**
     * 🚨 재캘리브레이션 요청
     */
    private void requestRecalibration(String reason) {
        Log.w(TAG, "재캘리브레이션 요청: " + reason);
        
        // TODO: 사용자에게 재캘리브레이션 알림
        // 또는 MainActivity/GazeTrackingService에 알림 전송
        
        // 통계 기록
        recordRecalibrationRequest(reason);
    }
    
    /**
     * 📊 재캘리브레이션 요청 기록
     */
    private void recordRecalibrationRequest(String reason) {
        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60; // 분 단위
        
        Log.i(TAG, String.format("재캘리브레이션 통계: 이유='%s', 세션시간=%d분, 적응횟수=%d, 누적드리프트=%.1fpx",
              reason, sessionDuration, adaptationCount, getCurrentDriftMagnitude()));
    }
    
    /**
     * 📈 현재 드리프트 크기 조회
     */
    public float getCurrentDriftMagnitude() {
        return (float) Math.sqrt(totalDriftX * totalDriftX + totalDriftY * totalDriftY);
    }
    
    /**
     * ✅ 적응 가능 여부 확인
     */
    public boolean canAdapt() {
        long currentTime = System.currentTimeMillis();
        boolean intervalOk = (currentTime - lastAdaptationTime) >= MIN_ADAPTATION_INTERVAL;
        boolean countOk = adaptationCount < MAX_ADAPTATIONS_PER_SESSION;
        boolean consecutiveOk = consecutiveAdaptations < MAX_CONSECUTIVE_ADAPTATIONS;
        boolean driftOk = getCurrentDriftMagnitude() < MAX_CUMULATIVE_DRIFT;
        
        return intervalOk && countOk && consecutiveOk && driftOk;
    }
    
    /**
     * 📊 안전성 상태 보고서
     */
    public String getSafetyReport() {
        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60;
        long nextAdaptationTime = Math.max(0, (lastAdaptationTime + MIN_ADAPTATION_INTERVAL - System.currentTimeMillis()) / 1000);
        
        return String.format("적응형 안전성 상태:\n" +
                "- 세션 시간: %d분\n" +
                "- 적응 횟수: %d/%d\n" +
                "- 연속 적응: %d/%d\n" +
                "- 누적 드리프트: %.1fpx/%.1fpx\n" +
                "- 다음 적응 가능: %s\n" +
                "- 적응 가능 여부: %s",
                sessionDuration,
                adaptationCount, MAX_ADAPTATIONS_PER_SESSION,
                consecutiveAdaptations, MAX_CONSECUTIVE_ADAPTATIONS,
                getCurrentDriftMagnitude(), MAX_CUMULATIVE_DRIFT,
                nextAdaptationTime > 0 ? nextAdaptationTime + "초 후" : "즉시",
                canAdapt() ? "✅" : "❌");
    }
    
    /**
     * ⚠️ 위험 수준 평가
     */
    public SafetyLevel evaluateSafetyLevel() {
        float driftRatio = getCurrentDriftMagnitude() / MAX_CUMULATIVE_DRIFT;
        float adaptationRatio = (float) adaptationCount / MAX_ADAPTATIONS_PER_SESSION;
        
        if (driftRatio > 0.9f || adaptationRatio > 0.9f) {
            return SafetyLevel.CRITICAL;
        } else if (driftRatio > 0.7f || adaptationRatio > 0.7f) {
            return SafetyLevel.WARNING;
        } else if (driftRatio > 0.5f || adaptationRatio > 0.5f) {
            return SafetyLevel.CAUTION;
        } else {
            return SafetyLevel.SAFE;
        }
    }
    
    public enum SafetyLevel {
        SAFE,       // 안전
        CAUTION,    // 주의
        WARNING,    // 경고
        CRITICAL    // 위험
    }
    
    /**
     * 🎯 권장 조치 제안
     */
    public String getRecommendedAction() {
        SafetyLevel level = evaluateSafetyLevel();
        
        switch (level) {
            case SAFE:
                return "정상 동작 중입니다.";
            case CAUTION:
                return "적응형 기능이 활발히 동작하고 있습니다. 모니터링을 계속하세요.";
            case WARNING:
                return "과도한 적응이 감지되었습니다. 캘리브레이션 상태를 확인해보세요.";
            case CRITICAL:
                return "재캘리브레이션이 필요합니다. 시선 보정을 다시 수행하세요.";
            default:
                return "상태를 확인할 수 없습니다.";
        }
    }
    
    /**
     * 🔧 디버그 로그 출력
     */
    public void logDebugInfo() {
        Log.d(TAG, "=== 적응형 안전성 디버그 ===");
        Log.d(TAG, getSafetyReport());
        Log.d(TAG, "안전성 수준: " + evaluateSafetyLevel());
        Log.d(TAG, "권장 조치: " + getRecommendedAction());
        Log.d(TAG, "==========================");
    }
}
