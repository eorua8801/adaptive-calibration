package camp.visual.android.sdk.sample.ui.views.overlay;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;

public class SystemMenuOverlay extends EdgeMenuOverlay {
    
    private static final String TAG = "SystemMenuOverlay";
    private Context context;
    
    public SystemMenuOverlay(Context context) {
        super(context, Corner.RIGHT_TOP);
        this.context = context;
    }
    
    @Override
    protected void initMenuButtons() {
        // 🔔 알림창
        addMenuButton("🔔", "알림", () -> {
            Log.d(TAG, "알림창 열기 실행");
            performNotificationAction();
        });
        
        // ⚙️ 설정
        addMenuButton("⚙", "설정", () -> {
            Log.d(TAG, "설정 열기 실행");
            performSettingsAction();
        });
        
        // 📷 화면캡처
        addMenuButton("📷", "캡처", () -> {
            Log.d(TAG, "화면캡처 실행");
            performScreenshotAction();
        });
        
        // 🔊 볼륨
        addMenuButton("🔊", "볼륨", () -> {
            Log.d(TAG, "볼륨 조절 실행");
            performVolumeAction();
        });
    }
    
    private void performNotificationAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // AccessibilityService를 통한 알림창 열기
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                );
                Log.d(TAG, "알림창 열기 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "알림창 열기 실패: " + e.getMessage());
        }
    }
    
    private void performSettingsAction() {
        try {
            // 시스템 설정 열기
            Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            Log.d(TAG, "설정 열기 성공");
        } catch (Exception e) {
            Log.e(TAG, "설정 열기 실패: " + e.getMessage());
        }
    }
    
    private void performScreenshotAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // Android 9+ 에서 지원하는 스크린샷 기능
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    MyAccessibilityService.getInstance().performGlobalAction(
                        android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                    );
                    Log.d(TAG, "스크린샷 성공");
                } else {
                    // 구형 버전에서는 전원+볼륨다운 조합 시뮬레이션
                    Log.d(TAG, "구형 안드로이드에서는 스크린샷 기능이 제한됨");
                }
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "스크린샷 실행 실패: " + e.getMessage());
        }
    }
    
    private void performVolumeAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // 퀵 설정 패널 열기 (볼륨 조절 포함)
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
                );
                Log.d(TAG, "퀵 설정 (볼륨) 열기 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "볼륨 조절 실행 실패: " + e.getMessage());
        }
    }
}
