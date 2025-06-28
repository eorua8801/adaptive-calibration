package camp.visual.android.sdk.sample.core.utils;

import android.util.Log;
import camp.visual.android.sdk.sample.core.constants.AppConstants;
import camp.visual.android.sdk.sample.core.security.SecurityManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 🚀 성능 최적화된 로거
 * - 로그 빈도 제한
 * - 보안 강화
 * - 배포/디버그 모드별 다른 동작
 * - 성능 영향 최소화
 */
public final class PerformanceLogger {
    
    // 로그 빈도 제어를 위한 타임스탬프 저장
    private static final ConcurrentHashMap<String, Long> lastLogTimes = new ConcurrentHashMap<>();
    
    /**
     * 🎯 조건부 디버그 로그 (빈도 제한 적용)
     */
    public static void logIfNeeded(String tag, String message) {
        logIfNeeded(tag, message, AppConstants.Logging.LOG_INTERVAL_MS);
    }
    
    /**
     * 🎯 조건부 디버그 로그 (사용자 정의 간격)
     */
    public static void logIfNeeded(String tag, String message, long intervalMs) {
        // 디버그 모드 (개발 단계에서는 활성화)
        final boolean DEBUG = true;
        if (!DEBUG) {
            return;
        }
        
        String key = tag + ":" + message.hashCode();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastLogTimes.get(key);
        
        if (lastTime == null || currentTime - lastTime > intervalMs) {
            Log.d(tag, message);
            lastLogTimes.put(key, currentTime);
        }
    }
    
    /**
     * 🔍 좌표 로그 (보안 적용)
     */
    public static void logCoordinates(String tag, String prefix, float x, float y) {
        final boolean DEBUG = true;
        if (DEBUG) {
            String message = prefix + ": " + SecurityManager.sanitizeCoordinates(x, y);
            logIfNeeded(tag, message);
        }
    }
    
    /**
     * ⚡ 성능 메트릭 로그
     */
    public static void logPerformanceMetric(String tag, String metricName, long value) {
        logPerformanceMetric(tag, metricName, String.valueOf(value));
    }
    
    public static void logPerformanceMetric(String tag, String metricName, float value) {
        logPerformanceMetric(tag, metricName, String.format("%.2f", value));
    }
    
    public static void logPerformanceMetric(String tag, String metricName, String value) {
        String message = String.format("[PERF] %s: %s", metricName, value);
        logIfNeeded(tag, message, AppConstants.Logging.LOG_INTERVAL_MS * 5); // 성능 로그는 더 적게
    }
    
    /**
     * 🔥 중요한 정보 로그 (항상 출력)
     */
    public static void logImportant(String tag, String message) {
        Log.i(tag, "[IMPORTANT] " + message);
    }
    
    /**
     * ⚠️ 경고 로그
     */
    public static void logWarning(String tag, String message) {
        Log.w(tag, "[WARNING] " + message);
    }
    
    /**
     * 🚨 에러 로그 (보안 적용)
     */
    public static void logError(String tag, String message) {
        SecurityManager.secureErrorLog(tag, message, null);
    }
    
    /**
     * 🚨 에러 로그 with Throwable (보안 적용)
     */
    public static void logError(String tag, String message, Throwable throwable) {
        SecurityManager.secureErrorLog(tag, message, throwable);
    }
    
    /**
     * 🎯 시선 추적 관련 로그
     */
    public static class GazeLogger {
        
        public static void logGazePoint(float x, float y, String state) {
            String message = String.format("Gaze[%s]: %s", 
                state, SecurityManager.sanitizeCoordinates(x, y));
            logIfNeeded(AppConstants.Logging.TAG_SERVICE, message);
        }
        
        public static void logCalibrationProgress(float progress, int pointIndex) {
            String message = String.format("Calibration: Point %d, Progress %.1f%%", 
                pointIndex, progress * 100);
            logIfNeeded(AppConstants.Logging.TAG_CALIBRATION, message);
        }
        
        public static void logFilterPerformance(String filterName, long processingTimeMs) {
            logPerformanceMetric(AppConstants.Logging.TAG_SERVICE, 
                filterName + "_ProcessingTime", processingTimeMs);
        }
        
        public static void logTrackingState(String trackingState, String eyeMovementState) {
            String message = String.format("Tracking: %s, EyeMovement: %s", 
                trackingState, eyeMovementState);
            logIfNeeded(AppConstants.Logging.TAG_SERVICE, message, 2000L); // 2초마다
        }
    }
    
    /**
     * 📊 성능 모니터링 로거
     */
    public static class PerformanceMetrics {
        
        public static void logFPS(int currentFPS, int targetFPS) {
            logPerformanceMetric(AppConstants.Logging.TAG_PERFORMANCE, 
                "FPS", String.format("%d/%d", currentFPS, targetFPS));
        }
        
        public static void logMemoryUsage(long usedMB, long totalMB) {
            logPerformanceMetric(AppConstants.Logging.TAG_PERFORMANCE, 
                "Memory", String.format("%dMB/%dMB", usedMB, totalMB));
        }
        
        public static void logCPUUsage(float cpuPercent) {
            logPerformanceMetric(AppConstants.Logging.TAG_PERFORMANCE, 
                "CPU", String.format("%.1f%%", cpuPercent));
        }
        
        public static void logBatteryLevel(int batteryPercent, boolean isCharging) {
            String status = isCharging ? "Charging" : "Discharging";
            logPerformanceMetric(AppConstants.Logging.TAG_PERFORMANCE, 
                "Battery", String.format("%d%% (%s)", batteryPercent, status));
        }
        
        public static void logFrameDrop(long timestamp, int consecutiveDrops) {
            String message = String.format("Frame dropped at %d (consecutive: %d)", 
                timestamp, consecutiveDrops);
            logWarning(AppConstants.Logging.TAG_PERFORMANCE, message);
        }
    }
    
    /**
     * 🔒 보안 관련 로거
     */
    public static class SecurityLogger {
        
        public static void logPermissionGranted(String permission) {
            SecurityManager.secureLog(AppConstants.Logging.TAG_SECURITY, 
                "Permission granted: " + permission);
        }
        
        public static void logPermissionDenied(String permission) {
            logWarning(AppConstants.Logging.TAG_SECURITY, 
                "Permission denied: " + permission);
        }
        
        public static void logSecurityViolation(String violation) {
            Log.e(AppConstants.Logging.TAG_SECURITY, "[SECURITY_VIOLATION] " + violation);
        }
        
        public static void logLicenseValidation(boolean isValid) {
            String message = "License validation: " + (isValid ? "SUCCESS" : "FAILED");
            if (isValid) {
                SecurityManager.secureLog(AppConstants.Logging.TAG_SECURITY, message);
            } else {
                logError(AppConstants.Logging.TAG_SECURITY, message);
            }
        }
    }
    
    /**
     * 🧹 로그 정리 메서드
     */
    public static void clearLogCache() {
        lastLogTimes.clear();
        logImportant(AppConstants.Logging.TAG_MAIN, "Log cache cleared");
    }
    
    /**
     * 📊 로그 통계 출력
     */
    public static void printLogStatistics() {
        final boolean DEBUG = true;
        if (DEBUG) {
            int totalEntries = lastLogTimes.size();
            logImportant(AppConstants.Logging.TAG_MAIN, 
                String.format("Log statistics: %d unique log entries tracked", totalEntries));
        }
    }
    
    // Private constructor to prevent instantiation
    private PerformanceLogger() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
