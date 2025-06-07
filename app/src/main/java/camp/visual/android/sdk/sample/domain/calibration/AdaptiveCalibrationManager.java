package camp.visual.android.sdk.sample.domain.calibration;

import android.util.Log;

import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;

/**
 * 🆕 적응형 캘리브레이션 관리자 (2단계) - SDK 호환성 수정
 * 사용자의 피로도와 집중도를 시뮬레이션하여
 * 최적의 캘리브레이션 타이밍을 자동 감지하고 제안하는 시스템
 *
 * Note: UserStatusInfo가 현재 SDK에서 지원되지 않으므로
 * 시뮬레이션 기반으로 구현하고 향후 SDK 업데이트 시 실제 데이터로 교체 예정
 */
public class AdaptiveCalibrationManager {
    private static final String TAG = "AdaptiveCalibration";

    // 🔧 SDK 호환성: UserStatusInfo 대신 시뮬레이션 데이터 사용
    public static class SimulatedUserStatus {
        public final float attentionScore;      // 0.0 ~ 1.0
        public final float drowsinessIntensity; // 0.0 ~ 1.0
        public final boolean isDrowsy;
        public final long timestamp;

        public SimulatedUserStatus(float attention, float drowsiness, boolean drowsy) {
            this.attentionScore = Math.max(0f, Math.min(1f, attention));
            this.drowsinessIntensity = Math.max(0f, Math.min(1f, drowsiness));
            this.isDrowsy = drowsy;
            this.timestamp = System.currentTimeMillis();
        }

        // 기본 상태 (보통 집중도)
        public static SimulatedUserStatus createDefault() {
            return new SimulatedUserStatus(0.7f, 0.1f, false);
        }

        // 높은 집중도 상태
        public static SimulatedUserStatus createFocused() {
            return new SimulatedUserStatus(0.9f, 0.05f, false);
        }

        // 피로한 상태
        public static SimulatedUserStatus createTired() {
            return new SimulatedUserStatus(0.4f, 0.3f, true);
        }
    }

    // 적응형 캘리브레이션 콜백 인터페이스
    public interface AdaptiveCalibrationCallback {
        void onOptimalCalibrationTimeDetected(CalibrationRecommendation recommendation);
        void onCalibrationQualityAssessment(CalibrationQuality quality);
        void onUserStatusChanged(UserStatus status);
    }

    // 캘리브레이션 추천 정보
    public static class CalibrationRecommendation {
        public final CalibrationModeType recommendedMode;
        public final int confidenceLevel; // 0-100
        public final String reason;
        public final long timestamp;

