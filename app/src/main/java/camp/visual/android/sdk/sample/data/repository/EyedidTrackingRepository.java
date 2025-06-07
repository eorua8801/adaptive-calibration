package camp.visual.android.sdk.sample.data.repository;

import android.content.Context;
import android.util.Log;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.StatusCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.AccuracyCriteria;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;

public class EyedidTrackingRepository implements EyeTrackingRepository {
    private static final String TAG = "EyedidTracking";
    private static final String LICENSE_KEY = "dev_plnp4o1ya7d0tif2rmgko169l1z4jnali2q4f63f";

    private GazeTracker gazeTracker;
    private int currentFPS = 30; // 기본 FPS
    private boolean performanceMonitoringEnabled = true;

    // 🆕 AccuracyCriteria 설정 저장 (캘리브레이션 시 사용)
    private AccuracyCriteria accuracyCriteria = AccuracyCriteria.HIGH;

    @Override
    public void initialize(Context context, InitializationCallback callback) {
        // 🔧 수정: GazeTrackerOptions를 기본 설정으로 단순화
        GazeTrackerOptions options = new GazeTrackerOptions.Builder().build();

        GazeTracker.initGazeTracker(context, LICENSE_KEY, (tracker, error) -> {
            if (tracker != null) {
                gazeTracker = tracker;

                // 🆕 초기화 후 FPS 설정 (GazeTracker 인스턴스에서 직접 호출)
                setTrackingFPS(currentFPS);

                Log.d(TAG, "시선 추적 SDK 초기화 성공 (HIGH 정확도 준비됨, FPS: " + currentFPS + ")");
            } else {
                Log.e(TAG, "시선 추적 SDK 초기화 실패: " + error);
            }
            callback.onInitialized(tracker, error);
        }, options);
    }

    @Override
    public void startTracking() {
        if (gazeTracker != null) {
            gazeTracker.startTracking();
        }
    }

    @Override
    public void stopTracking() {
        if (gazeTracker != null) {
            gazeTracker.stopTracking();
        }
    }

    @Override
    public void startCalibration(CalibrationModeType type) {
        if (gazeTracker != null) {
            // 🆕 AccuracyCriteria.HIGH 적용하여 캘리브레이션 시작
            boolean success = gazeTracker.startCalibration(type, accuracyCriteria);
            Log.d(TAG, "캘리브레이션 시작 (AccuracyCriteria.HIGH): " + (success ? "성공" : "실패"));
        }
    }

    @Override
    public void setTrackingCallback(TrackingCallback callback) {
        if (gazeTracker != null) {
            gazeTracker.setTrackingCallback(callback);
        }
    }

    @Override
    public void setCalibrationCallback(CalibrationCallback callback) {
        if (gazeTracker != null) {
            gazeTracker.setCalibrationCallback(callback);
        }
    }

    @Override
    public void setStatusCallback(StatusCallback callback) {
        if (gazeTracker != null) {
            gazeTracker.setStatusCallback(callback);
        }
    }

    @Override
    public GazeTracker getTracker() {
        return gazeTracker;
    }

    // 🆕 동적 FPS 조정 기능 (GazeTracker 인스턴스에서 직접 호출)
    public void setTrackingFPS(int fps) {
        if (gazeTracker != null && fps >= 1 && fps <= 30) {
            int oldFPS = currentFPS;
            currentFPS = fps;
            gazeTracker.setTrackingFPS(fps); // GazeTracker 인스턴스 메서드 사용
            Log.d(TAG, "추적 FPS 변경: " + oldFPS + " -> " + fps);
        } else if (gazeTracker == null) {
            // 초기화 전에는 값만 저장
            currentFPS = fps;
            Log.d(TAG, "FPS 설정 저장 (초기화 후 적용): " + fps);
        }
    }

    public int getCurrentFPS() {
        return currentFPS;
    }

    // 🆕 성능 기반 FPS 자동 조정
    public void adjustFPSBasedOnPerformance(int batteryLevel, float cpuUsage, long availableMemoryMB) {
        if (!performanceMonitoringEnabled) return;

        int optimalFPS = calculateOptimalFPS(batteryLevel, cpuUsage, availableMemoryMB);

        if (optimalFPS != currentFPS) {
            setTrackingFPS(optimalFPS);
            Log.d(TAG, "성능 기반 FPS 자동 조정: " + currentFPS + " -> " + optimalFPS +
                    " (배터리: " + batteryLevel + "%, CPU: " + cpuUsage + "%, 메모리: " + availableMemoryMB + "MB)");
        }
    }

    private int calculateOptimalFPS(int batteryLevel, float cpuUsage, long availableMemoryMB) {
        // 배터리 수준에 따른 기본 FPS 결정
        int baseFPS;
        if (batteryLevel < 15) {
            baseFPS = 10; // 극저전력 모드
        } else if (batteryLevel < 30) {
            baseFPS = 15; // 절전 모드
        } else if (batteryLevel < 50) {
            baseFPS = 20; // 효율 모드
        } else {
            baseFPS = 30; // 표준 모드
        }

        // CPU 사용률에 따른 조정
        if (cpuUsage > 80) {
            baseFPS = Math.max(10, baseFPS - 10);
        } else if (cpuUsage > 60) {
            baseFPS = Math.max(15, baseFPS - 5);
        }

        // 메모리 사용량에 따른 조정 (이미 MB 단위)
        if (availableMemoryMB < 100) {
            baseFPS = Math.max(10, baseFPS - 5);
        } else if (availableMemoryMB < 200) {
            baseFPS = Math.max(15, baseFPS - 3);
        }

        return Math.max(10, Math.min(30, baseFPS));
    }

    public void setPerformanceMonitoringEnabled(boolean enabled) {
        performanceMonitoringEnabled = enabled;
        Log.d(TAG, "성능 모니터링 " + (enabled ? "활성화" : "비활성화"));
    }

    public boolean isPerformanceMonitoringEnabled() {
        return performanceMonitoringEnabled;
    }

    // 🆕 AccuracyCriteria 설정 메서드
    public void setAccuracyCriteria(AccuracyCriteria criteria) {
        accuracyCriteria = criteria;
        Log.d(TAG, "AccuracyCriteria 설정: " + criteria.toString());
    }

    public AccuracyCriteria getAccuracyCriteria() {
        return accuracyCriteria;
    }
}