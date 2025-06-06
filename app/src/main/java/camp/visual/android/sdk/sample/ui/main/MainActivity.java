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
    private final String EYEDID_SDK_LICENSE = "dev_plnp4o1ya7d0tif2rmgko169l1z4jnali2q4f63f";
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
                .setTitle("화면 오버레이 권한 필요")
                .setMessage("시선 커서를 표시하기 위해 권한이 필요합니다.\n\n" +
                        "설정에서 'EyedidSampleApp'을 찾아 허용해주세요.")
                .setPositiveButton("설정으로 이동", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
                })
                .setNegativeButton("나중에", null)
                .setCancelable(false)
                .show();
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
                .setTitle("접근성 서비스 설정")
                .setMessage("시선으로 터치하고 스크롤하기 위해 접근성 서비스가 필요합니다.\n\n" +
                        "💡 이 설정은 한 번만 하면 계속 유지됩니다.\n\n" +
                        "설정에서 'EyedidSampleApp'을 찾아 활성화해주세요.")
                .setPositiveButton("설정으로 이동", (d, which) -> {
                    openAccessibilitySettings();
                })
                .setNegativeButton("나중에", null)
                .show();
    }

    private void openAccessibilitySettings() {
        try {
            ComponentName componentName = new ComponentName(getPackageName(),
                    MyAccessibilityService.class.getName());
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle bundle = new Bundle();
                String showArgs = componentName.flattenToString();
                bundle.putString(":settings:fragment_args_key", showArgs);
                intent.putExtra(":settings:show_fragment_args", bundle);
                intent.putExtra(":settings:fragment_args_key", showArgs);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            showToast("'EyedidSampleApp'을 찾아 활성화해주세요", false);

        } catch (Exception e) {
            Log.d("MainActivity", "특정 서비스 설정 이동 실패, 일반 설정으로 이동");
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallbackIntent);
                showToast("접근성 설정에서 'EyedidSampleApp'을 찾아 활성화해주세요", false);
            } catch (Exception ex) {
                showToast("설정 화면을 열 수 없습니다", false);
            }
        }
    }

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
        if (gazeTracker != null) {
            gazeTracker.stopTracking();
        }
        backgroundThread.quitSafely();
        instance = null;
    }
}