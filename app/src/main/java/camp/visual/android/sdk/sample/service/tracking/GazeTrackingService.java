package camp.visual.android.sdk.sample.service.tracking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import camp.visual.android.sdk.sample.R;
import camp.visual.android.sdk.sample.data.repository.EyeTrackingRepository;
import camp.visual.android.sdk.sample.data.repository.EyedidTrackingRepository;
import camp.visual.android.sdk.sample.data.settings.SettingsRepository;
import camp.visual.android.sdk.sample.data.settings.SharedPrefsSettingsRepository;
import camp.visual.android.sdk.sample.domain.filter.EnhancedOneEuroFilterManager;
import camp.visual.android.sdk.sample.domain.interaction.ClickDetector;
import camp.visual.android.sdk.sample.domain.interaction.EdgeScrollDetector;
// SwipeDetector 제거 - EdgeScrollDetector가 스와이프 기능도 포함
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.domain.performance.PerformanceMonitor;
import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;
import camp.visual.android.sdk.sample.ui.main.MainActivity;
import camp.visual.android.sdk.sample.ui.views.CalibrationViewer;
import camp.visual.android.sdk.sample.ui.views.OverlayCursorView;
import camp.visual.android.sdk.sample.ui.views.overlay.EdgeMenuManager;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;

import java.lang.ref.WeakReference;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.metrics.BlinkInfo;
import camp.visual.eyedid.gazetracker.metrics.FaceInfo;
import camp.visual.eyedid.gazetracker.metrics.GazeInfo;
import camp.visual.eyedid.gazetracker.metrics.UserStatusInfo;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

public class GazeTrackingService extends Service implements PerformanceMonitor.PerformanceCallback {

    private static final String TAG = "GazeTrackingService";
    private static final String CHANNEL_ID = "GazeTrackingServiceChannel";

    // 컴포넌트
    private EyedidTrackingRepository trackingRepository;
    private SettingsRepository settingsRepository;
    private UserSettings userSettings;
    private ClickDetector clickDetector;
    private EdgeScrollDetector edgeScrollDetector;
    // SwipeDetector 제거 - EdgeScrollDetector가 스와이프도 담당

    // 🆕 향상된 필터링 시스템
    private EnhancedOneEuroFilterManager enhancedFilterManager;

    // 🆕 성능 모니터링 시스템
    private PerformanceMonitor performanceMonitor;

    // 시스템 서비스 및 UI
    private WindowManager windowManager;
    private OverlayCursorView overlayCursorView;
    private CalibrationViewer calibrationViewer;
    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 상태 변수
    private long lastValidTimestamp = 0;
    private long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN = 1500;
    private boolean isCalibrating = false;
    private boolean skipProgress = false;

    // 🆕 성능 최적화 상태
    private boolean performanceOptimizationEnabled = true;
    private long lastPerformanceCheck = 0;
    private static final long PERFORMANCE_CHECK_INTERVAL = 10000; // 10초마다 체크

    // 🆕 엣지 메뉴 매니저
    private EdgeMenuManager edgeMenuManager;

    // 🔄 서비스 인스턴스 (WeakReference로 메모리 누수 방지)
    private static WeakReference<GazeTrackingService> instanceRef;

    @Override
    public void onCreate() {
        super.onCreate();
        instanceRef = new WeakReference<>(this);

        initRepositories();
        initDetectors();
        createNotificationChannel();
        initSystemServices();
        initViews();
        initPerformanceMonitoring();
        initEdgeMenuManager(); // 🆕 엣지 메뉴 매니저 초기화
        initGazeTracker();

        checkAccessibilityService();
    }

    private void initRepositories() {
        trackingRepository = new EyedidTrackingRepository();
        settingsRepository = new SharedPrefsSettingsRepository(this);
        userSettings = settingsRepository.getUserSettings();
    }

