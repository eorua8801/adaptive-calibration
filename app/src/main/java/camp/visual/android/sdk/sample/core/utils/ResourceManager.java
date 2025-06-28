package camp.visual.android.sdk.sample.core.utils;

import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import camp.visual.android.sdk.sample.core.constants.AppConstants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 🧹 리소스 관리자
 * - WeakReference 사용으로 메모리 누수 방지
 * - 자동 리소스 정리
 * - Handler, View, Listener 등 관리
 */
public final class ResourceManager {
    
    private final List<WeakReference<Handler>> handlers = new CopyOnWriteArrayList<>();
    private final List<WeakReference<View>> overlayViews = new CopyOnWriteArrayList<>();
    private final List<WeakReference<Object>> listeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> cleanupTasks = new CopyOnWriteArrayList<>();
    
    private WeakReference<WindowManager> windowManagerRef;
    private boolean isDestroyed = false;
    
    /**
     * 🔧 Handler 등록
     */
    public void registerHandler(Handler handler) {
        if (handler != null && !isDestroyed) {
            handlers.add(new WeakReference<>(handler));
            PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                "Handler registered, total: " + handlers.size());
        }
    }
    
    /**
     * 🖼️ 오버레이 뷰 등록
     */
    public void registerOverlayView(View view, WindowManager windowManager) {
        if (view != null && windowManager != null && !isDestroyed) {
            overlayViews.add(new WeakReference<>(view));
            this.windowManagerRef = new WeakReference<>(windowManager);
            PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                "Overlay view registered, total: " + overlayViews.size());
        }
    }
    
    /**
     * 🎧 리스너 등록
     */
    public void registerListener(Object listener) {
        if (listener != null && !isDestroyed) {
            listeners.add(new WeakReference<>(listener));
            PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                "Listener registered, total: " + listeners.size());
        }
    }
    
    /**
     * 🧹 정리 작업 등록
     */
    public void registerCleanupTask(Runnable cleanupTask) {
        if (cleanupTask != null && !isDestroyed) {
            cleanupTasks.add(cleanupTask);
        }
    }
    
    /**
     * 🧹 모든 Handler 정리
     */
    public void cleanupHandlers() {
        int cleanedCount = 0;
        Iterator<WeakReference<Handler>> iterator = handlers.iterator();
        
        while (iterator.hasNext()) {
            WeakReference<Handler> handlerRef = iterator.next();
            Handler handler = handlerRef.get();
            
            if (handler != null) {
                try {
                    handler.removeCallbacksAndMessages(null);
                    cleanedCount++;
                } catch (Exception e) {
                    PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                        "Handler cleanup error", e);
                }
            }
            iterator.remove();
        }
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "Cleaned up " + cleanedCount + " handlers");
    }
    
    /**
     * 🖼️ 모든 오버레이 뷰 정리
     */
    public void cleanupOverlayViews() {
        WindowManager windowManager = windowManagerRef != null ? windowManagerRef.get() : null;
        if (windowManager == null) {
            PerformanceLogger.logWarning(AppConstants.Logging.TAG_MAIN, 
                "WindowManager is null, cannot cleanup overlay views");
            return;
        }
        
        int cleanedCount = 0;
        Iterator<WeakReference<View>> iterator = overlayViews.iterator();
        
        while (iterator.hasNext()) {
            WeakReference<View> viewRef = iterator.next();
            View view = viewRef.get();
            
            if (view != null) {
                try {
                    windowManager.removeView(view);
                    cleanedCount++;
                } catch (IllegalArgumentException e) {
                    // 이미 제거된 뷰는 무시
                    PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                        "View already removed: " + e.getMessage());
                } catch (Exception e) {
                    PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                        "Overlay view cleanup error", e);
                }
            }
            iterator.remove();
        }
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "Cleaned up " + cleanedCount + " overlay views");
    }
    
    /**
     * 🎧 모든 리스너 정리
     */
    public void cleanupListeners() {
        int totalCount = listeners.size();
        listeners.clear();
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "Cleaned up " + totalCount + " listeners");
    }
    
    /**
     * 🧹 사용자 정의 정리 작업 실행
     */
    public void executeCleanupTasks() {
        int executedCount = 0;
        
        for (Runnable task : cleanupTasks) {
            try {
                task.run();
                executedCount++;
            } catch (Exception e) {
                PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                    "Cleanup task execution error", e);
            }
        }
        
        cleanupTasks.clear();
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "Executed " + executedCount + " cleanup tasks");
    }
    
    /**
     * 🧹 약한 참조들 정리 (GC된 객체들 제거)
     */
    public void cleanupWeakReferences() {
        int beforeHandlers = handlers.size();
        int beforeViews = overlayViews.size();
        int beforeListeners = listeners.size();
        
        // GC된 객체들의 WeakReference 제거
        handlers.removeIf(ref -> ref.get() == null);
        overlayViews.removeIf(ref -> ref.get() == null);
        listeners.removeIf(ref -> ref.get() == null);
        
        int cleanedHandlers = beforeHandlers - handlers.size();
        int cleanedViews = beforeViews - overlayViews.size();
        int cleanedListeners = beforeListeners - listeners.size();
        
        if (cleanedHandlers > 0 || cleanedViews > 0 || cleanedListeners > 0) {
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                String.format("Cleaned weak references - Handlers: %d, Views: %d, Listeners: %d",
                    cleanedHandlers, cleanedViews, cleanedListeners));
        }
    }
    
    /**
     * 🧹 전체 정리 (주로 onDestroy에서 호출)
     */
    public void cleanupAll() {
        if (isDestroyed) {
            PerformanceLogger.logWarning(AppConstants.Logging.TAG_MAIN, 
                "ResourceManager already destroyed");
            return;
        }
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "Starting complete resource cleanup");
        
        try {
            // 1. 사용자 정의 정리 작업 먼저 실행
            executeCleanupTasks();
            
            // 2. Handler 정리
            cleanupHandlers();
            
            // 3. 오버레이 뷰 정리
            cleanupOverlayViews();
            
            // 4. 리스너 정리
            cleanupListeners();
            
            // 5. WeakReference 정리
            cleanupWeakReferences();
            
            // 6. WindowManager 참조 정리
            if (windowManagerRef != null) {
                windowManagerRef.clear();
                windowManagerRef = null;
            }
            
            isDestroyed = true;
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                "Resource cleanup completed successfully");
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Error during resource cleanup", e);
        }
    }
    
    /**
     * 📊 리소스 상태 확인
     */
    public ResourceStatus getResourceStatus() {
        // 살아있는 참조들만 카운트
        int aliveHandlers = 0;
        int aliveViews = 0;
        int aliveListeners = 0;
        
        for (WeakReference<Handler> ref : handlers) {
            if (ref.get() != null) aliveHandlers++;
        }
        
        for (WeakReference<View> ref : overlayViews) {
            if (ref.get() != null) aliveViews++;
        }
        
        for (WeakReference<Object> ref : listeners) {
            if (ref.get() != null) aliveListeners++;
        }
        
        return new ResourceStatus(aliveHandlers, aliveViews, aliveListeners, 
            cleanupTasks.size(), isDestroyed);
    }
    
    /**
     * 📊 리소스 상태 정보 클래스
     */
    public static class ResourceStatus {
        public final int aliveHandlers;
        public final int aliveViews;
        public final int aliveListeners;
        public final int pendingCleanupTasks;
        public final boolean isDestroyed;
        
        public ResourceStatus(int aliveHandlers, int aliveViews, int aliveListeners, 
                            int pendingCleanupTasks, boolean isDestroyed) {
            this.aliveHandlers = aliveHandlers;
            this.aliveViews = aliveViews;
            this.aliveListeners = aliveListeners;
            this.pendingCleanupTasks = pendingCleanupTasks;
            this.isDestroyed = isDestroyed;
        }
        
        @Override
        public String toString() {
            return String.format("ResourceStatus{handlers=%d, views=%d, listeners=%d, " +
                    "cleanupTasks=%d, destroyed=%s}", 
                    aliveHandlers, aliveViews, aliveListeners, pendingCleanupTasks, isDestroyed);
        }
    }
    
    /**
     * 🔍 디버그용 상태 출력
     */
    public void printStatus() {
        ResourceStatus status = getResourceStatus();
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "ResourceManager status: " + status.toString());
    }
    
    /**
     * 🔄 주기적 정리 (메모리 최적화)
     */
    public void performPeriodicCleanup() {
        if (!isDestroyed) {
            cleanupWeakReferences();
            PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                "Periodic cleanup performed");
        }
    }
    
    /**
     * 🚨 응급 정리 (OutOfMemoryError 등의 상황에서)
     */
    public void performEmergencyCleanup() {
        PerformanceLogger.logWarning(AppConstants.Logging.TAG_MAIN, 
            "Emergency cleanup initiated");
        
        try {
            // 강제로 모든 Handler 콜백 제거
            for (WeakReference<Handler> handlerRef : handlers) {
                Handler handler = handlerRef.get();
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }
            }
            
            // 약한 참조들 즉시 정리
            cleanupWeakReferences();
            
            // GC 제안
            System.gc();
            
            PerformanceLogger.logWarning(AppConstants.Logging.TAG_MAIN, 
                "Emergency cleanup completed");
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Emergency cleanup failed", e);
        }
    }
}
