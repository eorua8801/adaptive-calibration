package camp.visual.android.sdk.sample.domain.interaction;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import camp.visual.android.sdk.sample.domain.model.UserSettings;

public class EdgeScrollDetector {
    private static final String TAG = "EdgeScrollDetector";

    private final UserSettings settings;
    private final Vibrator vibrator;

    public enum Edge {
        TOP, BOTTOM, LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM, NONE
    }

    public enum ScrollAction {
        SCROLL_UP, SCROLL_DOWN, LEFT_TOP_ACTION, LEFT_BOTTOM_ACTION, RIGHT_TOP_ACTION, RIGHT_BOTTOM_ACTION, NONE
    }

    // 상단 응시 관련 변수
    private int topGazeConsecutiveFrames = 0;
    private long topGazeStartTime = 0;
    private boolean topGazeVibrated1s = false;
    private boolean topGazeVibrated2s = false;
    private boolean topGazeTriggered = false;

    // 하단 응시 관련 변수
    private int bottomGazeConsecutiveFrames = 0;
    private long bottomGazeStartTime = 0;
    private boolean bottomGazeVibrated1s = false;
    private boolean bottomGazeVibrated2s = false;
    private boolean bottomGazeTriggered = false;

    // 🆕 좌측 상단 응시 관련 변수
    private int leftTopGazeConsecutiveFrames = 0;
    private long leftTopGazeStartTime = 0;
    private boolean leftTopGazeVibrated1s = false;
    private boolean leftTopGazeVibrated2s = false;
    private boolean leftTopGazeTriggered = false;

    // 🆕 좌측 하단 응시 관련 변수
    private int leftBottomGazeConsecutiveFrames = 0;
    private long leftBottomGazeStartTime = 0;
    private boolean leftBottomGazeVibrated1s = false;
    private boolean leftBottomGazeVibrated2s = false;
    private boolean leftBottomGazeTriggered = false;

    // 🆕 우측 상단 응시 관련 변수
    private int rightTopGazeConsecutiveFrames = 0;
    private long rightTopGazeStartTime = 0;
    private boolean rightTopGazeVibrated1s = false;
    private boolean rightTopGazeVibrated2s = false;
    private boolean rightTopGazeTriggered = false;

    // 🆕 우측 하단 응시 관련 변수
    private int rightBottomGazeConsecutiveFrames = 0;
    private long rightBottomGazeStartTime = 0;
    private boolean rightBottomGazeVibrated1s = false;
    private boolean rightBottomGazeVibrated2s = false;
    private boolean rightBottomGazeTriggered = false;

    // 현재 감지된 엣지
    private Edge currentEdge = Edge.NONE;

    private static final int EDGE_THRESHOLD_FRAMES = 5; // 연속 5프레임 이상

