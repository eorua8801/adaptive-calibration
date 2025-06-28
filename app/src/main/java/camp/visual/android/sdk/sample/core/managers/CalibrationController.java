package camp.visual.android.sdk.sample.core.managers;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import camp.visual.android.sdk.sample.core.constants.AppConstants;
import camp.visual.android.sdk.sample.core.security.SecurityManager;
import camp.visual.android.sdk.sample.core.utils.PerformanceLogger;
import camp.visual.android.sdk.sample.core.utils.ResourceManager;
import camp.visual.android.sdk.sample.domain.calibration.AdaptiveCalibrationManager;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.service.tracking.GazeTrackingService;
import camp.visual.android.sdk.sample.ui.views.CalibrationViewer;
import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.constant.AccuracyCriteria;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.constant.InitializationErrorType;

import java.lang.ref.WeakReference;

/**
 * 🎯 캘리브레이션 컨트롤러
 * - MainActivity에서 캘리브레이션 관련 로직 분리
 * - 적응형 캘리브레이션 관리
 * - 메모리 효율적인 리소스 관리
 * - 보안 강화 및 에러 처리
 */
public class CalibrationController {
    
    private final WeakReference<Activity> activityRef;
    private final ResourceManager resourceManager;
    private final AdaptiveCalibrationManager adaptiveManager;
    
    private GazeTracker gazeTracker;
    private CalibrationViewer calibrationViewer;
    private CalibrationCallback calibrationCallback;
    private CalibrationControllerCallback controllerCallback;
    
    private Handler mainHandler;
    private boolean isCalibrating = false;
    private boolean skipProgress = false;
    private boolean isFirstPoint = false;
    private UserSettings userSettings;
    
    // 캘리브레이션 통계
    private long calibrationStartTime = 0;
    private int currentPointIndex = 0;
    private CalibrationModeType currentMode = CalibrationModeType.DEFAULT;
    private AccuracyCriteria currentAccuracy = AccuracyCriteria.DEFAULT;
    
