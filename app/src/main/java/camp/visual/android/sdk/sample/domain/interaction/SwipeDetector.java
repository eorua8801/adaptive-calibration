package camp.visual.android.sdk.sample.domain.interaction;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import camp.visual.android.sdk.sample.domain.model.UserSettings;

public class SwipeDetector {
    private static final String TAG = "SwipeDetector";
    
    private final UserSettings settings;
    private final Vibrator vibrator;
    
    public enum SwipeDirection {
        LEFT_TO_RIGHT, RIGHT_TO_LEFT, NONE
    }
    
    public enum SwipeState {
        WAITING, TRIGGER_READY, SWIPING, COMPLETED, FAILED
    }
    
    // 스와이프 설정 (강화된 요구사항)
    private static final float MIN_SWIPE_DISTANCE = 300f; // 최소 스와이프 거리 (dp)
    private static final float TRIGGER_ZONE_WIDTH = 120f; // 트리거 존 넓이 (dp)
    private static final float TRIGGER_ZONE_HEIGHT = 200f; // 트리거 존 높이 (dp)
    private static final long TRIGGER_WAIT_TIME = 800; // 트리거 존에서 대기 시간 (ms)
    private static final long MAX_SWIPE_TIME = 2000; // 최대 스와이프 완료 시간 (ms)
    private static final float MIN_SWIPE_SPEED = 150f; // 최소 스와이프 속도 (dp/s)
    
    // 상태 변수
    private SwipeState currentState = SwipeState.WAITING;
    private SwipeDirection expectedDirection = SwipeDirection.NONE;
    
    // 위치 추적
    private float startX, startY;
    private float currentX, currentY;
    private long triggerStartTime = 0;
    private long swipeStartTime = 0;
    private boolean triggerVibrated = false;
    
    // 화면 정보
    private float screenWidth, screenHeight;
    private float density;
    
    public SwipeDetector(UserSettings settings, Context context) {
        this.settings = settings;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.density = context.getResources().getDisplayMetrics().density;
    }
    