        public CalibrationRecommendation(CalibrationModeType mode, int confidence, String reason) {
            this.recommendedMode = mode;
            this.confidenceLevel = confidence;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // 캘리브레이션 품질 평가
    public static class CalibrationQuality {
        public final int qualityScore; // 0-100
        public final boolean needsRecalibration;
        public final String assessment;
        public final long timestamp;

        public CalibrationQuality(int score, boolean needsRecalibration, String assessment) {
            this.qualityScore = score;
            this.needsRecalibration = needsRecalibration;
            this.assessment = assessment;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // 사용자 상태 분석 결과
    public static class UserStatus {
        public final boolean isOptimalForCalibration;
        public final int alertnessLevel; // 0-100
        public final int attentionLevel; // 0-100
        public final String statusDescription;
        public final long timestamp;

        public UserStatus(boolean isOptimal, int alertness, int attention, String description) {
            this.isOptimalForCalibration = isOptimal;
            this.alertnessLevel = alertness;
            this.attentionLevel = attention;
            this.statusDescription = description;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // 설정값들
    private boolean enabled = true;
    private float attentionThreshold = 0.8f; // 80% 이상 집중도
    private float drowsinessThreshold = 0.1f; // 10% 이하 졸음
    private int minimumObservationTime = 10000; // 10초 관찰
    private int cooldownPeriod = 60000; // 1분 쿨다운

    // 상태 추적
    private AdaptiveCalibrationCallback callback;
    private SimulatedUserStatus lastUserStatus;
    private long lastRecommendationTime = 0;
    private long observationStartTime = 0;
    private boolean isObserving = false;

    // 통계 데이터
    private float averageAttentionScore = 0.5f;
    private int optimalConditionCount = 0;
    private int totalObservationCount = 0;

    // 🆕 시뮬레이션 관련
    private boolean simulationMode = true;
    private long lastSimulationUpdate = 0;
    private static final long SIMULATION_UPDATE_INTERVAL = 5000; // 5초마다 업데이트

    public AdaptiveCalibrationManager() {
        Log.d(TAG, "적응형 캘리브레이션 관리자 초기화 (시뮬레이션 모드)");
        // 기본 상태로 초기화
        lastUserStatus = SimulatedUserStatus.createDefault();
    }

    public void setCallback(AdaptiveCalibrationCallback callback) {
        this.callback = callback;
    }

    /**
     * 🔧 SDK 호환성: 시뮬레이션된 사용자 상태 분석
     * 실제 UserStatusInfo 대신 시뮬레이션 데이터 사용
     */
    public void simulateUserStatusUpdate() {
        if (!enabled || !simulationMode) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSimulationUpdate < SIMULATION_UPDATE_INTERVAL) {
            return; // 아직 업데이트 시간이 아님
        }

        // 시뮬레이션된 사용자 상태 생성
        SimulatedUserStatus simulatedStatus = generateSimulatedUserStatus();
        analyzeUserStatus(simulatedStatus);

        lastSimulationUpdate = currentTime;
    }

    /**
     * 🆕 시뮬레이션된 사용자 상태 생성
     */
    private SimulatedUserStatus generateSimulatedUserStatus() {
        // 시간에 따른 동적 상태 변화 시뮬레이션
        long currentTime = System.currentTimeMillis();
        float timeBasedVariation = (float) Math.sin(currentTime / 30000.0) * 0.2f; // 30초 주기

        // 기본 집중도에 시간 변화 적용
        float baseAttention = 0.7f + timeBasedVariation;
        float baseDrowsiness = 0.1f - (timeBasedVariation * 0.5f);

        // 간헐적으로 좋은 조건 생성 (20% 확률)
        if (Math.random() < 0.2) {
            return SimulatedUserStatus.createFocused();
        }
        // 간헐적으로 피로한 조건 생성 (10% 확률)
        else if (Math.random() < 0.1) {
            return SimulatedUserStatus.createTired();
        }

        boolean isDrowsy = baseDrowsiness > 0.2f;
        return new SimulatedUserStatus(baseAttention, baseDrowsiness, isDrowsy);
    }

    /**
     * 🆕 사용자 상태 분석 (시뮬레이션 또는 실제 데이터)
     */
    public void analyzeUserStatus(SimulatedUserStatus userStatus) {
        if (!enabled || userStatus == null) return;

        lastUserStatus = userStatus;

        // 사용자 상태 분석
        UserStatus status = evaluateUserStatus(userStatus);

        // 콜백 호출
        if (callback != null) {
            callback.onUserStatusChanged(status);
        }

        // 최적 조건 감지 로직
        if (status.isOptimalForCalibration) {
            handleOptimalConditionDetected(userStatus);
        }

        // 통계 업데이트
        updateStatistics(userStatus);

        Log.d(TAG, String.format("사용자 상태 분석: 집중도 %.1f%%, 각성도 %.1f%%, 최적조건: %s",
                status.attentionLevel / 100.0f * 100,
                status.alertnessLevel / 100.0f * 100,
                status.isOptimalForCalibration ? "예" : "아니오"));
    }

    /**
     * 🆕 사용자 상태 종합 평가
     */
    private UserStatus evaluateUserStatus(SimulatedUserStatus userStatus) {
        // 집중도 점수 계산 (0-100)
        int attentionLevel = Math.round(userStatus.attentionScore * 100);

        // 각성도 점수 계산 (졸음의 반대)
        int alertnessLevel = Math.round((1.0f - userStatus.drowsinessIntensity) * 100);

        // 최적 조건 판단
        boolean isOptimal = isOptimalCalibrationCondition(userStatus);

        // 상태 설명 생성
        String description = generateStatusDescription(attentionLevel, alertnessLevel, userStatus.isDrowsy);

        return new UserStatus(isOptimal, alertnessLevel, attentionLevel, description);
    }

    /**
     * 🆕 최적 캘리브레이션 조건 판단
     */
    private boolean isOptimalCalibrationCondition(SimulatedUserStatus userStatus) {
        boolean highAttention = userStatus.attentionScore >= attentionThreshold;
        boolean lowDrowsiness = userStatus.drowsinessIntensity <= drowsinessThreshold;
        boolean notDrowsy = !userStatus.isDrowsy;
        boolean cooldownPassed = (System.currentTimeMillis() - lastRecommendationTime) >= cooldownPeriod;

        return highAttention && lowDrowsiness && notDrowsy && cooldownPassed;
    }

    /**
     * 🆕 최적 조건 감지 시 처리
     */
    private void handleOptimalConditionDetected(SimulatedUserStatus userStatus) {
        if (!isObserving) {
            // 관찰 시작
            observationStartTime = System.currentTimeMillis();
            isObserving = true;
            optimalConditionCount = 1;
            Log.d(TAG, "최적 조건 관찰 시작");
            return;
        }

        // 관찰 중인 경우
        long observationTime = System.currentTimeMillis() - observationStartTime;
        optimalConditionCount++;

        if (observationTime >= minimumObservationTime) {
            // 충분한 관찰 시간 후 추천 생성
            CalibrationRecommendation recommendation = generateCalibrationRecommendation(userStatus);

            if (callback != null) {
                callback.onOptimalCalibrationTimeDetected(recommendation);
            }

            // 상태 리셋
            isObserving = false;
            lastRecommendationTime = System.currentTimeMillis();

            Log.d(TAG, String.format("캘리브레이션 추천 생성: %s (신뢰도: %d%%)",
                    recommendation.recommendedMode, recommendation.confidenceLevel));
        }
    }

    /**
     * 🆕 캘리브레이션 추천 생성 (SDK 호환 모드)
     */
    private CalibrationRecommendation generateCalibrationRecommendation(SimulatedUserStatus userStatus) {
        CalibrationModeType recommendedMode;
        int confidenceLevel;
        String reason;

        // 🔧 SDK 호환성: 기존 CalibrationModeType 사용
        if (userStatus.attentionScore >= 0.9f && userStatus.drowsinessIntensity <= 0.05f) {
            // 매우 높은 집중도: 5포인트 캘리브레이션 추천 (SIX_POINT 대신)
            recommendedMode = CalibrationModeType.FIVE_POINT;
            confidenceLevel = 95;
            reason = "매우 높은 집중도와 각성 상태로 정밀 캘리브레이션에 최적";
        } else if (userStatus.attentionScore >= 0.8f && userStatus.drowsinessIntensity <= 0.1f) {
            // 높은 집중도: 표준 캘리브레이션 추천
            recommendedMode = CalibrationModeType.FIVE_POINT;
            confidenceLevel = 85;
            reason = "높은 집중도로 표준 캘리브레이션에 적합";
        } else {
            // 보통 집중도: 간단 캘리브레이션 추천
            recommendedMode = CalibrationModeType.ONE_POINT;
            confidenceLevel = 70;
            reason = "보통 집중도로 간단 캘리브레이션 권장";
        }

        // 관찰 지속성에 따른 신뢰도 보정
        float consistencyBonus = Math.min(0.1f, optimalConditionCount / 10.0f);
        confidenceLevel = Math.min(100, Math.round(confidenceLevel * (1.0f + consistencyBonus)));

        return new CalibrationRecommendation(recommendedMode, confidenceLevel, reason);
    }

    /**
     * 🆕 캘리브레이션 완료 후 품질 평가
     */
    public CalibrationQuality evaluateCalibrationQuality(SimulatedUserStatus preCalibrationStatus,
                                                         SimulatedUserStatus postCalibrationStatus,
                                                         boolean calibrationSuccess) {
        if (!enabled) {
            return new CalibrationQuality(50, false, "적응형 평가 비활성화됨");
        }

        int qualityScore = 50; // 기본 점수
        boolean needsRecalibration = false;
        String assessment;

        if (!calibrationSuccess) {
            qualityScore = 20;
            needsRecalibration = true;
            assessment = "캘리브레이션 실패 - 재시도 필요";
        } else {
            // 캘리브레이션 전후 상태 비교
            float attentionDrop = preCalibrationStatus.attentionScore - postCalibrationStatus.attentionScore;
            float drowsinessIncrease = postCalibrationStatus.drowsinessIntensity - preCalibrationStatus.drowsinessIntensity;

            // 집중도 유지 평가
            if (attentionDrop <= 0.1f) {
                qualityScore += 20; // 집중도 잘 유지됨
            } else if (attentionDrop <= 0.2f) {
                qualityScore += 10; // 약간의 집중도 감소
            } else {
                qualityScore -= 10; // 집중도 크게 감소
            }

            // 피로도 증가 평가
            if (drowsinessIncrease <= 0.1f) {
                qualityScore += 15; // 피로도 증가 최소
            } else if (drowsinessIncrease <= 0.2f) {
                qualityScore += 5; // 적당한 피로도 증가
            } else {
                qualityScore -= 15; // 과도한 피로도 증가
            }

            // 캘리브레이션 타이밍 평가
            if (preCalibrationStatus.attentionScore >= 0.8f) {
                qualityScore += 10; // 좋은 타이밍에 수행됨
            }

            // 점수 범위 제한
            qualityScore = Math.max(0, Math.min(100, qualityScore));

            // 재캘리브레이션 필요성 판단
            needsRecalibration = qualityScore < 60 || postCalibrationStatus.drowsinessIntensity > 0.3f;

            // 평가 메시지 생성
            if (qualityScore >= 80) {
                assessment = "우수한 캘리브레이션 품질";
            } else if (qualityScore >= 60) {
                assessment = "양호한 캘리브레이션 품질";
            } else {
                assessment = "캘리브레이션 품질 개선 필요";
            }

            if (needsRecalibration) {
                assessment += " - 재캘리브레이션 권장";
            }
        }

        Log.d(TAG, String.format("캘리브레이션 품질 평가: %d점, 재시도 필요: %s",
                qualityScore, needsRecalibration ? "예" : "아니오"));

        return new CalibrationQuality(qualityScore, needsRecalibration, assessment);
    }

    /**
     * 🆕 상태 설명 생성
     */
    private String generateStatusDescription(int attentionLevel, int alertnessLevel, boolean isDrowsy) {
        if (isDrowsy) {
            return "졸림 상태 - 휴식 후 캘리브레이션 권장";
        }

        if (attentionLevel >= 90 && alertnessLevel >= 90) {
            return "매우 집중된 상태 - 정밀 캘리브레이션 최적";
        } else if (attentionLevel >= 80 && alertnessLevel >= 80) {
            return "집중된 상태 - 캘리브레이션 적합";
        } else if (attentionLevel >= 60 && alertnessLevel >= 60) {
            return "보통 상태 - 간단 캘리브레이션 가능";
        } else {
            return "집중도 부족 - 잠시 후 재시도 권장";
        }
    }

    /**
     * 🆕 통계 데이터 업데이트
     */
    private void updateStatistics(SimulatedUserStatus userStatus) {
        totalObservationCount++;

        // 이동 평균으로 평균 집중도 계산
        float alpha = 0.1f; // 학습률
        averageAttentionScore = averageAttentionScore * (1 - alpha) + userStatus.attentionScore * alpha;
    }

    // 설정 메서드들
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Log.d(TAG, "적응형 캘리브레이션 " + (enabled ? "활성화" : "비활성화"));
    }

    public void setAttentionThreshold(float threshold) {
        this.attentionThreshold = Math.max(0.5f, Math.min(1.0f, threshold));
        Log.d(TAG, "집중도 임계값 설정: " + this.attentionThreshold);
    }

    public void setDrowsinessThreshold(float threshold) {
        this.drowsinessThreshold = Math.max(0.0f, Math.min(0.5f, threshold));
        Log.d(TAG, "졸음 임계값 설정: " + this.drowsinessThreshold);
    }

    public void setMinimumObservationTime(int timeMs) {
        this.minimumObservationTime = Math.max(5000, timeMs); // 최소 5초
        Log.d(TAG, "최소 관찰 시간 설정: " + this.minimumObservationTime + "ms");
    }

    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
        Log.d(TAG, "시뮬레이션 모드 " + (simulationMode ? "활성화" : "비활성화"));
    }

    // 상태 조회 메서드들
    public boolean isEnabled() { return enabled; }
    public float getAttentionThreshold() { return attentionThreshold; }
    public float getDrowsinessThreshold() { return drowsinessThreshold; }
    public float getAverageAttentionScore() { return averageAttentionScore; }
    public int getTotalObservationCount() { return totalObservationCount; }
    public SimulatedUserStatus getLastUserStatus() { return lastUserStatus; }
    public boolean isSimulationMode() { return simulationMode; }

    /**
     * 🆕 사용자 상태 기반 권장 설정 제공 (SDK 호환 버전)
     */
    public UserSettings.Builder getRecommendedSettings(SimulatedUserStatus userStatus) {
        UserSettings.Builder builder = new UserSettings.Builder();

        if (userStatus == null) {
            return builder; // 기본 설정 반환
        }

        // 집중도에 따른 캘리브레이션 전략 추천
        if (userStatus.attentionScore >= 0.9f) {
            builder.calibrationStrategy(UserSettings.CalibrationStrategy.PRECISION);
        } else if (userStatus.attentionScore >= 0.7f) {
            builder.calibrationStrategy(UserSettings.CalibrationStrategy.BALANCED);
        } else {
            builder.calibrationStrategy(UserSettings.CalibrationStrategy.QUICK_START);
        }

        // 피로도에 따른 성능 모드 추천
        if (userStatus.drowsinessIntensity > 0.2f || userStatus.isDrowsy) {
            builder.performanceMode(UserSettings.PerformanceMode.POWER_SAVING);
        } else if (userStatus.attentionScore >= 0.8f) {
            builder.performanceMode(UserSettings.PerformanceMode.PERFORMANCE);
        } else {
            builder.performanceMode(UserSettings.PerformanceMode.BALANCED);
        }

        Log.d(TAG, String.format("권장 설정 생성: 집중도 %.2f, 졸음 %.2f",
                userStatus.attentionScore, userStatus.drowsinessIntensity));

        return builder;
    }

    /**
     * 🆕 디버그 정보 출력
     */
    public String getDebugInfo() {
        if (lastUserStatus == null) {
            return "사용자 상태 데이터 없음";
        }

        return String.format(
                "적응형 캘리브레이션 상태:\n" +
                        "- 활성화: %s\n" +
                        "- 시뮬레이션 모드: %s\n" +
                        "- 관찰 중: %s\n" +
                        "- 평균 집중도: %.2f\n" +
                        "- 총 관찰 횟수: %d\n" +
                        "- 마지막 집중도: %.2f\n" +
                        "- 마지막 졸음 정도: %.2f\n" +
                        "- 최적 조건 횟수: %d",
                enabled ? "예" : "아니오",
                simulationMode ? "예" : "아니오",
                isObserving ? "예" : "아니오",
                averageAttentionScore,
                totalObservationCount,
                lastUserStatus.attentionScore,
                lastUserStatus.drowsinessIntensity,
                optimalConditionCount
        );
    }
}