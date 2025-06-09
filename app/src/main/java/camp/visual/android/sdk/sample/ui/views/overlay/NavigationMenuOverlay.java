package camp.visual.android.sdk.sample.ui.views.overlay;

import android.content.Context;
import android.util.Log;

import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;

public class NavigationMenuOverlay extends EdgeMenuOverlay {
    
    private static final String TAG = "NavigationMenuOverlay";
    
    public NavigationMenuOverlay(Context context) {
        super(context, Corner.LEFT_TOP);
    }
    
    @Override
    protected void initMenuButtons() {
        // 🔙 뒤로가기
        addMenuButton("◀", "뒤로", () -> {
            Log.d(TAG, "뒤로가기 실행");
            performBackAction();
        });
        
        // 🏠 홈
        addMenuButton("⌂", "홈", () -> {
            Log.d(TAG, "홈 버튼 실행");
            performHomeAction();
        });
        
        // ⧉ 최근 앱
        addMenuButton("⧉", "최근", () -> {
            Log.d(TAG, "최근 앱 실행");
            performRecentAppsAction();
        });
        
        // ✕ 앱 종료
        addMenuButton("✕", "종료", () -> {
            Log.d(TAG, "앱 종료 실행");
            performAppCloseAction();
        });
    }
    
    private void performBackAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // AccessibilityService를 통한 뒤로가기
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                );
                Log.d(TAG, "뒤로가기 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "뒤로가기 실행 실패: " + e.getMessage());
        }
    }
    
    private void performHomeAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // AccessibilityService를 통한 홈 버튼
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
                );
                Log.d(TAG, "홈 버튼 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "홈 버튼 실행 실패: " + e.getMessage());
        }
    }
    
    private void performRecentAppsAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // AccessibilityService를 통한 최근 앱
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
                );
                Log.d(TAG, "최근 앱 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "최근 앱 실행 실패: " + e.getMessage());
        }
    }
    
    private void performAppCloseAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // 현재 앱 강제 종료 (뒤로가기를 여러 번 실행)
                for (int i = 0; i < 3; i++) {
                    MyAccessibilityService.getInstance().performGlobalAction(
                        android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                    );
                    try {
                        Thread.sleep(200); // 각 뒤로가기 사이 딜레이
                    } catch (InterruptedException ignored) {}
                }
                Log.d(TAG, "앱 종료 시도 완료");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "앱 종료 실행 실패: " + e.getMessage());
        }
    }
}
