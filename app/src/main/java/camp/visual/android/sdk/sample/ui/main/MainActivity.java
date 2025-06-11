package camp.visual.android.sdk.sample.ui.main;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import camp.visual.android.sdk.sample.R;
import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;
import camp.visual.android.sdk.sample.service.tracking.GazeTrackingService;
import camp.visual.android.sdk.sample.ui.settings.SettingsActivity;
import camp.visual.android.sdk.sample.ui.views.CalibrationViewer;
import camp.visual.android.sdk.sample.ui.views.PointView;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.data.settings.SettingsRepository;
import camp.visual.android.sdk.sample.data.settings.SharedPrefsSettingsRepository;
import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.StatusCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;
import camp.visual.eyedid.gazetracker.constant.StatusErrorType;
import camp.visual.eyedid.gazetracker.metrics.BlinkInfo;
import camp.visual.eyedid.gazetracker.metrics.FaceInfo;
import camp.visual.eyedid.gazetracker.metrics.GazeInfo;
import camp.visual.eyedid.gazetracker.metrics.UserStatusInfo;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;
import camp.visual.eyedid.gazetracker.util.ViewLayoutChecker;

public class MainActivity extends AppCompatActivity {
    private GazeTracker gazeTracker;
    private final String EYEDID_SDK_LICENSE = "dev_ktygge55mai7a041aglteb4onei9a7m9j7tcqagm";
    private final CalibrationModeType calibrationType = CalibrationModeType.DEFAULT;
    private final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };
    private final int REQ_PERMISSION = 1000;
    private final int REQ_OVERLAY_PERMISSION = 1001;

    private View layoutProgress;
    private PointView viewPoint;
    private boolean skipProgress = false;
    // 🔥 종료 버튼 추가
    private Button btnCalibration, btnSettings, btnExit;
    private CalibrationViewer viewCalibration;
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private Handler backgroundHandler;
    private final HandlerThread backgroundThread = new HandlerThread("background");

    private TextView statusText;
    private ProgressBar progressBar;
    private Handler handler = new Handler();
    private SettingsRepository settingsRepository;
    private UserSettings userSettings;

    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    private final TrackingCallback trackingCallback = new TrackingCallback() {
        @Override
        public void onMetrics(long timestamp, GazeInfo gazeInfo, FaceInfo faceInfo, BlinkInfo blinkInfo,
                              UserStatusInfo userStatusInfo) {
            if (gazeInfo.trackingState == TrackingState.SUCCESS) {
                viewPoint.setPosition(gazeInfo.x, gazeInfo.y);
            }
        }

        @Override
        public void onDrop(long timestamp) {
            Log.d("MainActivity", "drop frame " + timestamp);
        }
    };

    private boolean isFirstPoint = false;

    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {

        @Override
        public void onCalibrationProgress(float progress) {
            if (!skipProgress)  {
                runOnUiThread(() -> viewCalibration.setPointAnimationPower(progress));
            }
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            runOnUiThread(() -> {
                viewCalibration.setVisibility(View.VISIBLE);
                if (isFirstPoint) {
                    backgroundHandler.postDelayed(() -> showCalibrationPointView(x, y), 2500);
                } else {
                    showCalibrationPointView(x, y);
                }
            });
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            hideCalibrationView();
            showToast("보정 완료", true);
        }

        @Override
        public void onCalibrationCanceled(double[] doubles) {
            hideCalibrationView();
            showToast("보정 취소", true);
        }
    };

    private final StatusCallback statusCallback = new StatusCallback() {
        @Override
        public void onStarted() {
            runOnUiThread(() -> {
                if (btnCalibration != null) {
                    btnCalibration.setEnabled(true);
                }
                updateStatusText("시선 추적 활성화됨 ✅");
            });
        }

        @Override
        public void onStopped(StatusErrorType error) {
            runOnUiThread(() -> {
                if (btnCalibration != null) {
                    btnCalibration.setEnabled(false);
                }
                updateStatusText("시선 추적 중지됨 ❌");
            });
            if (error != StatusErrorType.ERROR_NONE) {
                if (error == StatusErrorType.ERROR_CAMERA_START) {
                    showToast("카메라 시작 오류", false);
                } else if (error == StatusErrorType.ERROR_CAMERA_INTERRUPT) {
                    showToast("카메라 중단 오류", false);
                }
            }
        }
    };

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnCalibration) {
                Log.d("MainActivity", "시선 보정 버튼 클릭됨");
                showCalibrationDialog();
            } else if (v == btnExit) {
                Log.d("MainActivity", "앱 종료 버튼 클릭됨");
                showExitDialog();
            } else {
                Log.w("MainActivity", "알 수 없는 버튼 클릭: " + v);
            }
        }
    };

    private final InitializationCallback initializationCallback = (gazeTracker, error) -> {
        if (gazeTracker == null) {
            showToast("초기화 오류: " + error.name(), true);
            hideProgress();
        } else {
            if (isServiceRunning()) {
                Log.d("MainActivity", "서비스가 실행 중이므로 MainActivity SDK 사용하지 않음");
                gazeTracker.stopTracking();
                if (btnCalibration != null) {
                    btnCalibration.setEnabled(true);
                }
                updateStatusText("서비스 연결됨 ✅");
            } else {
                this.gazeTracker = gazeTracker;
                this.gazeTracker.setTrackingCallback(trackingCallback);
                this.gazeTracker.setCalibrationCallback(calibrationCallback);
                this.gazeTracker.setStatusCallback(statusCallback);

                this.gazeTracker.startTracking();

                runOnUiThread(() -> {
                    if (btnCalibration != null) {
                        btnCalibration.setEnabled(true);
                    }
                    updateStatusText("시선 추적 초기화됨 ✅");
                });
            }
        }
        hideProgress();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        settingsRepository = new SharedPrefsSettingsRepository(this);
        userSettings = settingsRepository.getUserSettings();

        initViews();
        checkPermission();
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        startServicesAndCheckPermissions();
    }

    private void showCalibrationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("시선 보정")
                .setMessage("화면에 나타나는 5개의 점을 차례로 응시해 주세요.\n\n" +
                        "⚠️ 기존 위치 조정값이 초기화되고 새로운 보정을 실행합니다.\n\n" +
                        "약 10-15초 정도 소요됩니다.")
                .setPositiveButton("시작", (dialog, which) -> {
                    if (isServiceRunning()) {
                        startCalibration();
                        showToast("시선 보정 시작", true);
                    } else {
                        showToast("시선 추적 시스템 초기화 중", false);
                        showProgress();
                        initTracker();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 🆕 앱 종료 확인 다이얼로그
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("시선 추적 서비스를 완전히 종료하시겠습니까?")
                .setPositiveButton("종료", (dialog, which) -> {
                    exitApp();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 🆕 앱 완전 종료 메서드
    private void exitApp() {
        try {
            // 1. 서비스 중지
            if (isServiceRunning()) {
                Intent serviceIntent = new Intent(this, GazeTrackingService.class);
                stopService(serviceIntent);
                Log.d("MainActivity", "서비스 중지됨");
            }

            // 2. 본 액티비티 SDK 중지
            if (gazeTracker != null) {
                gazeTracker.stopTracking();
                gazeTracker = null;
                Log.d("MainActivity", "MainActivity SDK 중지됨");
            }

            // 3. 백그라운드 스레드 정리
            if (backgroundThread != null) {
                backgroundThread.quitSafely();
            }

            // 4. 액티비티 종료
            finish();

            // 5. 시스템에 앱 완전 종료 요청
            System.exit(0);

        } catch (Exception e) {
            Log.e("MainActivity", "앱 종료 중 오류: " + e.getMessage(), e);
            // 오류가 있어도 강제 종료
            finish();
            System.exit(0);
        }
    }

    private void startServicesAndCheckPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();
        } else {
            if (!isAccessibilityServiceEnabled()) {
                showAccessibilityPermissionDialog();
            }

            Intent serviceIntent = new Intent(this, GazeTrackingService.class);
            startForegroundService(serviceIntent);

            showServiceStartMessage();
        }
    }

    private void showServiceStartMessage() {
        handler.postDelayed(() -> {
            if (isServiceRunning()) {
                Toast.makeText(this, "시선 추적 활성화", Toast.LENGTH_SHORT).show();
                updateStatusText("시선 추적 활성화됨 ✅");
            }
        }, 1500);
    }

    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎯 시선 커서 표시 권한 설정")
                .setMessage("시선을 따라 움직이는 커서를 표시하기 위해 권한이 필요합니다.\n\n" +
                        "📋 다음 화면에서 할 일:\n" +
                        "1️⃣ 'EyedidSampleApp' 확인 (이미 선택됨)\n" +
                        "2️⃣ '다른 앱 위에 표시' 토글 ON\n" +
                        "3️⃣ 뒤로가기 버튼으로 앱 복귀\n\n" +
                        "💡 이 설정은 한 번만 하면 계속 유지됩니다!")
                .setPositiveButton("🔧 설정 화면으로", (dialog, which) -> {
                    openOverlaySettings();
                })
                .setNegativeButton("나중에", (dialog, which) -> {
                    updateStatusText("오버레이 권한 필요 ⚠️");
                    Toast.makeText(this, "시선 커서 없이 사용됩니다", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void openOverlaySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
            
            // 🆕 권한 모니터링 시작
            startOverlayPermissionMonitoring();
            
            // 🆕 복귀 유도 토스트
            Toast.makeText(this, "🔍 권한 설정을 기다리는 중... 설정 후 뒤로가기를 눌러주세요", Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e("MainActivity", "오버레이 설정 화면 열기 실패: " + e.getMessage());
            Toast.makeText(this, "설정 화면을 열 수 없습니다. 수동으로 설정해주세요", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);

            Log.d("MainActivity", "접근성 서비스 전체 활성화 상태: " + accessibilityEnabled);

            if (accessibilityEnabled != 1) {
                Log.d("MainActivity", "접근성 서비스가 전체적으로 비활성화됨");
                return false;
            }

            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            Log.d("MainActivity", "활성화된 접근성 서비스들: " + enabledServices);

            if (enabledServices == null || enabledServices.isEmpty()) {
                Log.d("MainActivity", "활성화된 접근성 서비스가 없음");
                return false;
            }

            final String packageName = getPackageName();
            final String serviceName = MyAccessibilityService.class.getSimpleName();
            final String fullServiceName = MyAccessibilityService.class.getName();

            boolean isEnabled = enabledServices.contains(packageName + "/" + fullServiceName) ||
                    enabledServices.contains(packageName + "/" + serviceName) ||
                    enabledServices.contains(fullServiceName) ||
                    enabledServices.contains(serviceName);

            Log.d("MainActivity", "우리 접근성 서비스 활성화 상태: " + isEnabled);
            Log.d("MainActivity", "찾는 서비스: " + packageName + "/" + fullServiceName);

            return isEnabled;

        } catch (Exception e) {
            Log.e("MainActivity", "접근성 서비스 확인 중 오류: " + e.getMessage(), e);
            return false;
        }
    }

    private void showAccessibilityPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎮 시선 터치/스크롤 설정")
                .setMessage("시선으로 클릭하고 스크롤하기 위해 접근성 서비스가 필요합니다.\n\n" +
                        "📋 설정 화면에서 할 일:\n" +
                        "1️⃣ 'EyedidSampleApp' 찾기\n" +
                        "2️⃣ 앱 이름 터치\n" +
                        "3️⃣ 상단 토글 스위치 ON\n" +
                        "4️⃣ '확인' 버튼 클릭 (보안 경고 무시)\n" +
                        "5️⃣ 뒤로가기로 앱 복귀\n\n" +
                        "🔒 보안 경고가 나와도 '확인'을 눌러주세요!\n" +
                        "💡 이 권한은 시선 터치 기능에만 사용됩니다.")
                .setPositiveButton("🔧 설정하러 가기", (d, which) -> {
                    openAccessibilitySettingsDirectly();
                })
                .setNegativeButton("🎯 일단 보정만", (d, which) -> {
                    Toast.makeText(this, "시선 보정은 가능하지만, 터치/스크롤 기능은 제한됩니다", Toast.LENGTH_LONG).show();
                    updateStatusText("접근성 서비스 권한 필요 ⚠️");
                })
                .show();
    }

    private void openAccessibilitySettingsDirectly() {
        try {
            // 🎯 방법 1: 우리 앱 접근성 서비스로 바로 이동 시도
            ComponentName componentName = new ComponentName(getPackageName(),
                    MyAccessibilityService.class.getName());
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle bundle = new Bundle();
                String showArgs = componentName.flattenToString();
                bundle.putString(":settings:fragment_args_key", showArgs);
                intent.putExtra(":settings:show_fragment_args", bundle);
                intent.putExtra(":settings:fragment_args_key", showArgs);
                intent.putExtra(":settings:show_fragment_title", "EyedidSampleApp");
            }

            startActivity(intent);
            
            // 🆕 접근성 권한 모니터링 시작
            startAccessibilityPermissionMonitoring();
            
            // 🆕 상세 가이드 표시
            showDetailedAccessibilityGuide();

        } catch (Exception e) {
            Log.d("MainActivity", "직접 이동 실패, 대안 방법 시도: " + e.getMessage());
            // 🆕 대안: 앱 정보 화면으로 이동
            openAppInfoForAccessibility();
        }
    }

    // 🆕 대안 방법: 앱 정보 → 접근성으로 이동
    private void openAppInfoForAccessibility() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            
            Toast.makeText(this, "📱 앱 정보 → 접근성 → 서비스 사용 ON", Toast.LENGTH_LONG).show();
            startAccessibilityPermissionMonitoring();
            
        } catch (Exception ex) {
            Log.e("MainActivity", "앱 정보 화면도 열기 실패: " + ex.getMessage());
            // 최후의 방법: 일반 접근성 설정
            openGeneralAccessibilitySettings();
        }
    }

    // 🆕 최후 대안: 일반 접근성 설정
    private void openGeneralAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "📋 설치된 앱 → EyedidSampleApp → 토글 ON", Toast.LENGTH_LONG).show();
            startAccessibilityPermissionMonitoring();
        } catch (Exception e) {
            Toast.makeText(this, "설정 화면을 열 수 없습니다. 수동으로 설정해주세요", Toast.LENGTH_LONG).show();
        }
    }

    // 🆕 더 상세한 접근성 가이드
    private void showDetailedAccessibilityGuide() {
        handler.postDelayed(() -> {
            Toast.makeText(this, "🔍 접근성 서비스 활성화를 기다리는 중...", Toast.LENGTH_LONG).show();
        }, 2000);
        
        handler.postDelayed(() -> {
            Toast.makeText(this, "💡 설정 완료 후 뒤로가기를 눌러 앱으로 돌아와주세요", Toast.LENGTH_LONG).show();
        }, 5000);
    }

    // 🆕 권한 모니터링 시스템
    private Runnable overlayPermissionMonitor;
    private Runnable accessibilityPermissionMonitor;
    private boolean isMonitoringOverlay = false;
    private boolean isMonitoringAccessibility = false;
    private int overlayCheckCount = 0;
    private int accessibilityCheckCount = 0;
    private static final int MAX_MONITOR_CHECKS = 60; // 5분간 모니터링

    // 🆕 오버레이 권한 모니터링 시작
    private void startOverlayPermissionMonitoring() {
        if (isMonitoringOverlay) return;
        
        isMonitoringOverlay = true;
        overlayCheckCount = 0;
        
        overlayPermissionMonitor = new Runnable() {
            @Override
            public void run() {
                if (overlayCheckCount >= MAX_MONITOR_CHECKS) {
                    stopOverlayPermissionMonitoring();
                    return;
                }
                
                overlayCheckCount++;
                
                if (Settings.canDrawOverlays(MainActivity.this)) {
                    // 🎉 오버레이 권한 설정 완료!
                    onOverlayPermissionGranted();
                    stopOverlayPermissionMonitoring();
                } else {
                    // 5초 후 다시 체크
                    handler.postDelayed(this, 5000);
                }
            }
        };
        
        handler.postDelayed(overlayPermissionMonitor, 3000); // 3초 후 시작
    }

    // 🆕 접근성 권한 모니터링 시작
    private void startAccessibilityPermissionMonitoring() {
        if (isMonitoringAccessibility) return;
        
        isMonitoringAccessibility = true;
        accessibilityCheckCount = 0;
        
        accessibilityPermissionMonitor = new Runnable() {
            @Override
            public void run() {
                if (accessibilityCheckCount >= MAX_MONITOR_CHECKS) {
                    stopAccessibilityPermissionMonitoring();
                    return;
                }
                
                accessibilityCheckCount++;
                
                if (isAccessibilityServiceEnabled()) {
                    // 🎉 접근성 서비스 활성화 완료!
                    onAccessibilityPermissionGranted();
                    stopAccessibilityPermissionMonitoring();
                } else {
                    // 5초 후 다시 체크
                    handler.postDelayed(this, 5000);
                }
            }
        };
        
        handler.postDelayed(accessibilityPermissionMonitor, 3000); // 3초 후 시작
    }

    // 🆕 오버레이 권한 설정 완료 처리
    private void onOverlayPermissionGranted() {
        runOnUiThread(() -> {
            showToast("✅ 오버레이 권한 설정 완료!", true);
            updateStatusText("오버레이 권한 설정됨 ✅");
            
            // 다음 단계로 자동 진행
            handler.postDelayed(() -> {
                if (!isAccessibilityServiceEnabled()) {
                    showToast("🎮 이제 접근성 서비스만 설정하면 끝이에요!", true);
                    handler.postDelayed(() -> {
                        showAccessibilityPermissionDialog();
                    }, 2000);
                } else {
                    // 모든 권한 완료!
                    showAllPermissionsCompleteDialog();
                }
            }, 1500);
        });
    }

    // 🆕 접근성 권한 설정 완료 처리
    private void onAccessibilityPermissionGranted() {
        runOnUiThread(() -> {
            showToast("✅ 접근성 서비스 활성화 완료!", true);
            updateStatusText("모든 권한 설정 완료 ✅");
            
            // 모든 설정 완료!
            handler.postDelayed(() -> {
                showAllPermissionsCompleteDialog();
            }, 1500);
        });
    }

    // 🆕 모든 권한 설정 완료 다이얼로그
    private void showAllPermissionsCompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 설정 완료!")
                .setMessage("모든 권한 설정이 완료되었습니다!\n\n" +
                           "🎯 이제 다음 단계를 진행해보세요:\n" +
                           "1️⃣ 정밀 보정으로 시선 추적 정확도 최적화\n" +
                           "2️⃣ 시선으로 화면 터치해보기\n" +
                           "3️⃣ 화면 가장자리 응시로 스크롤/메뉴 사용\n\n" +
                           "💡 언제든 '시선 보정' 버튼으로 정확도를 개선할 수 있어요!")
                .setPositiveButton("🎯 정밀 보정 시작", (dialog, which) -> {
                    showCalibrationDialog();
                })
                .setNegativeButton("나중에 하기", (dialog, which) -> {
                    showToast("모든 기능이 준비되었습니다! 언제든 시선 보정을 해보세요", true);
                })
                .show();
    }

    // 🆕 권한 모니터링 중지 메서드들
    private void stopOverlayPermissionMonitoring() {
        isMonitoringOverlay = false;
        if (overlayPermissionMonitor != null) {
            handler.removeCallbacks(overlayPermissionMonitor);
        }
    }

    private void stopAccessibilityPermissionMonitoring() {
        isMonitoringAccessibility = false;
        if (accessibilityPermissionMonitor != null) {
            handler.removeCallbacks(accessibilityPermissionMonitor);
        }
    }

    // 🆕 모든 권한 상태 체크 및 자동 진행
    private void checkAllPermissionsAndProceed() {
        boolean hasOverlay = Settings.canDrawOverlays(this);
        boolean hasAccessibility = isAccessibilityServiceEnabled();
        
        Log.d("MainActivity", "권한 상태 체크 - 오버레이: " + hasOverlay + ", 접근성: " + hasAccessibility);
        
        if (hasOverlay && hasAccessibility) {
            // 🎉 모든 권한 완료!
            updateStatusText("모든 권한 설정 완료 ✅");
            if (!hasShownWelcomeDialog) {
                hasShownWelcomeDialog = true;
                showAllPermissionsCompleteDialog();
            }
        } else if (hasOverlay && !hasAccessibility) {
            // 오버레이는 됐고, 접근성만 남음
            updateStatusText("접근성 서비스 권한 필요 ⚠️");
            showToast("✅ 오버레이 권한 완료! 이제 접근성 서비스만 설정하면 끝이에요", true);
        } else if (!hasOverlay && hasAccessibility) {
            // 접근성은 됐고, 오버레이만 남음
            updateStatusText("오버레이 권한 필요 ⚠️");
            showToast("✅ 접근성 서비스 완료! 이제 오버레이 권한만 설정하면 끝이에요", true);
        } else {
            // 둘 다 안됨
            updateStatusText("권한 설정 필요 ⚠️");
        }
    }

    // 🆕 환영 다이얼로그 표시 여부 플래그
    private boolean hasShownWelcomeDialog = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                showToast("오버레이 권한 허용됨", true);
                if (!isAccessibilityServiceEnabled()) {
                    showAccessibilityPermissionDialog();
                }

                Intent serviceIntent = new Intent(this, GazeTrackingService.class);
                startForegroundService(serviceIntent);
            } else {
                showToast("오버레이 권한이 필요합니다", false);
                updateStatusText("오버레이 권한 필요 ⚠️");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        userSettings = settingsRepository.getUserSettings();

        // 🆕 권한 설정에서 돌아왔는지 확인하고 자동 진행
        checkAllPermissionsAndProceed();

        if (!Settings.canDrawOverlays(this)) {
            updateStatusText("오버레이 권한 필요 ⚠️");
            return;
        }

        if (!isAccessibilityServiceEnabled()) {
            updateStatusText("접근성 서비스 권한 필요 ⚠️");
            handler.postDelayed(() -> {
                showToast("접근성 서비스를 활성화하면 시선으로 터치할 수 있습니다", true);
            }, 1000);
        }

        if (isServiceRunning()) {
            Log.d("MainActivity", "서비스 실행 중 - UI 활성화");

            if (btnCalibration != null) {
                btnCalibration.setEnabled(true);
            }
            hideProgress();
            updateStatusText("시선 추적 활성화됨 ✅");

            if (gazeTracker != null) {
                Log.d("MainActivity", "서비스 실행 중이므로 MainActivity tracker 해제");
                gazeTracker.stopTracking();
                gazeTracker = null;
            }
        } else {
            Log.d("MainActivity", "서비스 시작");
            updateStatusText("서비스 시작 중...");

            Intent serviceIntent = new Intent(this, GazeTrackingService.class);
            startForegroundService(serviceIntent);

            backgroundHandler.postDelayed(() -> {
                runOnUiThread(() -> {
                    if (isServiceRunning()) {
                        if (btnCalibration != null) {
                            btnCalibration.setEnabled(true);
                        }
                        hideProgress();
                        updateStatusText("시선 추적 활성화됨 ✅");
                        showServiceStartMessage();
                    } else {
                        updateStatusText("서비스 시작 실패 ❌");
                    }
                });
            }, 2000);
        }
    }

    private void initViews() {
        Log.d("MainActivity", "initViews 시작");

        try {
            TextView txtSDKVersion = findViewById(R.id.txt_sdk_version);
            if (txtSDKVersion != null) {
                txtSDKVersion.setText(GazeTracker.getVersionName());
                Log.d("MainActivity", "SDK 버전 텍스트 설정 완료");
            } else {
                Log.w("MainActivity", "txt_sdk_version을 찾을 수 없음");
            }

            layoutProgress = findViewById(R.id.layout_progress);
            viewCalibration = findViewById(R.id.view_calibration);
            viewPoint = findViewById(R.id.view_point);
            statusText = findViewById(R.id.text_status);
            progressBar = findViewById(R.id.progress_bar);

            Log.d("MainActivity", "기본 뷰들 초기화 완료");

            // 🔥 시선 보정 버튼만 찾기 (실제 XML에 있는 ID만 사용)
            btnCalibration = findViewById(R.id.btn_calibration);

            if (btnCalibration != null) {
                btnCalibration.setOnClickListener(onClickListener);
                btnCalibration.setText("시선 보정");
                btnCalibration.setEnabled(false);
                Log.d("MainActivity", "시선 보정 버튼 초기화 성공");
            } else {
                Log.e("MainActivity", "btn_calibration을 찾을 수 없음");
            }

            // 🔥 설정 버튼 안전하게 초기화
            btnSettings = findViewById(R.id.btn_settings);
            if (btnSettings != null) {
                btnSettings.setOnClickListener(view -> {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                });
                Log.d("MainActivity", "설정 버튼 초기화 성공");
            } else {
                Log.w("MainActivity", "설정 버튼을 찾을 수 없음");
            }

            // 🆕 종료 버튼 연결
            btnExit = findViewById(R.id.btn_exit);
            if (btnExit != null) {
                btnExit.setOnClickListener(onClickListener);
                Log.d("MainActivity", "종료 버튼 초기화 성공");
            } else {
                Log.w("MainActivity", "종료 버튼을 찾을 수 없음");
            }

            // 뷰 포인트 안전하게 설정
            if (viewPoint != null) {
                viewPoint.setPosition(-999, -999);
                Log.d("MainActivity", "뷰 포인트 초기화 성공");
            }

            updateStatusText("시스템 초기화 중...");

            // 레이아웃 체커 안전하게 설정
            if (viewCalibration != null && viewPoint != null) {
                viewCalibration.post(() -> {
                    viewLayoutChecker.setOverlayView(viewPoint, (x, y) -> {
                        viewPoint.setOffset(x, y);
                        viewCalibration.setOffset(x, y);
                        Log.d("MainActivity", "Offset 설정됨: x=" + x + ", y=" + y);
                    });
                });
            }

            Log.d("MainActivity", "initViews 완료");

        } catch (Exception e) {
            Log.e("MainActivity", "initViews 전체 오류: " + e.getMessage(), e);
        }
    }

    private void updateStatusText(String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
    }

    private void showProgress() {
        if (layoutProgress != null) {
            runOnUiThread(() -> layoutProgress.setVisibility(View.VISIBLE));
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        updateStatusText("시스템 초기화 중...");
    }

    private void hideProgress() {
        if (layoutProgress != null) {
            runOnUiThread(() -> layoutProgress.setVisibility(View.GONE));
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showToast(String message, boolean isSuccess) {
        Toast.makeText(this, message, isSuccess ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }

    private void checkPermission() {
        if (hasPermissions()) {
            checkPermission(true);
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_PERMISSION);
        }
    }

    private boolean hasPermissions() {
        int result;
        for (String perms : PERMISSIONS) {
            if (perms.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                if (!Settings.canDrawOverlays(this)) {
                    return false;
                }
            }
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private void checkPermission(boolean isGranted) {
        if (!isGranted) {
            showToast("권한이 필요합니다", true);
            finish();
        } else {
            permissionGranted();
        }
    }

    private void permissionGranted() {
        if (isServiceRunning()) {
            Log.d("MainActivity", "서비스가 이미 실행 중입니다. SDK 재초기화를 건너뜁니다.");
            hideProgress();
            if (btnCalibration != null) {
                btnCalibration.setEnabled(true);
            }
            updateStatusText("서비스 연결됨 ✅");
            showToast("시선 추적 서비스 연결됨", true);
        } else {
            showProgress();
            initTracker();
        }
    }

    private boolean isServiceRunning() {
        return GazeTrackingService.getInstance() != null;
    }

    private void initTracker() {
        if (isServiceRunning()) {
            Log.d("MainActivity", "서비스 연결 완료");
            if (btnCalibration != null) {
                btnCalibration.setEnabled(true);
            }
            hideProgress();
            updateStatusText("서비스 연결됨 ✅");
        } else {
            Log.d("MainActivity", "새로운 SDK 초기화 시작");
            GazeTrackerOptions options = new GazeTrackerOptions.Builder().build();
            GazeTracker.initGazeTracker(this, EYEDID_SDK_LICENSE, initializationCallback, options);
        }
    }

    private void hideCalibrationView() {
        runOnUiThread(() -> {
            viewCalibration.setVisibility(View.INVISIBLE);
            if (btnCalibration != null) {
                btnCalibration.setEnabled(true);
            }
            viewPoint.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0) {
                boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                checkPermission(cameraPermissionAccepted);
            }
        }
    }

    private void showCalibrationPointView(final float x, final float y){
        skipProgress = true;
        viewCalibration.setPointAnimationPower(0);
        viewCalibration.setEnableText(false);
        viewCalibration.nextPointColor();
        viewCalibration.setPointPosition(x, y);
        long delay = isFirstPoint ? 0 : 1200;

        backgroundHandler.postDelayed(() -> {
            if(gazeTracker != null)
                gazeTracker.startCollectSamples();
            skipProgress = false;
        }, delay);

        isFirstPoint = false;
    }

    private void startCalibration() {
        Log.d("MainActivity", "캘리브레이션 시작 요청");

        boolean serviceRunning = isServiceRunning();
        Log.d("MainActivity", "서비스 실행 상태: " + serviceRunning);

        if (serviceRunning) {
            Log.d("MainActivity", "서비스에서 캘리브레이션 실행 시도");
            try {
                GazeTrackingService service = GazeTrackingService.getInstance();

                if (service != null) {
                    service.triggerCalibration();
                } else {
                    showToast("서비스에 연결할 수 없습니다", false);
                    Intent serviceIntent = new Intent(this, GazeTrackingService.class);
                    startForegroundService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e("MainActivity", "서비스 캘리브레이션 호출 중 오류: " + e.getMessage(), e);
                showToast("캘리브레이션 실행 오류", false);
                attemptMainActivityCalibration();
            }
            return;
        }

        attemptMainActivityCalibration();
    }

    private void attemptMainActivityCalibration() {
        Log.d("MainActivity", "MainActivity에서 캘리브레이션 실행 시도");

        if (gazeTracker == null) {
            Log.e("MainActivity", "GazeTracker가 null입니다");
            showToast("시선 추적기가 초기화되지 않았습니다", false);
            showProgress();
            initTracker();
            return;
        }

        boolean isSuccess = gazeTracker.startCalibration(calibrationType);

        if (isSuccess) {
            isFirstPoint = true;
            runOnUiThread(() -> {
                viewCalibration.setPointPosition(-9999, -9999);
                viewCalibration.setEnableText(true);
                viewPoint.setVisibility(View.INVISIBLE);
                if (btnCalibration != null) {
                    btnCalibration.setEnabled(false);
                }
            });
        } else {
            showToast("캘리브레이션 시작 실패", false);
        }
    }

    public void triggerCalibrationFromService() {
        runOnUiThread(() -> {
            if (btnCalibration != null && btnCalibration.isEnabled()) {
                startCalibration();
            } else {
                showToast("캘리브레이션을 시작할 수 없습니다", false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 🆕 권한 모니터링 중지
        stopOverlayPermissionMonitoring();
        stopAccessibilityPermissionMonitoring();
        
        if (gazeTracker != null) {
            gazeTracker.stopTracking();
        }
        backgroundThread.quitSafely();
        instance = null;
    }
}