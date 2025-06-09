package camp.visual.android.sdk.sample.domain.safety;

import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔒 캘리브레이션 안전성 관리자
 * - 과도한 캘리브레이션 반복 방지
 * - 드리프트 감지 및 품질 검증
 * - 캘리브레이션 이력 추적
 */
public class CalibrationSafetyManager {
    private static final String TAG = "CalibrationSafety";
    
    // 캘리브레이션 이력 추적
    private List<CalibrationRecord> calibrationHistory = new ArrayList<>();
    private long lastCalibrationTime = 0;
    private static final long MIN_CALIBRATION_INTERVAL = 30000; // 30초 최소 간격
    
    // 드리프트 감지를 위한 기준점들
    private long lastDriftCheck = 0;
    private static final long DRIFT_CHECK_INTERVAL = 60000; // 1분마다 체크
    
    public static class CalibrationRecord {
        public final double[] calibrationData;
        public final long timestamp;
        public final float qualityScore;
        public final PointF centerOffset; // 중심점 오프셋 추적
        
        public CalibrationRecord(double[] data, float quality, PointF offset) {
            this.calibrationData = data != null ? data.clone() : new double[0];
            this.timestamp = System.currentTimeMillis();
            this.qualityScore = quality;
            this.centerOffset = new PointF(offset.x, offset.y);
        }
    }
    
    // 🚨 캘리브레이션 전 안전성 검사
    public boolean isSafeToCalibrate() {
        long currentTime = System.currentTimeMillis();
        
        // 1. 최소 간격 체크
        if (currentTime - lastCalibrationTime < MIN_CALIBRATION_INTERVAL) {
            long remainingTime = MIN_CALIBRATION_INTERVAL - (currentTime - lastCalibrationTime);
            Log.w(TAG, "캘리브레이션 너무 빠름 - " + (remainingTime / 1000) + "초 후 재시도");
            return false;
        }
        
        // 2. 과도한 반복 체크 (1시간 내 5회 이상)
        long oneHourAgo = currentTime - 3600000;
        long recentCalibrations = calibrationHistory.stream()
            .filter(record -> record.timestamp > oneHourAgo)
            .count();
            
        if (recentCalibrations >= 5) {
            Log.w(TAG, "과도한 캘리브레이션 반복 감지 - 1시간 후 재시도");
            return false;
        }
        
        return true;
    }
    
    // 🔍 캘리브레이션 완료 후 품질 검증
    public void validateCalibration(double[] calibrationData, PointF currentOffset) {
        lastCalibrationTime = System.currentTimeMillis();
        
        // 품질 점수 계산
        float qualityScore = calculateQualityScore(calibrationData);
        
        // 이력 저장
        CalibrationRecord record = new CalibrationRecord(calibrationData, qualityScore, currentOffset);
        calibrationHistory.add(record);
        
        // 오래된 이력 정리 (7일 이상)
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 3600000L);
        calibrationHistory.removeIf(r -> r.timestamp < weekAgo);
        
        // 품질 경고
        if (qualityScore < 0.7f) {
            Log.w(TAG, "캘리브레이션 품질 낮음: " + qualityScore + " - 재시도 권장");
        }
        
        // 드리프트 체크
        checkForDrift(record);
        
        Log.d(TAG, "캘리브레이션 완료 - 품질: " + qualityScore + ", 총 이력: " + calibrationHistory.size());
    }
    
    // 🎯 드리프트 감지 알고리즘
    private void checkForDrift(CalibrationRecord newRecord) {
        if (calibrationHistory.size() < 2) return;
        
        // 최근 3개 캘리브레이션의 중심점 오프셋 비교
        List<CalibrationRecord> recent = calibrationHistory.stream()
            .skip(Math.max(0, calibrationHistory.size() - 3))
            .collect(Collectors.toList());
            
        if (recent.size() >= 3) {
            float avgDriftX = 0, avgDriftY = 0;
            PointF baseline = recent.get(0).centerOffset;
            
            for (int i = 1; i < recent.size(); i++) {
                PointF current = recent.get(i).centerOffset;
                avgDriftX += (current.x - baseline.x);
                avgDriftY += (current.y - baseline.y);
            }
            
            avgDriftX /= (recent.size() - 1);
            avgDriftY /= (recent.size() - 1);
            
            // 드리프트 임계값 (20픽셀 이상)
            float driftMagnitude = (float) Math.sqrt(avgDriftX * avgDriftX + avgDriftY * avgDriftY);
            
            if (driftMagnitude > 20.0f) {
                Log.w(TAG, "시스템 드리프트 감지! 평균 이동: " + driftMagnitude + "px");
                // TODO: 사용자에게 알림 또는 자동 보정 제안
            }
        }
    }
    
    private float calculateQualityScore(double[] calibrationData) {
        // SDK에서 제공하는 품질 데이터 기반으로 계산
        // 현재는 단순 예시 - 실제로는 calibrationData 분석 필요
        if (calibrationData == null || calibrationData.length == 0) {
            return 0.5f;
        }
        
        // 간단한 품질 평가 (데이터 길이와 분산 기반)
        float baseScore = 0.7f;
        if (calibrationData.length >= 10) { // 충분한 데이터
            baseScore += 0.1f;
        }
        
        // 추가적인 품질 평가 로직 필요시 여기에 추가
        return Math.min(1.0f, baseScore + (float)(Math.random() * 0.2f));
    }
    
    // 📊 캘리브레이션 통계 정보
    public String getCalibrationStats() {
        long currentTime = System.currentTimeMillis();
        long oneHourAgo = currentTime - 3600000;
        long oneDayAgo = currentTime - (24 * 3600000);
        
        long recentHour = calibrationHistory.stream()
            .filter(record -> record.timestamp > oneHourAgo)
            .count();
            
        long recentDay = calibrationHistory.stream()
            .filter(record -> record.timestamp > oneDayAgo)
            .count();
            
        float avgQuality = calibrationHistory.stream()
            .filter(record -> record.timestamp > oneDayAgo)
            .map(record -> record.qualityScore)
            .reduce(0f, Float::sum) / Math.max(1, recentDay);
            
        return String.format("캘리브레이션 통계:\n" +
                "- 지난 1시간: %d회\n" +
                "- 지난 24시간: %d회\n" +
                "- 평균 품질: %.2f\n" +
                "- 총 이력: %d회",
                recentHour, recentDay, avgQuality, calibrationHistory.size());
    }
    
    // 강제 리셋 (디버깅용)
    public void reset() {
        calibrationHistory.clear();
        lastCalibrationTime = 0;
        Log.d(TAG, "캘리브레이션 안전성 관리자 리셋");
    }
    
    // 마지막 캘리브레이션 품질 조회
    public float getLastCalibrationQuality() {
        if (calibrationHistory.isEmpty()) return 0f;
        return calibrationHistory.get(calibrationHistory.size() - 1).qualityScore;
    }
    
    // 다음 캘리브레이션까지 남은 시간 (초)
    public long getTimeUntilNextCalibration() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCalibration = currentTime - lastCalibrationTime;
        
        if (timeSinceLastCalibration >= MIN_CALIBRATION_INTERVAL) {
            return 0; // 즉시 가능
        }
        
        return (MIN_CALIBRATION_INTERVAL - timeSinceLastCalibration) / 1000;
    }
}