    public CalibrationController(Activity activity, ResourceManager resourceManager, UserSettings userSettings) {
        this.activityRef = new WeakReference<>(activity);
        this.resourceManager = resourceManager;
        this.userSettings = userSettings;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.adaptiveManager = new AdaptiveCalibrationManager();
        
        resourceManager.registerHandler(mainHandler);
        initializeAdaptiveManager();
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "CalibrationController 초기화 완료");
    }
    
    /**
     * 🤖 적응형 캘리브레이션 매니저 초기화
     */
    private void initializeAdaptiveManager() {
        adaptiveManager.setCallback(new AdaptiveCalibrationManager.AdaptiveCalibrationCallback() {
            @Override
            public void onOptimalCalibrationTimeDetected(AdaptiveCalibrationManager.CalibrationRecommendation recommendation) {
                handleOptimalCalibrationDetected(recommendation);
            }
            
            @Override
            public void onCalibrationQualityAssessment(AdaptiveCalibrationManager.CalibrationQuality quality) {
                handleCalibrationQualityAssessment(quality);
            }
            
            @Override
            public void onUserStatusChanged(AdaptiveCalibrationManager.UserStatus status) {
                if (controllerCallback != null) {
                    controllerCallback.onUserStatusChanged(status);
                }
            }
        });
    }
    
    /**
     * 🎯 캘리브레이션 뷰어 설정
     */
    public void setCalibrationViewer(CalibrationViewer calibrationViewer) {
        this.calibrationViewer = calibrationViewer;
        if (calibrationViewer != null) {
            resourceManager.registerOverlayView(calibrationViewer, null);
        }
    }
    
    /**
     * 🎯 콜백 설정
     */
    public void setCallback(CalibrationControllerCallback callback) {
        this.controllerCallback = callback;
    }
    
    /**
     * 🚀 GazeTracker 초기화
     */
    public void initializeGazeTracker() {
        Activity activity = activityRef.get();
        if (activity == null) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, "Activity is null");
            if (controllerCallback != null) {
                controllerCallback.onCalibrationError("액티비티 참조 오류");
            }
            return;
        }
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "GazeTracker 초기화 시작...");
        
        // 🔥 1. 보안 검사
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "1/4: 보안 검사 실행 중...");
        if (!SecurityManager.RuntimeSecurityCheck.performSecurityCheck()) {
            PerformanceLogger.SecurityLogger.logSecurityViolation("Security check failed");
            String errorMsg = "보안 검사에 실패했습니다.\n\n액세스 권한이 바르게 설정되었는지 확인해주세요.";
            if (controllerCallback != null) {
                controllerCallback.onCalibrationError(errorMsg);
            }
            return;
        }
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "1/4: 보안 검사 완료 ✅");
        
        // 🔥 2. 라이센스 키 확인
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "2/4: 라이센스 키 확인 중...");
        String license = SecurityManager.getSecureLicense();
        if (license == null || license.trim().isEmpty()) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, "License key is invalid: " + license);
            String errorMsg = "라이센스 키 오류가 발생했습니다.\n\n이는 EyeDID SDK 라이센스 문제일 수 있습니다.\n개발자에게 문의하세요.";
            if (controllerCallback != null) {
                controllerCallback.onCalibrationError(errorMsg);
            }
            return;
        }
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "2/4: 라이센스 키 확인 완료 ✅ (길이: " + license.length() + ")");
        
        // 🔥 3. EyeDID SDK 초기화 시작
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "3/4: EyeDID SDK 초기화 시작...");
        
        try {
            // 🔥 EyeDID SDK 표준 초기화 방식 (UserStatusInfo 포함)
            GazeTracker.initGazeTracker(activity, license, initializationCallback);
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "3/4: SDK 초기화 요청 전송 완료 ✅");
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, "Failed to call GazeTracker.initGazeTracker", e);
            String errorMsg = "EyeDID SDK 초기화 호출에 실패했습니다.\n\n" +
                             "다음을 확인해주세요:\n" +
                             "• 전면 카메라가 사용 가능한지\n" +
                             "• 다른 앱에서 카메라를 사용 중이지 않은지\n" +
                             "• 충분한 조명이 있는지\n\n" +
                             "오류: " + e.getMessage();
            if (controllerCallback != null) {
                controllerCallback.onCalibrationError(errorMsg);
            }
        }
    }
    
    private final InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            Activity activity = activityRef.get();
            if (activity == null) {
                PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, "Activity is null in initialization callback");
                return;
            }
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "4/4: EyeDID SDK 초기화 콜백 수신...");
            
            if (gazeTracker == null) {
                // 🔥 상세한 에러 메시지 생성
                String errorDetail = "Unknown";
                final String userMessage; // 🔧 final 로 선언
                
                if (error != null) {
                    errorDetail = error.name();
                    
                    // 🔥 enum switch 대신 if-else 체인 사용 (컴파일 에러 방지)
                    String errorName = error.name();
                    if (errorName.contains("CAMERA_PERMISSION") || errorName.contains("PERMISSION")) {
                        userMessage = "시선 추적 시스템 초기화에 실패했습니다.\n\n• 카메라 권한이 거부되었습니다.\n앱 설정에서 카메라 권한을 허용해주세요.";
                    } else if (errorName.contains("CAMERA_ALREADY_IN_USE") || errorName.contains("ALREADY_IN_USE")) {
                        userMessage = "시선 추적 시스템 초기화에 실패했습니다.\n\n• 다른 앱에서 카메라를 사용 중입니다.\n다른 카메라 앱을 종료하고 다시 시도해주세요.";
                    } else if (errorName.contains("CAMERA_INTERRUPTED") || errorName.contains("INTERRUPTED")) {
                        userMessage = "시선 추적 시스템 초기화에 실패했습니다.\n\n• 카메라 연결이 중단되었습니다.\n전면 카메라가 가려지지 않았는지 확인해주세요.";
                    } else if (errorName.contains("INVALID_LICENSE_KEY") || errorName.contains("LICENSE")) {
                        userMessage = "시선 추적 시스템 초기화에 실패했습니다.\n\n• 라이센스 키가 유효하지 않습니다.\n개발자에게 문의하세요.";
                    } else if (errorName.contains("NOT_SUPPORTED_DEVICE") || errorName.contains("NOT_SUPPORTED")) {
                        userMessage = "시선 추적 시스템 초기화에 실패했습니다.\n\n• 지원되지 않는 디바이스입니다.\nAndroid 6.0 이상과 전면 카메라가 필요합니다.";
                    } else if (errorName.contains("INIT_ALREADY_IN_PROGRESS") || errorName.contains("ALREADY_IN_PROGRESS")) {
                        userMessage = "시선 추적 시스템 초기화에 실패했습니다.\n\n• 이미 초기화가 진행 중입니다.\n잠시 기다려주세요.";
                    } else {
                        userMessage = "시선 추적 시스템 초기화에 실패했습니다.\n\n다음을 확인해주세요:\n" +
                                     "• 전면 카메라가 가려지지 않았는지\n" +
                                     "• 충분한 조명이 있는지\n" +
                                     "• 다른 카메라 앱이 실행 중이지 않은지\n" +
                                     "• 네트워크 연결이 안정적인지\n\n" +
                                     "에러 코드: " + errorName;
                    }
                } else {
                    userMessage = "시선 추적 시스템 초기화에 실패했습니다.";
                }
                
                String logMsg = "GazeTracker 초기화 실패: " + errorDetail;
                PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, logMsg);
                
                activity.runOnUiThread(() -> {
                    PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, "4/4: SDK 초기화 실패 ❌");
                    if (controllerCallback != null) {
                        controllerCallback.onCalibrationError(userMessage);
                    }
                });
                return;
            }
            
            // 🔥 초기화 성공
            CalibrationController.this.gazeTracker = gazeTracker;
            setupCalibrationCallback();
            
            activity.runOnUiThread(() -> {
                PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
                    "4/4: GazeTracker 초기화 성공 ✅ (UserStatus 활성화)");
                PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
                    "🎉 모든 초기화 단계 완료! 이제 캘리브레이션을 시작할 수 있습니다.");
                
                if (controllerCallback != null) {
                    controllerCallback.onInitializationComplete(true);
                }
            });
        }
    };
    
    /**
     * 🎯 캘리브레이션 콜백 설정
     */
    private void setupCalibrationCallback() {
        calibrationCallback = new CalibrationCallback() {
            @Override
            public void onCalibrationProgress(float progress) {
                if (!skipProgress && calibrationViewer != null) {
                    mainHandler.post(() -> {
                        calibrationViewer.setPointAnimationPower(progress);
                        
                        // 적응형 매니저에 진행률 알림
                        PerformanceLogger.GazeLogger.logCalibrationProgress(progress, currentPointIndex);
                    });
                }
            }
            
            @Override
            public void onCalibrationNextPoint(float x, float y) {
                currentPointIndex++;
                mainHandler.post(() -> {
                    showCalibrationPointView(x, y);
                });
            }
            
            @Override
            public void onCalibrationFinished(double[] calibrationData) {
                long duration = System.currentTimeMillis() - calibrationStartTime;
                PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
                    String.format("캘리브레이션 완료: %d포인트, %dms 소요", currentPointIndex, duration));
                
                hideCalibrationView();
                isCalibrating = false;
                
                mainHandler.post(() -> {
                    if (controllerCallback != null) {
                        controllerCallback.onCalibrationCompleted(calibrationData, duration);
                    }
                    
                    // 🆕 캘리브레이션 품질 평가 (적응형 매니저에서)
                    // 실제 구현에서는 calibration 전후의 UserStatusInfo를 비교해야 함
                    evaluateCalibrationQuality(true);
                });
            }
            
            @Override
            public void onCalibrationCanceled(double[] calibrationData) {
                PerformanceLogger.logWarning(AppConstants.Logging.TAG_CALIBRATION, "캘리브레이션 취소됨");
                
                hideCalibrationView();
                isCalibrating = false;
                
                mainHandler.post(() -> {
                    if (controllerCallback != null) {
                        controllerCallback.onCalibrationCanceled();
                    }
                    
                    evaluateCalibrationQuality(false);
                });
            }
        };
        
        if (gazeTracker != null) {
            gazeTracker.setCalibrationCallback(calibrationCallback);
        }
    }
    
    /**
     * 🎯 캘리브레이션 시작 (기본 모드)
     */
    public boolean startCalibration() {
        return startCalibration(CalibrationModeType.FIVE_POINT, AccuracyCriteria.HIGH);
    }
    
    /**
     * 🎯 캘리브레이션 시작 (사용자 지정 모드)
     */
    public boolean startCalibration(CalibrationModeType mode, AccuracyCriteria accuracy) {
        if (gazeTracker == null) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, "GazeTracker is null");
            return false;
        }
        
        if (isCalibrating) {
            PerformanceLogger.logWarning(AppConstants.Logging.TAG_CALIBRATION, "이미 캘리브레이션 진행 중");
            return false;
        }
        
        // 서비스 우선 확인
        if (isServiceRunning()) {
            return startCalibrationViaService();
        }
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            String.format("캘리브레이션 시작: Mode=%s, Accuracy=%s", mode, accuracy));
        
        isCalibrating = true;
        isFirstPoint = true;
        currentPointIndex = 0;
        currentMode = mode;
        currentAccuracy = accuracy;
        calibrationStartTime = System.currentTimeMillis();
        
        if (calibrationViewer != null) {
            calibrationViewer.setVisibility(View.VISIBLE);
            calibrationViewer.setPointPosition(-9999, -9999);
            calibrationViewer.setEnableText(true);
        }
        
        boolean success = false;
        try {
            success = gazeTracker.startCalibration(mode, accuracy);
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, 
                "GazeTracker.startCalibration() exception", e);
            if (controllerCallback != null) {
                controllerCallback.onCalibrationError("캘리브레이션 시작 중 오류가 발생했습니다: " + e.getMessage());
            }
            resetCalibrationState();
            return false;
        }
        
        if (!success) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, "캘리브레이션 시작 실패 - GazeTracker에서 false 반환");
            if (controllerCallback != null) {
                controllerCallback.onCalibrationError("캘리브레이션을 시작할 수 없습니다.\n\nGazeTracker가 올바르게 초기화되었는지 확인해주세요.");
            }
            resetCalibrationState();
            return false;
        }
        
        if (controllerCallback != null) {
            controllerCallback.onCalibrationStarted(mode, accuracy);
        }
        
        return true;
    }
    
    /**
     * 🎯 서비스를 통한 캘리브레이션 시작
     */
    private boolean startCalibrationViaService() {
        try {
            GazeTrackingService service = GazeTrackingService.getInstance();
            if (service != null) {
                service.triggerCalibration();
                PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
                    "서비스를 통한 캘리브레이션 시작");
                return true;
            }
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, 
                "서비스 캘리브레이션 실패", e);
        }
        return false;
    }
    
    /**
     * 🎯 적응형 캘리브레이션 시작
     */
    public boolean startAdaptiveCalibration() {
        // 현재 사용자 상태에 따른 최적 모드 결정
        if (adaptiveManager.getLastUserStatusInfo() != null) {
            UserSettings.Builder recommendedSettings = adaptiveManager.getRecommendedSettings(
                adaptiveManager.getLastUserStatusInfo());
            
            CalibrationModeType recommendedMode = determineCalibrationMode(recommendedSettings);
            AccuracyCriteria recommendedAccuracy = determineAccuracyCriteria(recommendedSettings);
            
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
                "적응형 캘리브레이션 - 추천 모드: " + recommendedMode + ", 정확도: " + recommendedAccuracy);
            
            return startCalibration(recommendedMode, recommendedAccuracy);
        } else {
            PerformanceLogger.logWarning(AppConstants.Logging.TAG_CALIBRATION, 
                "사용자 상태 정보 없음, 기본 캘리브레이션 실행");
            return startCalibration();
        }
    }
    
    /**
     * 🛑 캘리브레이션 중지
     */
    public void stopCalibration() {
        if (!isCalibrating) return;
        
        PerformanceLogger.logWarning(AppConstants.Logging.TAG_CALIBRATION, "캘리브레이션 중지 요청");
        
        if (gazeTracker != null) {
            gazeTracker.stopCalibration();
        }
        
        resetCalibrationState();
    }
    
    /**
     * 🎯 캘리브레이션 포인트 표시
     */
    private void showCalibrationPointView(float x, float y) {
        if (calibrationViewer == null) return;
        
        PerformanceLogger.GazeLogger.logCalibrationProgress(0, currentPointIndex);
        PerformanceLogger.logIfNeeded(AppConstants.Logging.TAG_CALIBRATION, 
            String.format("캘리브레이션 포인트 %d: %s", currentPointIndex, 
                SecurityManager.sanitizeCoordinates(x, y)));
        
        skipProgress = true;
        calibrationViewer.setPointAnimationPower(0);
        calibrationViewer.setEnableText(false);
        calibrationViewer.nextPointColor();
        calibrationViewer.setPointPosition(x, y);
        
        long delay = isFirstPoint ? 2500 : 1200;
        
        mainHandler.postDelayed(() -> {
            if (gazeTracker != null && isCalibrating) {
                gazeTracker.startCollectSamples();
                skipProgress = false;
            }
        }, delay);
        
        isFirstPoint = false;
    }
    
    /**
     * 🎯 캘리브레이션 뷰 숨기기
     */
    private void hideCalibrationView() {
        mainHandler.post(() -> {
            if (calibrationViewer != null) {
                calibrationViewer.setVisibility(View.INVISIBLE);
            }
            
            if (controllerCallback != null) {
                controllerCallback.onCalibrationViewHidden();
            }
        });
    }
    
    /**
     * 🔄 캘리브레이션 상태 리셋
     */
    private void resetCalibrationState() {
        isCalibrating = false;
        isFirstPoint = false;
        currentPointIndex = 0;
        skipProgress = false;
        hideCalibrationView();
    }
    
    /**
     * 🤖 최적 캘리브레이션 시점 감지 처리
     */
    private void handleOptimalCalibrationDetected(AdaptiveCalibrationManager.CalibrationRecommendation recommendation) {
        Activity activity = activityRef.get();
        if (activity == null) return;
        
        activity.runOnUiThread(() -> {
            if (controllerCallback != null) {
                controllerCallback.onOptimalCalibrationDetected(recommendation);
            }
        });
    }
    
    /**
     * 📊 캘리브레이션 품질 평가 처리
     */
    private void handleCalibrationQualityAssessment(AdaptiveCalibrationManager.CalibrationQuality quality) {
        Activity activity = activityRef.get();
        if (activity == null) return;
        
        activity.runOnUiThread(() -> {
            if (controllerCallback != null) {
                controllerCallback.onCalibrationQualityAssessed(quality);
            }
            
            // 품질이 낮으면 사용자에게 알림
            if (quality.needsRecalibration) {
                showQualityWarning(quality);
            }
        });
    }
    
    /**
     * ⚠️ 품질 경고 표시
     */
    private void showQualityWarning(AdaptiveCalibrationManager.CalibrationQuality quality) {
        Activity activity = activityRef.get();
        if (activity == null) return;
        
        String message = String.format("캘리브레이션 품질: %d점\n%s", 
            quality.qualityScore, quality.assessment);
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 📊 캘리브레이션 품질 평가 수행
     */
    private void evaluateCalibrationQuality(boolean success) {
        // 실제 구현에서는 캘리브레이션 전후의 UserStatusInfo를 비교해야 함
        // 현재는 기본적인 평가만 수행
        if (adaptiveManager.getLastUserStatusInfo() != null) {
            AdaptiveCalibrationManager.CalibrationQuality quality = 
                adaptiveManager.evaluateCalibrationQuality(
                    adaptiveManager.getLastUserStatusInfo(), // pre
                    adaptiveManager.getLastUserStatusInfo(), // post (실제로는 다른 값이어야 함)
                    success
                );
            
            handleCalibrationQualityAssessment(quality);
        }
    }
    
    /**
     * 🔍 서비스 실행 상태 확인
     */
    private boolean isServiceRunning() {
        return GazeTrackingService.getInstance() != null;
    }
    
    /**
     * 🎯 권장 설정에서 캘리브레이션 모드 결정
     */
    private CalibrationModeType determineCalibrationMode(UserSettings.Builder settingsBuilder) {
        // 실제 구현에서는 settingsBuilder에서 정보를 추출해야 함
        // 현재는 기본값 반환
        return CalibrationModeType.DEFAULT;
    }
    
    /**
     * 🎯 권장 설정에서 정확도 기준 결정
     */
    private AccuracyCriteria determineAccuracyCriteria(UserSettings.Builder settingsBuilder) {
        // 실제 구현에서는 settingsBuilder에서 정보를 추출해야 함
        // 현재는 기본값 반환
        return AccuracyCriteria.DEFAULT;
    }
    
    // Getter 메서드들
    public boolean isCalibrating() { return isCalibrating; }
    public CalibrationModeType getCurrentMode() { return currentMode; }
    public AccuracyCriteria getCurrentAccuracy() { return currentAccuracy; }
    public int getCurrentPointIndex() { return currentPointIndex; }
    public long getCalibrationDuration() { 
        return isCalibrating ? System.currentTimeMillis() - calibrationStartTime : 0; 
    }
    public AdaptiveCalibrationManager getAdaptiveManager() { return adaptiveManager; }
    public GazeTracker getGazeTracker() { return gazeTracker; }
    
    /**
     * 🧹 리소스 정리
     */
    public void cleanup() {
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "CalibrationController 정리 시작");
        
        // 진행 중인 캘리브레이션 중지
        if (isCalibrating) {
            stopCalibration();
        }
        
        // GazeTracker 정리
        if (gazeTracker != null) {
            try {
                gazeTracker.removeCalibrationCallback();
                // 필요시 GazeTracker.releaseGazeTracker(gazeTracker) 호출
            } catch (Exception e) {
                PerformanceLogger.logError(AppConstants.Logging.TAG_CALIBRATION, 
                    "GazeTracker cleanup error", e);
            }
            gazeTracker = null;
        }
        
        // 적응형 매니저 정리
        if (adaptiveManager != null) {
            adaptiveManager.cleanup();
        }
        
        // 핸들러 정리
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        // 참조 정리
        controllerCallback = null;
        calibrationCallback = null;
        calibrationViewer = null;
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_CALIBRATION, 
            "CalibrationController 정리 완료");
    }
    
    // 콜백 인터페이스
    public interface CalibrationControllerCallback {
        void onInitializationComplete(boolean success);
        void onCalibrationStarted(CalibrationModeType mode, AccuracyCriteria accuracy);
        void onCalibrationCompleted(double[] calibrationData, long duration);
        void onCalibrationCanceled();
        void onCalibrationError(String error);
        void onCalibrationViewHidden();
        void onOptimalCalibrationDetected(AdaptiveCalibrationManager.CalibrationRecommendation recommendation);
        void onCalibrationQualityAssessed(AdaptiveCalibrationManager.CalibrationQuality quality);
        void onUserStatusChanged(AdaptiveCalibrationManager.UserStatus status);
    }
}
