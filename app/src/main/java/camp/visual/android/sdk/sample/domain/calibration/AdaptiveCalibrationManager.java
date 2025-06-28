package camp.visual.android.sdk.sample.domain.calibration;

import android.util.Log;
import camp.visual.android.sdk.sample.core.constants.AppConstants;
import camp.visual.android.sdk.sample.core.utils.PerformanceLogger;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.eyedid.gazetracker.constant.AccuracyCriteria;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.metrics.UserStatusInfo;

/**
 * 🆕 적응형 캘리브레이션 관리자 (실제 UserStatusInfo 활용)
 * - 공식 SDK UserStatusInfo 사용
 * - 실시간 사용자 상태 기반 최적 캘리브레이션 타이밍 감지
 * - 피로도, 집중도, 졸음 상태 종합 분석
 * - 성능 최적화 및 보안 강화
 */
public class AdaptiveCalibrationManager {
    
    // 적응형 캘리브레이션 콜백 인터페이스
    public interface AdaptiveCalibrationCallback {
        void onOptimalCalibrationTimeDetected(CalibrationRecommendation recommendation);
        void onCalibrationQualityAssessment(CalibrationQuality quality);
        void onUserStatusChanged(UserStatus status);
    }

    // 캘리브레이션 추천 정보
    public static class CalibrationRecommendation {
        public final CalibrationModeType recommendedMode;
        public final AccuracyCriteria recommendedAccuracy;
        public final int confidenceLevel; // 0-100
        public final String reason;
        public final long timestamp;

