package camp.visual.android.sdk.sample.service.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class MyAccessibilityService extends AccessibilityService {

    private static MyAccessibilityService instance;
    private static final String TAG = "MyAccessibilityService";

    // 스크롤 설정
    private static final float SCROLL_AMOUNT_SMALL = 0.15f; // 화면 높이의 15%
    private static final float SCROLL_AMOUNT_MEDIUM = 0.2f; // 화면 높이의 20%
    private static final float SCROLL_AMOUNT_LARGE = 0.3f; // 화면 높이의 30%

    // 현재 진행 중인 제스처 여부
    private boolean isGestureInProgress = false;
    
    // 스와이프 설정
    private static final float SWIPE_DISTANCE_RATIO = 0.8f; // 화면 넓이의 80%

    // 제스처 완료 핸들러
    private Handler gestureHandler = new Handler(Looper.getMainLooper());
    private Runnable gestureCompletionRunnable = new Runnable() {
        @Override
        public void run() {
            isGestureInProgress = false;
        }
    };

    public static MyAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "접근성 서비스가 연결되었습니다.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 필요한 경우 특정 이벤트 처리
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "접근성 서비스 중단됨");
    }

    public static void performClickAt(float x, float y) {
        if (instance != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (instance.isGestureInProgress) {
                Log.d(TAG, "다른 제스처가 진행 중입니다. 클릭 무시됨");
                return;
            }

            instance.isGestureInProgress = true;

            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            GestureDescription.StrokeDescription clickStroke =
                    new GestureDescription.StrokeDescription(clickPath, 0, 50);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(clickStroke);

            Log.d(TAG, "클릭 실행 (접근성 서비스): x=" + x + ", y=" + y);

            // 화면 크기 정보도 로그에 출력
            DisplayMetrics dm = instance.getResources().getDisplayMetrics();
            Log.d(TAG, "화면 크기: " + dm.widthPixels + "x" + dm.heightPixels);

            instance.dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "클릭 제스처 완료됨");
                    instance.gestureHandler.postDelayed(instance.gestureCompletionRunnable, 100);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.d(TAG, "클릭 제스처 취소됨");
                    instance.gestureHandler.postDelayed(instance.gestureCompletionRunnable, 50);
                }
            }, null);
        } else {
            Log.e(TAG, "접근성 서비스가 초기화되지 않았거나 API 레벨이 낮음");
        }
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public enum ScrollAmount {
        SMALL, MEDIUM, LARGE
    }

    public void performScroll(Direction direction) {
        performScroll(direction, ScrollAmount.MEDIUM);
    }

    public void performScroll(Direction direction, ScrollAmount amount) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isGestureInProgress) {
                Log.d(TAG, "다른 제스처가 진행 중입니다. 스크롤 무시됨");
                return;
            }

            isGestureInProgress = true;

            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            float startX = screenWidth / 2f;
            float startY, endY;
            float scrollAmount;

            // 스크롤 거리 설정
            switch (amount) {
                case SMALL:
                    scrollAmount = SCROLL_AMOUNT_SMALL;
                    break;
                case LARGE:
                    scrollAmount = SCROLL_AMOUNT_LARGE;
                    break;
                case MEDIUM:
                default:
                    scrollAmount = SCROLL_AMOUNT_MEDIUM;
                    break;
            }

            if (direction == Direction.UP) {
                // 위로 스크롤 (화면은 아래로 이동)
                startY = screenHeight * 0.6f;
                endY = screenHeight * (0.6f - scrollAmount);
            } else {
                // 아래로 스크롤 (화면은 위로 이동)
                startY = screenHeight * 0.4f;
                endY = screenHeight * (0.4f + scrollAmount);
            }

            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(startX, endY);

            // 스크롤 기간 설정 - 스크롤 거리에 따라 조정
            long duration = 200 + (long)(scrollAmount * 300); // 200~300ms

            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, duration);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(stroke);

            Log.d(TAG, direction + " 스크롤 수행 중... 거리: " + (scrollAmount * 100) + "% 화면");

            dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "스크롤 제스처 완료됨");
                    gestureHandler.postDelayed(gestureCompletionRunnable, 300);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.d(TAG, "스크롤 제스처 취소됨");
                    gestureHandler.postDelayed(gestureCompletionRunnable, 150);
                }
            }, null);
        } else {
            Log.e(TAG, "API 레벨이 낮아 제스처를 지원하지 않음");
        }
    }

    /**
     * 연속 스크롤을 수행하는 메서드
     * @param direction 스크롤 방향
     * @param count 연속 스크롤 횟수
     */
    public void performContinuousScroll(Direction direction, int count) {
        if (count <= 0) return;

        if (isGestureInProgress) {
            // 이미 제스처가 진행 중이면 잠시 후 다시 시도
            gestureHandler.postDelayed(() -> performContinuousScroll(direction, count), 500);
            return;
        }

        // 첫 스크롤 실행
        performScroll(direction, ScrollAmount.MEDIUM);

        // 남은 스크롤 예약 (각 스크롤 간 500ms 간격)
        if (count > 1) {
            gestureHandler.postDelayed(() -> performContinuousScroll(direction, count - 1), 500);
        }
    }

    /**
     * 강화된 스와이프 제스처를 수행하는 메서드
     * @param direction 스와이프 방향 (LEFT 또는 RIGHT)
     */
    public void performSwipe(Direction direction) {
        if (direction != Direction.LEFT && direction != Direction.RIGHT) {
            Log.e(TAG, "스와이프는 LEFT 또는 RIGHT 방향만 지원됩니다");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isGestureInProgress) {
                Log.d(TAG, "다른 제스처가 진행 중입니다. 스와이프 무시됨");
                return;
            }

            isGestureInProgress = true;

            DisplayMetrics dm = getResources().getDisplayMetrics();
            int screenHeight = dm.heightPixels;
            int screenWidth = dm.widthPixels;
            
            // 스와이프 시작/끝 좌표 계산
            float startY = screenHeight * 0.5f; // 화면 중앙 높이
            float endY = startY;
            float startX, endX;
            
            if (direction == Direction.LEFT) {
                // 우측에서 좌측으로 스와이프 (뒤로가기)
                startX = screenWidth * 0.95f;
                endX = screenWidth * 0.1f;
                Log.d(TAG, "우측→좌측 스와이프 실행 (뒤로가기)");
            } else {
                // 좌측에서 우측으로 스와이프 (앞으로가기)
                startX = screenWidth * 0.05f;
                endX = screenWidth * 0.9f;
                Log.d(TAG, "좌측→우측 스와이프 실행 (앞으로가기)");
            }

            // 스와이프 경로 생성
            Path swipePath = new Path();
            swipePath.moveTo(startX, startY);
            swipePath.lineTo(endX, endY);

            // 스와이프 지속 시간 (빠르고 강력한 스와이프)
            long swipeDuration = 400; // 400ms로 빠른 스와이프

            GestureDescription.StrokeDescription swipeStroke =
                    new GestureDescription.StrokeDescription(swipePath, 0, swipeDuration);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(swipeStroke);

            Log.d(TAG, String.format("스와이프 실행: (%.1f,%.1f) → (%.1f,%.1f), 지속시간: %dms", 
                    startX, startY, endX, endY, swipeDuration));

            dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "스와이프 제스처 완료됨");
                    gestureHandler.postDelayed(gestureCompletionRunnable, 200);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.d(TAG, "스와이프 제스처 취소됨");
                    gestureHandler.postDelayed(gestureCompletionRunnable, 100);
                }
            }, null);
        } else {
            Log.e(TAG, "API 레벨이 낮아 스와이프 제스처를 지원하지 않음");
        }
    }

    /**
     * 네비게이션 스와이프 (좌우 엣지에서 시작되는 시스템 네비게이션)
     * @param direction 스와이프 방향
     */
    public void performNavigationSwipe(Direction direction) {
        if (direction != Direction.LEFT && direction != Direction.RIGHT) {
            Log.e(TAG, "네비게이션 스와이프는 LEFT 또는 RIGHT 방향만 지원됩니다");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isGestureInProgress) {
                Log.d(TAG, "다른 제스처가 진행 중입니다. 네비게이션 스와이프 무시됨");
                return;
            }

            isGestureInProgress = true;

            DisplayMetrics dm = getResources().getDisplayMetrics();
            int screenHeight = dm.heightPixels;
            int screenWidth = dm.widthPixels;
            
            // 네비게이션 스와이프 좌표 (엣지에서 시작)
            float startY = screenHeight * 0.7f; // 하단 30% 지점
            float endY = startY;
            float startX, endX;
            
            if (direction == Direction.LEFT) {
                // 우측 엣지에서 시작하는 뒤로가기 스와이프
                startX = screenWidth - 1f; // 완전히 오른쪽 가장자리
                endX = screenWidth * 0.5f; // 화면 중앙까지
                Log.d(TAG, "우측 엣지→중앙 네비게이션 스와이프 (뒤로가기)");
            } else {
                // 좌측 엣지에서 시작하는 앞으로가기/메뉴 스와이프
                startX = 1f; // 완전히 왼쪽 가장자리
                endX = screenWidth * 0.5f; // 화면 중앙까지
                Log.d(TAG, "좌측 엣지→중앙 네비게이션 스와이프 (앞으로가기/메뉴)");
            }

            // 네비게이션 스와이프 경로 생성
            Path navPath = new Path();
            navPath.moveTo(startX, startY);
            navPath.lineTo(endX, endY);

            // 네비게이션 스와이프는 더 느리고 부드럽게
            long navDuration = 600;

            GestureDescription.StrokeDescription navStroke =
                    new GestureDescription.StrokeDescription(navPath, 0, navDuration);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(navStroke);

            Log.d(TAG, String.format("네비게이션 스와이프: (%.1f,%.1f) → (%.1f,%.1f), 지속시간: %dms", 
                    startX, startY, endX, endY, navDuration));

            dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "네비게이션 스와이프 완료됨");
                    gestureHandler.postDelayed(gestureCompletionRunnable, 250);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.d(TAG, "네비게이션 스와이프 취소됨");
                    gestureHandler.postDelayed(gestureCompletionRunnable, 125);
                }
            }, null);
        } else {
            Log.e(TAG, "API 레벨이 낮아 네비게이션 스와이프를 지원하지 않음");
        }
    }

    /**
     * 정적 메서드로 스와이프 수행
     */
    public static void performSwipeAction(Direction direction) {
        if (instance != null) {
            instance.performSwipe(direction);
        } else {
            Log.e(TAG, "접근성 서비스 인스턴스가 없습니다 - 스와이프 실행 불가");
        }
    }

    /**
     * 정적 메서드로 네비게이션 스와이프 수행
     */
    public static void performNavigationSwipeAction(Direction direction) {
        if (instance != null) {
            instance.performNavigationSwipe(direction);
        } else {
            Log.e(TAG, "접근성 서비스 인스턴스가 없습니다 - 네비게이션 스와이프 실행 불가");
        }
    }

    // Optional: fallback for clicking views if needed later
    @Nullable
    private AccessibilityNodeInfo findClickableNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        return findFirstClickableNode(root);
    }

    @Nullable
    private AccessibilityNodeInfo findFirstClickableNode(AccessibilityNodeInfo node) {
        if (node.isClickable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findFirstClickableNode(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }
}