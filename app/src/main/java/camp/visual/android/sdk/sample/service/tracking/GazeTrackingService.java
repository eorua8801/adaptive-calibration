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
import camp.visual.android.sdk.sample.domain.interaction.ClickDetector;
import camp.visual.android.sdk.sample.domain.interaction.EdgeScrollDetector;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;
import camp.visual.android.sdk.sample.ui.main.MainActivity;
import camp.visual.android.sdk.sample.ui.views.CalibrationViewer;
import camp.visual.android.sdk.sample.ui.views.OverlayCursorView;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.filter.OneEuroFilterManager;
import camp.visual.eyedid.gazetracker.metrics.BlinkInfo;
import camp.visual.eyedid.gazetracker.metrics.FaceInfo;
import camp.visual.eyedid.gazetracker.metrics.GazeInfo;
import camp.visual.eyedid.gazetracker.metrics.UserStatusInfo;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

public class GazeTrackingService extends Service {

    private static final String TAG = "GazeTrackingService";
    private static final String CHANNEL_ID = "GazeTrackingServiceChannel";

    // 컴포넌트
    private EyeTrackingRepository trackingRepository;
    private SettingsRepository settingsRepository;
    private UserSettings userSettings;
    private ClickDetector clickDetector;
    private EdgeScrollDetector edgeScrollDetector;

    // 시스템 서비스 및 UI
    private WindowManager windowManager;
    private OverlayCursorView overlayCursorView;
    private CalibrationViewer calibrationViewer;
    private Vibrator vibrator;
    private OneEuroFilterManager oneEuroFilterManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 상태 변수
    private long lastValidTimestamp = 0;
    private long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN = 1500;
    private boolean isCalibrating = false;
    private boolean skipProgress = false;

    // 서비스 인스턴스
    private static GazeTrackingService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        initRepositories();
        initDetectors();
        createNotificationChannel();
        initSystemServices();
        initViews();
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

        oneEuroFilterManager = new OneEuroFilterManager(
                2,
                (float) userSettings.getOneEuroFreq(),
                (float) userSettings.getOneEuroMinCutoff(),
                (float) userSettings.getOneEuroBeta(),
                (float) userSettings.getOneEuroDCutoff()
        );

        Log.d(TAG, "OneEuroFilter 초기화 - 프리셋: " + userSettings.getOneEuroFilterPreset().getDisplayName());
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
                Log.d(TAG, "GazeTracker 초기화 성공");

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

    // 단순화된 자동 보정 (5포인트만)
    private void startAutoCalibration() {
        Log.d(TAG, "자동 보정 시작");
        Toast.makeText(this, "시선 보정 시작", Toast.LENGTH_SHORT).show();

        handler.postDelayed(() -> {
            startCalibration();
        }, 1000);
    }

    // 5포인트 캘리브레이션 시작
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

    // 상태 초기화
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
                // 필터링 적용
                float filteredX, filteredY;
                long filterTime = android.os.SystemClock.elapsedRealtime();

                if (oneEuroFilterManager.filterValues(filterTime, gazeInfo.x, gazeInfo.y)) {
                    float[] filtered = oneEuroFilterManager.getFilteredValues();
                    filteredX = filtered[0];
                    filteredY = filtered[1];
                } else {
                    filteredX = gazeInfo.x;
                    filteredY = gazeInfo.y;
                }

                // 오프셋 적용
                filteredX += userSettings.getCursorOffsetX();
                filteredY += userSettings.getCursorOffsetY();

                float safeX = Math.max(0, Math.min(filteredX, screenWidth - 1));
                float safeY = Math.max(0, Math.min(filteredY, screenHeight - 1));

                if (!isCalibrating) {
                    overlayCursorView.updatePosition(safeX, safeY);
                    lastValidTimestamp = System.currentTimeMillis();

                    // 엣지 스크롤 처리
                    EdgeScrollDetector.Edge edge = edgeScrollDetector.update(safeY, screenHeight);

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
                    } else if (!edgeScrollDetector.isActive()) {
                        boolean clicked = clickDetector.update(safeX, safeY);
                        overlayCursorView.setProgress(clickDetector.getProgress());
                        overlayCursorView.setCursorText("●");

                        if (clicked) {
                            performClick(safeX, safeY);
                        }
                    }
                }
            }
        }

        @Override
        public void onDrop(long timestamp) {}
    };

    private void resetAll() {
        edgeScrollDetector.resetAll();
        clickDetector.reset();
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
            Toast.makeText(GazeTrackingService.this, "보정 완료", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCalibrationCanceled(double[] calibrationData) {
            resetCalibrationState();
            Toast.makeText(GazeTrackingService.this, "보정 취소", Toast.LENGTH_SHORT).show();
        }
    };

    private void showCalibrationPointView(final float x, final float y) {
        Log.d(TAG, "캘리브레이션 포인트: (" + x + ", " + y + ")");

        float adjustedX = x;
        float adjustedY = y;

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

    /**
     * 메인 화면에서 캘리브레이션 트리거
     */
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

    /**
     * MainActivity에서 캘리브레이션을 실행할 수 있도록 하는 메서드
     */
    public static void triggerMainActivityCalibration() {
        if (MainActivity.getInstance() != null) {
            MainActivity.getInstance().triggerCalibrationFromService();
        } else {
            Log.w(TAG, "MainActivity 인스턴스를 찾을 수 없습니다");
        }
    }

    /**
     * 현재 서비스 인스턴스를 반환
     */
    public static GazeTrackingService getInstance() {
        return instance;
    }

    /**
     * 사용자 설정을 새로고침하는 메서드
     */
    public void refreshSettings() {
        userSettings = settingsRepository.getUserSettings();
        clickDetector = new ClickDetector(userSettings);
        edgeScrollDetector = new EdgeScrollDetector(userSettings, this);

        oneEuroFilterManager = new OneEuroFilterManager(
                2,
                (float) userSettings.getOneEuroFreq(),
                (float) userSettings.getOneEuroMinCutoff(),
                (float) userSettings.getOneEuroBeta(),
                (float) userSettings.getOneEuroDCutoff()
        );

        Log.d(TAG, "사용자 설정이 새로고침되었습니다");
        Log.d(TAG, "현재 커서 오프셋: X=" + userSettings.getCursorOffsetX() + ", Y=" + userSettings.getCursorOffsetY());
        Log.d(TAG, "현재 OneEuroFilter 프리셋: " + userSettings.getOneEuroFilterPreset().getDisplayName());
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
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "서비스 종료됨");

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

        // 시선 추적 중지
        if (trackingRepository != null && trackingRepository.getTracker() != null) {
            trackingRepository.stopTracking();
        }

        instance = null;
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