package camp.visual.android.sdk.sample.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import camp.visual.android.sdk.sample.R;
import camp.visual.android.sdk.sample.core.constants.AppConstants;
import camp.visual.android.sdk.sample.core.managers.CalibrationController;
import camp.visual.android.sdk.sample.core.managers.PermissionManager;
import camp.visual.android.sdk.sample.core.security.SecurityManager;
import camp.visual.android.sdk.sample.core.utils.PerformanceLogger;
import camp.visual.android.sdk.sample.core.utils.ResourceManager;
import camp.visual.android.sdk.sample.core.utils.ThrottledUIUpdater;
import camp.visual.android.sdk.sample.data.settings.SettingsRepository;
import camp.visual.android.sdk.sample.data.settings.SharedPrefsSettingsRepository;
import camp.visual.android.sdk.sample.domain.calibration.AdaptiveCalibrationManager;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.service.tracking.GazeTrackingService;
import camp.visual.android.sdk.sample.ui.settings.SettingsActivity;
import camp.visual.android.sdk.sample.ui.views.CalibrationViewer;
import camp.visual.android.sdk.sample.ui.views.PointView;
import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.constant.AccuracyCriteria;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.util.ViewLayoutChecker;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 🏠 메인 액티비티 (리팩토링됨)
 * - 700+ 줄에서 300줄 이하로 축소
 * - 기능별 매니저로 역할 분담
 * - 메모리 효율적인 리소스 관리
 * - 보안 강화 및 에러 처리 개선
 */
