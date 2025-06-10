package camp.visual.android.sdk.sample.ui.views;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.graphics.PixelFormat;

import camp.visual.android.sdk.sample.ui.views.overlay.EdgeMenuManager;

public class CursorLayerManager implements EdgeMenuManager.UILayerCallback {
    
    private static final String TAG = "CursorLayerManager";
    
    private Context context;
    private WindowManager windowManager;
    private OverlayCursorView cursorView;
    private WindowManager.LayoutParams currentParams;
    
    private boolean isNotificationPanelOpen = false;
    private boolean isHighestLayer = false;
    
    public CursorLayerManager(Context context, OverlayCursorView cursorView) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.cursorView = cursorView;
        
        // 초기 파라미터 설정
        setupInitialParams();
    }
    
    private void setupInitialParams() {
        currentParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        currentParams.gravity = Gravity.TOP | Gravity.START;
    }
    
    public void addCursorToWindow() {
        try {
            windowManager.addView(cursorView, currentParams);
            Log.d(TAG, "커서 뷰 추가 성공");
        } catch (Exception e) {
            Log.e(TAG, "커서 뷰 추가 실패: " + e.getMessage(), e);
        }
    }
    
    public void removeCursorFromWindow() {
        try {
            if (cursorView != null && cursorView.getParent() != null) {
                windowManager.removeView(cursorView);
                Log.d(TAG, "커서 뷰 제거 성공");
            }
        } catch (Exception e) {
            Log.e(TAG, "커서 뷰 제거 실패: " + e.getMessage(), e);
        }
    }
    
    // 🆕 EdgeMenuManager.UILayerCallback 구현
    @Override
    public void onNotificationPanelStateChanged(boolean isOpen) {
        this.isNotificationPanelOpen = isOpen;
        Log.d(TAG, "알림 패널 상태 변경: " + (isOpen ? "열림" : "닫힘"));
        
        // 🔧 수정: 알림 패널 상태가 변경될 때만 레이어 조정
        if (isOpen) {
            requestHighestLayer();
        } else {
            requestNormalLayer();
        }
    }
    
    @Override
    public void requestHighestLayer() {
        if (isHighestLayer) {
            Log.d(TAG, "이미 최고 레이어 상태");
            return;
        }
        
        Log.d(TAG, "최고 레이어 요청 - 커서 레이어 업그레이드");
        updateCursorLayer(true);
    }
    
    @Override
    public void requestNormalLayer() {
        if (!isHighestLayer) {
            Log.d(TAG, "이미 일반 레이어 상태");
            return;
        }
        
        Log.d(TAG, "일반 레이어 요청 - 커서 레이어 정상화");
        updateCursorLayer(false);
    }
    
    private void updateCursorLayer(boolean useHighestLayer) {
        try {
            Log.d(TAG, "커서 레이어 업데이트 시작: " + (useHighestLayer ? "최고 우선순위" : "일반 우선순위"));
            
            // 🔧 수정: updateViewLayout 사용으로 안전하게 업데이트
            if (cursorView != null && cursorView.getParent() != null) {
                // 새로운 파라미터 설정
                WindowManager.LayoutParams newParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    getOptimalWindowType(useHighestLayer),
                    getOptimalFlags(useHighestLayer),
                    PixelFormat.TRANSLUCENT
                );
                newParams.gravity = Gravity.TOP | Gravity.START;
                
                // 🆕 알림 패널이 열려있을 때 추가 설정
                if (useHighestLayer && isNotificationPanelOpen) {
                    newParams.alpha = 1.0f; // 완전 불투명
                    newParams.dimAmount = 0f; // dim 효과 제거
                    Log.d(TAG, "알림 패널 대응 모드 - 추가 설정 적용");
                }
                
                currentParams = newParams;
                isHighestLayer = useHighestLayer;
                
                // 🔧 핵심 수정: updateViewLayout 사용
                windowManager.updateViewLayout(cursorView, newParams);
                
                Log.d(TAG, "커서 레이어 업데이트 성공: " + 
                    (useHighestLayer ? "최고 우선순위" : "일반 우선순위") +
                    " | 타입: " + getWindowTypeName(newParams.type));
                    
            } else {
                Log.w(TAG, "커서 뷰가 윈도우에 추가되지 않음 - 다시 추가 시도");
                addCursorToWindow();
                // 추가 후 다시 레이어 조정 시도
                if (useHighestLayer) {
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    handler.postDelayed(() -> updateCursorLayer(true), 100);
                }
            }
                
        } catch (Exception e) {
            Log.e(TAG, "커서 레이어 업데이트 실패: " + e.getMessage(), e);
            
            // 실패 시 fallback
            tryFallbackCursorUpdate(useHighestLayer);
        }
    }
    
    // 🆕 윈도우 타입 결정 (더 안전한 방식)
    private int getOptimalWindowType(boolean useHighestLayer) {
        if (useHighestLayer && isNotificationPanelOpen) {
            // 🔧 수정: Android 버전별 최적 타입 선택
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // API 26+에서는 TYPE_APPLICATION_OVERLAY가 최상위
                return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                // 이전 버전에서 TYPE_SYSTEM_OVERLAY 시도
                return WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            }
        } else {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
    }
    
    // 🆕 윈도우 플래그 결정
    private int getOptimalFlags(boolean useHighestLayer) {
        int baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        if (useHighestLayer && isNotificationPanelOpen) {
            // 🔧 수정: 알림 패널에 대응하는 강력한 플래그 조합
            baseFlags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            baseFlags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            baseFlags |= WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
            baseFlags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
            baseFlags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }
        
        return baseFlags;
    }
    
    private void tryFallbackCursorUpdate(boolean useHighestLayer) {
        try {
            Log.d(TAG, "커서 fallback 업데이트 시도");
            
            // 🔧 수정: 안전한 fallback - visibility 조정 우선 시도
            if (cursorView != null) {
                cursorView.setVisibility(android.view.View.VISIBLE);
                cursorView.bringToFront(); // 뷰를 최상위로
                Log.d(TAG, "커서 visibility 및 우선순위 조정 완료");
            }
            
            // 그래도 안 되면 뷰 재추가
            if (cursorView != null && cursorView.getParent() == null) {
                setupInitialParams();
                addCursorToWindow();
                isHighestLayer = false; // 안전한 상태로 리셋
                Log.d(TAG, "커서 재추가 완료");
            }
            
        } catch (Exception e2) {
            Log.e(TAG, "커서 fallback도 실패: " + e2.getMessage());
            
            // 🆕 최종 비상 수단: 강제 재시작
            try {
                removeCursorFromWindow();
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(() -> {
                    setupInitialParams();
                    addCursorToWindow();
                    Log.d(TAG, "커서 강제 재시작 완료");
                }, 200);
            } catch (Exception e3) {
                Log.e(TAG, "커서 강제 재시작도 실패: " + e3.getMessage());
            }
        }
    }
    
    // 🆕 수동 레이어 조정 메서드
    public void forceHighestLayer() {
        Log.d(TAG, "커서 레이어 강제 최고 우선순위 설정");
        updateCursorLayer(true);
    }
    
    public void forceNormalLayer() {
        Log.d(TAG, "커서 레이어 강제 일반 우선순위 설정");
        updateCursorLayer(false);
    }
    
    // 🆕 현재 상태 확인
    public boolean isInHighestLayer() {
        return isHighestLayer;
    }
    
    public boolean isNotificationPanelOpen() {
        return isNotificationPanelOpen;
    }
    
    // 🆕 커서 상태 업데이트 (편의 메서드)
    public void updateCursorPosition(float x, float y) {
        if (cursorView != null) {
            cursorView.updatePosition(x, y);
            
            // 🆕 알림 패널이 열려있을 때 커서가 보이는지 확인
            if (isNotificationPanelOpen && cursorView.getVisibility() != android.view.View.VISIBLE) {
                Log.w(TAG, "알림 패널 상태에서 커서가 보이지 않음 - 강제 표시");
                cursorView.setVisibility(android.view.View.VISIBLE);
                cursorView.bringToFront();
            }
        }
    }
    
    public void updateCursorText(String text) {
        if (cursorView != null) {
            cursorView.setCursorText(text);
        }
    }
    
    public void updateCursorProgress(float progress) {
        if (cursorView != null) {
            cursorView.setProgress(progress);
        }
    }
    
    // 🆕 정리 메서드
    public void cleanup() {
        try {
            removeCursorFromWindow();
            Log.d(TAG, "커서 레이어 매니저 정리 완료");
        } catch (Exception e) {
            Log.e(TAG, "커서 레이어 매니저 정리 실패: " + e.getMessage());
        }
    }
    
    // 🆕 상태 정보 반환
    public String getLayerStatus() {
        return "커서 레이어: " + 
            (isHighestLayer ? "최고 우선순위" : "일반 우선순위") +
            (isNotificationPanelOpen ? " (알림 패널 열림)" : " (일반 상태)") +
            " | 타입: " + getWindowTypeName(currentParams.type) +
            " | 표시 상태: " + (cursorView != null && cursorView.getVisibility() == android.view.View.VISIBLE ? "보임" : "숨김");
    }
    
    private String getWindowTypeName(int type) {
        switch (type) {
            case WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:
                return "APPLICATION_OVERLAY";
            case WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY:
                return "SYSTEM_OVERLAY";
            case WindowManager.LayoutParams.TYPE_SYSTEM_ERROR:
                return "SYSTEM_ERROR";
            default:
                return "UNKNOWN(" + type + ")";
        }
    }
    
    // 🆕 디버깅 메서드
    public void debugCursorState() {
        Log.d(TAG, "=== 커서 상태 디버깅 ===");
        Log.d(TAG, "cursorView != null: " + (cursorView != null));
        if (cursorView != null) {
            Log.d(TAG, "cursorView.getParent() != null: " + (cursorView.getParent() != null));
            Log.d(TAG, "cursorView.getVisibility(): " + cursorView.getVisibility());
            Log.d(TAG, "cursorView.isShown(): " + cursorView.isShown());
        }
        Log.d(TAG, "isHighestLayer: " + isHighestLayer);
        Log.d(TAG, "isNotificationPanelOpen: " + isNotificationPanelOpen);
        Log.d(TAG, getLayerStatus());
        Log.d(TAG, "========================");
    }
}
