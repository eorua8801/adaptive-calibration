package camp.visual.android.sdk.sample.core.utils;

import android.os.Handler;
import android.os.Looper;
import camp.visual.android.sdk.sample.core.constants.AppConstants;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚡ UI 업데이트 최적화 매니저
 * - 60fps 유지를 위한 업데이트 간격 제어
 * - 중복 업데이트 방지
 * - 배치 업데이트 지원
 * - 메모리 효율적인 콜백 관리
 */
public final class ThrottledUIUpdater {
    
    private final Handler mainHandler;
    private final ConcurrentHashMap<String, Long> lastUpdateTimes;
    private final ConcurrentHashMap<String, Runnable> pendingUpdates;
    private final long defaultUpdateInterval;
    
    // 싱글톤 인스턴스
    private static volatile ThrottledUIUpdater instance;
    
    private ThrottledUIUpdater() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.lastUpdateTimes = new ConcurrentHashMap<>();
        this.pendingUpdates = new ConcurrentHashMap<>();
        this.defaultUpdateInterval = AppConstants.UI.UI_UPDATE_INTERVAL_MS;
    }
    
    public static ThrottledUIUpdater getInstance() {
        if (instance == null) {
            synchronized (ThrottledUIUpdater.class) {
                if (instance == null) {
                    instance = new ThrottledUIUpdater();
                }
            }
        }
        return instance;
    }
    
    /**
     * ⚡ 기본 간격으로 UI 업데이트 (60fps)
     */
    public void updateIfNeeded(String key, Runnable updateAction) {
        updateIfNeeded(key, updateAction, defaultUpdateInterval);
    }
    
    /**
     * ⚡ 사용자 정의 간격으로 UI 업데이트
     */
    public void updateIfNeeded(String key, Runnable updateAction, long intervalMs) {
        if (updateAction == null) return;
        
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastUpdateTimes.get(key);
        
        if (lastTime == null || currentTime - lastTime >= intervalMs) {
            // 즉시 실행
            executeOnMainThread(updateAction);
            lastUpdateTimes.put(key, currentTime);
            
            // 대기 중인 업데이트 제거
            pendingUpdates.remove(key);
        } else {
            // 지연 실행 (이전 대기 중인 업데이트는 취소)
            Runnable previousUpdate = pendingUpdates.put(key, updateAction);
            if (previousUpdate != null) {
                mainHandler.removeCallbacks(previousUpdate);
            }
            
            long delay = intervalMs - (currentTime - lastTime);
            mainHandler.postDelayed(() -> {
                Runnable pendingUpdate = pendingUpdates.remove(key);
                if (pendingUpdate != null) {
                    executeOnMainThread(pendingUpdate);
                    lastUpdateTimes.put(key, System.currentTimeMillis());
                }
            }, delay);
        }
    }
    
    /**
     * 🎯 시선 커서 업데이트 (고빈도)
     */
    public void updateCursorPosition(float x, float y, CursorUpdateCallback callback) {
        String key = "cursor_position";
        updateIfNeeded(key, () -> {
            if (callback != null) {
                callback.onUpdateCursor(x, y);
            }
        }, 16L); // 60fps for smooth cursor movement
    }
    
    /**
     * 📊 진행률 업데이트
     */
    public void updateProgress(String progressKey, float progress, ProgressUpdateCallback callback) {
        updateIfNeeded(progressKey + "_progress", () -> {
            if (callback != null) {
                callback.onUpdateProgress(progress);
            }
        }, 50L); // 20fps for progress updates
    }
    
    /**
     * 📝 텍스트 업데이트
     */
    public void updateText(String textKey, String text, TextUpdateCallback callback) {
        updateIfNeeded(textKey + "_text", () -> {
            if (callback != null) {
                callback.onUpdateText(text);
            }
        }, 100L); // 10fps for text updates
    }
    
    /**
     * 🎨 배치 UI 업데이트 (여러 UI 요소를 한번에)
     */
    public void batchUpdate(String batchKey, BatchUpdateCallback callback) {
        updateIfNeeded(batchKey + "_batch", () -> {
            if (callback != null) {
                callback.onBatchUpdate();
            }
        });
    }
    
    /**
     * ⚡ 메인 스레드에서 실행 보장
     */
    public void executeOnMainThread(Runnable action) {
        if (action == null) return;
        
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 이미 메인 스레드인 경우 즉시 실행
            try {
                action.run();
            } catch (Exception e) {
                PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                    "UI update action failed", e);
            }
        } else {
            // 메인 스레드로 전달
            mainHandler.post(() -> {
                try {
                    action.run();
                } catch (Exception e) {
                    PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                        "UI update action failed on main thread", e);
                }
            });
        }
    }
    
    /**
     * ⏰ 지연 실행
     */
    public void executeDelayed(Runnable action, long delayMs) {
        if (action == null) return;
        
        mainHandler.postDelayed(() -> {
            try {
                action.run();
            } catch (Exception e) {
                PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                    "Delayed action failed", e);
            }
        }, delayMs);
    }
    
    /**
     * 🚫 특정 키의 업데이트 취소
     */
    public void cancelUpdate(String key) {
        Runnable pendingUpdate = pendingUpdates.remove(key);
        if (pendingUpdate != null) {
            mainHandler.removeCallbacks(pendingUpdate);
            PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                "Cancelled update: " + key);
        }
    }
    
    /**
     * 🚫 모든 대기 중인 업데이트 취소
     */
    public void cancelAllUpdates() {
        for (Runnable pendingUpdate : pendingUpdates.values()) {
            if (pendingUpdate != null) {
                mainHandler.removeCallbacks(pendingUpdate);
            }
        }
        pendingUpdates.clear();
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "All pending updates cancelled");
    }
    
    /**
     * 🧹 업데이트 히스토리 정리
     */
    public void clearUpdateHistory() {
        lastUpdateTimes.clear();
        PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
            "Update history cleared");
    }
    
    /**
     * 📊 업데이트 통계
     */
    public UpdateStatistics getStatistics() {
        return new UpdateStatistics(
            lastUpdateTimes.size(),
            pendingUpdates.size()
        );
    }
    
    /**
     * 🔧 우선순위 기반 업데이트 시스템
     */
    public enum UpdatePriority {
        HIGH(8L),    // 120fps - 매우 중요한 업데이트 (사용자 입력 반응)
        NORMAL(16L), // 60fps - 일반적인 UI 업데이트
        LOW(33L),    // 30fps - 덜 중요한 업데이트 (배경 정보)
        BACKGROUND(100L); // 10fps - 백그라운드 업데이트
        
        private final long intervalMs;
        
        UpdatePriority(long intervalMs) {
            this.intervalMs = intervalMs;
        }
        
        public long getIntervalMs() {
            return intervalMs;
        }
    }
    
    /**
     * ⚡ 우선순위 기반 업데이트
     */
    public void updateWithPriority(String key, Runnable updateAction, UpdatePriority priority) {
        updateIfNeeded(key, updateAction, priority.getIntervalMs());
    }
    
    // 콜백 인터페이스들
    public interface CursorUpdateCallback {
        void onUpdateCursor(float x, float y);
    }
    
    public interface ProgressUpdateCallback {
        void onUpdateProgress(float progress);
    }
    
    public interface TextUpdateCallback {
        void onUpdateText(String text);
    }
    
    public interface BatchUpdateCallback {
        void onBatchUpdate();
    }
    
    /**
     * 📊 업데이트 통계 클래스
     */
    public static class UpdateStatistics {
        public final int totalTrackedUpdates;
        public final int pendingUpdates;
        
        public UpdateStatistics(int totalTrackedUpdates, int pendingUpdates) {
            this.totalTrackedUpdates = totalTrackedUpdates;
            this.pendingUpdates = pendingUpdates;
        }
        
        @Override
        public String toString() {
            return String.format("UpdateStats{tracked=%d, pending=%d}", 
                totalTrackedUpdates, pendingUpdates);
        }
    }
    
    /**
     * 🧹 리소스 정리 (주로 앱 종료 시)
     */
    public void cleanup() {
        cancelAllUpdates();
        clearUpdateHistory();
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "ThrottledUIUpdater cleanup completed");
    }
    
    /**
     * 📊 성능 메트릭 출력
     */
    public void logPerformanceMetrics() {
        UpdateStatistics stats = getStatistics();
        PerformanceLogger.logPerformanceMetric(
            AppConstants.Logging.TAG_PERFORMANCE, 
            "UI_UpdateStats", stats.toString());
    }
    
    /**
     * 🔄 주기적 정리 작업
     */
    public void performPeriodicCleanup() {
        // 5분 이상 된 업데이트 기록 제거
        long cleanupThreshold = System.currentTimeMillis() - 300000L; // 5분
        
        // removeIf 메서드의 반환값을 사용하여 제거된 항목 수를 직접 계산
        int sizeBefore = lastUpdateTimes.size();
        lastUpdateTimes.entrySet().removeIf(entry -> entry.getValue() < cleanupThreshold);
        int removedCount = sizeBefore - lastUpdateTimes.size();
        
        if (removedCount > 0) {
            PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                "Cleaned up " + removedCount + " old update records");
        }
    }
}