        public CalibrationRecommendation(CalibrationModeType mode, AccuracyCriteria accuracy, 
                                       int confidence, String reason) {
            this.recommendedMode = mode;
            this.recommendedAccuracy = accuracy;
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
        public final AccuracyCriteria suggestedAccuracy;
        public final long timestamp;

        public CalibrationQuality(int score, boolean needsRecalibration, 
                                String assessment, AccuracyCriteria suggestedAccuracy) {
            this.qualityScore = score;
            this.needsRecalibration = needsRecalibration;
            this.assessment = assessment;
            this.suggestedAccuracy = suggestedAccuracy;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // 사용자 상태 분석 결과
    public static class UserStatus {
        public final boolean isOptimalForCalibration;
        public final int alertnessLevel; // 0-100
        public final int attentionLevel; // 0-100
        public final String statusDescription;
        public final CalibrationModeType suggestedMode;
        public final long timestamp;

        public UserStatus(boolean isOptimal, int alertness, int attention, 
                         String description, CalibrationModeType suggestedMode) {
            this.isOptimalForCalibration = isOptimal;
            this.alertnessLevel = alertness;
            this.attentionLevel = attention;
            this.statusDescription = description;
            this.suggestedMode = suggestedMode;
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
    private UserStatusInfo lastUserStatusInfo;
    private long lastRecommendationTime = 0;
    private long observationStartTime = 0;
    private boolean isObserving = false;

    // 통계 데이터
    private float averageAttentionScore = 0.5f;
    private int optimalConditionCount = 0;
    private int totalObservationCount = 0;
    private long lastAnalysisTime = 0;

    public AdaptiveCalibrationManager() {
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "실제 UserStatusInfo 기반 적응형 캘리브레이션 관리자 초기화");
    }

    public void setCallback(AdaptiveCalibrationCallback callback) {
        this.callback = callback;
    }

    /**
     * 🆕 실제 UserStatusInfo 분석
     * 공식 SDK에서 제공하는 실제 사용자 상태 데이터 활용
     */
    public void analyzeUserStatus(UserStatusInfo userStatusInfo) {
        if (!enabled || userStatusInfo == null) return;

        lastUserStatusInfo = userStatusInfo;
        lastAnalysisTime = System.currentTimeMillis();

        // 사용자 상태 분석
        UserStatus status = evaluateUserStatus(userStatusInfo);

        // 콜백 호출
        if (callback != null) {
            callback.onUserStatusChanged(status);
        }

        // 최적 조건 감지 로직
        if (status.isOptimalForCalibration) {
            handleOptimalConditionDetected(userStatusInfo);
        } else if (isObserving) {
            // 최적 조건이 아니면 관찰 중단
            resetObservation();
        }

        // 통계 업데이트
        updateStatistics(userStatusInfo);

        // 성능 로깅
        PerformanceLogger.GazeLogger.logTrackingState(
            "AdaptiveCalibration",
            String.format("Attention:%.1f%%, Drowsy:%s, Optimal:%s",
                userStatusInfo.attentionScore * 100,
                userStatusInfo.isDrowsy ? "Y" : "N",
                status.isOptimalForCalibration ? "Y" : "N")
        );
    }

    /**
     * 🆕 실제 UserStatusInfo 기반 사용자 상태 종합 평가
     */
    private UserStatus evaluateUserStatus(UserStatusInfo userStatusInfo) {
        // 집중도 점수 계산 (0-100)
        int attentionLevel = Math.round(userStatusInfo.attentionScore * 100);

        // 각성도 점수 계산 (졸음의 반대)
        int alertnessLevel = Math.round((1.0f - userStatusInfo.drowsinessIntensity) * 100);

        // 최적 조건 판단
        boolean isOptimal = isOptimalCalibrationCondition(userStatusInfo);

        // 상태 설명 생성
        String description = generateStatusDescription(attentionLevel, alertnessLevel, userStatusInfo.isDrowsy);

        // 추천 캘리브레이션 모드 결정
        CalibrationModeType suggestedMode = determineSuggestedCalibrationMode(userStatusInfo);

        return new UserStatus(isOptimal, alertnessLevel, attentionLevel, description, suggestedMode);
    }

    /**
     * 🆕 최적 캘리브레이션 조건 판단 (실제 데이터 기반)
     */
    private boolean isOptimalCalibrationCondition(UserStatusInfo userStatusInfo) {
        boolean highAttention = userStatusInfo.attentionScore >= attentionThreshold;
        boolean lowDrowsiness = userStatusInfo.drowsinessIntensity <= drowsinessThreshold;
        boolean notDrowsy = !userStatusInfo.isDrowsy;
        boolean cooldownPassed = (System.currentTimeMillis() - lastRecommendationTime) >= cooldownPeriod;

        return highAttention && lowDrowsiness && notDrowsy && cooldownPassed;
    }

    /**
     * 🆕 캘리브레이션 모드 추천 (실제 데이터 기반)
     */
    private CalibrationModeType determineSuggestedCalibrationMode(UserStatusInfo userStatusInfo) {
        if (userStatusInfo.isDrowsy || userStatusInfo.attentionScore < 0.5f) {
            return CalibrationModeType.ONE_POINT; // 간단한 1포인트
        } else if (userStatusInfo.attentionScore >= 0.9f && userStatusInfo.drowsinessIntensity <= 0.05f) {
            return CalibrationModeType.FIVE_POINT; // 정밀한 5포인트
        } else {
            return CalibrationModeType.DEFAULT; // 기본 모드
        }
    }

    /**
     * 🆕 최적 조건 감지 시 처리
     */
    private void handleOptimalConditionDetected(UserStatusInfo userStatusInfo) {
        if (!isObserving) {
            // 관찰 시작
            observationStartTime = System.currentTimeMillis();
            isObserving = true;
            optimalConditionCount = 1;
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
                "최적 조건 관찰 시작 - 집중도: " + (userStatusInfo.attentionScore * 100) + "%");
            return;
        }

        // 관찰 중인 경우
        long observationTime = System.currentTimeMillis() - observationStartTime;
        optimalConditionCount++;

        if (observationTime >= minimumObservationTime) {
            // 충분한 관찰 시간 후 추천 생성
            CalibrationRecommendation recommendation = generateCalibrationRecommendation(userStatusInfo);

            if (callback != null) {
                callback.onOptimalCalibrationTimeDetected(recommendation);
            }

            // 상태 리셋
            resetObservation();
            lastRecommendationTime = System.currentTimeMillis();

            PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
                String.format("캘리브레이션 추천: %s (신뢰도: %d%%, 정확도: %s)",
                    recommendation.recommendedMode, recommendation.confidenceLevel, 
                    recommendation.recommendedAccuracy));
        }
    }

    /**
     * 🆕 캘리브레이션 추천 생성 (실제 데이터 기반)
     */
    private CalibrationRecommendation generateCalibrationRecommendation(UserStatusInfo userStatusInfo) {
        CalibrationModeType recommendedMode;
        AccuracyCriteria recommendedAccuracy;
        int confidenceLevel;
        String reason;

        if (userStatusInfo.attentionScore >= 0.9f && userStatusInfo.drowsinessIntensity <= 0.05f) {
            // 매우 높은 집중도: 고정밀도 캘리브레이션
            recommendedMode = CalibrationModeType.FIVE_POINT;
            recommendedAccuracy = AccuracyCriteria.HIGH;
            confidenceLevel = 95;
            reason = "매우 높은 집중도와 각성 상태로 고정밀도 캘리브레이션에 최적";
        } else if (userStatusInfo.attentionScore >= 0.8f && userStatusInfo.drowsinessIntensity <= 0.1f) {
            // 높은 집중도: 표준 캘리브레이션
            recommendedMode = CalibrationModeType.FIVE_POINT;
            recommendedAccuracy = AccuracyCriteria.DEFAULT;
            confidenceLevel = 85;
            reason = "높은 집중도로 표준 캘리브레이션에 적합";
        } else if (userStatusInfo.attentionScore >= 0.6f && !userStatusInfo.isDrowsy) {
            // 보통 집중도: 기본 캘리브레이션
            recommendedMode = CalibrationModeType.DEFAULT;
            recommendedAccuracy = AccuracyCriteria.DEFAULT;
            confidenceLevel = 70;
            reason = "보통 집중도로 기본 캘리브레이션 권장";
        } else {
            // 낮은 집중도: 간단 캘리브레이션
            recommendedMode = CalibrationModeType.ONE_POINT;
            recommendedAccuracy = AccuracyCriteria.LOW;
            confidenceLevel = 60;
            reason = "낮은 집중도로 간단 캘리브레이션 권장";
        }

        // 관찰 지속성에 따른 신뢰도 보정
        float consistencyBonus = Math.min(0.15f, optimalConditionCount / 10.0f);
        confidenceLevel = Math.min(100, Math.round(confidenceLevel * (1.0f + consistencyBonus)));

        return new CalibrationRecommendation(recommendedMode, recommendedAccuracy, confidenceLevel, reason);
    }

    /**
     * 🆕 캘리브레이션 완료 후 품질 평가 (실제 데이터 기반)
     */
    public CalibrationQuality evaluateCalibrationQuality(UserStatusInfo preCalibrationStatus,
                                                         UserStatusInfo postCalibrationStatus,
                                                         boolean calibrationSuccess) {
        if (!enabled) {
            return new CalibrationQuality(50, false, "적응형 평가 비활성화됨", AccuracyCriteria.DEFAULT);
        }

        int qualityScore = 50; // 기본 점수
        boolean needsRecalibration = false;
        String assessment;
        AccuracyCriteria suggestedAccuracy = AccuracyCriteria.DEFAULT;

        if (!calibrationSuccess) {
            qualityScore = 20;
            needsRecalibration = true;
            assessment = "캘리브레이션 실패 - 재시도 필요";
            suggestedAccuracy = AccuracyCriteria.LOW;
        } else {
            // 캘리브레이션 전후 상태 비교
            float attentionDrop = preCalibrationStatus.attentionScore - postCalibrationStatus.attentionScore;
            float drowsinessIncrease = postCalibrationStatus.drowsinessIntensity - preCalibrationStatus.drowsinessIntensity;

            // 집중도 유지 평가
            if (attentionDrop <= 0.1f) {
                qualityScore += 25; // 집중도 잘 유지됨
            } else if (attentionDrop <= 0.2f) {
                qualityScore += 15; // 약간의 집중도 감소
            } else {
                qualityScore -= 10; // 집중도 크게 감소
            }

            // 피로도 증가 평가
            if (drowsinessIncrease <= 0.1f) {
                qualityScore += 20; // 피로도 증가 최소
            } else if (drowsinessIncrease <= 0.2f) {
                qualityScore += 10; // 적당한 피로도 증가
            } else {
                qualityScore -= 15; // 과도한 피로도 증가
            }

            // 캘리브레이션 타이밍 평가
            if (preCalibrationStatus.attentionScore >= 0.8f) {
                qualityScore += 15; // 좋은 타이밍에 수행됨
            }

            // 졸음 상태 체크
            if (postCalibrationStatus.isDrowsy) {
                qualityScore -= 20;
                needsRecalibration = true;
            }

            // 점수 범위 제한
            qualityScore = Math.max(0, Math.min(100, qualityScore));

            // 재캘리브레이션 필요성 판단
            needsRecalibration = qualityScore < 60 || postCalibrationStatus.drowsinessIntensity > 0.3f;

            // 정확도 추천
            if (qualityScore >= 80 && postCalibrationStatus.attentionScore >= 0.8f) {
                suggestedAccuracy = AccuracyCriteria.HIGH;
            } else if (qualityScore < 50 || postCalibrationStatus.isDrowsy) {
                suggestedAccuracy = AccuracyCriteria.LOW;
            }

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

        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            String.format("캘리브레이션 품질 평가: %d점, 재시도 필요: %s, 권장 정확도: %s",
                qualityScore, needsRecalibration ? "예" : "아니오", suggestedAccuracy));

        return new CalibrationQuality(qualityScore, needsRecalibration, assessment, suggestedAccuracy);
    }

    /**
     * 🆕 상태 설명 생성
     */
    private String generateStatusDescription(int attentionLevel, int alertnessLevel, boolean isDrowsy) {
        if (isDrowsy) {
            return "졸림 상태 - 휴식 후 캘리브레이션 권장";
        }

        if (attentionLevel >= 90 && alertnessLevel >= 90) {
            return "매우 집중된 상태 - 고정밀도 캘리브레이션 최적";
        } else if (attentionLevel >= 80 && alertnessLevel >= 80) {
            return "집중된 상태 - 표준 캘리브레이션 적합";
        } else if (attentionLevel >= 60 && alertnessLevel >= 60) {
            return "보통 상태 - 기본 캘리브레이션 가능";
        } else {
            return "집중도 부족 - 간단 캘리브레이션 또는 휴식 권장";
        }
    }

    /**
     * 🆕 통계 데이터 업데이트
     */
    private void updateStatistics(UserStatusInfo userStatusInfo) {
        totalObservationCount++;

        // 이동 평균으로 평균 집중도 계산
        float alpha = 0.1f; // 학습률
        averageAttentionScore = averageAttentionScore * (1 - alpha) + userStatusInfo.attentionScore * alpha;
    }

    /**
     * 🧹 관찰 상태 리셋
     */
    private void resetObservation() {
        isObserving = false;
        observationStartTime = 0;
        optimalConditionCount = 0;
    }

    // 설정 메서드들
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "적응형 캘리브레이션 " + (enabled ? "활성화" : "비활성화"));
    }

