package camp.visual.android.sdk.sample.ui.views.overlay;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.graphics.PixelFormat;

import camp.visual.android.sdk.sample.ui.views.overlay.EdgeMenuOverlay.MenuButton;

public class EdgeMenuManager {
    
    private static final String TAG = "EdgeMenuManager";
    
    private Context context;
    private WindowManager windowManager;
    
    private NavigationMenuOverlay navigationMenu;
    private SystemMenuOverlay systemMenu;
    
    private EdgeMenuOverlay activeMenu = null;
    private long cancelStartTime = 0;
    private boolean isCanceling = false;
    
    // 클릭 감지용
    private MenuButton hoveredButton = null;
    private long hoverStartTime = 0;
    private static final long HOVER_CLICK_DURATION = 1000; // 1초 hover로 클릭
    
    public EdgeMenuManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        initMenus();
    }
    
    private void initMenus() {
        // 네비게이션 메뉴 (좌측 상단)
        navigationMenu = new NavigationMenuOverlay(context);
        
        // 시스템 메뉴 (우측 상단)
        systemMenu = new SystemMenuOverlay(context);
    }
    
    public void showNavigationMenu() {
        if (activeMenu != null) {
            hideActiveMenu();
        }
        
        showMenu(navigationMenu);
        Log.d(TAG, "네비게이션 메뉴 표시");
    }
    
    public void showSystemMenu() {
        if (activeMenu != null) {
            hideActiveMenu();
        }
        
        showMenu(systemMenu);
        Log.d(TAG, "시스템 메뉴 표시");
    }
    
    private void showMenu(EdgeMenuOverlay menu) {
        try {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            
            windowManager.addView(menu, params);
            menu.showMenu();
            activeMenu = menu;
            
            // 취소 상태 초기화
            isCanceling = false;
            cancelStartTime = 0;
            
        } catch (Exception e) {
            Log.e(TAG, "메뉴 표시 실패: " + e.getMessage(), e);
        }
    }
    
    public void hideActiveMenu() {
        if (activeMenu != null) {
            try {
                activeMenu.hideMenu();
                
                // 애니메이션 완료 후 뷰 제거
                activeMenu.postDelayed(() -> {
                    try {
                        if (activeMenu != null) {
                            windowManager.removeView(activeMenu);
                            activeMenu = null;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "메뉴 뷰 제거 중 오류: " + e.getMessage());
                    }
                }, 250);
                
                Log.d(TAG, "활성 메뉴 숨김");
            } catch (Exception e) {
                Log.e(TAG, "메뉴 숨김 실패: " + e.getMessage(), e);
            }
        }
        
        // 상태 초기화
        isCanceling = false;
        cancelStartTime = 0;
        hoveredButton = null;
        hoverStartTime = 0;
        
        // 호버 진행률 초기화
        if (activeMenu != null) {
            activeMenu.setHoverProgress(0f);
        }
    }
    
    public void startCanceling() {
        if (activeMenu != null && !isCanceling) {
            isCanceling = true;
            cancelStartTime = System.currentTimeMillis();
            activeMenu.startCanceling();
            Log.d(TAG, "메뉴 취소 시작");
        }
    }
    
    public void updateCancelProgress() {
        if (activeMenu != null && isCanceling && cancelStartTime > 0) {
            long duration = System.currentTimeMillis() - cancelStartTime;
            float progress = Math.min(1f, duration / 2000f); // 2초 동안 취소
            
            activeMenu.updateCancelProgress(progress);
            
            if (progress >= 1f) {
                // 취소 완료
                hideActiveMenu();
            }
        }
    }
    
    public void cancelCanceling() {
        if (activeMenu != null && isCanceling) {
            isCanceling = false;
            cancelStartTime = 0;
            activeMenu.cancelCanceling();
            Log.d(TAG, "메뉴 취소 중단");
        }
    }
    
    public void updateGazePosition(float x, float y) {
        if (activeMenu == null || !activeMenu.isVisible()) {
            return;
        }
        
        // 메뉴 버튼 호버 감지
        MenuButton buttonUnderGaze = activeMenu.getButtonAt(x, y);
        
        if (buttonUnderGaze != hoveredButton) {
            // 새로운 버튼에 호버
            hoveredButton = buttonUnderGaze;
            hoverStartTime = hoveredButton != null ? System.currentTimeMillis() : 0;
            activeMenu.setHoveredButton(hoveredButton);
            
            // 이전 호버 진행률 초기화
            activeMenu.setHoverProgress(0f);
            
            if (hoveredButton != null) {
                Log.d(TAG, "버튼 호버 시작: " + hoveredButton.label);
            }
        }
        
        // 호버 클릭 처리 및 진행률 업데이트
        if (hoveredButton != null && hoverStartTime > 0) {
            long hoverDuration = System.currentTimeMillis() - hoverStartTime;
            float progress = Math.min(1f, (float) hoverDuration / HOVER_CLICK_DURATION);
            
            // 시각적 호버 진행률 업데이트
            activeMenu.setHoverProgress(progress);
            
            if (hoverDuration >= HOVER_CLICK_DURATION) {
                // 호버 클릭 실행
                Log.d(TAG, "호버 클릭 실행: " + hoveredButton.label);
                hoveredButton.execute();
                
                // 메뉴 숨기기
                hideActiveMenu();
            }
        } else {
            // 호버되지 않은 상태에서는 진행률 0
            activeMenu.setHoverProgress(0f);
        }
    }
    
    public boolean isMenuVisible() {
        return activeMenu != null && activeMenu.isVisible();
    }
    
    public boolean isCancelingActive() {
        return isCanceling;
    }
    
    public boolean isNavigationMenuActive() {
        return activeMenu == navigationMenu;
    }
    
    public boolean isSystemMenuActive() {
        return activeMenu == systemMenu;
    }
    
    public void cleanup() {
        try {
            if (activeMenu != null) {
                windowManager.removeView(activeMenu);
                activeMenu = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "정리 중 오류: " + e.getMessage());
        }
    }
    
    // 디버깅용 메서드
    public String getCurrentMenuStatus() {
        if (activeMenu == null) {
            return "메뉴 없음";
        }
        
        String menuType = activeMenu == navigationMenu ? "네비게이션" : "시스템";
        String state = activeMenu.getCurrentState().toString();
        String cancelStatus = isCanceling ? " (취소 중)" : "";
        String hoverStatus = hoveredButton != null ? " (호버: " + hoveredButton.label + ")" : "";
        
        return menuType + " 메뉴 - " + state + cancelStatus + hoverStatus;
    }
}