    private void initDetectors() {
        clickDetector = new ClickDetector(userSettings);
        edgeScrollDetector = new EdgeScrollDetector(userSettings, this);
        // SwipeDetector 제거 - EdgeScrollDetector가 모든 엣지 기능 담당

        // 🆕 향상된 OneEuroFilter 초기화
        enhancedFilterManager = new EnhancedOneEuroFilterManager(
                userSettings.getOneEuroFreq(),
                userSettings.getOneEuroMinCutoff(),
                userSettings.getOneEuroBeta(),
                userSettings.getOneEuroDCutoff()
        );

        Log.d(TAG, "향상된 OneEuroFilter 초기화 - 프리셋: " + userSettings.getOneEuroFilterPreset().getDisplayName());
        Log.d(TAG, "안경 보정 기능: " + (enhancedFilterManager.isGlassesCompensationEnabled() ? "활성화" : "비활성화"));
        Log.d(TAG, "통합 엣지 감지기(스크롤+스와이프) 초기화 완료");
    }

    // 🆕 엣지 메뉴 매니저 초기화
    private void initEdgeMenuManager() {
        edgeMenuManager = new EdgeMenuManager(this);
        Log.d(TAG, "엣지 메뉴 매니저 초기화 완료");
    }

    // 🆕 성능 모니터링 초기화
    private void initPerformanceMonitoring() {
        performanceMonitor = new PerformanceMonitor(this);
        performanceMonitor.setCallback(this);

        if (performanceOptimizationEnabled) {
            performanceMonitor.startMonitoring();
            Log.d(TAG, "성능 모니터링 시작");
        }
    }

