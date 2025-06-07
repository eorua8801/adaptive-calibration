package camp.visual.android.sdk.sample.domain.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 🆕 시스템 성능 모니터링 클래스
 * 배터리, CPU, 메모리 상태를 모니터링하여 동적 FPS 조정에 활용
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";

    private Context context;
    private Handler handler;
    private PerformanceCallback callback;
    private boolean isMonitoring = false;

    // 모니터링 주기 (5초마다)
    private static final long MONITORING_INTERVAL_MS = 5000;

    // 성능 임계값들
    private static final int BATTERY_LOW_THRESHOLD = 20;
    private static final int BATTERY_CRITICAL_THRESHOLD = 10;
    private static final float CPU_HIGH_THRESHOLD = 70.0f;
    private static final float CPU_CRITICAL_THRESHOLD = 85.0f;
    private static final long MEMORY_LOW_THRESHOLD_MB = 200;
    private static final long MEMORY_CRITICAL_THRESHOLD_MB = 100;

    // 최근 성능 데이터
    private int lastBatteryLevel = 100;
    private float lastCpuUsage = 0f;
    private long lastAvailableMemoryMB = 0;
    private boolean lastIsCharging = false;

    public interface PerformanceCallback {
        void onPerformanceChanged(PerformanceMetrics metrics);
        void onPerformanceAlert(AlertType alertType, PerformanceMetrics metrics);
    }

    public enum AlertType {
        BATTERY_LOW,
        BATTERY_CRITICAL,
        CPU_HIGH,
        CPU_CRITICAL,
        MEMORY_LOW,
        MEMORY_CRITICAL
    }

    public static class PerformanceMetrics {
        public final int batteryLevel;
        public final boolean isCharging;
        public final float cpuUsage;
        public final long availableMemoryMB;
        public final long totalMemoryMB;
        public final long timestamp;

        public PerformanceMetrics(int batteryLevel, boolean isCharging, float cpuUsage,
                                  long availableMemoryMB, long totalMemoryMB) {
            this.batteryLevel = batteryLevel;
            this.isCharging = isCharging;
            this.cpuUsage = cpuUsage;
            this.availableMemoryMB = availableMemoryMB;
            this.totalMemoryMB = totalMemoryMB;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("배터리: %d%% (%s), CPU: %.1f%%, 메모리: %dMB/%dMB",
                    batteryLevel, isCharging ? "충전중" : "방전중", cpuUsage, availableMemoryMB, totalMemoryMB);
        }
    }

    public PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void setCallback(PerformanceCallback callback) {
        this.callback = callback;
    }

    public void startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "이미 모니터링 중입니다");
            return;
        }

        isMonitoring = true;
        handler.post(monitoringRunnable);
        Log.d(TAG, "성능 모니터링 시작 (주기: " + MONITORING_INTERVAL_MS + "ms)");
    }

    public void stopMonitoring() {
        isMonitoring = false;
        handler.removeCallbacks(monitoringRunnable);
        Log.d(TAG, "성능 모니터링 중지");
    }

    private final Runnable monitoringRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMonitoring) return;

            try {
                PerformanceMetrics metrics = collectPerformanceMetrics();

                // 성능 변화 체크 및 콜백 호출
                if (callback != null) {
                    callback.onPerformanceChanged(metrics);
                    checkForAlerts(metrics);
                }

                // 로그 출력 (너무 자주 출력하지 않도록 제한)
                if (hasSignificantChange(metrics)) {
                    Log.d(TAG, "성능 상태: " + metrics.toString());
                    updateLastMetrics(metrics);
                }

            } catch (Exception e) {
                Log.e(TAG, "성능 모니터링 중 오류: " + e.getMessage());
            }

            // 다음 모니터링 예약
            handler.postDelayed(this, MONITORING_INTERVAL_MS);
        }
    };

    private PerformanceMetrics collectPerformanceMetrics() {
        int batteryLevel = getBatteryLevel();
        boolean isCharging = isCharging();
        float cpuUsage = getCpuUsage();
        long availableMemoryMB = getAvailableMemoryMB();
        long totalMemoryMB = getTotalMemoryMB();

        return new PerformanceMetrics(batteryLevel, isCharging, cpuUsage, availableMemoryMB, totalMemoryMB);
    }

    private int getBatteryLevel() {
        try {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    return (int) ((level / (float) scale) * 100);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "배터리 레벨 조회 실패: " + e.getMessage());
        }
        return lastBatteryLevel; // 실패 시 이전 값 반환
    }

    private boolean isCharging() {
        try {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
            }
        } catch (Exception e) {
            Log.w(TAG, "충전 상태 조회 실패: " + e.getMessage());
        }
        return lastIsCharging;
    }

    private float getCpuUsage() {
        try {
            // /proc/stat에서 CPU 사용률 계산 (간단한 근사치)
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String cpuLine = reader.readLine();
            reader.close();

            if (cpuLine != null && cpuLine.startsWith("cpu ")) {
                String[] tokens = cpuLine.split("\\s+");
                if (tokens.length >= 5) {
                    long idle = Long.parseLong(tokens[4]);
                    long total = 0;
                    for (int i = 1; i < tokens.length; i++) {
                        total += Long.parseLong(tokens[i]);
                    }

                    // 간단한 CPU 사용률 근사치 (정확하지 않지만 경향성 파악 가능)
                    float usage = ((float)(total - idle) / total) * 100;
                    return Math.max(0, Math.min(100, usage));
                }
            }
        } catch (IOException | NumberFormatException e) {
            Log.w(TAG, "CPU 사용률 조회 실패: " + e.getMessage());
        }
        return lastCpuUsage; // 실패 시 이전 값 반환
    }

    private long getAvailableMemoryMB() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.availMem / (1024 * 1024); // MB로 변환
        } catch (Exception e) {
            Log.w(TAG, "사용 가능한 메모리 조회 실패: " + e.getMessage());
            return lastAvailableMemoryMB;
        }
    }

    private long getTotalMemoryMB() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.totalMem / (1024 * 1024); // MB로 변환
        } catch (Exception e) {
            Log.w(TAG, "전체 메모리 조회 실패: " + e.getMessage());
            return 2048; // 기본값 2GB
        }
    }

    private void checkForAlerts(PerformanceMetrics metrics) {
        // 배터리 알림
        if (!metrics.isCharging) {
            if (metrics.batteryLevel <= BATTERY_CRITICAL_THRESHOLD) {
                callback.onPerformanceAlert(AlertType.BATTERY_CRITICAL, metrics);
            } else if (metrics.batteryLevel <= BATTERY_LOW_THRESHOLD) {
                callback.onPerformanceAlert(AlertType.BATTERY_LOW, metrics);
            }
        }

        // CPU 알림
        if (metrics.cpuUsage >= CPU_CRITICAL_THRESHOLD) {
            callback.onPerformanceAlert(AlertType.CPU_CRITICAL, metrics);
        } else if (metrics.cpuUsage >= CPU_HIGH_THRESHOLD) {
            callback.onPerformanceAlert(AlertType.CPU_HIGH, metrics);
        }

        // 메모리 알림
        if (metrics.availableMemoryMB <= MEMORY_CRITICAL_THRESHOLD_MB) {
            callback.onPerformanceAlert(AlertType.MEMORY_CRITICAL, metrics);
        } else if (metrics.availableMemoryMB <= MEMORY_LOW_THRESHOLD_MB) {
            callback.onPerformanceAlert(AlertType.MEMORY_LOW, metrics);
        }
    }

    private boolean hasSignificantChange(PerformanceMetrics metrics) {
        boolean batteryChanged = Math.abs(metrics.batteryLevel - lastBatteryLevel) >= 5;
        boolean cpuChanged = Math.abs(metrics.cpuUsage - lastCpuUsage) >= 10;
        boolean memoryChanged = Math.abs(metrics.availableMemoryMB - lastAvailableMemoryMB) >= 50;
        boolean chargingChanged = metrics.isCharging != lastIsCharging;

        return batteryChanged || cpuChanged || memoryChanged || chargingChanged;
    }

    private void updateLastMetrics(PerformanceMetrics metrics) {
        lastBatteryLevel = metrics.batteryLevel;
        lastCpuUsage = metrics.cpuUsage;
        lastAvailableMemoryMB = metrics.availableMemoryMB;
        lastIsCharging = metrics.isCharging;
    }

    // 현재 성능 상태를 즉시 조회하는 메서드
    public PerformanceMetrics getCurrentMetrics() {
        return collectPerformanceMetrics();
    }

    // 성능 등급 반환 (FPS 조정에 활용)
    public int getPerformanceGrade() {
        PerformanceMetrics metrics = getCurrentMetrics();

        int grade = 100; // 최고 성능에서 시작

        // 배터리 상태에 따른 차감
        if (!metrics.isCharging) {
            if (metrics.batteryLevel <= 10) grade -= 40;
            else if (metrics.batteryLevel <= 20) grade -= 25;
            else if (metrics.batteryLevel <= 50) grade -= 15;
        }

        // CPU 사용률에 따른 차감
        if (metrics.cpuUsage >= 85) grade -= 30;
        else if (metrics.cpuUsage >= 70) grade -= 20;
        else if (metrics.cpuUsage >= 50) grade -= 10;

        // 메모리 사용률에 따른 차감
        if (metrics.availableMemoryMB <= 100) grade -= 20;
        else if (metrics.availableMemoryMB <= 200) grade -= 10;

        return Math.max(10, Math.min(100, grade)); // 10-100 범위로 제한
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }
}