    public void setAttentionThreshold(float threshold) {
        this.attentionThreshold = Math.max(0.5f, Math.min(1.0f, threshold));
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "집중도 임계값 설정: " + this.attentionThreshold);
    }

    public void setDrowsinessThreshold(float threshold) {
        this.drowsinessThreshold = Math.max(0.0f, Math.min(0.5f, threshold));
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "졸음 임계값 설정: " + this.drowsinessThreshold);
    }

    public void setMinimumObservationTime(int timeMs) {
        this.minimumObservationTime = Math.max(5000, timeMs); // 최소 5초
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "최소 관찰 시간 설정: " + this.minimumObservationTime + "ms");
    }

    // 상태 조회 메서드들
    public boolean isEnabled() { return enabled; }
    public float getAttentionThreshold() { return attentionThreshold; }
    public float getDrowsinessThreshold() { return drowsinessThreshold; }
    public float getAverageAttentionScore() { return averageAttentionScore; }
    public int getTotalObservationCount() { return totalObservationCount; }
    public UserStatusInfo getLastUserStatusInfo() { return lastUserStatusInfo; }
    public boolean isObserving() { return isObserving; }

    /**
     * 🆕 사용자 상태 기반 권장 설정 제공 (실제 데이터 기반)
     */
    public UserSettings.Builder getRecommendedSettings(UserStatusInfo userStatusInfo) {
        UserSettings.Builder builder = new UserSettings.Builder();

        if (userStatusInfo == null) {
            return builder; // 기본 설정 반환
        }

        // 집중도에 따른 캘리브레이션 전략 추천
        if (userStatusInfo.attentionScore >= 0.9f) {
            builder.calibrationStrategy(UserSettings.CalibrationStrategy.PRECISION);
        } else if (userStatusInfo.attentionScore >= 0.7f) {
            builder.calibrationStrategy(UserSettings.CalibrationStrategy.BALANCED);
        } else {
            builder.calibrationStrategy(UserSettings.CalibrationStrategy.QUICK_START);
        }

        // 피로도에 따른 성능 모드 추천
        if (userStatusInfo.drowsinessIntensity > 0.2f || userStatusInfo.isDrowsy) {
            builder.performanceMode(UserSettings.PerformanceMode.POWER_SAVING);
        } else if (userStatusInfo.attentionScore >= 0.8f) {
            builder.performanceMode(UserSettings.PerformanceMode.PERFORMANCE);
        } else {
            builder.performanceMode(UserSettings.PerformanceMode.BALANCED);
        }

        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            String.format("권장 설정 생성: 집중도 %.2f, 졸음 %.2f",
                userStatusInfo.attentionScore, userStatusInfo.drowsinessIntensity));

        return builder;
    }

    /**
     * 🆕 디버그 정보 출력
     */
    public String getDebugInfo() {
        if (lastUserStatusInfo == null) {
            return "사용자 상태 데이터 없음";
        }

        return String.format(
                "적응형 캘리브레이션 상태:\n" +
                        "- 활성화: %s\n" +
                        "- 관찰 중: %s\n" +
                        "- 평균 집중도: %.2f\n" +
                        "- 총 관찰 횟수: %d\n" +
                        "- 마지막 집중도: %.2f\n" +
                        "- 마지막 졸음 정도: %.2f\n" +
                        "- 졸림 상태: %s\n" +
                        "- 최적 조건 횟수: %d\n" +
                        "- 마지막 분석 시간: %d초 전",
                enabled ? "예" : "아니오",
                isObserving ? "예" : "아니오",
                averageAttentionScore,
                totalObservationCount,
                lastUserStatusInfo.attentionScore,
                lastUserStatusInfo.drowsinessIntensity,
                lastUserStatusInfo.isDrowsy ? "예" : "아니오",
                optimalConditionCount,
                (System.currentTimeMillis() - lastAnalysisTime) / 1000
        );
    }

    /**
     * 🧹 리소스 정리
     */
    public void cleanup() {
        callback = null;
        resetObservation();
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "AdaptiveCalibrationManager 정리 완료");
    }
}
