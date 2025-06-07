package camp.visual.android.sdk.sample.data.repository;

import android.content.Context;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.StatusCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;

/**
 * 시선 추적 저장소 인터페이스
 * EyedID SDK와의 상호작용을 추상화
 */
public interface EyeTrackingRepository {

    /**
     * 시선 추적 시스템 초기화
     */
    void initialize(Context context, InitializationCallback callback);

    /**
     * 시선 추적 시작
     */
    void startTracking();

    /**
     * 시선 추적 중지
     */
    void stopTracking();

    /**
     * 캘리브레이션 시작
     */
    void startCalibration(CalibrationModeType type);

    /**
     * 추적 콜백 설정
     */
    void setTrackingCallback(TrackingCallback callback);

    /**
     * 캘리브레이션 콜백 설정
     */
    void setCalibrationCallback(CalibrationCallback callback);

    /**
     * 상태 콜백 설정
     */
    void setStatusCallback(StatusCallback callback);

    /**
     * GazeTracker 인스턴스 반환
     */
    GazeTracker getTracker();
}