    public EdgeScrollDetector(UserSettings settings, Context context) {
        this.settings = settings;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    // 🆕 호환성을 위한 오버로드 메서드 (y만 사용하는 기존 코드용)
    public Edge update(float y, float screenHeight) {
        // 기본값으로 x를 대충 중간으로 설정
        float defaultX = 500f; // 좌우 엣지 감지 안함
        return update(defaultX, y, 1000f, screenHeight); // 임의의 화면 넓이 설정
    }

    public Edge update(float x, float y, float screenWidth, float screenHeight) {
        if (!settings.isEdgeScrollEnabled() || !settings.isScrollEnabled()) {
            return Edge.NONE;
        }

        float topMargin = screenHeight * settings.getEdgeMarginRatio();
        float bottomMargin = screenHeight * (1 - settings.getEdgeMarginRatio());
        float leftMargin = screenWidth * settings.getEdgeMarginRatio();
        float rightMargin = screenWidth * (1 - settings.getEdgeMarginRatio());
        
        // 🆕 좌우 엣지를 상/하단으로 나누기 위한 중앙선
        float middleY = screenHeight / 2f;

        boolean isInTopEdge = y < topMargin;
        boolean isInBottomEdge = y > bottomMargin;
        boolean isInLeftEdge = x < leftMargin;
        boolean isInRightEdge = x > rightMargin;
        
        // 좌우 엣지에서 상/하단 구분
        boolean isInTopHalf = y < middleY;
        boolean isInBottomHalf = y >= middleY;

        // 엣지 영역이 바뀌면 다른 엣지들 상태 초기화
        if (isInTopEdge && currentEdge != Edge.TOP) {
            resetOtherEdges(Edge.TOP);
            currentEdge = Edge.TOP;
        } else if (isInBottomEdge && currentEdge != Edge.BOTTOM) {
            resetOtherEdges(Edge.BOTTOM);
            currentEdge = Edge.BOTTOM;
        } else if (isInLeftEdge && isInTopHalf && currentEdge != Edge.LEFT_TOP) {
            resetOtherEdges(Edge.LEFT_TOP);
            currentEdge = Edge.LEFT_TOP;
        } else if (isInLeftEdge && isInBottomHalf && currentEdge != Edge.LEFT_BOTTOM) {
            resetOtherEdges(Edge.LEFT_BOTTOM);
            currentEdge = Edge.LEFT_BOTTOM;
        } else if (isInRightEdge && isInTopHalf && currentEdge != Edge.RIGHT_TOP) {
            resetOtherEdges(Edge.RIGHT_TOP);
            currentEdge = Edge.RIGHT_TOP;
        } else if (isInRightEdge && isInBottomHalf && currentEdge != Edge.RIGHT_BOTTOM) {
            resetOtherEdges(Edge.RIGHT_BOTTOM);
            currentEdge = Edge.RIGHT_BOTTOM;
        } else if (!isInTopEdge && !isInBottomEdge && !isInLeftEdge && !isInRightEdge) {
            resetAll();
            currentEdge = Edge.NONE;
            return Edge.NONE;
        }

        // 현재 감지된 엣지 반환
        return currentEdge;
    }

    public ScrollAction processTopEdge() {
        topGazeConsecutiveFrames++;

        if (topGazeConsecutiveFrames >= EDGE_THRESHOLD_FRAMES) {
            if (topGazeStartTime == 0) {
                topGazeStartTime = System.currentTimeMillis();
                topGazeVibrated1s = false;
                topGazeVibrated2s = false;
                topGazeTriggered = false;
                Log.d(TAG, "상단 응시 감지 - 타이머 시작");
                vibrator.vibrate(50);
                return ScrollAction.NONE;
            } else {
                long duration = System.currentTimeMillis() - topGazeStartTime;

                if (duration > 1000 && !topGazeVibrated1s) {
                    vibrator.vibrate(100);
                    topGazeVibrated1s = true;
                    Log.d(TAG, "상단 응시 1초 경과");
                    return ScrollAction.NONE;
                } else if (duration > 2000 && !topGazeVibrated2s) {
                    vibrator.vibrate(100);
                    topGazeVibrated2s = true;
                    Log.d(TAG, "상단 응시 2초 경과");
                    return ScrollAction.NONE;
                }

                if (duration >= settings.getEdgeTriggerMs() && !topGazeTriggered) {
                    Log.d(TAG, "상단 응시 완료 - 하단 스크롤 실행");
                    topGazeTriggered = true;
                    vibrator.vibrate(300);
                    return ScrollAction.SCROLL_DOWN;
                }
            }
        }
        return ScrollAction.NONE;
    }

    public ScrollAction processBottomEdge() {
        bottomGazeConsecutiveFrames++;

        if (bottomGazeConsecutiveFrames >= EDGE_THRESHOLD_FRAMES) {
            if (bottomGazeStartTime == 0) {
                bottomGazeStartTime = System.currentTimeMillis();
                bottomGazeVibrated1s = false;
                bottomGazeVibrated2s = false;
                bottomGazeTriggered = false;
                Log.d(TAG, "하단 응시 감지 - 타이머 시작");
                vibrator.vibrate(50);
                return ScrollAction.NONE;
            } else {
                long duration = System.currentTimeMillis() - bottomGazeStartTime;

                if (duration > 1000 && !bottomGazeVibrated1s) {
                    vibrator.vibrate(100);
                    bottomGazeVibrated1s = true;
                    Log.d(TAG, "하단 응시 1초 경과");
                    return ScrollAction.NONE;
                } else if (duration > 2000 && !bottomGazeVibrated2s) {
                    vibrator.vibrate(100);
                    bottomGazeVibrated2s = true;
                    Log.d(TAG, "하단 응시 2초 경과");
                    return ScrollAction.NONE;
                }

                if (duration >= settings.getEdgeTriggerMs() && !bottomGazeTriggered) {
                    Log.d(TAG, "하단 응시 완료 - 상단 스크롤 실행");
                    bottomGazeTriggered = true;
                    vibrator.vibrate(300);
                    return ScrollAction.SCROLL_UP;
                }
            }
        }
        return ScrollAction.NONE;
    }

    // 🆕 좌측 상단 엣지 처리
    public ScrollAction processLeftTopEdge() {
        leftTopGazeConsecutiveFrames++;

        if (leftTopGazeConsecutiveFrames >= EDGE_THRESHOLD_FRAMES) {
            if (leftTopGazeStartTime == 0) {
                leftTopGazeStartTime = System.currentTimeMillis();
                leftTopGazeVibrated1s = false;
                leftTopGazeVibrated2s = false;
                leftTopGazeTriggered = false;
                Log.d(TAG, "좌측 상단 응시 감지 - 타이머 시작");
                vibrator.vibrate(50);
                return ScrollAction.NONE;
            } else {
                long duration = System.currentTimeMillis() - leftTopGazeStartTime;

                if (duration > 1000 && !leftTopGazeVibrated1s) {
                    vibrator.vibrate(100);
                    leftTopGazeVibrated1s = true;
                    Log.d(TAG, "좌측 상단 응시 1초 경과");
                    return ScrollAction.NONE;
                } else if (duration > 2000 && !leftTopGazeVibrated2s) {
                    vibrator.vibrate(100);
                    leftTopGazeVibrated2s = true;
                    Log.d(TAG, "좌측 상단 응시 2초 경과");
                    return ScrollAction.NONE;
                }

                if (duration >= settings.getEdgeTriggerMs() && !leftTopGazeTriggered) {
                    Log.d(TAG, "좌측 상단 액션 트리거!");
                    leftTopGazeTriggered = true;
                    vibrator.vibrate(300);
                    return ScrollAction.LEFT_TOP_ACTION;
                }
            }
        }
        return ScrollAction.NONE;
    }

    // 🆕 좌측 하단 엣지 처리
    public ScrollAction processLeftBottomEdge() {
        leftBottomGazeConsecutiveFrames++;

        if (leftBottomGazeConsecutiveFrames >= EDGE_THRESHOLD_FRAMES) {
            if (leftBottomGazeStartTime == 0) {
                leftBottomGazeStartTime = System.currentTimeMillis();
                leftBottomGazeVibrated1s = false;
                leftBottomGazeVibrated2s = false;
                leftBottomGazeTriggered = false;
                Log.d(TAG, "좌측 하단 응시 감지 - 타이머 시작");
                vibrator.vibrate(50);
                return ScrollAction.NONE;
            } else {
                long duration = System.currentTimeMillis() - leftBottomGazeStartTime;

                if (duration > 1000 && !leftBottomGazeVibrated1s) {
                    vibrator.vibrate(100);
                    leftBottomGazeVibrated1s = true;
                    Log.d(TAG, "좌측 하단 응시 1초 경과");
                    return ScrollAction.NONE;
                } else if (duration > 2000 && !leftBottomGazeVibrated2s) {
                    vibrator.vibrate(100);
                    leftBottomGazeVibrated2s = true;
                    Log.d(TAG, "좌측 하단 응시 2초 경과");
                    return ScrollAction.NONE;
                }

                if (duration >= settings.getEdgeTriggerMs() && !leftBottomGazeTriggered) {
                    Log.d(TAG, "좌측 하단 액션 트리거!");
                    leftBottomGazeTriggered = true;
                    vibrator.vibrate(300);
                    return ScrollAction.LEFT_BOTTOM_ACTION;
                }
            }
        }
        return ScrollAction.NONE;
    }

    // 🆕 우측 상단 엣지 처리
    public ScrollAction processRightTopEdge() {
        rightTopGazeConsecutiveFrames++;

        if (rightTopGazeConsecutiveFrames >= EDGE_THRESHOLD_FRAMES) {
            if (rightTopGazeStartTime == 0) {
                rightTopGazeStartTime = System.currentTimeMillis();
                rightTopGazeVibrated1s = false;
                rightTopGazeVibrated2s = false;
                rightTopGazeTriggered = false;
                Log.d(TAG, "우측 상단 응시 감지 - 타이머 시작");
                vibrator.vibrate(50);
                return ScrollAction.NONE;
            } else {
                long duration = System.currentTimeMillis() - rightTopGazeStartTime;

                if (duration > 1000 && !rightTopGazeVibrated1s) {
                    vibrator.vibrate(100);
                    rightTopGazeVibrated1s = true;
                    Log.d(TAG, "우측 상단 응시 1초 경과");
                    return ScrollAction.NONE;
                } else if (duration > 2000 && !rightTopGazeVibrated2s) {
                    vibrator.vibrate(100);
                    rightTopGazeVibrated2s = true;
                    Log.d(TAG, "우측 상단 응시 2초 경과");
                    return ScrollAction.NONE;
                }

                if (duration >= settings.getEdgeTriggerMs() && !rightTopGazeTriggered) {
                    Log.d(TAG, "우측 상단 액션 트리거!");
                    rightTopGazeTriggered = true;
                    vibrator.vibrate(300);
                    return ScrollAction.RIGHT_TOP_ACTION;
                }
            }
        }
        return ScrollAction.NONE;
    }

    // 🆕 우측 하단 엣지 처리
    public ScrollAction processRightBottomEdge() {
        rightBottomGazeConsecutiveFrames++;

        if (rightBottomGazeConsecutiveFrames >= EDGE_THRESHOLD_FRAMES) {
            if (rightBottomGazeStartTime == 0) {
                rightBottomGazeStartTime = System.currentTimeMillis();
                rightBottomGazeVibrated1s = false;
                rightBottomGazeVibrated2s = false;
                rightBottomGazeTriggered = false;
                Log.d(TAG, "우측 하단 응시 감지 - 타이머 시작");
                vibrator.vibrate(50);
                return ScrollAction.NONE;
            } else {
                long duration = System.currentTimeMillis() - rightBottomGazeStartTime;

                if (duration > 1000 && !rightBottomGazeVibrated1s) {
                    vibrator.vibrate(100);
                    rightBottomGazeVibrated1s = true;
                    Log.d(TAG, "우측 하단 응시 1초 경과");
                    return ScrollAction.NONE;
                } else if (duration > 2000 && !rightBottomGazeVibrated2s) {
                    vibrator.vibrate(100);
                    rightBottomGazeVibrated2s = true;
                    Log.d(TAG, "우측 하단 응시 2초 경과");
                    return ScrollAction.NONE;
                }

                if (duration >= settings.getEdgeTriggerMs() && !rightBottomGazeTriggered) {
                    Log.d(TAG, "우측 하단 액션 트리거!");
                    rightBottomGazeTriggered = true;
                    vibrator.vibrate(300);
                    return ScrollAction.RIGHT_BOTTOM_ACTION;
                }
            }
        }
        return ScrollAction.NONE;
    }

    public String getEdgeStateText() {
        if (currentEdge == Edge.TOP) {
            if (topGazeStartTime == 0) return "▲";
            long duration = System.currentTimeMillis() - topGazeStartTime;
            if (duration > 2000) return "②";
            if (duration > 1000) return "①";
            return "▲";
        } else if (currentEdge == Edge.BOTTOM) {
            if (bottomGazeStartTime == 0) return "▼";
            long duration = System.currentTimeMillis() - bottomGazeStartTime;
            if (duration > 2000) return "②";
            if (duration > 1000) return "①";
            return "▼";
        } else if (currentEdge == Edge.LEFT_TOP) {
            if (leftTopGazeStartTime == 0) return "◤";
            long duration = System.currentTimeMillis() - leftTopGazeStartTime;
            if (duration > 2000) return "②";
            if (duration > 1000) return "①";
            return "◤";
        } else if (currentEdge == Edge.LEFT_BOTTOM) {
            if (leftBottomGazeStartTime == 0) return "◣";
            long duration = System.currentTimeMillis() - leftBottomGazeStartTime;
            if (duration > 2000) return "②";
            if (duration > 1000) return "①";
            return "◣";
        } else if (currentEdge == Edge.RIGHT_TOP) {
            if (rightTopGazeStartTime == 0) return "◥";
            long duration = System.currentTimeMillis() - rightTopGazeStartTime;
            if (duration > 2000) return "②";
            if (duration > 1000) return "①";
            return "◥";
        } else if (currentEdge == Edge.RIGHT_BOTTOM) {
            if (rightBottomGazeStartTime == 0) return "◢";
            long duration = System.currentTimeMillis() - rightBottomGazeStartTime;
            if (duration > 2000) return "②";
            if (duration > 1000) return "①";
            return "◢";
        }

        return "●";
    }

    private void resetTop() {
        topGazeConsecutiveFrames = 0;
        topGazeStartTime = 0;
        topGazeVibrated1s = false;
        topGazeVibrated2s = false;
        topGazeTriggered = false;
    }

    private void resetBottom() {
        bottomGazeConsecutiveFrames = 0;
        bottomGazeStartTime = 0;
        bottomGazeVibrated1s = false;
        bottomGazeVibrated2s = false;
        bottomGazeTriggered = false;
    }

    private void resetLeftTop() {
        leftTopGazeConsecutiveFrames = 0;
        leftTopGazeStartTime = 0;
        leftTopGazeVibrated1s = false;
        leftTopGazeVibrated2s = false;
        leftTopGazeTriggered = false;
    }

    private void resetLeftBottom() {
        leftBottomGazeConsecutiveFrames = 0;
        leftBottomGazeStartTime = 0;
        leftBottomGazeVibrated1s = false;
        leftBottomGazeVibrated2s = false;
        leftBottomGazeTriggered = false;
    }

    private void resetRightTop() {
        rightTopGazeConsecutiveFrames = 0;
        rightTopGazeStartTime = 0;
        rightTopGazeVibrated1s = false;
        rightTopGazeVibrated2s = false;
        rightTopGazeTriggered = false;
    }

    private void resetRightBottom() {
        rightBottomGazeConsecutiveFrames = 0;
        rightBottomGazeStartTime = 0;
        rightBottomGazeVibrated1s = false;
        rightBottomGazeVibrated2s = false;
        rightBottomGazeTriggered = false;
    }

    private void resetOtherEdges(Edge currentEdge) {
        if (currentEdge != Edge.TOP) resetTop();
        if (currentEdge != Edge.BOTTOM) resetBottom();
        if (currentEdge != Edge.LEFT_TOP) resetLeftTop();
        if (currentEdge != Edge.LEFT_BOTTOM) resetLeftBottom();
        if (currentEdge != Edge.RIGHT_TOP) resetRightTop();
        if (currentEdge != Edge.RIGHT_BOTTOM) resetRightBottom();
    }

    public void resetAll() {
        resetTop();
        resetBottom();
        resetLeftTop();
        resetLeftBottom();
        resetRightTop();
        resetRightBottom();
        currentEdge = Edge.NONE;
    }

    public boolean isActive() {
        return currentEdge != Edge.NONE;
    }
}