    private void initSystemServices() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("시선 추적 실행 중")
                .setContentText("백그라운드에서 시선을 추적하고 있습니다")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);
    }

    private void initViews() {
        // 시선 커서 뷰 초기화
        overlayCursorView = new OverlayCursorView(this);

        WindowManager.LayoutParams cursorParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        cursorParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(overlayCursorView, cursorParams);

        // 캘리브레이션 뷰 초기화
        calibrationViewer = new CalibrationViewer(this);
        WindowManager.LayoutParams calibrationParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        calibrationParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(calibrationViewer, calibrationParams);
        calibrationViewer.setVisibility(View.INVISIBLE);
    }

    private void initGazeTracker() {
        trackingRepository.initialize(this, (tracker, error) -> {
            if (tracker != null) {
                trackingRepository.setTrackingCallback(trackingCallback);
                trackingRepository.setCalibrationCallback(calibrationCallback);
                trackingRepository.startTracking();
                Log.d(TAG, "GazeTracker 초기화 성공 (HIGH 정확도 모드)");

                // 자동 보정 시작
                if (userSettings.isAutoOnePointCalibrationEnabled() && !isCalibrating) {
                    startAutoCalibration();
                }
            } else {
                Log.e(TAG, "GazeTracker 초기화 실패: " + error);
                Toast.makeText(this, "시선 추적 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startAutoCalibration() {
        Log.d(TAG, "자동 보정 시작");
        Toast.makeText(this, "시선 보정 시작", Toast.LENGTH_SHORT).show();

        handler.postDelayed(() -> {
            startCalibration();
        }, 1000);
    }

    private void startCalibration() {
        if (trackingRepository == null || trackingRepository.getTracker() == null) {
            Log.e(TAG, "trackingRepository 또는 tracker가 null입니다");
            return;
        }

        if (isCalibrating) {
            Log.w(TAG, "이미 캘리브레이션 진행 중입니다");
            return;
        }

        isCalibrating = true;
        overlayCursorView.setVisibility(View.INVISIBLE);

        boolean ok = trackingRepository.getTracker().startCalibration(CalibrationModeType.DEFAULT);
        if (!ok) {
            resetCalibrationState();
            Toast.makeText(this, "보정 실패", Toast.LENGTH_SHORT).show();
        }
    }

    // 🆕 캘리브레이션 완료 후 커서 오프셋 리셋
    private void resetCursorOffsetsAfterCalibration() {
        try {
            // 현재 오프셋 값 로깅
            float currentOffsetX = userSettings.getCursorOffsetX();
            float currentOffsetY = userSettings.getCursorOffsetY();
            
            if (currentOffsetX != 0f || currentOffsetY != 0f) {
                Log.d(TAG, String.format("캘리브레이션 완료 - 기존 오프셋 리셋: X=%.1f, Y=%.1f → X=0, Y=0", 
                        currentOffsetX, currentOffsetY));
                
                // 오프셋을 0,0으로 리셋한 새 설정 생성
                UserSettings resetSettings = new UserSettings.Builder()
                        .calibrationStrategy(userSettings.getCalibrationStrategy())
                        .backgroundLearningEnabled(userSettings.isBackgroundLearningEnabled())
                        .autoOnePointCalibrationEnabled(userSettings.isAutoOnePointCalibrationEnabled())
                        .cursorOffsetX(0f)  // 🔧 X 오프셋 리셋
                        .cursorOffsetY(0f)  // 🔧 Y 오프셋 리셋
                        .oneEuroFilterPreset(userSettings.getOneEuroFilterPreset())
                        .clickTiming(userSettings.getClickTiming())
                        .performanceOptimizationEnabled(userSettings.isPerformanceOptimizationEnabled())
                        .performanceMode(userSettings.getPerformanceMode())
                        .glassesCompensationEnabled(userSettings.isGlassesCompensationEnabled())
                        .refractionCorrectionFactor(userSettings.getRefractionCorrectionFactor())
                        .dynamicFilteringEnabled(userSettings.isDynamicFilteringEnabled())
                        .targetFPS(userSettings.getTargetFPS())
                        .build();
                
                // 설정 저장
                settingsRepository.saveUserSettings(resetSettings);
                
                // 현재 userSettings 업데이트
                userSettings = resetSettings;
                
                // 감지기들도 새 설정으로 업데이트
                refreshDetectorsWithNewSettings();
                
                Log.d(TAG, "커서 오프셋 리셋 완료 - 새로운 캘리브레이션 기준으로 정확한 추적 시작");
            } else {
                Log.d(TAG, "캘리브레이션 완료 - 기존 오프셋이 이미 0이므로 리셋 불필요");
            }
        } catch (Exception e) {
            Log.e(TAG, "커서 오프셋 리셋 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    // 🆕 새 설정으로 감지기들 업데이트
    private void refreshDetectorsWithNewSettings() {
        try {
            clickDetector = new ClickDetector(userSettings);
            edgeScrollDetector = new EdgeScrollDetector(userSettings, this);
            // SwipeDetector 제거 - EdgeScrollDetector가 모든 엣지 기능 담당
            
            // 향상된 필터 매니저도 업데이트
            enhancedFilterManager = new EnhancedOneEuroFilterManager(
                    userSettings.getOneEuroFreq(),
                    userSettings.getOneEuroMinCutoff(),
                    userSettings.getOneEuroBeta(),
                    userSettings.getOneEuroDCutoff()
            );
            
            Log.d(TAG, "모든 감지기가 새 설정으로 업데이트됨");
        } catch (Exception e) {
            Log.e(TAG, "감지기 업데이트 중 오류: " + e.getMessage(), e);
        }
    }

    private void resetCalibrationState() {
        isCalibrating = false;
        calibrationViewer.setVisibility(View.INVISIBLE);
        overlayCursorView.setVisibility(View.VISIBLE);
    }

    private final TrackingCallback trackingCallback = new TrackingCallback() {
        @Override
        public void onMetrics(long timestamp, GazeInfo gazeInfo, FaceInfo faceInfo, BlinkInfo blinkInfo, UserStatusInfo userStatusInfo) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            float screenWidth = dm.widthPixels;
            float screenHeight = dm.heightPixels;

            if (gazeInfo.trackingState == TrackingState.SUCCESS) {
                // 🆕 향상된 필터링 시스템 사용
                float filteredX, filteredY;
                long filterTime = android.os.SystemClock.elapsedRealtime();

                // fixationX/Y 데이터도 함께 활용하여 필터링
                if (enhancedFilterManager.filterValues(filterTime, gazeInfo.x, gazeInfo.y,
                        gazeInfo.fixationX, gazeInfo.fixationY, gazeInfo.trackingState)) {
                    float[] filtered = enhancedFilterManager.getFilteredValues();
                    filteredX = filtered[0];
                    filteredY = filtered[1];

                    // 필터 상태 로깅 (디버깅용)
                    if (timestamp % 1000 == 0) { // 1초마다 한 번씩만
                        Log.v(TAG, "필터 상태: " + enhancedFilterManager.getCurrentFilterInfo());
                    }
                } else {
                    // 🆕 TrackingState 기반 폴백 처리
                    if (enhancedFilterManager.filterValues(filterTime, gazeInfo.x, gazeInfo.y)) {
                        float[] filtered = enhancedFilterManager.getFilteredValues();
                        filteredX = filtered[0];
                        filteredY = filtered[1];
                    } else {
                        filteredX = gazeInfo.x;
                        filteredY = gazeInfo.y;
                    }
                }

                // 오프셋 적용
                filteredX += userSettings.getCursorOffsetX();
                filteredY += userSettings.getCursorOffsetY();

                float safeX = Math.max(0, Math.min(filteredX, screenWidth - 1));
                float safeY = Math.max(0, Math.min(filteredY, screenHeight - 1));

                if (!isCalibrating) {
                    overlayCursorView.updatePosition(safeX, safeY);
                    lastValidTimestamp = System.currentTimeMillis();

                    // 🆕 메뉴가 열려있으면 메뉴 상호작용 처리
                    if (edgeMenuManager.isMenuVisible()) {
                        edgeMenuManager.updateGazePosition(safeX, safeY);
                        
                        // 메뉴가 열려있을 때는 엣지 감지로 취소 처리
                        EdgeScrollDetector.Edge edge = edgeScrollDetector.update(safeX, safeY, screenWidth, screenHeight);
                        handleMenuCancellation(edge);
                        return; // 메뉴 상호작용 중에는 다른 상호작용 비활성화
                    }

                    // 🆕 엣지 스크롤 처리 (좌우 모서리 포함)
                    EdgeScrollDetector.Edge edge = edgeScrollDetector.update(safeX, safeY, screenWidth, screenHeight);

                    if (edge == EdgeScrollDetector.Edge.TOP) {
                        overlayCursorView.setTextPosition(false);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processTopEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.SCROLL_DOWN) {
                            overlayCursorView.setCursorText("③");
                            scrollDown(userSettings.getContinuousScrollCount());
                            handler.postDelayed(() -> resetAll(), 500);
                        }
                    } else if (edge == EdgeScrollDetector.Edge.BOTTOM) {
                        overlayCursorView.setTextPosition(true);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processBottomEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.SCROLL_UP) {
                            overlayCursorView.setCursorText("③");
                            scrollUp(userSettings.getContinuousScrollCount());
                            handler.postDelayed(() -> resetAll(), 500);
                        }
                    } else if (edge == EdgeScrollDetector.Edge.LEFT_TOP) {
                        // 🆕 좌측 상단 엣지 처리 - 네비게이션 메뉴
                        overlayCursorView.setTextPosition(false);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processLeftTopEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.LEFT_TOP_ACTION) {
                            overlayCursorView.setCursorText("③");
                            Log.d(TAG, "네비게이션 메뉴 호출!");
                            edgeMenuManager.showNavigationMenu();
                            handler.postDelayed(() -> resetAll(), 500);
                        }
                    } else if (edge == EdgeScrollDetector.Edge.LEFT_BOTTOM) {
                        // 🆕 좌측 하단 엣지 처리 - 좌→우 스와이프
                        overlayCursorView.setTextPosition(false);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processLeftBottomEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.LEFT_BOTTOM_SWIPE_RIGHT) {
                            overlayCursorView.setCursorText("➡️");
                            Log.d(TAG, "좌측→우측 스와이프 완료! 앞으로가기 실행");
                            MyAccessibilityService.performSwipeAction(MyAccessibilityService.Direction.RIGHT);
                            handler.postDelayed(() -> resetAll(), 800);
                        }
                    } else if (edge == EdgeScrollDetector.Edge.RIGHT_TOP) {
                        // 🆕 우측 상단 엣지 처리 - 시스템 메뉴
                        overlayCursorView.setTextPosition(false);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processRightTopEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.RIGHT_TOP_ACTION) {
                            overlayCursorView.setCursorText("③");
                            Log.d(TAG, "시스템 메뉴 호출!");
                            edgeMenuManager.showSystemMenu();
                            handler.postDelayed(() -> resetAll(), 500);
                        }
                    } else if (edge == EdgeScrollDetector.Edge.RIGHT_BOTTOM) {
                        // 🆕 우측 하단 엣지 처리 - 우→좌 스와이프
                        overlayCursorView.setTextPosition(false);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processRightBottomEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.RIGHT_BOTTOM_SWIPE_LEFT) {
                            overlayCursorView.setCursorText("⬅️");
                            Log.d(TAG, "우측→좌측 스와이프 완료! 뒤로가기 실행");
                            MyAccessibilityService.performSwipeAction(MyAccessibilityService.Direction.LEFT);
                            handler.postDelayed(() -> resetAll(), 800);
                        }
                    } else if (!edgeScrollDetector.isActive()) {
                        // 🆕 엣지가 활성화되지 않은 경우에만 클릭 감지
                        boolean clicked = clickDetector.update(safeX, safeY);
                        overlayCursorView.setProgress(clickDetector.getProgress());
                        overlayCursorView.setCursorText("●");

                        if (clicked) {
                            performClick(safeX, safeY);
                        }
                    }
                }
            }

            // 🆕 성능 기반 FPS 조정 (주기적으로)
            checkAndAdjustPerformance();
        }

        @Override
        public void onDrop(long timestamp) {
            // 🆕 프레임 드롭 감지 시 성능 조정
            Log.w(TAG, "프레임 드롭 감지: " + timestamp);
            if (performanceOptimizationEnabled && performanceMonitor != null) {
                handler.post(() -> {
                    PerformanceMonitor.PerformanceMetrics metrics = performanceMonitor.getCurrentMetrics();
                    trackingRepository.adjustFPSBasedOnPerformance(
                            metrics.batteryLevel, metrics.cpuUsage, metrics.availableMemoryMB
                    );
                });
            }
        }
    };

    // 🆕 메뉴 취소 처리
    private void handleMenuCancellation(EdgeScrollDetector.Edge edge) {
        // 메뉴가 열린 상태에서 같은 모서리를 다시 응시하면 취소 시작
        if (edge == EdgeScrollDetector.Edge.LEFT_TOP && edgeMenuManager.isNavigationMenuActive()) {
            if (!edgeMenuManager.isCancelingActive()) {
                edgeMenuManager.startCanceling();
                overlayCursorView.setCursorText("삭제");
            } else {
                edgeMenuManager.updateCancelProgress();
            }
        } else if (edge == EdgeScrollDetector.Edge.RIGHT_TOP && edgeMenuManager.isSystemMenuActive()) {
            if (!edgeMenuManager.isCancelingActive()) {
                edgeMenuManager.startCanceling();
                overlayCursorView.setCursorText("삭제");
            } else {
                edgeMenuManager.updateCancelProgress();
            }
        } else if (edgeMenuManager.isCancelingActive()) {
            // 다른 엣지로 이동하면 취소 중단
            edgeMenuManager.cancelCanceling();
            overlayCursorView.setCursorText("●");
        }
    }

    // 🆕 성능 체크 및 조정
    private void checkAndAdjustPerformance() {
        long currentTime = System.currentTimeMillis();
        if (performanceOptimizationEnabled && performanceMonitor != null &&
                currentTime - lastPerformanceCheck > PERFORMANCE_CHECK_INTERVAL) {

            lastPerformanceCheck = currentTime;

            PerformanceMonitor.PerformanceMetrics metrics = performanceMonitor.getCurrentMetrics();
            trackingRepository.adjustFPSBasedOnPerformance(
                    metrics.batteryLevel, metrics.cpuUsage, metrics.availableMemoryMB
            );
        }
    }

    // 🆕 PerformanceMonitor.PerformanceCallback 구현
    @Override
    public void onPerformanceChanged(PerformanceMonitor.PerformanceMetrics metrics) {
        if (performanceOptimizationEnabled) {
            trackingRepository.adjustFPSBasedOnPerformance(
                    metrics.batteryLevel, metrics.cpuUsage, metrics.availableMemoryMB
            );
        }
    }

    @Override
    public void onPerformanceAlert(PerformanceMonitor.AlertType alertType, PerformanceMonitor.PerformanceMetrics metrics) {
        String alertMessage = "";
        switch (alertType) {
            case BATTERY_CRITICAL:
                alertMessage = "배터리 부족! 성능 최적화 모드 활성화";
                break;
            case CPU_CRITICAL:
                alertMessage = "CPU 과부하! FPS 자동 조정 중";
                break;
            case MEMORY_CRITICAL:
                alertMessage = "메모리 부족! 성능 조정 중";
                break;
        }

        if (!alertMessage.isEmpty()) {
            Log.w(TAG, "성능 알림: " + alertMessage + " - " + metrics.toString());
        }
    }

    private void resetAll() {
        edgeScrollDetector.resetAll();
        clickDetector.reset();
        // SwipeDetector 제거 - EdgeScrollDetector가 모든 엣지 기능 담당
        overlayCursorView.setCursorText("●");
        overlayCursorView.setTextPosition(false);
        overlayCursorView.setProgress(0f);
    }

    private void scrollUp(int count) {
        if (MyAccessibilityService.getInstance() != null) {
            Log.d(TAG, "위로 스크롤 실행 (" + count + "회)");

            if (count <= 1) {
                MyAccessibilityService.getInstance().performScroll(MyAccessibilityService.Direction.UP);
            } else {
                MyAccessibilityService.getInstance().performContinuousScroll(MyAccessibilityService.Direction.UP, count);
            }

            lastScrollTime = System.currentTimeMillis();
        } else {
            Log.e(TAG, "접근성 서비스가 실행되지 않았습니다");
        }
    }

    private void scrollDown(int count) {
        if (MyAccessibilityService.getInstance() != null) {
            Log.d(TAG, "아래로 스크롤 실행 (" + count + "회)");

            if (count <= 1) {
                MyAccessibilityService.getInstance().performScroll(MyAccessibilityService.Direction.DOWN);
            } else {
                MyAccessibilityService.getInstance().performContinuousScroll(MyAccessibilityService.Direction.DOWN, count);
            }

            lastScrollTime = System.currentTimeMillis();
        } else {
            Log.e(TAG, "접근성 서비스가 실행되지 않았습니다");
        }
    }

    private void performClick(float x, float y) {
        Log.d(TAG, "클릭 실행 (커서 위치): (" + x + ", " + y + ")");

        float cursorX = x;
        float cursorY = y;

        int statusBarHeight = getStatusBarHeight();
        float adjustedX = cursorX;
        float adjustedY = cursorY + statusBarHeight;

        Log.d(TAG, "클릭 실행 (최종 위치): (" + adjustedX + ", " + adjustedY + ")");

        vibrator.vibrate(100);
        MyAccessibilityService.performClickAt(adjustedX, adjustedY);
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            if (!skipProgress) {
                calibrationViewer.setPointAnimationPower(progress);
            }
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            new Handler(Looper.getMainLooper()).post(() -> {
                calibrationViewer.setVisibility(View.VISIBLE);
                showCalibrationPointView(x, y);
            });
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            hideCalibrationView();
            isCalibrating = false;
            
            // 🆕 캘리브레이션 완료 후 커서 오프셋 자동 리셋
            resetCursorOffsetsAfterCalibration();
            
            Toast.makeText(GazeTrackingService.this, "보정 완료 (HIGH 정확도)\n커서 오프셋이 초기화되었습니다", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCalibrationCanceled(double[] calibrationData) {
            resetCalibrationState();
            Toast.makeText(GazeTrackingService.this, "보정 취소", Toast.LENGTH_SHORT).show();
        }
    };

    private void showCalibrationPointView(final float x, final float y) {
        Log.d(TAG, "캘리브레이션 포인트 원본: (" + x + ", " + y + ")");

        // 🎯 화면 크기 정보 가져오기
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float screenWidth = dm.widthPixels;
        float screenHeight = dm.heightPixels;
        
        // 🎯 안전 마진 설정 (화면 가장자리에서 최소 거리)
        float marginX = screenWidth * 0.08f;  // 화면 너비의 8% (약 80-100px)
        float marginY = screenHeight * 0.08f; // 화면 높이의 8%
        
        // 🎯 캘리브레이션 포인트를 안전 영역 내로 조정
        float adjustedX = Math.max(marginX, Math.min(x, screenWidth - marginX));
        float adjustedY = Math.max(marginY, Math.min(y, screenHeight - marginY));
        
        // 📊 조정 결과 로깅
        if (adjustedX != x || adjustedY != y) {
            Log.d(TAG, String.format("캘리브레이션 포인트 조정: (%.0f, %.0f) → (%.0f, %.0f)", 
                    x, y, adjustedX, adjustedY));
        } else {
            Log.d(TAG, "캘리브레이션 포인트: 조정 불필요 (이미 안전 영역 내)");
        }

        skipProgress = true;
        calibrationViewer.setPointAnimationPower(0);
        calibrationViewer.setEnableText(true);
        calibrationViewer.nextPointColor();
        calibrationViewer.setPointPosition(adjustedX, adjustedY);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (trackingRepository.getTracker() != null) {
                trackingRepository.getTracker().startCollectSamples();
                skipProgress = false;
            }
        }, 1000);
    }

    private void hideCalibrationView() {
        new Handler(Looper.getMainLooper()).post(() -> {
            calibrationViewer.setVisibility(View.INVISIBLE);
            overlayCursorView.setVisibility(View.VISIBLE);
            overlayCursorView.setCursorText("●");
            overlayCursorView.setTextPosition(false);
        });
    }

    public void triggerCalibration() {
        Log.d(TAG, "수동 보정 요청");

        if (trackingRepository == null || trackingRepository.getTracker() == null) {
            Toast.makeText(this, "시선 추적 시스템 초기화 안됨", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCalibrating) {
            Toast.makeText(this, "이미 보정 중", Toast.LENGTH_SHORT).show();
            return;
        }

        startCalibration();
    }

    public static void triggerMainActivityCalibration() {
        if (MainActivity.getInstance() != null) {
            MainActivity.getInstance().triggerCalibrationFromService();
        } else {
            Log.w(TAG, "MainActivity 인스턴스를 찾을 수 없습니다");
        }
    }

    public static GazeTrackingService getInstance() {
        return instanceRef != null ? instanceRef.get() : null;
    }

    // 🆕 수동 커서 오프셋 리셋 메서드 (설정 화면에서 호출 가능)
    public void resetCursorOffsets() {
        Log.d(TAG, "수동 커서 오프셋 리셋 요청");
        resetCursorOffsetsAfterCalibration();
        Toast.makeText(this, "커서 오프셋이 초기화되었습니다", Toast.LENGTH_SHORT).show();
    }
    
    // 🆕 현재 오프셋 정보 조회
    public String getCurrentOffsetInfo() {
        return String.format("현재 커서 오프셋: X=%.1f, Y=%.1f", 
                userSettings.getCursorOffsetX(), userSettings.getCursorOffsetY());
    }
    
    // 🆕 오프셋 적용 상태 확인
    public boolean hasActiveOffsets() {
        return userSettings.getCursorOffsetX() != 0f || userSettings.getCursorOffsetY() != 0f;
    }

    public void refreshSettings() {
        userSettings = settingsRepository.getUserSettings();
        clickDetector = new ClickDetector(userSettings);
        edgeScrollDetector = new EdgeScrollDetector(userSettings, this);
        // SwipeDetector 제거 - EdgeScrollDetector가 모든 엣지 기능 담당

        // 🆕 향상된 필터 매니저 재초기화
        enhancedFilterManager = new EnhancedOneEuroFilterManager(
                userSettings.getOneEuroFreq(),
                userSettings.getOneEuroMinCutoff(),
                userSettings.getOneEuroBeta(),
                userSettings.getOneEuroDCutoff()
        );

        Log.d(TAG, "사용자 설정이 새로고침되었습니다");
        Log.d(TAG, "현재 커서 오프셋: X=" + userSettings.getCursorOffsetX() + ", Y=" + userSettings.getCursorOffsetY());
        Log.d(TAG, "현재 OneEuroFilter 프리셋: " + userSettings.getOneEuroFilterPreset().getDisplayName());
        Log.d(TAG, "향상된 필터 상태: " + enhancedFilterManager.getCurrentFilterInfo());
        Log.d(TAG, "통합 엣지 감지기(스크롤+스와이프) 재초기화 완료");
    }

    // 🆕 성능 최적화 설정 메서드들
    public void setPerformanceOptimizationEnabled(boolean enabled) {
        performanceOptimizationEnabled = enabled;

        if (performanceMonitor != null) {
            if (enabled && !performanceMonitor.isMonitoring()) {
                performanceMonitor.startMonitoring();
                Log.d(TAG, "성능 최적화 활성화");
            } else if (!enabled && performanceMonitor.isMonitoring()) {
                performanceMonitor.stopMonitoring();
                Log.d(TAG, "성능 최적화 비활성화");
            }
        }
    }

    public boolean isPerformanceOptimizationEnabled() {
        return performanceOptimizationEnabled;
    }

    // 🆕 안경 보정 기능 설정
    public void setGlassesCompensationEnabled(boolean enabled) {
        if (enhancedFilterManager != null) {
            enhancedFilterManager.setGlassesCompensationEnabled(enabled);
            Log.d(TAG, "안경 보정 기능 " + (enabled ? "활성화" : "비활성화"));
        }
    }

    public boolean isGlassesCompensationEnabled() {
        return enhancedFilterManager != null && enhancedFilterManager.isGlassesCompensationEnabled();
    }

    // 🆕 현재 성능 상태 조회
    public PerformanceMonitor.PerformanceMetrics getCurrentPerformanceMetrics() {
        return performanceMonitor != null ? performanceMonitor.getCurrentMetrics() : null;
    }

    // 🆕 현재 FPS 조회
    public int getCurrentFPS() {
        return trackingRepository != null ? trackingRepository.getCurrentFPS() : 30;
    }

    // 🆕 수동 FPS 설정
    public void setManualFPS(int fps) {
        if (trackingRepository != null) {
            trackingRepository.setTrackingFPS(fps);
            Log.d(TAG, "수동 FPS 설정: " + fps);
        }
    }

    private void checkAccessibilityService() {
        if (MyAccessibilityService.getInstance() == null) {
            Toast.makeText(this, "접근성 서비스를 켜주세요", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "접근성 서비스가 활성화되지 않음");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "서비스 시작됨");

        // 🆕 시작 시 성능 상태 로깅
        if (performanceMonitor != null) {
            PerformanceMonitor.PerformanceMetrics metrics = performanceMonitor.getCurrentMetrics();
            Log.d(TAG, "시작 시 성능 상태: " + metrics.toString());
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "서비스 종료됨");

        // 🆕 성능 모니터링 중지
        if (performanceMonitor != null) {
            performanceMonitor.stopMonitoring();
        }

        // 뷰 제거
        if (overlayCursorView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayCursorView);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "커서 뷰 제거 중 오류: " + e.getMessage());
            }
        }
        if (calibrationViewer != null && windowManager != null) {
            try {
                windowManager.removeView(calibrationViewer);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "캘리브레이션 뷰 제거 중 오류: " + e.getMessage());
            }
        }

        // 🔧 개선: 시선 추적 리소스 완전 정리
        if (trackingRepository != null) {
            trackingRepository.cleanup(); // 🔴 CRITICAL: GazeTracker.releaseGazeTracker() 호출
        }

        // 🆕 핸들러 정리
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        // 🆕 엣지 메뉴 매니저 정리
        if (edgeMenuManager != null) {
            edgeMenuManager.cleanup();
        }

        // 🧹 WeakReference 정리
        if (instanceRef != null) {
            instanceRef.clear();
            instanceRef = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "시선 추적 서비스 채널",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("시선 추적 서비스가 백그라운드에서 실행 중입니다");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}