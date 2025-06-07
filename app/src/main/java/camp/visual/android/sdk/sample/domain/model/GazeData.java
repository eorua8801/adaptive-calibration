package camp.visual.android.sdk.sample.domain.model;

import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

/**
 * 시선 추적 데이터 모델
 * GazeTrackingService에서 호환성을 위해 getTrackingState() 메서드 추가
 */
public class GazeData {
    private final float x;
    private final float y;
    private final long timestamp;
    private final TrackingState state;

    public GazeData(float x, float y, long timestamp, TrackingState state) {
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
        this.state = state;
    }

    // 🔧 생성자 순서 변경 버전 (TrackingState가 세 번째 파라미터)
    public GazeData(float x, float y, TrackingState state, long timestamp) {
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
        this.state = state;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public TrackingState getState() {
        return state;
    }

    // 🆕 호환성을 위한 getTrackingState() 메서드 추가
    public TrackingState getTrackingState() {
        return state;
    }

    public boolean isValid() {
        return state == TrackingState.SUCCESS;
    }

    @Override
    public String toString() {
        return String.format("GazeData{x=%.2f, y=%.2f, timestamp=%d, state=%s}",
                x, y, timestamp, state);
    }
}