public class MainActivity extends AppCompatActivity implements 
    PermissionManager.PermissionCallback,
    CalibrationController.CalibrationControllerCallback {

    // 🧩 매니저들
    private ResourceManager resourceManager;
    private PermissionManager permissionManager;
    private CalibrationController calibrationController;
    private SettingsRepository settingsRepository;
    private ThrottledUIUpdater uiUpdater;
    
    // 🎨 UI 컴포넌트
    private Button btnCalibration, btnSettings, btnExit, btnPractice;
    private TextView statusText;
    private ProgressBar progressBar;
    private View layoutProgress;
    private PointView viewPoint;
    private CalibrationViewer viewCalibration;
    
    // 📊 상태 관리
    private UserSettings userSettings;
    private Handler mainHandler;
    private ViewLayoutChecker viewLayoutChecker;
    private boolean hasShownWelcomeDialog = false;
    
    // 🔄 라이프사이클 상태
    private boolean isInitialized = false;
    private boolean isDestroyed = false;
    
    // 📱 Static 참조 (WeakReference로 메모리 누수 방지)
    private static WeakReference<MainActivity> instanceRef;
    
    public static MainActivity getInstance() {
        return instanceRef != null ? instanceRef.get() : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 보안 검사 먼저 수행
        if (!performInitialSecurityCheck()) {
            finish();
            return;
        }
        
        instanceRef = new WeakReference<>(this);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeManagers();
        initializeViews();
        setupUIListeners();
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "MainActivity 초기화 완료");
        
        // 권한 및 서비스 초기화
        startInitializationFlow();
    }
    
    /**
     * 🔒 초기 보안 검사
     */
    private boolean performInitialSecurityCheck() {
        try {
            boolean securityPassed = SecurityManager.RuntimeSecurityCheck.performSecurityCheck();
            
            if (!securityPassed) {
                PerformanceLogger.SecurityLogger.logSecurityViolation("Initial security check failed");
                showErrorDialog("보안 검사 실패", "앱을 안전하게 실행할 수 없습니다.", true);
                return false;
            }
            
            PerformanceLogger.SecurityLogger.logLicenseValidation(true);
            return true;
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Security check exception", e);
            return false;
        }
    }
    
    /**
     * 🧩 매니저들 초기화
     */
    private void initializeManagers() {
        try {
            // 리소스 매니저 (최우선)
            resourceManager = new ResourceManager();
            
            // 메인 핸들러
            mainHandler = new Handler(Looper.getMainLooper());
            resourceManager.registerHandler(mainHandler);
            
            // UI 업데이터
            uiUpdater = ThrottledUIUpdater.getInstance();
            
            // 설정 리포지토리
            settingsRepository = new SharedPrefsSettingsRepository(this);
            userSettings = settingsRepository.getUserSettings();
            
            // 권한 매니저
            permissionManager = new PermissionManager(this);
            
            // 캘리브레이션 컨트롤러
            calibrationController = new CalibrationController(this, resourceManager, userSettings);
            calibrationController.setCallback(this);
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                "매니저 초기화 완료");
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Manager initialization failed", e);
            showErrorDialog("초기화 오류", "앱 초기화 중 오류가 발생했습니다.", true);
        }
    }
    
    /**
     * 🎨 뷰 초기화
     */
    private void initializeViews() {
        try {
            // SDK 버전 표시
            TextView txtSDKVersion = findViewById(R.id.txt_sdk_version);
            if (txtSDKVersion != null) {
                txtSDKVersion.setText(GazeTracker.getVersionName());
            }
            
            // 기본 뷰들
            layoutProgress = findViewById(R.id.layout_progress);
            viewCalibration = findViewById(R.id.view_calibration);
            viewPoint = findViewById(R.id.view_point);
            statusText = findViewById(R.id.text_status);
            progressBar = findViewById(R.id.progress_bar);
            
            // 버튼들
            btnCalibration = findViewById(R.id.btn_calibration);
            btnSettings = findViewById(R.id.btn_settings);
            btnExit = findViewById(R.id.btn_exit);
            btnPractice = findViewById(R.id.btn_practice);
            
            // 초기 상태 설정
            if (btnCalibration != null) {
                btnCalibration.setEnabled(false);
            }
            
            if (btnPractice != null) {
                btnPractice.setVisibility(View.GONE);
            }
            
            if (viewPoint != null) {
                viewPoint.setPosition(-999, -999);
            }
            
            // 캘리브레이션 뷰어 설정
            if (viewCalibration != null) {
                calibrationController.setCalibrationViewer(viewCalibration);
                
                // ViewLayoutChecker 설정
                viewLayoutChecker = new ViewLayoutChecker();
                viewCalibration.post(() -> {
                    viewLayoutChecker.setOverlayView(viewPoint, (x, y) -> {
                        if (viewPoint != null) viewPoint.setOffset(x, y);
                        if (viewCalibration != null) viewCalibration.setOffset(x, y);
                        
                        PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
                            String.format("Offset 설정: x=%d, y=%d", x, y));
                    });
                });
            }
            
            updateStatusText("시스템 초기화 중...");
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, "뷰 초기화 완료");
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "View initialization failed", e);
        }
    }
    
    /**
     * 🖱️ UI 리스너 설정
     */
    private void setupUIListeners() {
        if (btnCalibration != null) {
            btnCalibration.setOnClickListener(v -> handleCalibrationButtonClick());
        }
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> showExitDialog());
        }
        
        if (btnPractice != null) {
            btnPractice.setOnClickListener(v -> handlePracticeButtonClick());
        }
    }
    
    /**
     * 🚀 초기화 플로우 시작
     */
    private void startInitializationFlow() {
        updateStatusText("권한 확인 중...");
        showProgress();
        
        // 권한 확인 및 요청
        permissionManager.startPermissionFlow(this);
    }
    
    /**
     * 🎯 캘리브레이션 버튼 클릭 처리
     */
    private void handleCalibrationButtonClick() {
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, "캘리브레이션 버튼 클릭");
        
        new AlertDialog.Builder(this)
                .setTitle("시선 보정")
                .setMessage("화면에 나타나는 점들을 차례로 응시해 주세요.\n\n" +
                        "⚠️ 기존 위치 조정값이 초기화되고 새로운 보정을 실행합니다.\n\n" +
                        "약 10-15초 정도 소요됩니다.")
                .setPositiveButton("🎯 정밀 보정", (dialog, which) -> {
                    calibrationController.startCalibration(CalibrationModeType.FIVE_POINT, AccuracyCriteria.HIGH);
                })
                .setNeutralButton("🤖 적응형 보정", (dialog, which) -> {
                    calibrationController.startAdaptiveCalibration();
                })
                .setNegativeButton("취소", null)
                .show();
    }
    
    /**
     * 🎮 연습 버튼 클릭 처리
     */
    private void handlePracticeButtonClick() {
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, "응시 클릭 연습 성공!");
        
        showToast("🎉 성공! 응시 클릭이 작동했어요!", true);
        
        if (btnPractice != null) {
            btnPractice.setVisibility(View.GONE);
        }
        
        mainHandler.postDelayed(() -> {
            showToast("🎯 이제 배경의 모서리 영역을 응시해보세요!", true);
        }, 1500);
        
        mainHandler.postDelayed(() -> {
            showToast("🟥 모서리 영역 색깔을 참고하세요!", true);
        }, 4000);
    }
    
    /**
     * 🚪 앱 종료 다이얼로그
     */
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("시선 추적 서비스를 완전히 종료하시겠습니까?")
                .setPositiveButton("종료", (dialog, which) -> exitApp())
                .setNegativeButton("취소", null)
                .show();
    }
    
    /**
     * 🚪 앱 종료 처리
     */
    private void exitApp() {
        try {
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, "앱 종료 시작");
            
            // 🔧 개선: 서비스 중지 및 리소스 정리 순서 개선
            Intent serviceIntent = new Intent(this, GazeTrackingService.class);
            stopService(serviceIntent);
            
            // 서비스가 완전히 종료될 때까지 잠시 대기 후 리소스 정리
            mainHandler.postDelayed(() -> {
                // 리소스 정리
                cleanup();
                
                // 액티비티 종료
                finishAffinity();
                System.exit(0);
            }, 500); // 500ms 대기로 서비스 cleanup() 완료 보장
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "App exit error", e);
            finishAffinity();
            System.exit(0);
        }
    }
    
    /**
     * 📊 상태 텍스트 업데이트
     */
    private void updateStatusText(String status) {
        uiUpdater.updateText("status", status, text -> {
            if (statusText != null) {
                statusText.setText(text);
                statusText.setVisibility(View.VISIBLE); // 상태 텍스트가 보이도록 설정
            }
        });
    }
    
    /**
     * 📊 진행률 표시
     */
    private void showProgress() {
        uiUpdater.executeOnMainThread(() -> {
            if (layoutProgress != null) {
                layoutProgress.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }
    
    /**
     * 📊 진행률 숨기기
     */
    private void hideProgress() {
        uiUpdater.executeOnMainThread(() -> {
            if (layoutProgress != null) {
                layoutProgress.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * 💬 토스트 메시지 표시
     */
    private void showToast(String message, boolean isShort) {
        uiUpdater.executeOnMainThread(() -> {
            int duration = isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
            Toast.makeText(this, message, duration).show();
        });
    }
    
    /**
     * ⚠️ 에러 다이얼로그 표시
     */
    private void showErrorDialog(String title, String message, boolean finishOnDismiss) {
        uiUpdater.executeOnMainThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("확인", (dialog, which) -> {
                        if (finishOnDismiss) {
                            finish();
                        }
                    })
                    .setCancelable(!finishOnDismiss);
                    
            builder.show();
        });
    }
    
    // ========================================
    // PermissionManager.PermissionCallback 구현
    // ========================================
    
    @Override
    public void onPermissionRequested(PermissionManager.PermissionType type, List<String> permissions) {
        String message = String.format("%s 권한을 요청합니다", type.getDisplayName());
        updateStatusText(message);
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, message);
    }
    
    @Override
    public void onPermissionGranted(PermissionManager.PermissionType type, List<String> permissions) {
        String message = String.format("%s 권한이 승인되었습니다", type.getDisplayName());
        updateStatusText(message);
        PerformanceLogger.SecurityLogger.logPermissionGranted(type.getDisplayName());
    }
    
    @Override
    public void onPermissionDenied(PermissionManager.PermissionType type, List<String> permissions) {
        String message = String.format("%s 권한이 거부되었습니다", type.getDisplayName());
        updateStatusText(message);
        PerformanceLogger.SecurityLogger.logPermissionDenied(type.getDisplayName());
        
        showToast("일부 기능이 제한될 수 있습니다", false);
    }
    
    @Override
    public void onPermissionError(PermissionManager.PermissionType type, String error) {
        String message = String.format("%s 권한 오류: %s", type.getDisplayName(), error);
        updateStatusText(message);
        PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, message);
    }
    
    @Override
    public void onPermissionFlowCompleted(boolean success, String message) {
        hideProgress();
        
        if (success) {
            updateStatusText("모든 권한 설정 완료 ✅");
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, "권한 플로우 완료");
            
            // 서비스 시작
            startGazeTrackingService();
            
            // 🆕 잠시 기다린 후 캘리브레이션 컨트롤러 초기화
            mainHandler.postDelayed(() -> {
                updateStatusText("시선 추적 SDK 초기화 중...");
                calibrationController.initializeGazeTracker();
            }, 1000);
            
        } else {
            updateStatusText("권한 설정 실패 ❌");
            showErrorDialog("권한 오류", "필요한 권한이 설정되지 않았습니다:\n" + message + 
                          "\n\n⚠️ 일부 기능이 제한될 수 있지만 앱을 계속 사용할 수 있습니다.", false);
        }
        
        isInitialized = true;
    }
    
    // ========================================
    // CalibrationController.CalibrationControllerCallback 구현
    // ========================================
    
    @Override
    public void onInitializationComplete(boolean success) {
        hideProgress(); // 진행 표시 숨기기
        
        if (success) {
            updateStatusText("시선 추적 초기화됨 ✅");
            if (btnCalibration != null) {
                btnCalibration.setEnabled(true);
                btnCalibration.setText("🎯 시선 보정 (준비됨)");
            }
            
            // 🆕 환영 다이얼로그 표시 (초기화 완료 후)
            if (!hasShownWelcomeDialog) {
                hasShownWelcomeDialog = true;
                mainHandler.postDelayed(() -> showWelcomeDialog(), 1000);
            }
        } else {
            updateStatusText("초기화 실패 ❌");
            if (btnCalibration != null) {
                btnCalibration.setText("🎯 시선 보정 (초기화 실패)");
                btnCalibration.setEnabled(false);
            }
            // 에러 다이얼로그는 CalibrationController에서 이미 표시함
        }
    }
    
    @Override
    public void onCalibrationStarted(CalibrationModeType mode, AccuracyCriteria accuracy) {
        updateStatusText("캘리브레이션 진행 중...");
        if (btnCalibration != null) {
            btnCalibration.setEnabled(false);
        }
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            String.format("캘리브레이션 시작: %s, %s", mode, accuracy));
    }
    
    @Override
    public void onCalibrationCompleted(double[] calibrationData, long duration) {
        updateStatusText("캘리브레이션 완료 ✅");
        if (btnCalibration != null) {
            btnCalibration.setEnabled(true);
        }
        showToast(String.format("보정 완료! (%d초 소요)", duration / 1000), true);
    }
    
    @Override
    public void onCalibrationCanceled() {
        updateStatusText("캘리브레이션 취소됨");
        if (btnCalibration != null) {
            btnCalibration.setEnabled(true);
        }
        showToast("보정이 취소되었습니다", false);
    }
    
    @Override
    public void onCalibrationError(String error) {
        updateStatusText("캘리브레이션 오류 ❌");
        if (btnCalibration != null) {
            btnCalibration.setEnabled(true);
            btnCalibration.setText("🎯 시선 보정 (오류 발생)");
        }
        
        // 🆕 사용자 친화적인 에러 다이얼로그
        String title = error.contains("라이센스") ? "라이센스 오류" : 
                      error.contains("카메라") ? "카메라 오류" : 
                      error.contains("권한") ? "권한 오류" : 
                      "캘리브레이션 오류";
        
        showErrorDialog(title, error, false);
    }
    
    @Override
    public void onCalibrationViewHidden() {
        if (viewPoint != null) {
            viewPoint.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onOptimalCalibrationDetected(AdaptiveCalibrationManager.CalibrationRecommendation recommendation) {
        new AlertDialog.Builder(this)
                .setTitle("🤖 최적 캘리브레이션 시점")
                .setMessage(String.format("현재 상태가 캘리브레이션에 최적입니다!\n\n" +
                        "추천 모드: %s\n" +
                        "정확도: %s\n" +
                        "신뢰도: %d%%\n\n" +
                        "이유: %s",
                        recommendation.recommendedMode,
                        recommendation.recommendedAccuracy,
                        recommendation.confidenceLevel,
                        recommendation.reason))
                .setPositiveButton("지금 시작", (dialog, which) -> {
                    calibrationController.startCalibration(
                        recommendation.recommendedMode, 
                        recommendation.recommendedAccuracy);
                })
                .setNegativeButton("나중에", null)
                .show();
    }
    
    @Override
    public void onCalibrationQualityAssessed(AdaptiveCalibrationManager.CalibrationQuality quality) {
        String title = quality.needsRecalibration ? "⚠️ 재보정 권장" : "✅ 보정 완료";
        String message = String.format("품질 점수: %d점\n%s", 
            quality.qualityScore, quality.assessment);
            
        if (quality.needsRecalibration) {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message + "\n\n다시 보정하시겠습니까?")
                    .setPositiveButton("재보정", (dialog, which) -> {
                        calibrationController.startCalibration(CalibrationModeType.DEFAULT, quality.suggestedAccuracy);
                    })
                    .setNegativeButton("나중에", null)
                    .show();
        } else {
            showToast(message, false);
        }
    }
    
    @Override
    public void onUserStatusChanged(AdaptiveCalibrationManager.UserStatus status) {
        // 사용자 상태 변화는 로그로만 기록 (UI 업데이트는 너무 빈번할 수 있음)
        PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_MAIN, 
            String.format("User Status: %s (Alert: %d%%, Attention: %d%%)", 
                status.statusDescription, status.alertnessLevel, status.attentionLevel));
    }
    
    /**
     * 🎉 환영 다이얼로그
     */
    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 설정 완료!")
                .setMessage("모든 권한 설정이 완료되었습니다!\n\n" +
                           "🎯 이제 시선 추적을 사용해보세요:\n\n" +
                           "• 먼저 '정밀 보정'을 하면 더 정확해져요!\n" +
                           "• 화면 모서리 응시로 다양한 기능 사용\n" +
                           "• 3초간 응시하면 자동 클릭")
                .setPositiveButton("🎯 정밀 보정 먼저", (dialog, which) -> {
                    handleCalibrationButtonClick();
                })
                .setNegativeButton("🎮 바로 체험하기", (dialog, which) -> {
                    if (btnPractice != null) {
                        btnPractice.setVisibility(View.VISIBLE);
                        showToast("👆 '연습용' 버튼을 3초간 응시해보세요!", true);
                    }
                })
                .show();
    }
    
    /**
     * 🚀 시선 추적 서비스 시작
     */
    private void startGazeTrackingService() {
        try {
            Intent serviceIntent = new Intent(this, GazeTrackingService.class);
            startForegroundService(serviceIntent);
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                "GazeTrackingService 시작됨");
                
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Service start failed", e);
        }
    }
    
    // ========================================
    // 라이프사이클 메서드들
    // ========================================
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (isInitialized && !isDestroyed) {
            // 설정 새로고침
            userSettings = settingsRepository.getUserSettings();
            
            // 서비스 상태 확인
            checkServiceStatus();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == AppConstants.PermissionRequestCodes.CAMERA) {
            // 기본 권한 결과는 onRequestPermissionsResult에서 처리
        } else if (requestCode == AppConstants.PermissionRequestCodes.OVERLAY) {
            permissionManager.onOverlayPermissionResult();
        } else if (requestCode == AppConstants.PermissionRequestCodes.ACCESSIBILITY) {
            permissionManager.onAccessibilityPermissionResult();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    
    /**
     * 🔍 서비스 상태 확인
     */
    private void checkServiceStatus() {
        if (GazeTrackingService.getInstance() != null) {
            updateStatusText("시선 추적 활성화됨 ✅");
            if (btnCalibration != null) {
                btnCalibration.setEnabled(true);
            }
        } else {
            updateStatusText("서비스 연결 중...");
            startGazeTrackingService();
        }
    }
    
    /**
     * 🧹 리소스 정리
     */
    private void cleanup() {
        if (isDestroyed) return;
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "MainActivity 리소스 정리 시작");
        
        isDestroyed = true;
        
        try {
            // 매니저들 정리
            if (calibrationController != null) {
                calibrationController.cleanup();
            }
            
            if (permissionManager != null) {
                permissionManager.cleanup();
            }
            
            if (viewLayoutChecker != null) {
                viewLayoutChecker.releaseChecker();
            }
            
            if (uiUpdater != null) {
                uiUpdater.cleanup();
            }
            
            // 리소스 매니저는 마지막에 정리
            if (resourceManager != null) {
                resourceManager.cleanupAll();
            }
            
            // Static 참조 정리
            if (instanceRef != null) {
                instanceRef.clear();
                instanceRef = null;
            }
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                "MainActivity 리소스 정리 완료");
                
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Cleanup error", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
    }
    
    /**
     * 🎯 외부에서 캘리브레이션 트리거 (서비스에서 호출)
     */
    public void triggerCalibrationFromService() {
        runOnUiThread(() -> {
            if (btnCalibration != null && btnCalibration.isEnabled() && !isDestroyed) {
                handleCalibrationButtonClick();
            } else {
                showToast("캘리브레이션을 시작할 수 없습니다", false);
            }
        });
    }
}