    public void updateScreenSize(float width, float height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    public SwipeDirection update(float x, float y) {
        currentX = x;
        currentY = y;
        
        switch (currentState) {
            case WAITING:
                return checkTriggerZone(x, y);
            case TRIGGER_READY:
                return processTriggerWait(x, y);
            case SWIPING:
                return processSwipe(x, y);
            default:
                return SwipeDirection.NONE;
        }
    }
    
    private SwipeDirection checkTriggerZone(float x, float y) {
        float triggerZoneWidthPx = TRIGGER_ZONE_WIDTH * density;
        float triggerZoneHeightPx = TRIGGER_ZONE_HEIGHT * density;
        float bottomMargin = screenHeight * 0.85f; // 하단 15% 영역
        
        boolean inLeftTrigger = x < triggerZoneWidthPx && y > bottomMargin;
        boolean inRightTrigger = x > (screenWidth - triggerZoneWidthPx) && y > bottomMargin;
        
        if (inLeftTrigger) {
            startTrigger(x, y, SwipeDirection.LEFT_TO_RIGHT);
            Log.d(TAG, "좌측 하단 트리거 존 진입 - 우측 스와이프 대기");
        } else if (inRightTrigger) {
            startTrigger(x, y, SwipeDirection.RIGHT_TO_LEFT);
            Log.d(TAG, "우측 하단 트리거 존 진입 - 좌측 스와이프 대기");
        }
        
        return SwipeDirection.NONE;
    }
    
    private void startTrigger(float x, float y, SwipeDirection direction) {
        currentState = SwipeState.TRIGGER_READY;
        expectedDirection = direction;
        startX = x;
        startY = y;
        triggerStartTime = System.currentTimeMillis();
        triggerVibrated = false;
        
        // 즉시 약한 진동으로 피드백
        vibrator.vibrate(50);
        
        Log.d(TAG, String.format("트리거 시작: 방향=%s, 위치=(%.1f, %.1f)", 
                direction.name(), x, y));
    }
    
    private SwipeDirection processTriggerWait(float x, float y) {
        long waitTime = System.currentTimeMillis() - triggerStartTime;
        float triggerZoneWidthPx = TRIGGER_ZONE_WIDTH * density;
        float bottomMargin = screenHeight * 0.85f;
        
        // 트리거 존을 벗어났는지 확인
        boolean leftZoneExit = expectedDirection == SwipeDirection.LEFT_TO_RIGHT && 
                               (x > triggerZoneWidthPx || y < bottomMargin);
        boolean rightZoneExit = expectedDirection == SwipeDirection.RIGHT_TO_LEFT && 
                                (x < (screenWidth - triggerZoneWidthPx) || y < bottomMargin);
        
        if (leftZoneExit || rightZoneExit) {
            // 트리거 존에서 충분히 기다린 후 나가면 스와이프 시작
            if (waitTime >= TRIGGER_WAIT_TIME) {
                startSwipe(x, y);
                return SwipeDirection.NONE;
            } else {
                // 너무 빨리 나가면 실패
                Log.d(TAG, "트리거 존에서 너무 빨리 이탈 - 리셋");
                reset();
                return SwipeDirection.NONE;
            }
        }
        
        // 트리거 존에서 대기 중인 피드백
        if (waitTime > TRIGGER_WAIT_TIME / 2 && !triggerVibrated) {
            vibrator.vibrate(100);
            triggerVibrated = true;
            Log.d(TAG, "트리거 준비 완료 - 스와이프 시작 가능");
        }
        
        return SwipeDirection.NONE;
    }
    
    private void startSwipe(float x, float y) {
        currentState = SwipeState.SWIPING;
        swipeStartTime = System.currentTimeMillis();
        
        Log.d(TAG, String.format("스와이프 시작: 시작점=(%.1f, %.1f), 현재점=(%.1f, %.1f)", 
                startX, startY, x, y));
        
        // 스와이프 시작 진동
        vibrator.vibrate(150);
    }
    
    private SwipeDirection processSwipe(float x, float y) {
        long swipeTime = System.currentTimeMillis() - swipeStartTime;
        float swipeDistance = Math.abs(x - startX);
        float minSwipeDistancePx = MIN_SWIPE_DISTANCE * density;
        
        // 시간 초과 체크
        if (swipeTime > MAX_SWIPE_TIME) {
            Log.d(TAG, "스와이프 시간 초과 - 실패");
            reset();
            return SwipeDirection.NONE;
        }
        
        // 스와이프 방향 체크
        boolean correctDirection = false;
        if (expectedDirection == SwipeDirection.LEFT_TO_RIGHT && x > startX + minSwipeDistancePx) {
            correctDirection = true;
        } else if (expectedDirection == SwipeDirection.RIGHT_TO_LEFT && x < startX - minSwipeDistancePx) {
            correctDirection = true;
        }
        
        if (correctDirection) {
            // 스와이프 속도 체크
            float swipeSpeed = (swipeDistance / (swipeTime / 1000f)) / density; // dp/s
            
            if (swipeSpeed >= MIN_SWIPE_SPEED) {
                Log.d(TAG, String.format("스와이프 성공! 거리=%.1fdp, 속도=%.1fdp/s, 시간=%dms", 
                        swipeDistance / density, swipeSpeed, swipeTime));
                
                // 성공 진동
                vibrator.vibrate(new long[]{0, 100, 50, 100}, -1);
                
                SwipeDirection result = expectedDirection;
                reset();
                return result;
            }
        }
        
        // 잘못된 방향으로 이동하거나 너무 느리면 실패
        if ((expectedDirection == SwipeDirection.LEFT_TO_RIGHT && x < startX - 50 * density) ||
            (expectedDirection == SwipeDirection.RIGHT_TO_LEFT && x > startX + 50 * density)) {
            Log.d(TAG, "스와이프 방향 오류 - 실패");
            reset();
        }
        
        return SwipeDirection.NONE;
    }
    
    public void reset() {
        currentState = SwipeState.WAITING;
        expectedDirection = SwipeDirection.NONE;
        triggerStartTime = 0;
        swipeStartTime = 0;
        triggerVibrated = false;
        startX = startY = currentX = currentY = 0;
    }
    
    public String getStateText() {
        switch (currentState) {
            case TRIGGER_READY:
                long waitTime = System.currentTimeMillis() - triggerStartTime;
                if (waitTime > TRIGGER_WAIT_TIME) {
                    return expectedDirection == SwipeDirection.LEFT_TO_RIGHT ? "→" : "←";
                }
                return triggerVibrated ? "◊" : "◦";
            case SWIPING:
                return expectedDirection == SwipeDirection.LEFT_TO_RIGHT ? "⟶" : "⟵";
            default:
                return "●";
        }
    }
    
    public SwipeState getCurrentState() {
        return currentState;
    }
    
    public SwipeDirection getExpectedDirection() {
        return expectedDirection;
    }
    
    public boolean isActive() {
        return currentState != SwipeState.WAITING;
    }
    
    // 디버깅용 상태 정보
    public String getDebugInfo() {
        if (currentState == SwipeState.WAITING) {
            return "대기 중";
        }
        
        long currentTime = System.currentTimeMillis();
        String stateInfo = String.format("상태=%s, 방향=%s", 
                currentState.name(), expectedDirection.name());
        
        if (triggerStartTime > 0) {
            long triggerTime = currentTime - triggerStartTime;
            stateInfo += String.format(", 트리거시간=%dms", triggerTime);
        }
        
        if (swipeStartTime > 0) {
            long swipeTime = currentTime - swipeStartTime;
            float distance = Math.abs(currentX - startX) / density;
            stateInfo += String.format(", 스와이프시간=%dms, 거리=%.1fdp", swipeTime, distance);
        }
        
        return stateInfo;
    }
}
