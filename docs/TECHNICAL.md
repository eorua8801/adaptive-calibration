# 🔧 EyeID SDK 기반 시선 추적 시스템 - 기술 문서

[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com/about/versions/10)
[![EyeID SDK](https://img.shields.io/badge/EyeID%20SDK-Latest-blue.svg)](https://docs.eyedid.ai/)

> **완전한 기술 구현 가이드 - 비주얼캠프 개발팀을 위한 상세 문서**

---

## 📋 목차

1. [시스템 아키텍처](#1-시스템-아키텍처)
2. [EyeID SDK 연동](#2-eyeid-sdk-연동)
3. [핵심 컴포넌트 상세](#3-핵심-컴포넌트-상세)
4. [알고리즘 구현](#4-알고리즘-구현)
5. [성능 최적화](#5-성능-최적화)
6. [확장 개발 가이드](#6-확장-개발-가이드)
7. [문제 해결](#7-문제-해결)

---

## 1. 시스템 아키텍처

### 1.1 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────┐
│                 UI Layer                        │
│  ┌─────────────────────────────────────────────┤
│  │ MainActivity.java (설정 관리)                │
│  │ SettingsActivity.java (상세 설정)           │
│  │ CalibrationViewer.java (캘리브레이션 UI)    │
│  │ OverlayCursorView.java (시선 커서 표시)     │
│  └─────────────────────────────────────────────┤
├─────────────────────────────────────────────────┤
│              Domain Layer                       │
│  ┌─────────────┬─────────────┬─────────────┐    │
│  │   Filter    │ Interaction │Performance  │    │
│  │   Manager   │   Engine    │  Monitor    │    │
│  └─────────────┴─────────────┴─────────────┘    │
├─────────────────────────────────────────────────┤
│                Data Layer                       │
│  ┌─────────────────────┬─────────────────────┐  │
│  │  EyeTracking        │     Settings        │  │
│  │  Repository         │   Repository        │  │
│  └─────────────────────┴─────────────────────┘  │
├─────────────────────────────────────────────────┤
│               Service Layer                     │
│  ┌─────────────────────┬─────────────────────┐  │
│  │ GazeTrackingService │MyAccessibilityService│ │
│  │    (핵심 엔진)        │   (시스템 제어)       │  │
│  └─────────────────────┴─────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 1.2 서비스 우선 아키텍처

**설계 철학**: 서비스가 주도하고, UI는 보조 역할

```java
public class GazeTrackingService extends Service {
    private static GazeTrackingService instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this; // 싱글톤 인스턴스
        initializeComponents();
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    // 핵심: 모든 시선 추적 로직이 여기 집중
    private void initializeComponents() {
        enhancedFilterManager = new EnhancedOneEuroFilterManager(userSettings);
        clickDetector = new ClickDetector(userSettings);
        edgeScrollDetector = new EdgeScrollDetector(userSettings, this);
        performanceMonitor = new PerformanceMonitor(this);
    }
}
```

**장점**:
- 24시간 백그라운드 안정 작동
- MainActivity 종료되어도 지속 작동
- 메모리 효율성 극대화
- 각 모듈 독립성으로 장애 복구 용이

### 1.3 데이터 흐름

```
🎥 전면 카메라
    ↓
📊 EyeID SDK (Raw Gaze Data)
    ↓ gazeInfo.x, gazeInfo.y, trackingState
🔍 EnhancedOneEuroFilterManager
    ├── TrackingState 기반 필터 선택
    ├── 안경 굴절 보정 적용  
    └── 좌표계 변환 (앱→시스템)
    ↓ filteredX, filteredY
🎮 제스처 감지 엔진
    ├── ClickDetector (AOI 기반)
    ├── EdgeScrollDetector (6방향)
    └── 우선순위 기반 제스처 선택
    ↓ 제스처 이벤트
🖥️ MyAccessibilityService
    ├── performClickAt(x, y)
    ├── performScroll(direction)
    └── performSwipe(direction)
    ↓
✅ 시스템 수준 상호작용 실행
```

---

## 2. EyeID SDK 연동

### 2.1 SDK 초기화 및 설정

#### 라이센스 키 관리
```java
// AppConstants.java
public static final String EYEDID_SDK_LICENSE = "dev_ktygge55mai7a041aglteb4onei9a7m9j7tcqagm";

// SecurityManager.java - 보안 강화 버전
public static String getSecureLicense() {
    String licenseKey = "dev_ktygge55mai7a041aglteb4onei9a7m9j7tcqagm";
    
    // 라이센스 키 유효성 검사
    if (licenseKey == null || licenseKey.length() < 10) {
        Log.e(TAG, "Invalid license key");
        return null;
    }
    
    return licenseKey;
}
```

#### SDK 초기화 과정
```java
// EyedidTrackingRepository.java
@Override
public void initTracker(Context context, InitializationCallback callback) {
    try {
        String licenseKey = SecurityManager.getSecureLicense();
        if (licenseKey == null) {
            callback.onInitializationFailed("License key error");
            return;
        }
        
        // SDK 초기화 옵션 설정
        GazeTrackerOptions options = new GazeTrackerOptions.Builder()
            .setAccuracyLevel(AccuracyLevel.HIGH)
            .setFPSMode(FPSMode.DYNAMIC)
            .build();
            
        // EyeID SDK 초기화
        GazeTracker.initGazeTracker(context, licenseKey, new InitializationCallback() {
            @Override
            public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
                if (error == InitializationErrorType.ERROR_NONE) {
                    this.gazeTracker = gazeTracker;
                    setupTrackingCallbacks();
                    callback.onInitializationSuccess();
                } else {
                    callback.onInitializationFailed("SDK init failed: " + error);
                }
            }
        }, options);
        
    } catch (Exception e) {
        callback.onInitializationFailed("Exception: " + e.getMessage());
    }
}
```

### 2.2 추적 콜백 구현

#### 메인 메트릭 콜백
```java
// GazeTrackingService.java
private final TrackingCallback trackingCallback = new TrackingCallback() {
    @Override
    public void onMetrics(long timestamp, GazeInfo gazeInfo, FaceInfo faceInfo, 
                         BlinkInfo blinkInfo, UserStatusInfo userStatusInfo) {
        
        // 1. 기본 유효성 검사
        if (!isValidGazeData(gazeInfo)) {
            return;
        }
        
        // 2. 성능 프로파일링 시작
        long startTime = System.nanoTime();
        
        // 3. 향상된 필터링 적용
        boolean filtered = enhancedFilterManager.filterValues(
            timestamp, 
            gazeInfo.x, gazeInfo.y,
            gazeInfo.fixationX, gazeInfo.fixationY,
            gazeInfo.trackingState
        );
        
        if (!filtered) return;
        
        // 4. 필터링된 좌표 획득
        float[] coordinates = enhancedFilterManager.getFilteredValues();
        float filteredX = coordinates[0];
        float filteredY = coordinates[1];
        
        // 5. 제스처 감지 및 처리
        updateDetectors(filteredX, filteredY);
        
        // 6. UI 업데이트 (메인 스레드)
        updateOverlayCursor(filteredX, filteredY);
        
        // 7. 성능 메트릭 기록
        long processingTime = System.nanoTime() - startTime;
        performanceMonitor.recordFrame(processingTime);
    }
    
    @Override
    public void onTrackingStatus(TrackingEvent event) {
        switch (event.eventType) {
            case TRACKING_START:
                Log.i(TAG, "Tracking started");
                break;
            case TRACKING_STOP:
                Log.i(TAG, "Tracking stopped");
                break;
            case TRACKER_NOT_FOUND:
                Log.w(TAG, "Tracker not found");
                break;
        }
    }
};

// 유효성 검사 로직
private boolean isValidGazeData(GazeInfo gazeInfo) {
    return gazeInfo != null && 
           gazeInfo.trackingState != TrackingState.INIT &&
           !Float.isNaN(gazeInfo.x) && 
           !Float.isNaN(gazeInfo.y) &&
           gazeInfo.x >= 0 && gazeInfo.x <= screenWidth &&
           gazeInfo.y >= 0 && gazeInfo.y <= screenHeight;
}
```

### 2.3 캘리브레이션 구현

#### 5포인트 정밀 캘리브레이션
```java
// AdaptiveCalibrationManager.java
public class AdaptiveCalibrationManager {
    private final List<PointF> calibrationPoints = Arrays.asList(
        new PointF(0.5f, 0.5f),   // 중앙
        new PointF(0.1f, 0.1f),   // 좌상단
        new PointF(0.9f, 0.1f),   // 우상단
        new PointF(0.1f, 0.9f),   // 좌하단
        new PointF(0.9f, 0.9f)    // 우하단
    );
    
    public void startCalibration() {
        if (gazeTracker == null) {
            Log.e(TAG, "GazeTracker not initialized");
            return;
        }
        
        currentPointIndex = 0;
        calibrationSafetyWrapper.beginCalibration();
        
        // 첫 번째 캘리브레이션 포인트 표시
        showCalibrationPoint(calibrationPoints.get(currentPointIndex));
        
        // 캘리브레이션 시작
        gazeTracker.startCalibration(CalibrationModeType.DEFAULT);
    }
    
    private void showCalibrationPoint(PointF point) {
        float screenX = point.x * screenWidth;
        float screenY = point.y * screenHeight;
        
        // UI 업데이트 (메인 스레드에서)
        runOnUiThread(() -> {
            calibrationViewer.showPoint(screenX, screenY);
        });
    }
}

// 캘리브레이션 콜백
private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
    @Override
    public void onCalibrationProgress(float progress) {
        runOnUiThread(() -> {
            calibrationViewer.updateProgress(progress);
        });
    }
    
    @Override
    public void onCalibrationPointFinished(boolean isSuccess) {
        if (isSuccess) {
            currentPointIndex++;
            if (currentPointIndex < calibrationPoints.size()) {
                // 다음 포인트로 진행
                showCalibrationPoint(calibrationPoints.get(currentPointIndex));
            } else {
                // 캘리브레이션 완료
                onCalibrationComplete();
            }
        } else {
            // 실패시 재시도
            showCalibrationPoint(calibrationPoints.get(currentPointIndex));
        }
    }
    
    @Override
    public void onCalibrationFinished(CalibrationResult result) {
        if (result.isSuccess()) {
            Log.i(TAG, "Calibration completed successfully");
            resetCursorOffsetsAfterCalibration(); // 오프셋 초기화
        } else {
            Log.e(TAG, "Calibration failed");
        }
        
        calibrationViewer.hide();
    }
};
```

---

## 3. 핵심 컴포넌트 상세

### 3.1 EnhancedOneEuroFilterManager

#### 동적 필터 전환 시스템
```java
public class EnhancedOneEuroFilterManager {
    // 다중 필터 인스턴스 (TrackingState별)
    private final OneEuroFilter normalFilter;
    private final OneEuroFilter cautionFilter;
    private final OneEuroFilter stabilityFilter;
    
    // 안경 굴절 보정 관련
    private boolean glassesCompensationEnabled;
    private float refractionCorrectionFactor = 0.1f; // 기본 10%
    
    // 성능 통계
    private long totalFilteringTime = 0;
    private int frameCount = 0;
    
    public EnhancedOneEuroFilterManager(UserSettings settings) {
        initializeFilters(settings.getOneEuroFilterPreset());
        this.glassesCompensationEnabled = settings.isGlassesCompensationEnabled();
    }
    
    private void initializeFilters(OneEuroFilterPreset preset) {
        // SUCCESS 상태: 반응성 우선
        normalFilter = new OneEuroFilter(
            preset.freq, 
            preset.minCutoff, 
            preset.beta, 
            preset.dCutoff
        );
        
        // UNSURE 상태: 중간 안정성
        cautionFilter = new OneEuroFilter(
            preset.freq, 
            preset.minCutoff, 
            preset.beta * 0.5f, 
            preset.dCutoff
        );
        
        // FACE_MISSING/GAZE_NOT_FOUND: 안정성 우선
        stabilityFilter = new OneEuroFilter(
            preset.freq, 
            preset.minCutoff, 
            preset.beta * 0.1f, 
            preset.dCutoff
        );
    }
    
    public boolean filterValues(long timestamp, float x, float y,
                              float fixationX, float fixationY, 
                              TrackingState trackingState) {
        
        long startTime = System.nanoTime();
        
        try {
            // 1. TrackingState 기반 필터 선택
            OneEuroFilter selectedFilter = selectFilterByState(trackingState);
            
            // 2. 안경 굴절 보정 적용
            if (glassesCompensationEnabled && hasValidFixation(fixationX, fixationY)) {
                PointF corrected = applyGlassesCorrection(x, y, fixationX, fixationY);
                x = corrected.x;
                y = corrected.y;
            }
            
            // 3. 적응형 필터링 실행
            boolean result = selectedFilter.filter(timestamp, x, y);
            
            if (result) {
                lastFilteredValues[0] = selectedFilter.getFilteredX();
                lastFilteredValues[1] = selectedFilter.getFilteredY();
            }
            
            return result;
            
        } finally {
            // 성능 통계 업데이트
            long filteringTime = System.nanoTime() - startTime;
            totalFilteringTime += filteringTime;
            frameCount++;
        }
    }
    
    private OneEuroFilter selectFilterByState(TrackingState state) {
        switch (state) {
            case SUCCESS:
                return normalFilter;     // 반응성 중심
            case UNSURE:
                return cautionFilter;    // 중간 안정성
            case FACE_MISSING:
            case GAZE_NOT_FOUND:
            default:
                return stabilityFilter;  // 안정성 중심
        }
    }
    
    // 안경 굴절 보정 알고리즘
    private PointF applyGlassesCorrection(float gazeX, float gazeY, 
                                        float fixationX, float fixationY) {
        // fixation 데이터를 gaze 데이터에 융합
        float deltaX = fixationX - gazeX;
        float deltaY = fixationY - gazeY;
        
        // 보정 강도 적용 (매우 보수적)
        float correctedX = gazeX + deltaX * refractionCorrectionFactor;
        float correctedY = gazeY + deltaY * refractionCorrectionFactor;
        
        return new PointF(correctedX, correctedY);
    }
    
    private boolean hasValidFixation(float fixationX, float fixationY) {
        return !Float.isNaN(fixationX) && !Float.isNaN(fixationY) &&
               fixationX > 0 && fixationY > 0;
    }
    
    public float[] getFilteredValues() {
        return lastFilteredValues.clone();
    }
    
    // 성능 분석용
    public double getAverageFilteringTime() {
        return frameCount > 0 ? (totalFilteringTime / frameCount) / 1_000_000.0 : 0;
    }
}
```

#### OneEuroFilterPreset 설정
```java
public enum OneEuroFilterPreset {
    STABILITY("안정성 우선", 30.0, 0.1, 0.001, 1.0),
    BALANCED_STABILITY("균형 (안정성 강화)", 30.0, 0.3, 0.005, 1.0),
    BALANCED("균형", 30.0, 0.5, 0.007, 1.0),
    RESPONSIVE("반응성 우선", 30.0, 1.0, 0.01, 1.0);
    
    public final double freq;      // 주파수
    public final double minCutoff; // 최소 컷오프
    public final double beta;      // 베타 (반응성 vs 안정성)
    public final double dCutoff;   // 미분 컷오프
    
    OneEuroFilterPreset(String description, double freq, double minCutoff, 
                       double beta, double dCutoff) {
        this.description = description;
        this.freq = freq;
        this.minCutoff = minCutoff;
        this.beta = beta;
        this.dCutoff = dCutoff;
    }
}
```

### 3.2 ClickDetector

#### AOI 기반 시선 고정 클릭
```java
public class ClickDetector {
    private static final float DEFAULT_AOI_RADIUS = 40f;      // 관심 영역 반경 (px)
    private static final long DEFAULT_FIXATION_DURATION = 1000L; // 고정 시간 (ms)
    
    private float aoiRadius;
    private long fixationDuration;
    private long fixationStartTime = 0;
    private boolean isFixating = false;
    private float lastGazeX = 0, lastGazeY = 0;
    
    public ClickDetector(UserSettings settings) {
        this.aoiRadius = DEFAULT_AOI_RADIUS;
        this.fixationDuration = getFixationDurationFromSettings(settings.getClickTiming());
    }
    
    public boolean update(float gazeX, float gazeY) {
        long currentTime = System.currentTimeMillis();
        float distance = calculateDistance(gazeX, gazeY, lastGazeX, lastGazeY);
        
        if (distance <= aoiRadius) {
            // AOI 내부에 머무르는 경우
            if (!isFixating) {
                // 새로운 고정 시작
                fixationStartTime = currentTime;
                isFixating = true;
                onFixationStart(gazeX, gazeY);
            }
            
            // 고정 진행률 계산
            long elapsedTime = currentTime - fixationStartTime;
            float progress = Math.min(elapsedTime / (float)fixationDuration, 1.0f);
            onFixationProgress(gazeX, gazeY, progress);
            
            // 고정 완료 확인
            if (elapsedTime >= fixationDuration) {
                triggerClick(gazeX, gazeY);
                reset();
                return true;
            }
        } else {
            // AOI 외부로 이동
            if (isFixating) {
                onFixationEnd();
                reset();
            }
        }
        
        lastGazeX = gazeX;
        lastGazeY = gazeY;
        return false;
    }
    
    private void triggerClick(float x, float y) {
        // 좌표 변환 (앱 좌표 → 시스템 좌표)
        float systemX = x;
        float systemY = y + getStatusBarHeight();
        
        // 접근성 서비스를 통한 클릭 실행
        MyAccessibilityService.performClickAt(systemX, systemY);
        
        // 햅틱 피드백
        if (vibrator != null) {
            vibrator.vibrate(100);
        }
        
        // 로그 기록
        Log.d(TAG, String.format("Click triggered at (%.1f, %.1f)", systemX, systemY));
    }
    
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    private void onFixationStart(float x, float y) {
        // 시각적 피드백 시작
        if (overlayManager != null) {
            overlayManager.showFixationIndicator(x, y);
        }
    }
    
    private void onFixationProgress(float x, float y, float progress) {
        // 진행률 업데이트
        if (overlayManager != null) {
            overlayManager.updateFixationProgress(x, y, progress);
        }
    }
    
    private void onFixationEnd() {
        // 시각적 피드백 종료
        if (overlayManager != null) {
            overlayManager.hideFixationIndicator();
        }
    }
    
    public void reset() {
        isFixating = false;
        fixationStartTime = 0;
        onFixationEnd();
    }
    
    // 설정에 따른 고정 시간 조정
    private long getFixationDurationFromSettings(ClickTiming timing) {
        switch (timing) {
            case FAST: return 800L;
            case NORMAL: return 1000L;
            case SLOW: return 1500L;
            default: return DEFAULT_FIXATION_DURATION;
        }
    }
}
```

### 3.3 EdgeScrollDetector

#### 통합 엣지 감지 시스템
```java
public class EdgeScrollDetector {
    public enum Edge {
        TOP,           // 상단 → 아래로 스크롤
        BOTTOM,        // 하단 → 위로 스크롤  
        LEFT_TOP,      // 좌상단 → 네비게이션 메뉴
        RIGHT_TOP,     // 우상단 → 시스템 메뉴
        LEFT_BOTTOM,   // 좌하단 → 앞으로가기 스와이프
        RIGHT_BOTTOM,  // 우하단 → 뒤로가기 스와이프
        NONE
    }
    
    // 설정 가능한 파라미터
    private float edgeMarginRatio = 0.1f;           // 기본 10% 마진
    private float leftMarginMultiplier = 2.0f;      // 좌측 마진 더 넓게
    private float rightMarginMultiplier = 2.0f;     // 우측 마진 더 넓게
    private long activationDuration = 2000L;        // 2초 활성화 시간
    private long cooldownDuration = 1500L;          // 1.5초 쿨다운
    
    // 상태 관리
    private Edge currentEdge = Edge.NONE;
    private long edgeStartTime = 0;
    private boolean isEdgeActive = false;
    private long lastActionTime = 0;
    
    public EdgeScrollDetector(UserSettings settings, Context context) {
        loadSettingsFromUserPrefs(settings);
        this.context = context;
    }
    
    public Edge update(float x, float y, float screenWidth, float screenHeight) {
        long currentTime = System.currentTimeMillis();
        
        // 쿨다운 확인
        if (currentTime - lastActionTime < cooldownDuration) {
            return Edge.NONE;
        }
        
        Edge detectedEdge = detectEdge(x, y, screenWidth, screenHeight);
        
        if (detectedEdge != Edge.NONE) {
            if (currentEdge != detectedEdge) {
                // 새로운 엣지 감지
                currentEdge = detectedEdge;
                edgeStartTime = currentTime;
                isEdgeActive = false;
                onEdgeEnter(detectedEdge);
            } else {
                // 동일 엣지 지속
                long duration = currentTime - edgeStartTime;
                float progress = Math.min(duration / (float)activationDuration, 1.0f);
                onEdgeProgress(detectedEdge, progress);
                
                if (!isEdgeActive && duration >= activationDuration) {
                    isEdgeActive = true;
                    lastActionTime = currentTime;
                    onEdgeActivated(detectedEdge);
                    return currentEdge; // 활성화 신호
                }
            }
        } else {
            // 엣지 영역을 벗어남
            if (currentEdge != Edge.NONE) {
                onEdgeExit(currentEdge);
                reset();
            }
        }
        
        return Edge.NONE;
    }
    
    private Edge detectEdge(float x, float y, float screenWidth, float screenHeight) {
        float margin = screenHeight * edgeMarginRatio;
        
        boolean isTop = y < margin;
        boolean isBottom = y > screenHeight - margin;
        boolean isLeft = x < margin * leftMarginMultiplier;
        boolean isRight = x > screenWidth - margin * rightMarginMultiplier;
        
        // 우선순위: 모서리 > 가장자리
        if (isTop && isLeft) return Edge.LEFT_TOP;
        if (isTop && isRight) return Edge.RIGHT_TOP;
        if (isBottom && isLeft) return Edge.LEFT_BOTTOM;
        if (isBottom && isRight) return Edge.RIGHT_BOTTOM;
        if (isTop) return Edge.TOP;
        if (isBottom) return Edge.BOTTOM;
        
        return Edge.NONE;
    }
    
    private void onEdgeEnter(Edge edge) {
        // 엣지 진입 시각적 피드백
        if (overlayManager != null) {
            overlayManager.showEdgeIndicator(edge);
        }
        Log.d(TAG, "Edge entered: " + edge);
    }
    
    private void onEdgeProgress(Edge edge, float progress) {
        // 진행률 시각적 피드백
        if (overlayManager != null) {
            overlayManager.updateEdgeProgress(edge, progress);
        }
    }
    
    private void onEdgeActivated(Edge edge) {
        // 엣지 액션 실행
        executeEdgeAction(edge);
        
        // 시각적 피드백 완료
        if (overlayManager != null) {
            overlayManager.showEdgeActivated(edge);
        }
        
        Log.d(TAG, "Edge activated: " + edge);
    }
    
    private void onEdgeExit(Edge edge) {
        // 엣지 이탈 시각적 피드백 제거
        if (overlayManager != null) {
            overlayManager.hideEdgeIndicator(edge);
        }
    }
    
    private void executeEdgeAction(Edge edge) {
        switch (edge) {
            case TOP:
                MyAccessibilityService.performScroll(ScrollDirection.DOWN);
                break;
            case BOTTOM:
                MyAccessibilityService.performScroll(ScrollDirection.UP);
                break;
            case LEFT_TOP:
                showNavigationMenu();
                break;
            case RIGHT_TOP:
                showSystemMenu();
                break;
            case LEFT_BOTTOM:
                MyAccessibilityService.performSwipeAction(SwipeDirection.RIGHT);
                break;
            case RIGHT_BOTTOM:
                MyAccessibilityService.performSwipeAction(SwipeDirection.LEFT);
                break;
        }
    }
    
    public void reset() {
        if (currentEdge != Edge.NONE) {
            onEdgeExit(currentEdge);
        }
        currentEdge = Edge.NONE;
        isEdgeActive = false;
        edgeStartTime = 0;
    }
}
```

### 3.4 MyAccessibilityService

#### 시스템 수준 제어 구현
```java
public class MyAccessibilityService extends AccessibilityService {
    private static MyAccessibilityService instance;
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Accessibility service connected");
    }
    
    // 정밀 클릭 실행
    public static void performClickAt(float x, float y) {
        if (instance == null) {
            Log.e(TAG, "Accessibility service not available");
            return;
        }
        
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        
        GestureDescription.StrokeDescription stroke = 
            new GestureDescription.StrokeDescription(clickPath, 0, 100);
        builder.addStroke(stroke);
        
        GestureDescription gesture = builder.build();
        
        instance.dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, String.format("Click completed at (%.1f, %.1f)", x, y));
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Click gesture cancelled");
            }
        }, null);
    }
    
    // 스크롤 제스처 실행
    public static void performScroll(ScrollDirection direction) {
        if (instance == null) return;
        
        DisplayMetrics metrics = instance.getResources().getDisplayMetrics();
        float centerX = metrics.widthPixels / 2f;
        float centerY = metrics.heightPixels / 2f;
        float scrollDistance = metrics.heightPixels * 0.3f; // 30% 스크롤
        
        Path scrollPath = new Path();
        
        switch (direction) {
            case UP:
                scrollPath.moveTo(centerX, centerY + scrollDistance / 2);
                scrollPath.lineTo(centerX, centerY - scrollDistance / 2);
                break;
            case DOWN:
                scrollPath.moveTo(centerX, centerY - scrollDistance / 2);
                scrollPath.lineTo(centerX, centerY + scrollDistance / 2);
                break;
        }
        
        GestureDescription.StrokeDescription stroke = 
            new GestureDescription.StrokeDescription(scrollPath, 0, 300);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();
            
        instance.dispatchGesture(gesture, null, null);
    }
    
    // 스와이프 제스처 실행
    public static void performSwipeAction(SwipeDirection direction) {
        if (instance == null) return;
        
        DisplayMetrics metrics = instance.getResources().getDisplayMetrics();
        float centerY = metrics.heightPixels / 2f;
        float swipeDistance = metrics.widthPixels * 0.6f; // 60% 스와이프
        
        Path swipePath = new Path();
        
        switch (direction) {
            case LEFT: // 뒤로가기
                swipePath.moveTo(metrics.widthPixels * 0.8f, centerY);
                swipePath.lineTo(metrics.widthPixels * 0.2f, centerY);
                break;
            case RIGHT: // 앞으로가기
                swipePath.moveTo(metrics.widthPixels * 0.2f, centerY);
                swipePath.lineTo(metrics.widthPixels * 0.8f, centerY);
                break;
        }
        
        GestureDescription.StrokeDescription stroke = 
            new GestureDescription.StrokeDescription(swipePath, 0, 300);
        
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();
            
        instance.dispatchGesture(gesture, null, null);
    }
    
    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.i(TAG, "Accessibility service destroyed");
    }
}
```

---

## 4. 알고리즘 구현

### 4.1 좌표계 변환 알고리즘

#### Android 좌표계 분석
```java
/**
 * Android 좌표계 변환
 * 
 * EyeID SDK는 앱 영역 좌표를 반환 (상태바 제외)
 * AccessibilityService는 전체 화면 좌표 필요 (상태바 포함)
 * 
 * 변환 공식:
 * systemX = appX (X축은 변환 불필요)
 * systemY = appY + statusBarHeight (Y축은 상태바 높이만큼 보정)
 */
public class CoordinateTransform {
    private static int statusBarHeight = -1;
    
    public static PointF appToSystemCoordinates(float appX, float appY, Context context) {
        if (statusBarHeight == -1) {
            statusBarHeight = getStatusBarHeight(context);
        }
        
        return new PointF(appX, appY + statusBarHeight);
    }
    
    public static PointF systemToAppCoordinates(float systemX, float systemY, Context context) {
        if (statusBarHeight == -1) {
            statusBarHeight = getStatusBarHeight(context);
        }
        
        return new PointF(systemX, systemY - statusBarHeight);
    }
    
    private static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier(
            "status_bar_height", "dimen", "android");
        
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        
        // 기본값 (대부분 85px)
        return (int) (85 * context.getResources().getDisplayMetrics().density);
    }
}
```

### 4.2 다중 제스처 우선순위 알고리즘

#### 제스처 충돌 해결
```java
public class MultiGestureRecognizer {
    private final List<GestureDetector> detectors;
    private final Map<GestureType, Integer> priorityMap;
    
    public MultiGestureRecognizer() {
        detectors = Arrays.asList(
            clickDetector,
            edgeScrollDetector,
            // 추가 제스처 감지기들...
        );
        
        // 우선순위 설정 (높을수록 우선)
        priorityMap = new HashMap<>();
        priorityMap.put(GestureType.CLICK, 3);
        priorityMap.put(GestureType.EDGE_SCROLL, 2);
        priorityMap.put(GestureType.SWIPE, 1);
    }
    
    public GestureResult recognizeGesture(float x, float y, long timestamp) {
        List<GestureResult> detectedGestures = new ArrayList<>();
        
        // 모든 감지기에서 제스처 검사
        for (GestureDetector detector : detectors) {
            GestureResult result = detector.detect(x, y, timestamp);
            if (result.isDetected()) {
                detectedGestures.add(result);
            }
        }
        
        if (detectedGestures.isEmpty()) {
            return GestureResult.none();
        }
        
        // 우선순위 기반 제스처 선택
        return selectHighestPriorityGesture(detectedGestures);
    }
    
    private GestureResult selectHighestPriorityGesture(List<GestureResult> gestures) {
        return gestures.stream()
            .max(Comparator.comparing(this::calculateGestureScore))
            .orElse(GestureResult.none());
    }
    
    private double calculateGestureScore(GestureResult gesture) {
        int priority = priorityMap.getOrDefault(gesture.getType(), 0);
        double confidence = gesture.getConfidence();
        
        // 우선순위와 신뢰도를 종합한 스코어
        return priority * 10 + confidence;
    }
}
```

### 4.3 성능 최적화 알고리즘

#### 동적 FPS 조정
```java
public class AdaptivePerformanceOptimizer {
    private final MovingAverage batteryUsageRate = new MovingAverage(10);
    private final MovingAverage cpuUsageHistory = new MovingAverage(20);
    private final MovingAverage memoryUsageHistory = new MovingAverage(15);
    
    // 가중치 설정
    private static final double BATTERY_WEIGHT = 0.5;
    private static final double CPU_WEIGHT = 0.3;
    private static final double MEMORY_WEIGHT = 0.2;
    
    public OptimizationStrategy calculateOptimization(PerformanceMetrics metrics) {
        // 1. 배터리 소모율 계산
        double batteryScore = calculateBatteryScore(metrics.batteryLevel);
        
        // 2. CPU 부하 추세 분석
        cpuUsageHistory.add(metrics.cpuUsage);
        double cpuScore = calculateCPUScore();
        
        // 3. 메모리 압박 수준 계산
        memoryUsageHistory.add(metrics.availableMemoryMB);
        double memoryScore = calculateMemoryScore();
        
        // 4. 종합 스코어 계산 (0~1, 높을수록 최적화 필요)
        double optimizationScore = 
            batteryScore * BATTERY_WEIGHT +
            cpuScore * CPU_WEIGHT +
            memoryScore * MEMORY_WEIGHT;
        
        // 5. 최적화 전략 결정
        return determineOptimizationStrategy(optimizationScore);
    }
    
    private double calculateBatteryScore(int batteryLevel) {
        if (batteryLevel <= 10) return 1.0;      // 극한 상황
        if (batteryLevel <= 20) return 0.8;      // 높은 최적화 필요
        if (batteryLevel <= 40) return 0.6;      // 중간 최적화 필요
        if (batteryLevel <= 60) return 0.4;      // 낮은 최적화 필요
        return 0.2;                               // 최소 최적화
    }
    
    private double calculateCPUScore() {
        double currentCPU = cpuUsageHistory.getLatest();
        double avgCPU = cpuUsageHistory.getAverage();
        double trend = calculateTrend(cpuUsageHistory);
        
        // 현재 사용률 + 평균 사용률 + 증가 추세 고려
        double baseScore = Math.min(currentCPU / 100.0, 1.0);
        double avgScore = Math.min(avgCPU / 100.0, 1.0);
        double trendScore = Math.max(trend, 0) * 0.5; // 증가 추세만 고려
        
        return (baseScore * 0.5 + avgScore * 0.3 + trendScore * 0.2);
    }
    
    private double calculateMemoryScore() {
        double currentMemory = memoryUsageHistory.getLatest();
        double avgMemory = memoryUsageHistory.getAverage();
        
        // 메모리가 적을수록 높은 스코어 (최적화 필요)
        double currentScore = Math.max(0, 1.0 - currentMemory / 1000.0); // 1GB 기준
        double avgScore = Math.max(0, 1.0 - avgMemory / 1000.0);
        
        return (currentScore * 0.7 + avgScore * 0.3);
    }
    
    private OptimizationStrategy determineOptimizationStrategy(double score) {
        if (score > 0.8) {
            return OptimizationStrategy.AGGRESSIVE; // FPS 10, 기능 제한
        } else if (score > 0.6) {
            return OptimizationStrategy.MODERATE;   // FPS 20, 일부 제한
        } else if (score > 0.4) {
            return OptimizationStrategy.LIGHT;      // FPS 25, 경미한 제한
        } else {
            return OptimizationStrategy.NORMAL;     // FPS 30, 모든 기능
        }
    }
    
    private double calculateTrend(MovingAverage data) {
        List<Double> values = data.getRecentValues(5); // 최근 5개 값
        if (values.size() < 3) return 0;
        
        // 간단한 선형 추세 계산
        double sum = 0;
        for (int i = 1; i < values.size(); i++) {
            sum += values.get(i) - values.get(i-1);
        }
        return sum / (values.size() - 1);
    }
}

// 성능 최적화 전략
public enum OptimizationStrategy {
    NORMAL(30, false, false),
    LIGHT(25, false, true),
    MODERATE(20, true, true),
    AGGRESSIVE(10, true, true);
    
    public final int targetFPS;
    public final boolean reducedFeatures;
    public final boolean batteryOptimization;
    
    OptimizationStrategy(int fps, boolean reduced, boolean battery) {
        this.targetFPS = fps;
        this.reducedFeatures = reduced;
        this.batteryOptimization = battery;
    }
}
```

---

## 5. 성능 최적화

### 5.1 메모리 최적화

#### 객체 풀링 및 재사용
```java
public class MemoryOptimizedComponents {
    // 자주 생성되는 객체들의 풀
    private final Queue<PointF> pointPool = new ArrayDeque<>();
    private final Queue<float[]> arrayPool = new ArrayDeque<>();
    
    // 재사용 가능한 객체들
    private final PointF reusablePoint = new PointF();
    private final float[] reusableArray = new float[2];
    
    public PointF getPoint(float x, float y) {
        PointF point = pointPool.poll();
        if (point == null) {
            point = new PointF();
        }
        point.set(x, y);
        return point;
    }
    
    public void recyclePoint(PointF point) {
        if (pointPool.size() < 10) { // 최대 10개까지 풀링
            pointPool.offer(point);
        }
    }
    
    public float[] getArray() {
        float[] array = arrayPool.poll();
        if (array == null) {
            array = new float[2];
        }
        return array;
    }
    
    public void recycleArray(float[] array) {
        if (arrayPool.size() < 5) { // 최대 5개까지 풀링
            arrayPool.offer(array);
        }
    }
}

// 사용 예시
public class OptimizedClickDetector {
    private final MemoryOptimizedComponents memPool = new MemoryOptimizedComponents();
    
    public boolean update(float gazeX, float gazeY) {
        // 기존: new PointF(gazeX, gazeY) - 매번 새 객체 생성
        // 최적화: 객체 풀에서 재사용
        PointF currentPoint = memPool.getPoint(gazeX, gazeY);
        
        try {
            // 로직 처리...
            return processPoint(currentPoint);
        } finally {
            // 사용 완료 후 풀에 반환
            memPool.recyclePoint(currentPoint);
        }
    }
}
```

#### 메모리 누수 방지
```java
public class LeakSafeGazeTrackingService extends Service {
    private WeakReference<Context> contextRef;
    private Handler handler;
    private HandlerThread backgroundThread;
    
    @Override
    public void onCreate() {
        super.onCreate();
        contextRef = new WeakReference<>(this);
        
        // 백그라운드 스레드 생성
        backgroundThread = new HandlerThread("GazeProcessing");
        backgroundThread.start();
        handler = new Handler(backgroundThread.getLooper());
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 메모리 누수 방지를 위한 정리
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join(1000); // 1초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // WeakReference 정리
        if (contextRef != null) {
            contextRef.clear();
        }
        
        // 필터 정리
        if (enhancedFilterManager != null) {
            enhancedFilterManager.cleanup();
        }
        
        // 오버레이 뷰 정리
        cleanupOverlayViews();
        
        instance = null; // 싱글톤 참조 해제
    }
    
    private void cleanupOverlayViews() {
        if (overlayCursorView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayCursorView);
            } catch (IllegalArgumentException e) {
                // 이미 제거된 경우 무시
                Log.w(TAG, "View already removed: " + e.getMessage());
            }
            overlayCursorView = null;
        }
    }
}
```

### 5.2 CPU 최적화

#### 백그라운드 스레드 활용
```java
public class ThreadOptimizedProcessing {
    private final ExecutorService backgroundExecutor = 
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GazeProcessing");
            t.setPriority(Thread.NORM_PRIORITY - 1); // 낮은 우선순위
            return t;
        });
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public void processGazeData(GazeInfo gazeInfo) {
        // 무거운 연산은 백그라운드에서
        backgroundExecutor.execute(() -> {
            // 1. 필터링 (CPU 집약적)
            boolean filtered = enhancedFilterManager.filterValues(
                System.currentTimeMillis(), 
                gazeInfo.x, gazeInfo.y,
                gazeInfo.fixationX, gazeInfo.fixationY,
                gazeInfo.trackingState
            );
            
            if (!filtered) return;
            
            // 2. 제스처 감지 (CPU 집약적)
            float[] coordinates = enhancedFilterManager.getFilteredValues();
            GestureResult gesture = multiGestureRecognizer.recognizeGesture(
                coordinates[0], coordinates[1], System.currentTimeMillis());
            
            // 3. UI 업데이트는 메인 스레드로
            mainHandler.post(() -> {
                updateOverlayCursor(coordinates[0], coordinates[1]);
                
                if (gesture.isDetected()) {
                    executeGesture(gesture);
                }
            });
        });
    }
    
    public void shutdown() {
        backgroundExecutor.shutdown();
        try {
            if (!backgroundExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 알고리즘 최적화
```java
public class OptimizedDistanceCalculation {
    // 기존: 매번 Math.sqrt() 호출로 비용이 높음
    public static float calculateDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    // 최적화: 제곱 거리 비교로 sqrt() 호출 제거
    public static boolean isWithinDistance(float x1, float y1, float x2, float y2, float threshold) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float distanceSquared = dx * dx + dy * dy;
        float thresholdSquared = threshold * threshold;
        
        return distanceSquared <= thresholdSquared;
    }
    
    // 빠른 근사치 계산 (정확도는 약간 떨어지지만 매우 빠름)
    public static float fastDistance(float x1, float y1, float x2, float y2) {
        float dx = Math.abs(x2 - x1);
        float dy = Math.abs(y2 - y1);
        
        // 맨하탄 거리 근사
        return dx + dy;
    }
}

// 캐시를 활용한 필터 최적화
public class CachedOneEuroFilter {
    private float lastX = Float.NaN, lastY = Float.NaN;
    private long lastTimestamp = 0;
    private float[] cachedResult = new float[2];
    
    public boolean filter(long timestamp, float x, float y) {
        // 동일한 입력에 대해서는 캐시된 결과 반환
        if (timestamp == lastTimestamp && x == lastX && y == lastY) {
            return true;
        }
        
        // 실제 필터링 수행
        boolean result = super.filter(timestamp, x, y);
        
        if (result) {
            // 결과 캐시
            lastTimestamp = timestamp;
            lastX = x;
            lastY = y;
            cachedResult[0] = getFilteredX();
            cachedResult[1] = getFilteredY();
        }
        
        return result;
    }
    
    public float[] getFilteredValues() {
        return cachedResult.clone();
    }
}
```

### 5.3 배터리 최적화

#### 동적 FPS 조정 상세 구현
```java
public class BatteryOptimizedFPSManager {
    private final BatteryManager batteryManager;
    private int currentFPS = 30;
    private long lastOptimizationTime = 0;
    private static final long OPTIMIZATION_INTERVAL = 10000; // 10초마다 체크
    
    public BatteryOptimizedFPSManager(Context context) {
        this.batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }
    
    public void optimizePerformance() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOptimizationTime < OPTIMIZATION_INTERVAL) {
            return;
        }
        
        lastOptimizationTime = currentTime;
        
        PerformanceMetrics metrics = collectMetrics();
        int newFPS = calculateOptimalFPS(metrics);
        
        if (newFPS != currentFPS) {
            adjustFPS(newFPS);
        }
    }
    
    private PerformanceMetrics collectMetrics() {
        // 배터리 정보
        int batteryLevel = getBatteryLevel();
        boolean isCharging = isCharging();
        
        // CPU 사용률 (근사치)
        float cpuUsage = getCPUUsage();
        
        // 메모리 사용률
        long availableMemory = getAvailableMemory();
        
        return new PerformanceMetrics(batteryLevel, cpuUsage, availableMemory, isCharging);
    }
    
    private int calculateOptimalFPS(PerformanceMetrics metrics) {
        int baseFPS = 30;
        
        // 배터리 레벨에 따른 조정
        if (!metrics.isCharging) {
            if (metrics.batteryLevel <= 10) {
                baseFPS = 10; // 극한 절전
            } else if (metrics.batteryLevel <= 20) {
                baseFPS = 15; // 높은 절전
            } else if (metrics.batteryLevel <= 40) {
                baseFPS = 20; // 중간 절전
            } else if (metrics.batteryLevel <= 60) {
                baseFPS = 25; // 낮은 절전
            }
            // else: 정상 30fps
        }
        
        // CPU 사용률에 따른 추가 조정
        if (metrics.cpuUsage > 80) {
            baseFPS = Math.max(10, baseFPS - 5);
        } else if (metrics.cpuUsage > 90) {
            baseFPS = Math.max(10, baseFPS - 10);
        }
        
        // 메모리 부족시 추가 조정
        if (metrics.availableMemoryMB < 200) {
            baseFPS = Math.max(10, baseFPS - 5);
        }
        
        return baseFPS;
    }
    
    private void adjustFPS(int newFPS) {
        currentFPS = newFPS;
        
        // EyeID SDK FPS 조정
        if (gazeTracker != null) {
            gazeTracker.setTrackingFPS(newFPS);
        }
        
        Log.i(TAG, "FPS adjusted to: " + newFPS);
    }
    
    private int getBatteryLevel() {
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return 100; // 기본값
    }
    
    private boolean isCharging() {
        if (batteryManager != null) {
            int status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
            return status == BatteryManager.BATTERY_STATUS_CHARGING;
        }
        return false;
    }
    
    private float getCPUUsage() {
        // 간단한 CPU 사용률 근사치
        // 실제 구현에서는 더 정확한 방법 사용 권장
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return ((float)(totalMemory - freeMemory) / totalMemory) * 100;
    }
    
    private long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory() / (1024 * 1024); // MB 단위
    }
}
```

---

## 6. 확장 개발 가이드

### 6.1 새로운 제스처 추가하기

#### 단계별 제스처 추가 과정
```java
// 1단계: 제스처 타입 정의
public enum GestureType {
    CLICK,
    EDGE_SCROLL,
    SWIPE,
    CUSTOM_GESTURE // 새로운 제스처 타입 추가
}

// 2단계: 커스텀 제스처 감지기 생성
public class CustomGestureDetector implements GestureDetector {
    private UserSettings settings;
    private long gestureStartTime = 0;
    private boolean isActive = false;
    private float startX, startY;
    
    public CustomGestureDetector(UserSettings settings) {
        this.settings = settings;
    }
    
    @Override
    public GestureResult detect(float x, float y, long timestamp) {
        // 제스처 감지 로직 구현
        if (isGestureStartCondition(x, y)) {
            if (!isActive) {
                startGesture(x, y, timestamp);
            }
            return updateGesture(x, y, timestamp);
        } else {
            if (isActive) {
                return finishGesture(x, y, timestamp);
            }
        }
        
        return GestureResult.none();
    }
    
    private boolean isGestureStartCondition(float x, float y) {
        // 제스처 시작 조건 정의
        // 예: 특정 영역 내 시선 고정
        return isInSpecialArea(x, y);
    }
    
    private void startGesture(float x, float y, long timestamp) {
        isActive = true;
        startX = x;
        startY = y;
        gestureStartTime = timestamp;
    }
    
    private GestureResult updateGesture(float x, float y, long timestamp) {
        long duration = timestamp - gestureStartTime;
        
        if (duration >= settings.getCustomGestureDuration()) {
            // 제스처 완료
            return new GestureResult(GestureType.CUSTOM_GESTURE, 1.0f, true);
        }
        
        // 진행 중
        float progress = (float) duration / settings.getCustomGestureDuration();
        return new GestureResult(GestureType.CUSTOM_GESTURE, progress, false);
    }
    
    private GestureResult finishGesture(float x, float y, long timestamp) {
        reset();
        return GestureResult.none();
    }
    
    public void reset() {
        isActive = false;
        gestureStartTime = 0;
    }
    
    private boolean isInSpecialArea(float x, float y) {
        // 커스텀 제스처 영역 정의
        // 예: 화면 중앙 특정 영역
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        float radius = 100f;
        
        float distance = (float) Math.sqrt(
            Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
        
        return distance <= radius;
    }
}

// 3단계: MultiGestureRecognizer에 추가
public class ExtendedMultiGestureRecognizer extends MultiGestureRecognizer {
    private CustomGestureDetector customGestureDetector;
    
    public ExtendedMultiGestureRecognizer(UserSettings settings) {
        super();
        customGestureDetector = new CustomGestureDetector(settings);
        
        // 새로운 제스처 감지기 추가
        detectors.add(customGestureDetector);
        
        // 우선순위 설정
        priorityMap.put(GestureType.CUSTOM_GESTURE, 2);
    }
}

// 4단계: 제스처 실행 로직 추가
private void executeGesture(GestureResult gesture) {
    switch (gesture.getType()) {
        case CLICK:
            handleClick(gesture);
            break;
        case EDGE_SCROLL:
            handleEdgeScroll(gesture);
            break;
        case CUSTOM_GESTURE: // 새로운 제스처 처리
            handleCustomGesture(gesture);
            break;
    }
}

private void handleCustomGesture(GestureResult gesture) {
    // 커스텀 제스처 실행 로직
    Log.d(TAG, "Custom gesture detected!");
    
    // 예: 특별한 액션 실행
    MyAccessibilityService.performSpecialAction();
    
    // 시각적 피드백
    showCustomGestureFeedback();
    
    // 햅틱 피드백
    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
}
```

### 6.2 필터 알고리즘 커스터마이징

#### 고급 필터 개발
```java
// 커스텀 필터 인터페이스
public interface AdvancedFilter {
    boolean filter(long timestamp, float x, float y, TrackingState state, float confidence);
    float[] getFilteredValues();
    void reset();
    void updateSettings(FilterSettings settings);
}

// Kalman 필터 구현 예시
public class KalmanGazeFilter implements AdvancedFilter {
    private KalmanFilter xFilter, yFilter;
    private float[] lastFilteredValues = new float[2];
    private boolean initialized = false;
    
    public KalmanGazeFilter(FilterSettings settings) {
        initializeKalmanFilters(settings);
    }
    
    private void initializeKalmanFilters(FilterSettings settings) {
        // X축 Kalman 필터
        xFilter = new KalmanFilter(
            settings.processNoise,    // 프로세스 노이즈
            settings.measurementNoise // 측정 노이즈
        );
        
        // Y축 Kalman 필터
        yFilter = new KalmanFilter(
            settings.processNoise,
            settings.measurementNoise
        );
    }
    
    @Override
    public boolean filter(long timestamp, float x, float y, TrackingState state, float confidence) {
        if (!initialized) {
            xFilter.initialize(x);
            yFilter.initialize(y);
            initialized = true;
        }
        
        // TrackingState에 따른 노이즈 조정
        adjustNoiseByState(state, confidence);
        
        // Kalman 필터링 수행
        float filteredX = xFilter.update(x);
        float filteredY = yFilter.update(y);
        
        lastFilteredValues[0] = filteredX;
        lastFilteredValues[1] = filteredY;
        
        return true;
    }
    
    private void adjustNoiseByState(TrackingState state, float confidence) {
        float noiseFactor = 1.0f;
        
        switch (state) {
            case SUCCESS:
                noiseFactor = 0.5f + (1.0f - confidence) * 0.5f;
                break;
            case UNSURE:
                noiseFactor = 1.0f;
                break;
            case FACE_MISSING:
            case GAZE_NOT_FOUND:
                noiseFactor = 2.0f;
                break;
        }
        
        xFilter.setMeasurementNoise(baseNoise * noiseFactor);
        yFilter.setMeasurementNoise(baseNoise * noiseFactor);
    }
    
    @Override
    public float[] getFilteredValues() {
        return lastFilteredValues.clone();
    }
    
    @Override
    public void reset() {
        initialized = false;
        xFilter.reset();
        yFilter.reset();
    }
}

// 머신러닝 기반 적응형 필터
public class MLAdaptiveFilter implements AdvancedFilter {
    private TensorFlowLite model;
    private float[] inputBuffer = new float[10]; // 최근 10개 입력
    private float[] outputBuffer = new float[2]; // 필터링된 X, Y
    private int bufferIndex = 0;
    
    public MLAdaptiveFilter(Context context) {
        loadModel(context);
    }
    
    private void loadModel(Context context) {
        try {
            // TensorFlow Lite 모델 로드
            model = new Interpreter(loadModelFile(context, "gaze_filter_model.tflite"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to load ML model", e);
            // 폴백으로 기본 필터 사용
        }
    }
    
    @Override
    public boolean filter(long timestamp, float x, float y, TrackingState state, float confidence) {
        if (model == null) {
            // 모델 로드 실패시 기본 처리
            outputBuffer[0] = x;
            outputBuffer[1] = y;
            return true;
        }
        
        // 입력 버퍼 업데이트
        updateInputBuffer(x, y, state, confidence);
        
        // ML 모델 실행
        model.run(inputBuffer, outputBuffer);
        
        return true;
    }
    
    private void updateInputBuffer(float x, float y, TrackingState state, float confidence) {
        // 순환 버퍼에 최근 데이터 저장
        inputBuffer[bufferIndex % 5] = x;
        inputBuffer[(bufferIndex % 5) + 5] = y;
        
        // TrackingState와 confidence도 입력에 포함
        inputBuffer[8] = encodeTrackingState(state);
        inputBuffer[9] = confidence;
        
        bufferIndex++;
    }
}
```

### 6.3 성능 모니터링 확장

#### 고급 성능 분석 도구
```java
public class AdvancedPerformanceProfiler {
    private final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public static class PerformanceMetric {
        private final String name;
        private final MovingAverage values = new MovingAverage(100);
        private final AtomicLong totalCalls = new AtomicLong();
        private volatile long minValue = Long.MAX_VALUE;
        private volatile long maxValue = Long.MIN_VALUE;
        
        public PerformanceMetric(String name) {
            this.name = name;
        }
        
        public void record(long value) {
            values.add(value);
            totalCalls.incrementAndGet();
            
            // 최소/최대값 업데이트
            minValue = Math.min(minValue, value);
            maxValue = Math.max(maxValue, value);
        }
        
        public PerformanceReport getReport() {
            return new PerformanceReport(
                name,
                values.getAverage(),
                values.getStandardDeviation(),
                minValue,
                maxValue,
                totalCalls.get()
            );
        }
    }
    
    // 성능 측정 시작
    public PerformanceTimer startTiming(String operation) {
        return new PerformanceTimer(operation, this);
    }
    
    // 성능 데이터 기록
    public void recordPerformance(String operation, long durationNanos) {
        metrics.computeIfAbsent(operation, PerformanceMetric::new)
               .record(durationNanos);
    }
    
    // 주기적인 성능 리포트 생성
    public void startPeriodicReporting(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::generateReport, 
                                    intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }
    
    private void generateReport() {
        StringBuilder report = new StringBuilder("=== Performance Report ===\n");
        
        metrics.values().stream()
               .sorted(Comparator.comparing(m -> m.name))
               .forEach(metric -> {
                   PerformanceReport metricReport = metric.getReport();
                   report.append(String.format(
                       "%s: avg=%.2fms, std=%.2fms, min=%dms, max=%dms, calls=%d\n",
                       metricReport.name,
                       metricReport.averageMs,
                       metricReport.standardDeviationMs,
                       metricReport.minMs,
                       metricReport.maxMs,
                       metricReport.totalCalls
                   ));
               });
        
        Log.i(TAG, report.toString());
    }
    
    // 메모리 사용량 모니터링
    public void startMemoryMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double usagePercent = (usedMemory * 100.0) / maxMemory;
            
            recordPerformance("memory_usage_percent", (long) usagePercent);
            
            if (usagePercent > 80) {
                Log.w(TAG, String.format("High memory usage: %.1f%%", usagePercent));
                // 메모리 정리 트리거
                System.gc();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// 성능 측정 헬퍼 클래스
public class PerformanceTimer implements AutoCloseable {
    private final String operation;
    private final AdvancedPerformanceProfiler profiler;
    private final long startTime;
    
    public PerformanceTimer(String operation, AdvancedPerformanceProfiler profiler) {
        this.operation = operation;
        this.profiler = profiler;
        this.startTime = System.nanoTime();
    }
    
    @Override
    public void close() {
        long duration = System.nanoTime() - startTime;
        profiler.recordPerformance(operation, duration);
    }
}

// 사용 예시
public void processGazeData(GazeInfo gazeInfo) {
    try (PerformanceTimer timer = profiler.startTiming("gaze_processing")) {
        // 시선 데이터 처리...
        
        try (PerformanceTimer filterTimer = profiler.startTiming("filtering")) {
            enhancedFilterManager.filterValues(/*...*/);
        }
        
        try (PerformanceTimer gestureTimer = profiler.startTiming("gesture_detection")) {
            multiGestureRecognizer.recognizeGesture(/*...*/);
        }
    }
}
```

---

## 7. 문제 해결

### 7.1 일반적인 문제 및 해결책

#### SDK 초기화 관련
```java
public class SDKTroubleshooting {
    
    public static void diagnoseLicenseIssues() {
        String license = SecurityManager.getSecureLicense();
        
        if (license == null || license.trim().isEmpty()) {
            Log.e(TAG, "License key is null or empty");
            Log.e(TAG, "Check AppConstants.EYEDID_SDK_LICENSE");
            return;
        }
        
        if (license.length() < 10) {
            Log.e(TAG, "License key too short: " + license.length());
            return;
        }
        
        if (!license.startsWith("dev_")) {
            Log.w(TAG, "Using non-development license key");
        }
        
        Log.i(TAG, "License key validation passed");
    }
    
    public static void diagnosePermissionIssues(Context context) {
        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted");
        }
        
        // 오버레이 권한 확인
        if (!Settings.canDrawOverlays(context)) {
            Log.e(TAG, "Overlay permission not granted");
        }
        
        // 접근성 서비스 확인
        if (!isAccessibilityServiceEnabled(context)) {
            Log.e(TAG, "Accessibility service not enabled");
        }
    }
    
    private static boolean isAccessibilityServiceEnabled(Context context) {
        String serviceName = context.getPackageName() + "/" + 
                           MyAccessibilityService.class.getCanonicalName();
        
        String enabledServices = Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        
        return enabledServices != null && enabledServices.contains(serviceName);
    }
}
```

#### 성능 문제 진단
```java
public class PerformanceDiagnostics {
    
    public static void diagnoseFrameDrops() {
        // 프레임 드롭 패턴 분석
        if (consecutiveFrameDrops > 5) {
            Log.w(TAG, "Consecutive frame drops detected: " + consecutiveFrameDrops);
            
            // 가능한 원인들 체크
            checkCPUUsage();
            checkMemoryUsage();
            checkBatteryLevel();
        }
    }
    
    private static void checkCPUUsage() {
        // CPU 사용률이 높은지 확인
        float cpuUsage = getCurrentCPUUsage();
        if (cpuUsage > 80) {
            Log.w(TAG, "High CPU usage detected: " + cpuUsage + "%");
            Log.i(TAG, "Suggestion: Reduce FPS or enable performance mode");
        }
    }
    
    private static void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double usagePercent = (usedMemory * 100.0) / maxMemory;
        
        if (usagePercent > 85) {
            Log.w(TAG, "High memory usage: " + usagePercent + "%");
            Log.i(TAG, "Suggestion: Enable memory optimization mode");
        }
    }
    
    public static void diagnoseTrackingAccuracy() {
        // 추적 정확도 문제 진단
        if (trackingAccuracy < 0.8) {
            Log.w(TAG, "Low tracking accuracy: " + trackingAccuracy);
            
            // 가능한 해결책 제시
            Log.i(TAG, "Suggestions:");
            Log.i(TAG, "1. Recalibrate the system");
            Log.i(TAG, "2. Clean camera lens");
            Log.i(TAG, "3. Improve lighting conditions");
            Log.i(TAG, "4. Check if glasses compensation is enabled");
        }
    }
}
```

#### 디버깅 도구
```java
public class DebugUtils {
    private static boolean debugMode = BuildConfig.DEBUG;
    
    public static void enableDebugMode() {
        debugMode = true;
        Log.i(TAG, "Debug mode enabled");
    }
    
    public static void logCoordinateInfo(float gazeX, float gazeY, float systemX, float systemY) {
        if (!debugMode) return;
        
        Log.d(TAG, String.format(
            "Coordinates - Gaze: (%.1f, %.1f), System: (%.1f, %.1f), Offset: (%.1f, %.1f)",
            gazeX, gazeY, systemX, systemY, systemX - gazeX, systemY - gazeY
        ));
    }
    
    public static void logFilteringPerformance(String filterName, long processingTime, 
                                             boolean filtered, float[] values) {
        if (!debugMode) return;
        
        Log.d(TAG, String.format(
            "Filter[%s] - Time: %dμs, Filtered: %b, Values: (%.2f, %.2f)",
            filterName, processingTime / 1000, filtered, 
            values != null ? values[0] : 0, values != null ? values[1] : 0
        ));
    }
    
    public static void logGestureDetection(GestureType type, float confidence, boolean completed) {
        if (!debugMode) return;
        
        Log.d(TAG, String.format(
            "Gesture[%s] - Confidence: %.2f, Completed: %b",
            type, confidence, completed
        ));
    }
    
    // ADB 로그 필터링을 위한 태그 정리
    public static void printLogTags() {
        Log.i(TAG, "=== Debug Log Tags ===");
        Log.i(TAG, "Main service: " + AppConstants.Logging.TAG_SERVICE);
        Log.i(TAG, "Click detection: ClickDetector");
        Log.i(TAG, "Edge scroll: EdgeScrollDetector");  
        Log.i(TAG, "Filtering: EnhancedOneEuroFilter");
        Log.i(TAG, "Performance: " + AppConstants.Logging.TAG_PERFORMANCE);
        Log.i(TAG, "Security: " + AppConstants.Logging.TAG_SECURITY);
        Log.i(TAG, "=====================");
    }
}

// ADB 명령어 참조
/*
전체 로그 확인:
adb logcat | grep "GazeTrackingService\|ClickDetector\|EdgeScrollDetector"

성능 로그만:
adb logcat | grep "Performance\|Memory"

에러만:
adb logcat | grep "ERROR"

특정 태그:
adb logcat -s "GazeTrackingService:D"
*/
```

---

## 8. 부록

### 8.1 설정 파라미터 참조

```java
// 주요 설정값들과 권장 범위
public class ConfigurationReference {
    
    // 클릭 감지 설정
    public static final float CLICK_AOI_RADIUS_MIN = 20f;     // 최소 반경
    public static final float CLICK_AOI_RADIUS_MAX = 80f;     // 최대 반경
    public static final float CLICK_AOI_RADIUS_DEFAULT = 40f; // 기본 반경
    
    public static final long CLICK_DURATION_MIN = 500L;       // 최소 고정 시간
    public static final long CLICK_DURATION_MAX = 3000L;      // 최대 고정 시간
    public static final long CLICK_DURATION_DEFAULT = 1000L;  // 기본 고정 시간
    
    // 엣지 감지 설정
    public static final float EDGE_MARGIN_MIN = 0.05f;        // 최소 마진 (5%)
    public static final float EDGE_MARGIN_MAX = 0.2f;         // 최대 마진 (20%)
    public static final float EDGE_MARGIN_DEFAULT = 0.1f;     // 기본 마진 (10%)
    
    public static final long EDGE_ACTIVATION_MIN = 1000L;     // 최소 활성화 시간
    public static final long EDGE_ACTIVATION_MAX = 5000L;     // 최대 활성화 시간
    public static final long EDGE_ACTIVATION_DEFAULT = 2000L; // 기본 활성화 시간
    
    // 성능 설정
    public static final int FPS_MIN = 10;                     // 최소 FPS
    public static final int FPS_MAX = 30;                     // 최대 FPS
    public static final int FPS_DEFAULT = 30;                 // 기본 FPS
    
    // 필터링 설정 (OneEuro)
    public static final double FREQ_DEFAULT = 30.0;
    public static final double MIN_CUTOFF_MIN = 0.1;
    public static final double MIN_CUTOFF_MAX = 2.0;
    public static final double BETA_MIN = 0.001;
    public static final double BETA_MAX = 0.02;
    public static final double D_CUTOFF_DEFAULT = 1.0;
}
```

### 8.2 API 참조

#### 주요 메소드 시그니처
```java
// GazeTrackingService 주요 메소드
public class GazeTrackingService {
    public static GazeTrackingService getInstance();
    public void refreshSettings();
    public boolean isTrackingActive();
    public void triggerCalibration();
}

// EnhancedOneEuroFilterManager 주요 메소드  
public class EnhancedOneEuroFilterManager {
    public boolean filterValues(long timestamp, float x, float y, 
                              float fixationX, float fixationY, TrackingState trackingState);
    public float[] getFilteredValues();
    public void setRefractionCorrectionFactor(float factor);
    public double getAverageFilteringTime();
}

// ClickDetector 주요 메소드
public class ClickDetector {
    public boolean update(float gazeX, float gazeY);
    public void setAOIRadius(float radius);
    public void setFixationDuration(long duration);
    public void reset();
}

// EdgeScrollDetector 주요 메소드
public class EdgeScrollDetector {
    public Edge update(float x, float y, float screenWidth, float screenHeight);
    public void setEdgeMarginRatio(float ratio);
    public void setActivationDuration(long duration);
    public void reset();
}

// MyAccessibilityService 주요 메소드
public class MyAccessibilityService {
    public static void performClickAt(float x, float y);
    public static void performScroll(ScrollDirection direction);
    public static void performSwipeAction(SwipeDirection direction);
}
```

### 8.3 참고 링크

- **EyeID SDK 문서**: https://docs.eyedid.ai/
- **Android 접근성 가이드**: https://developer.android.com/accessibility
- **OneEuro Filter 논문**: https://cristal.univ-lille.fr/~casiez/1euro/
- **Android 성능 최적화**: https://developer.android.com/topic/performance

---

**이 문서는 EyeID SDK 기반 시선 추적 시스템의 완전한 기술 구현 가이드입니다.**  
**비주얼캠프 개발팀의 추가 개발 및 확장에 활용하시기 바랍니다.**

---

*Last updated: 2024년 기준*  
*Contact: [개발자 연락처]*
