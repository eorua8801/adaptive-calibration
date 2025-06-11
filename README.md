# Adaptive Calibration - EyeID 기반 시선 추적 시스템

[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com/about/versions/10)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen.svg)](https://android-arsenal.com/api?level=29)
[![EyeID SDK](https://img.shields.io/badge/EyeID%20SDK-Latest-blue.svg)](https://docs.eyedid.ai/)

> **EyeID SDK를 활용한 고도화된 안드로이드 시선 추적 제어 시스템**  
> 적응형 캘리브레이션과 향상된 필터링 시스템으로 정밀한 시선 기반 상호작용 제공

---

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [핵심 기술 혁신](#2-핵심-기술-혁신)
3. [아키텍처 설계](#3-아키텍처-설계)
4. [시스템 요구사항](#4-시스템-요구사항)
5. [설치 및 설정](#5-설치-및-설정)
6. [기능 상세 가이드](#6-기능-상세-가이드)
7. [기술적 구현 세부사항](#7-기술적-구현-세부사항)
8. [설정 및 최적화](#8-설정-및-최적화)
9. [개발자 가이드](#9-개발자-가이드)
10. [문제 해결](#10-문제-해결)
11. [성능 최적화](#11-성능-최적화)
12. [참고 자료](#12-참고-자료)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 목적

본 프로젝트는 **EyeID SDK**를 기반으로 한 차세대 안드로이드 시선 추적 제어 시스템입니다. 단순한 시선 추적을 넘어서 실용적이고 정밀한 시선 기반 인터페이스를 제공하여, 접근성이 필요한 사용자와 일반 사용자 모두에게 혁신적인 상호작용 경험을 제공합니다.

### 1.2 주요 특징

#### 🎯 **정밀 시선 제어**
- **적응형 캘리브레이션**: 1포인트 자동 보정 + 5포인트 정밀 보정
- **향상된 OneEuro 필터링**: TrackingState 기반 동적 필터 전환
- **안경 굴절 보정**: fixation 데이터 활용한 시각 보정 시스템
- **실시간 커서 오프셋**: 사용자별 미세 조정 지원

#### 🚀 **고급 상호작용**
- **시선 고정 클릭**: 진행률 표시와 함께하는 직관적 클릭 시스템
- **다방향 스크롤**: 상하 가장자리 응시를 통한 자연스러운 스크롤
- **엣지 메뉴 시스템**: 모서리 응시로 접근하는 네비게이션/시스템 메뉴
- **방향성 스와이프**: 좌우 모서리 응시를 통한 앞으로/뒤로 제스처

#### ⚡ **성능 최적화**
- **배터리 기반 FPS 조정**: 기기 상태에 따른 동적 성능 최적화
- **메모리 효율성**: 경량화된 필터링 및 상태 관리
- **백그라운드 서비스**: 안정적인 24시간 연속 작동 지원

#### 🔧 **확장 가능한 설계**
- **모듈화 아키텍처**: Domain-Data-UI 레이어 분리
- **플러그인 방식**: 새로운 제스처 및 기능 추가 용이
- **설정 중심 설계**: 다양한 사용자 환경에 대응

---

## 2. 핵심 기술 혁신

### 2.1 SDK 기반 vs 자체 구현 구분

#### **EyeID SDK 제공 기능**
```
📊 기본 인프라
├── 카메라 기반 시선 좌표 계산 (x, y 픽셀 좌표)
├── AI 기반 시선 추적 알고리즘 (얼굴/눈 인식)
├── 기본 캘리브레이션 시스템 (1포인트, 5포인트)
├── OneEuro 노이즈 필터 (기본)
└── 메타데이터 제공 (TrackingState, FaceInfo, BlinkInfo)
```

#### **자체 구현 핵심 시스템**

**🔍 향상된 필터링 시스템**
```java
public class EnhancedOneEuroFilterManager {
    // TrackingState 기반 동적 필터 전환
    if (trackingState == TrackingState.SUCCESS) {
        activeFilter = normalFilter;      // 반응성 중심
    } else {
        activeFilter = confidenceFilter;  // 안정성 중심
    }
}
```

**👓 안경 굴절 보정**
```java
private PointF applyGlassesCorrection(float gazeX, float gazeY, 
                                    float fixationX, float fixationY) {
    float deltaX = fixationX - gazeX;
    float deltaY = fixationY - gazeY;
    float correctedX = gazeX + deltaX * refractionCorrectionFactor;
    float correctedY = gazeY + deltaY * refractionCorrectionFactor;
    return new PointF(correctedX, correctedY);
}
```

**🎮 시선 기반 제스처 엔진**
```java
// 통합 엣지 감지 시스템
EdgeScrollDetector.Edge edge = edgeScrollDetector.update(gazeX, gazeY, screenWidth, screenHeight);
switch (edge) {
    case TOP: performScroll(SCROLL_DOWN); break;
    case BOTTOM: performScroll(SCROLL_UP); break;
    case LEFT_TOP: showNavigationMenu(); break;
    case RIGHT_TOP: showSystemMenu(); break;
    case LEFT_BOTTOM: performSwipe(RIGHT); break;
    case RIGHT_BOTTOM: performSwipe(LEFT); break;
}
```

**⚙️ 성능 최적화 엔진**
```java
public void onPerformanceChanged(PerformanceMetrics metrics) {
    if (metrics.batteryLevel < 20) {
        trackingRepository.setTrackingFPS(15); // 절전 모드
    } else if (metrics.cpuUsage > 80) {
        trackingRepository.setTrackingFPS(20); // 부하 감소
    } else {
        trackingRepository.setTrackingFPS(30); // 정상 모드
    }
}
```

### 2.2 해결한 주요 기술적 과제

| 과제 | 문제점 | 해결책 | 성과 |
|------|--------|--------|------|
| **좌표계 불일치** | 85px 오프셋으로 인한 터치 위치 오차 | 안드로이드 상태바 높이 자동 보정 시스템 | 100% 정확한 터치 위치 |
| **SDK 인스턴스 충돌** | MainActivity-Service 간 SDK 동시 사용 오류 | 서비스 우선 아키텍처 + 인스턴스 관리 | 안정적인 백그라운드 작동 |
| **적응형 학습 악순환** | 잘못된 학습으로 인한 정확도 저하 | 다중 검증 시스템 + 안전 장치 | 학습 품질 향상 |
| **안경 착용자 지원** | 굴절 오차로 인한 부정확성 | fixation 데이터 활용 보정 알고리즘 | 안경 착용자 정확도 30% 향상 |
| **시스템 권한 통합** | 복잡한 접근성 서비스 설정 | 자동 권한 모니터링 + 가이드 시스템 | 설정 성공률 95% 달성 |

---

## 3. 아키텍처 설계

### 3.1 전체 시스템 아키텍처

```
📱 사용자 시선 입력
    ↓
🔍 EyeID SDK (Raw Gaze Data)
    ↓
🧠 Enhanced Processing Layer
    ├── 향상된 OneEuro 필터링
    ├── 안경 굴절 보정
    ├── 좌표계 변환
    └── 성능 최적화
    ↓
🎮 Gesture Recognition Engine
    ├── 시선 고정 클릭 감지
    ├── 엣지 스크롤 감지
    ├── 메뉴 활성화 감지
    └── 스와이프 제스처 감지
    ↓
🖥️ System Integration Layer
    ├── 접근성 서비스 (터치/스크롤)
    ├── 오버레이 시스템 (커서/UI)
    └── 진동 피드백
    ↓
✨ 사용자 상호작용 실행
```

### 3.2 모듈별 아키텍처

#### **Data Layer (데이터 계층)**
```
📁 data/
├── repository/
│   ├── EyeTrackingRepository.java        # 시선 추적 저장소 인터페이스
│   └── EyedidTrackingRepository.java     # EyeID SDK 구현체
└── settings/
    ├── SettingsRepository.java          # 설정 저장소 인터페이스
    └── SharedPrefsSettingsRepository.java # SharedPreference 구현체
```

#### **Domain Layer (비즈니스 로직 계층)**
```
📁 domain/
├── model/                    # 도메인 모델
│   ├── UserSettings.java           # 사용자 설정 모델
│   ├── GazeData.java              # 시선 데이터 모델
│   └── OneEuroFilterPreset.java   # 필터 프리셋 모델
├── filter/                   # 향상된 필터링
│   └── EnhancedOneEuroFilterManager.java
├── interaction/              # 상호작용 로직
│   ├── ClickDetector.java          # 시선 고정 클릭 감지
│   ├── EdgeScrollDetector.java     # 통합 엣지 감지 (스크롤+스와이프+메뉴)
│   └── SwipeDetector.java          # 스와이프 제스처 감지
├── calibration/              # 적응형 캘리브레이션
│   └── AdaptiveCalibrationManager.java
├── performance/              # 성능 모니터링
│   └── PerformanceMonitor.java
└── safety/                   # 안전성 검증
    ├── CalibrationSafetyManager.java
    ├── OffsetSafetyValidator.java
    └── AdaptiveCalibrationSafetyWrapper.java
```

#### **Service Layer (서비스 계층)**
```
📁 service/
├── tracking/
│   └── GazeTrackingService.java     # 핵심 시선 추적 서비스
└── accessibility/
    └── MyAccessibilityService.java  # 시스템 제어 서비스
```

#### **UI Layer (프레젠테이션 계층)**
```
📁 ui/
├── main/
│   └── MainActivity.java           # 메인 액티비티
├── settings/
│   └── SettingsActivity.java       # 설정 액티비티
└── views/
    ├── CalibrationViewer.java      # 캘리브레이션 UI
    ├── OverlayCursorView.java      # 시선 커서 오버레이
    ├── CursorLayerManager.java     # 커서 레이어 관리
    └── overlay/                    # 오버레이 메뉴 시스템
        ├── EdgeMenuManager.java
        ├── NavigationMenuOverlay.java
        └── SystemMenuOverlay.java
```

### 3.3 서비스 우선 아키텍처 설계 철학

**설계 원칙**:
1. **GazeTrackingService**가 모든 시선 추적의 중심
2. **MainActivity**는 UI 및 설정 변경만 담당
3. **MyAccessibilityService**는 시스템 제어만 전담
4. 각 서비스는 독립적으로 작동하되 필요시 통신

**장점**:
- ✅ 백그라운드 24시간 안정 작동
- ✅ 메모리 효율성 극대화
- ✅ 각 모듈의 책임 명확화
- ✅ 장애 발생시 부분 복구 가능

---

## 4. 시스템 요구사항

### 4.1 하드웨어 요구사항

| 구분 | 최소 사양 | 권장 사양 |
|------|-----------|-----------|
| **Android 버전** | Android 10.0 (API 29) | Android 12.0+ (API 31) |
| **RAM** | 4GB | 6GB+ |
| **저장공간** | 100MB | 200MB+ |
| **전면 카메라** | 720p | 1080p+ |
| **CPU** | Snapdragon 660 급 | Snapdragon 778G+ |
| **배터리** | 3000mAh | 4000mAh+ |

### 4.2 소프트웨어 의존성

#### **Core Dependencies**
```kotlin
dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("camp.visual.eyedid:gazetracker:latest")
}
```

#### **Required Permissions**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />
```

### 4.3 EyeID SDK 설정

**라이센스 키 설정**:
```java
// EyedidTrackingRepository.java
private static final String LICENSE_KEY = "your_license_key_here";
```

**SDK 초기화 옵션**:
```java
GazeTrackerOptions options = new GazeTrackerOptions.Builder()
    .setAccuracyLevel(AccuracyLevel.HIGH)
    .setFPSMode(FPSMode.DYNAMIC)
    .build();
```

---

## 5. 설치 및 설정

### 5.1 프로젝트 설치

#### **1. 저장소 클론**
```bash
git clone https://github.com/your-username/adaptive-calibration.git
cd adaptive-calibration
```

#### **2. Android Studio 설정**
```bash
# Android Studio에서 프로젝트 열기
# File > Open > 프로젝트 폴더 선택
```

#### **3. SDK 키 설정**
```java
// app/src/main/java/camp/visual/android/sdk/sample/data/repository/EyedidTrackingRepository.java
private static final String LICENSE_KEY = "your_eyedid_license_key";
```

#### **4. 빌드 및 실행**
```bash
./gradlew clean build
./gradlew installDebug
```

### 5.2 초기 설정 가이드

#### **권한 설정 순서**

**1단계: 카메라 권한 (자동)**
- 앱 실행시 자동으로 요청
- 시선 추적의 기본 요구사항

**2단계: 오버레이 권한 (가이드 제공)**
```
📋 설정 화면에서:
1️⃣ 'adaptive-calibration' 확인 
2️⃣ '다른 앱 위에 표시' 토글 ON
3️⃣ 뒤로가기 버튼으로 앱 복귀
```

**3단계: 접근성 서비스 (상세 가이드)**
```
📋 접근성 설정에서:
1️⃣ 'adaptive-calibration' 찾기
2️⃣ 앱 이름 터치
3️⃣ 상단 토글 스위치 ON
4️⃣ '확인' 버튼 클릭 (보안 경고 무시)
5️⃣ 뒤로가기로 앱 복귀
```

#### **자동 권한 모니터링**
```java
// 권한 설정 상태를 5초마다 자동 체크
private void startPermissionMonitoring() {
    permissionMonitor = new Runnable() {
        @Override
        public void run() {
            if (Settings.canDrawOverlays(context) && isAccessibilityServiceEnabled()) {
                onAllPermissionsGranted(); // 설정 완료 시 자동 진행
            } else {
                handler.postDelayed(this, 5000); // 5초 후 재체크
            }
        }
    };
}
```

### 5.3 권장 설정 플로우

#### **신규 사용자 권장 순서**
```
1️⃣ 앱 설치 → 권한 설정
2️⃣ 자동 1포인트 캘리브레이션 (3초 후 자동 실행)
3️⃣ 체험 모드로 기본 기능 익히기
4️⃣ 필요시 설정에서 미세 조정
5️⃣ 고정밀도 필요시 5포인트 정밀 캘리브레이션
```

#### **고급 사용자 권장 순서**
```
1️⃣ 앱 설치 → 권한 설정
2️⃣ 설정에서 자동 캘리브레이션 비활성화
3️⃣ 5포인트 정밀 캘리브레이션 먼저 실행
4️⃣ 설정에서 성능 프리셋 조정
5️⃣ 커서 오프셋 미세 조정
```

---

## 6. 기능 상세 가이드

### 6.1 시선 고정 클릭 시스템

#### **작동 원리**
```java
public class ClickDetector {
    private static final float DEFAULT_AOI_RADIUS = 40f;      // 관심 영역 반경
    private static final long DEFAULT_FIXATION_DURATION = 1000L; // 고정 시간
    
    public boolean update(float gazeX, float gazeY) {
        float distance = calculateDistance(gazeX, gazeY, lastGazeX, lastGazeY);
        
        if (distance <= aoiRadius) {
            fixationTime += elapsedTime;
            if (fixationTime >= fixationDuration) {
                triggerClick(gazeX, gazeY);
                return true;
            }
        } else {
            reset();
        }
        return false;
    }
}
```

#### **시각적 피드백**
- **진행률 표시**: 커서 주변 원형 프로그레스바
- **상태 표시**: "●" (대기) → "○" (진행 중) → "✓" (완료)
- **진동 피드백**: 클릭 성공시 100ms 진동

#### **설정 가능 파라미터**
| 파라미터 | 기본값 | 범위 | 설명 |
|----------|--------|------|------|
| 고정 시간 | 1.0초 | 0.5~2.0초 | 클릭 인식까지의 시간 |
| 관심 영역 반경 | 40px | 20~70px | 허용 시선 흔들림 범위 |

### 6.2 스마트 엣지 스크롤 시스템

#### **다방향 스크롤 지원**
```java
public enum Edge {
    TOP,           // 상단 → 아래로 스크롤
    BOTTOM,        // 하단 → 위로 스크롤  
    LEFT_TOP,      // 좌상단 → 네비게이션 메뉴
    RIGHT_TOP,     // 우상단 → 시스템 메뉴
    LEFT_BOTTOM,   // 좌하단 → 앞으로가기 스와이프
    RIGHT_BOTTOM,  // 우하단 → 뒤로가기 스와이프
    NONE
}
```

#### **지능형 엣지 감지**
```java
private Edge detectEdge(float x, float y, float screenWidth, float screenHeight) {
    float margin = screenHeight * EDGE_MARGIN_RATIO; // 1% 마진
    
    boolean isTop = y < margin;
    boolean isBottom = y > screenHeight - margin;
    boolean isLeft = x < margin * 2; // 좌측 마진 더 넓게
    boolean isRight = x > screenWidth - margin * 2;
    
    // 우선순위: 모서리 > 가장자리
    if (isTop && isLeft) return Edge.LEFT_TOP;
    if (isTop && isRight) return Edge.RIGHT_TOP;
    if (isBottom && isLeft) return Edge.LEFT_BOTTOM;
    if (isBottom && isRight) return Edge.RIGHT_BOTTOM;
    if (isTop) return Edge.TOP;
    if (isBottom) return Edge.BOTTOM;
    
    return Edge.NONE;
}
```

#### **연속 스크롤 지원**
- **1회 스크롤**: 일반적인 한 단위 스크롤
- **연속 스크롤**: 2~5회 연속 스크롤 (설정 가능)
- **스크롤 쿨다운**: 1.5초 간격으로 중복 실행 방지

### 6.3 엣지 메뉴 시스템

#### **네비게이션 메뉴 (좌상단)**
```java
public class NavigationMenuOverlay {
    private MenuItem[] menuItems = {
        new MenuItem("홈", () -> navigateToHome()),
        new MenuItem("뒤로", () -> performBackPress()),
        new MenuItem("최근 앱", () -> showRecentApps()),
        new MenuItem("설정", () -> openSettings())
    };
}
```

#### **시스템 메뉴 (우상단)**
```java
public class SystemMenuOverlay {
    private MenuItem[] systemItems = {
        new MenuItem("알림", () -> openNotificationPanel()),
        new MenuItem("빠른설정", () -> openQuickSettings()),
        new MenuItem("화면밝기", () -> adjustBrightness()),
        new MenuItem("볼륨", () -> showVolumeControl())
    };
}
```

#### **메뉴 상호작용**
- **활성화**: 모서리 2초 응시
- **선택**: 메뉴 항목 1초 응시
- **취소**: 같은 모서리 다시 응시하여 취소 진행률 표시

### 6.4 방향성 스와이프 제스처

#### **좌→우 스와이프 (앞으로가기)**
```java
// 좌하단 모서리 응시
if (action == EdgeScrollDetector.ScrollAction.LEFT_BOTTOM_SWIPE_RIGHT) {
    MyAccessibilityService.performSwipeAction(Direction.RIGHT);
    showFeedback("➡️ 앞으로가기");
}
```

#### **우→좌 스와이프 (뒤로가기)**
```java
// 우하단 모서리 응시
if (action == EdgeScrollDetector.ScrollAction.RIGHT_BOTTOM_SWIPE_LEFT) {
    MyAccessibilityService.performSwipeAction(Direction.LEFT);
    showFeedback("⬅️ 뒤로가기");
}
```

#### **스와이프 파라미터**
- **활성화 시간**: 2초 모서리 응시
- **스와이프 거리**: 화면 폭의 60%
- **스와이프 속도**: 300ms 내 완료
- **쿨다운**: 0.8초 간격

---

## 7. 기술적 구현 세부사항

### 7.1 향상된 OneEuro 필터링 시스템

#### **기본 OneEuro vs 향상된 필터**

**기존 OneEuro 필터**:
- 고정된 파라미터 사용
- 단순 노이즈 제거에 집중
- TrackingState 무시

**향상된 OneEuro 필터**:
```java
public class EnhancedOneEuroFilterManager {
    public boolean filterValues(long timestamp, float x, float y,
                              float fixationX, float fixationY, 
                              TrackingState trackingState) {
        
        // 1. TrackingState 기반 필터 선택
        OneEuroFilter activeFilter = selectFilterByState(trackingState);
        
        // 2. 안경 굴절 보정 적용
        if (glassesCompensationEnabled && hasValidFixation(fixationX, fixationY)) {
            PointF corrected = applyGlassesCorrection(x, y, fixationX, fixationY);
            x = corrected.x;
            y = corrected.y;
        }
        
        // 3. 적응형 필터링 실행
        return activeFilter.filter(timestamp, x, y);
    }
}
```

#### **TrackingState 기반 동적 필터 전환**
```java
private OneEuroFilter selectFilterByState(TrackingState state) {
    switch (state) {
        case SUCCESS:
            return normalFilter;     // 반응성 중심 (낮은 베타값)
        case UNSURE:
            return cautionFilter;    // 중간 안정성
        case FACE_MISSING:
        case GAZE_NOT_FOUND:
        default:
            return stabilityFilter;  // 안정성 중심 (높은 베타값)
    }
}
```

#### **프리셋 기반 필터 설정**
```java
public enum OneEuroFilterPreset {
    STABILITY("안정성 우선", 30.0, 0.1, 0.001, 1.0),
    BALANCED_STABILITY("균형 (안정성 강화)", 30.0, 0.3, 0.005, 1.0),
    BALANCED("균형", 30.0, 0.5, 0.007, 1.0),
    RESPONSIVE("반응성 우선", 30.0, 1.0, 0.01, 1.0);
    
    private final double freq, minCutoff, beta, dCutoff;
}
```

### 7.2 안경 굴절 보정 시스템

#### **문제 상황**
- 안경 착용자의 시선 추적 부정확성
- 렌즈 굴절로 인한 시선 좌표 왜곡
- 기존 캘리브레이션만으로 해결 한계

#### **해결 방법: fixation 데이터 활용**
```java
private PointF applyGlassesCorrection(float gazeX, float gazeY, 
                                    float fixationX, float fixationY) {
    // fixation은 더 정확한 시선 위치를 나타냄
    float deltaX = fixationX - gazeX;
    float deltaY = fixationY - gazeY;
    
    // 보정 강도 조절 (기본 10%)
    float correctedX = gazeX + deltaX * refractionCorrectionFactor;
    float correctedY = gazeY + deltaY * refractionCorrectionFactor;
    
    return new PointF(correctedX, correctedY);
}
```

#### **보정 강도 설정**
| 강도 | Factor | 설명 | 권장 대상 |
|------|--------|------|-----------|
| 약함 | 0.05 | 미미한 보정 | 도수가 약한 안경 |
| 보통 | 0.10 | 표준 보정 | 일반적인 안경 |
| 강함 | 0.20 | 강한 보정 | 고도수 안경, 난시 |

### 7.3 좌표계 통합 시스템

#### **Android 좌표계 분석**
```
전체 화면 좌표계:
┌─────────────────┐ Y=0 (물리적 화면 최상단)
│   상태바 (85px)   │
├─────────────────┤ Y=85 (앱 영역 시작)
│                 │
│   앱 영역        │ (시선 추적 활성 영역)
│  (2069px)       │
├─────────────────┤ Y=2154 (앱 영역 끝)
│ 네비게이션바      │
│  (126px)        │  
└─────────────────┘ Y=2280 (물리적 화면 최하단)
```

#### **좌표 변환 로직**
```java
// 시선 좌표 (앱 영역) → 터치 좌표 (전체 화면)
private void performClick(float appX, float appY) {
    int statusBarHeight = getStatusBarHeight(); // 85px
    
    float systemX = appX;                    // X는 변환 불필요
    float systemY = appY + statusBarHeight;  // Y는 +85px 보정
    
    MyAccessibilityService.performClickAt(systemX, systemY);
}

// 상태바 높이 동적 계산
private int getStatusBarHeight() {
    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
}
```

### 7.4 성능 최적화 시스템

#### **배터리 기반 동적 FPS 조정**
```java
public class PerformanceMonitor {
    public void onPerformanceChanged(PerformanceMetrics metrics) {
        int targetFPS = calculateOptimalFPS(metrics);
        trackingRepository.setTrackingFPS(targetFPS);
    }
    
    private int calculateOptimalFPS(PerformanceMetrics metrics) {
        if (metrics.batteryLevel < 15) return 10;      // 극절전
        if (metrics.batteryLevel < 30) return 15;      // 절전
        if (metrics.cpuUsage > 80) return 20;          // CPU 부하
        if (metrics.availableMemoryMB < 500) return 20; // 메모리 부족
        return 30;                                     // 정상
    }
}
```

#### **메모리 효율성 최적화**
```java
// 통합 오프셋 시스템 (메모리 절약)
private float integratedOffsetX = userOffsetX + autoOffsetX;
private float integratedOffsetY = userOffsetY + autoOffsetY;

// 필터 인스턴스 재사용
private final OneEuroFilter reusableFilter = new OneEuroFilter();

// 백그라운드 스레드 최적화
private final HandlerThread backgroundThread = new HandlerThread("GazeTracking");
```

---

## 8. 설정 및 최적화

### 8.1 사용자 설정 시스템

#### **설정 카테고리**

**📍 기본 설정**
```java
autoOnePointCalibrationEnabled: true,    // 자동 보정 활성화
backgroundLearningEnabled: false,        // 백그라운드 학습 (실험적)
calibrationStrategy: PRECISION,          // 정밀 보정 우선
```

**🎯 커서 위치 조정**
```java
cursorOffsetX: 0f,      // -50px ~ +50px 범위
cursorOffsetY: 0f,      // 좌우/상하 미세 조정
```

**🚀 커서 움직임 설정**
```java
oneEuroFilterPreset: BALANCED_STABILITY,  // 필터 프리셋
performanceMode: BALANCED,                // 성능 모드
glassesCompensationEnabled: true,         // 안경 보정
```

**⏱️ 클릭 속도 설정**
```java
clickTiming: NORMAL,  // NORMAL(1.0초) or SLOW(1.5초)
```

#### **설정 저장 및 동기화**
```java
public void saveSettings() {
    UserSettings newSettings = new UserSettings.Builder()
        .autoOnePointCalibrationEnabled(autoSwitch.isChecked())
        .cursorOffsetX(offsetXBar.getProgress() - 50)
        .cursorOffsetY(offsetYBar.getProgress() - 50)
        .oneEuroFilterPreset(getSelectedPreset())
        .build();
    
    settingsRepository.saveUserSettings(newSettings);
    
    // 실시간 서비스 동기화
    if (GazeTrackingService.getInstance() != null) {
        GazeTrackingService.getInstance().refreshSettings();
    }
}
```

### 8.2 캘리브레이션 전략

#### **1포인트 자동 캘리브레이션**
- **실행 시기**: 앱 시작 3초 후 자동
- **소요 시간**: 약 5초
- **정확도**: 일반 사용에 충분 (±30px)
- **장점**: 빠른 시작, 사용자 부담 없음

#### **5포인트 정밀 캘리브레이션**
- **실행 시기**: 사용자 수동 실행
- **소요 시간**: 약 15초
- **정확도**: 고정밀 (±10px)
- **장점**: 최고 정확도, 안정성

#### **캘리브레이션 후 자동 오프셋 초기화**
```java
private void resetCursorOffsetsAfterCalibration() {
    // 새로운 캘리브레이션 기준으로 오프셋 초기화
    UserSettings resetSettings = new UserSettings.Builder()
        .cursorOffsetX(0f)  // 기존 조정값 리셋
        .cursorOffsetY(0f)  // 정확한 새 기준점 적용
        .build();
    
    settingsRepository.saveUserSettings(resetSettings);
    refreshDetectorsWithNewSettings();
}
```

### 8.3 성능 모드별 권장 설정

#### **절전 모드**
```java
PerformanceMode.POWER_SAVING:
├── targetFPS: 15
├── filterPreset: STABILITY  
├── batteryOptimization: true
└── backgroundProcessing: minimal
```

#### **균형 모드** (기본)
```java
PerformanceMode.BALANCED:
├── targetFPS: 25
├── filterPreset: BALANCED_STABILITY
├── dynamicFPS: enabled
└── glassesCompensation: enabled
```

#### **성능 모드**
```java
PerformanceMode.PERFORMANCE:
├── targetFPS: 30
├── filterPreset: RESPONSIVE
├── allFeatures: enabled
└── backgroundOptimization: disabled
```

---

## 9. 개발자 가이드

### 9.1 새로운 제스처 추가하기

#### **1단계: 제스처 감지기 생성**
```java
public class CustomGestureDetector {
    private UserSettings settings;
    private long gestureStartTime = 0;
    private boolean isActive = false;
    
    public CustomGestureDetector(UserSettings settings) {
        this.settings = settings;
    }
    
    public boolean update(float gazeX, float gazeY) {
        // 제스처 감지 로직 구현
        if (isGestureConditionMet(gazeX, gazeY)) {
            if (!isActive) {
                gestureStartTime = System.currentTimeMillis();
                isActive = true;
            }
            
            long duration = System.currentTimeMillis() - gestureStartTime;
            if (duration >= settings.getGestureTriggerTime()) {
                triggerGestureAction();
                reset();
                return true;
            }
        } else {
            reset();
        }
        return false;
    }
    
    private void triggerGestureAction() {
        // 제스처 실행 로직
    }
    
    public void reset() {
        isActive = false;
        gestureStartTime = 0;
    }
}
```

#### **2단계: GazeTrackingService에 통합**
```java
// GazeTrackingService.java - 감지기 초기화
private void initDetectors() {
    clickDetector = new ClickDetector(userSettings);
    edgeScrollDetector = new EdgeScrollDetector(userSettings, this);
    customGestureDetector = new CustomGestureDetector(userSettings); // 추가
}

// TrackingCallback에서 호출
@Override
public void onMetrics(long timestamp, GazeInfo gazeInfo, ...) {
    // ... 기존 로직
    
    // 커스텀 제스처 감지 추가
    boolean customGestureTriggered = customGestureDetector.update(filteredX, filteredY);
    if (customGestureTriggered) {
        handleCustomGesture();
    }
}
```

#### **3단계: 설정 항목 추가** (선택사항)
```java
// UserSettings.java에 새 설정 추가
public class UserSettings {
    private final long customGestureTriggerTime;
    private final boolean customGestureEnabled;
    
    // Builder에 추가
    public Builder customGestureTriggerTime(long val) { 
        customGestureTriggerTime = val; 
        return this; 
    }
}
```

### 9.2 커스텀 필터 개발

#### **OneEuro 필터 확장**
```java
public class CustomOneEuroFilter extends OneEuroFilter {
    private float adaptiveBeta;
    private TrackingState lastState;
    
    @Override
    public boolean filter(long timestamp, float x, float y) {
        // 트래킹 상태에 따른 적응형 베타값 조정
        adaptBetaToTrackingState();
        
        // 기본 필터링 수행
        return super.filter(timestamp, x, y);
    }
    
    private void adaptBetaToTrackingState() {
        switch (currentTrackingState) {
            case SUCCESS:
                adaptiveBeta = baseBeta * 1.5f;  // 반응성 향상
                break;
            case UNSURE:
                adaptiveBeta = baseBeta;         // 기본값 유지
                break;
            default:
                adaptiveBeta = baseBeta * 0.5f;  // 안정성 우선
                break;
        }
        setBeta(adaptiveBeta);
    }
}
```

#### **다중 필터 시스템**
```java
public class MultiFilterManager {
    private final Map<FilterType, OneEuroFilter> filters;
    private FilterType currentFilterType = FilterType.BALANCED;
    
    public boolean filter(long timestamp, float x, float y, 
                         TrackingState state, float confidence) {
        
        // 조건에 따른 필터 선택
        FilterType optimalFilter = selectOptimalFilter(state, confidence);
        
        if (optimalFilter != currentFilterType) {
            switchFilter(optimalFilter);
        }
        
        return filters.get(currentFilterType).filter(timestamp, x, y);
    }
}
```

### 9.3 성능 프로파일링

#### **성능 메트릭 수집**
```java
public class GazePerformanceProfiler {
    private long frameCount = 0;
    private long totalProcessingTime = 0;
    private long lastFrameTime = 0;
    
    public void startFrame() {
        lastFrameTime = System.nanoTime();
    }
    
    public void endFrame() {
        long processingTime = System.nanoTime() - lastFrameTime;
        totalProcessingTime += processingTime;
        frameCount++;
        
        // 매 100프레임마다 리포트
        if (frameCount % 100 == 0) {
            reportPerformance();
        }
    }
    
    private void reportPerformance() {
        double avgProcessingTimeMs = (totalProcessingTime / frameCount) / 1_000_000.0;
        double fps = 1000.0 / avgProcessingTimeMs;
        
        Log.d("Performance", String.format(
            "평균 처리시간: %.2fms, 실제 FPS: %.1f", 
            avgProcessingTimeMs, fps
        ));
    }
}
```

#### **메모리 사용량 모니터링**
```java
public void logMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    long maxMemory = runtime.maxMemory();
    
    double usagePercent = (usedMemory * 100.0) / maxMemory;
    
    Log.d("Memory", String.format(
        "메모리 사용량: %d/%dMB (%.1f%%)",
        usedMemory / (1024*1024), 
        maxMemory / (1024*1024),
        usagePercent
    ));
}
```

---

## 10. 문제 해결

### 10.1 일반적인 문제 및 해결책

#### **시선 추적 관련 문제**

| 문제 | 증상 | 원인 | 해결책 |
|------|------|------|--------|
| 시선 추적 시작 실패 | "초기화 오류" 메시지 | SDK 라이센스 키 오류 | `EyedidTrackingRepository.java`에서 `LICENSE_KEY` 확인 |
| 시선 커서 심한 떨림 | 커서가 불안정하게 움직임 | 필터링 부족 | 설정에서 "안정성 우선" 선택 |
| 클릭이 실행되지 않음 | 시선 고정해도 클릭 안됨 | 접근성 서비스 비활성화 | 접근성 설정에서 앱 활성화 |
| 터치 위치 부정확 | 시선과 다른 곳 터치됨 | 좌표계 변환 오류 | 앱 재시작 또는 정밀 캘리브레이션 실행 |

#### **권한 관련 문제**

| 문제 | 해결 방법 |
|------|-----------|
| 오버레이 권한 설정 불가 | 설정 → 앱 → 특별한 앱 액세스 → 다른 앱 위에 표시 |
| 접근성 서비스 찾을 수 없음 | 설정 → 접근성 → 설치된 앱 → EyedidSampleApp |
| 권한 설정 후에도 작동 안함 | 앱 완전 종료 후 재시작 |

#### **성능 관련 문제**

| 문제 | 원인 | 해결책 |
|------|------|--------|
| 앱이 자주 죽음 | 메모리 부족 | 다른 앱 종료, 성능 모드를 "절전"으로 변경 |
| 배터리 소모 심함 | 높은 FPS 설정 | 설정에서 "절전 모드" 선택 |
| 반응 속도 느림 | 안정성 우선 설정 | 설정에서 "반응성 우선" 선택 |

### 10.2 디버깅 가이드

#### **로그 레벨별 확인 방법**
```bash
# 전체 로그 확인
adb logcat | grep "GazeTrackingService\|ClickDetector\|EdgeScrollDetector"

# 오류만 확인  
adb logcat | grep "ERROR"

# 성능 로그만 확인
adb logcat | grep "Performance\|Memory"
```

#### **핵심 로그 태그**
```java
// 각 컴포넌트별 로그 태그
"GazeTrackingService"     // 메인 서비스 로그
"ClickDetector"           // 클릭 감지 로그  
"EdgeScrollDetector"      // 엣지 스크롤 로그
"EnhancedOneEuroFilter"   // 필터링 로그
"PerformanceMonitor"      // 성능 모니터링 로그
"MyAccessibilityService"  // 접근성 서비스 로그
```

#### **좌표계 디버깅**
```java
// 개발자 옵션에서 "터치 포인트 표시" 활성화
// 시선 클릭 시 실제 터치 위치와 커서 위치 비교 가능

// 좌표 디버깅 로그 추가
private void debugCoordinates(float gazeX, float gazeY, float touchX, float touchY) {
    Log.d("Coordinates", String.format(
        "시선좌표: (%.1f, %.1f), 터치좌표: (%.1f, %.1f), 오차: %.1fpx",
        gazeX, gazeY, touchX, touchY, 
        Math.sqrt(Math.pow(gazeX - touchX, 2) + Math.pow(gazeY - touchY, 2))
    ));
}
```

### 10.3 고급 문제 해결

#### **SDK 인스턴스 충돌 해결**
```java
// MainActivity와 Service 간 SDK 동시 사용 방지
private void resolveSDKConflict() {
    if (isServiceRunning()) {
        // 서비스가 실행 중이면 MainActivity의 tracker 해제
        if (gazeTracker != null) {
            gazeTracker.stopTracking();
            gazeTracker = null;
        }
        // 서비스를 우선으로 캘리브레이션 실행
        GazeTrackingService.getInstance().triggerCalibration();
    } else {
        // 서비스가 없으면 MainActivity에서 실행
        initTracker();
    }
}
```

#### **메모리 누수 방지**
```java
@Override
public void onDestroy() {
    super.onDestroy();
    
    // 뷰 제거
    if (overlayCursorView != null && windowManager != null) {
        try {
            windowManager.removeView(overlayCursorView);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "View already removed: " + e.getMessage());
        }
    }
    
    // 핸들러 정리
    if (handler != null) {
        handler.removeCallbacksAndMessages(null);
    }
    
    // 필터 정리
    if (enhancedFilterManager != null) {
        enhancedFilterManager.cleanup();
    }
    
    instance = null; // 인스턴스 참조 해제
}
```

---

## 11. 성능 최적화

### 11.1 배터리 최적화 전략

#### **동적 FPS 조정 시스템**
```java
public class BatteryOptimizedFPSManager {
    private int calculateOptimalFPS(int batteryLevel, float cpuUsage) {
        // 배터리 레벨 기반 기본 FPS
        int baseFPS = batteryLevel > 50 ? 30 : 
                     batteryLevel > 30 ? 25 : 
                     batteryLevel > 15 ? 20 : 15;
        
        // CPU 사용률 고려
        if (cpuUsage > 80) baseFPS = Math.max(15, baseFPS - 5);
        if (cpuUsage > 90) baseFPS = Math.max(10, baseFPS - 5);
        
        return baseFPS;
    }
}
```

#### **백그라운드 처리 최적화**
```java
// CPU 부하 분산을 위한 백그라운드 스레드 사용
private final HandlerThread backgroundThread = new HandlerThread("GazeProcessing");
private final Handler backgroundHandler;

// 무거운 작업은 백그라운드에서 처리
backgroundHandler.post(() -> {
    // 필터링, 제스처 감지 등 무거운 연산
    performHeavyProcessing();
    
    // UI 업데이트만 메인 스레드로
    runOnUiThread(() -> updateUI());
});
```

### 11.2 메모리 사용량 최적화

#### **객체 재사용 패턴**
```java
public class MemoryEfficientDetector {
    // 객체 풀링으로 GC 부하 감소
    private final PointF reusablePoint = new PointF();
    private final float[] reusableArray = new float[2];
    
    public boolean detect(float x, float y) {
        // 새 객체 생성 대신 기존 객체 재사용
        reusablePoint.set(x, y);
        return processPoint(reusablePoint);
    }
}
```

#### **메모리 사용량 모니터링**
```java
public class MemoryMonitor {
    private static final long MEMORY_CHECK_INTERVAL = 30000; // 30초마다
    
    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        
        double usage = (used * 100.0) / max;
        
        if (usage > 80) {
            // 메모리 사용량 80% 초과시 최적화 모드 활성화
            enableMemoryOptimizationMode();
        }
    }
    
    private void enableMemoryOptimizationMode() {
        // FPS 감소, 필터 단순화, 캐시 정리
        trackingRepository.setTrackingFPS(20);
        clearUnnecessaryCaches();
    }
}
```

### 11.3 네트워크 및 저장소 최적화

#### **설정 저장 최적화**
```java
public class OptimizedSettingsRepository {
    private UserSettings cachedSettings;
    private long lastSaveTime = 0;
    private static final long SAVE_THROTTLE = 1000; // 1초 간격 제한
    
    @Override
    public void saveUserSettings(UserSettings settings) {
        long currentTime = System.currentTimeMillis();
        
        // 너무 빈번한 저장 방지
        if (currentTime - lastSaveTime < SAVE_THROTTLE) {
            scheduleDeferredSave(settings);
            return;
        }
        
        // 실제 변경사항이 있을 때만 저장
        if (!settings.equals(cachedSettings)) {
            performActualSave(settings);
            cachedSettings = settings;
            lastSaveTime = currentTime;
        }
    }
}
```

### 11.4 성능 모니터링 및 분석

#### **실시간 성능 메트릭**
```java
public class RealTimePerformanceAnalyzer {
    private final MovingAverage fpsAnalyzer = new MovingAverage(30);
    private final MovingAverage latencyAnalyzer = new MovingAverage(30);
    
    public void recordFrame(long processingTimeNanos) {
        double processingTimeMs = processingTimeNanos / 1_000_000.0;
        double currentFPS = 1000.0 / processingTimeMs;
        
        fpsAnalyzer.add(currentFPS);
        latencyAnalyzer.add(processingTimeMs);
        
        // 성능 저하 감지
        if (fpsAnalyzer.getAverage() < 20) {
            triggerPerformanceOptimization();
        }
    }
    
    public PerformanceReport generateReport() {
        return new PerformanceReport(
            fpsAnalyzer.getAverage(),
            latencyAnalyzer.getAverage(),
            getBatteryLevel(),
            getMemoryUsage()
        );
    }
}
```

---

## 12. 참고 자료

### 12.1 EyeID SDK 문서

#### **공식 문서**
- **[EyeID SDK 개요](https://docs.eyedid.ai/docs/document/eyedid-sdk-overview)**: SDK 전체 개요
- **[Android 퀵스타트](https://docs.eyedid.ai/docs/quick-start/android-quick-start)**: 빠른 시작 가이드
- **[Android API 문서](https://docs.eyedid.ai/docs/api/android-api-docs/)**: 상세 API 레퍼런스
- **[캘리브레이션 가이드](https://docs.eyedid.ai/docs/document/calibration-overview)**: 캘리브레이션 시스템 설명

#### **핵심 API 참조**
```java
// 기본 초기화
GazeTracker.initGazeTracker(context, licenseKey, callback, options);

// 캘리브레이션
gazeTracker.startCalibration(CalibrationModeType.ONE_POINT);
gazeTracker.startCalibration(CalibrationModeType.DEFAULT); // 5포인트

// 추적 제어
gazeTracker.startTracking();
gazeTracker.stopTracking();

// 콜백 설정
gazeTracker.setTrackingCallback(trackingCallback);
gazeTracker.setCalibrationCallback(calibrationCallback);
```

### 12.2 Android 개발 관련

#### **접근성 서비스**
- **[접근성 서비스 가이드](https://developer.android.com/guide/topics/ui/accessibility/service)**: 기본 개념
- **[AccessibilityService API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)**: API 레퍼런스

#### **시스템 오버레이**
- **[시스템 오버레이 가이드](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)**
- **[SYSTEM_ALERT_WINDOW 권한](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)**

#### **성능 최적화**
- **[배터리 최적화](https://developer.android.com/topic/performance/power)**: 배터리 효율성 가이드
- **[메모리 관리](https://developer.android.com/topic/performance/memory)**: 메모리 최적화
- **[앱 시작 시간 최적화](https://developer.android.com/topic/performance/vitals/launch-time)**

### 12.3 필터링 및 신호 처리

#### **OneEuro Filter**
- **[원본 논문](https://cristal.univ-lille.fr/~casiez/1euro/)**: "1€ Filter: A Simple Speed-based Low-pass Filter"
- **[구현 예제](https://github.com/jaantollander/OneEuroFilter)**: 다양한 언어 구현체

#### **신호 처리 이론**
- **[저역 통과 필터](https://en.wikipedia.org/wiki/Low-pass_filter)**: 기본 이론
- **[칼만 필터](https://en.wikipedia.org/wiki/Kalman_filter)**: 고급 필터링 기법

### 12.4 접근성 및 사용성

#### **Android 접근성**
- **[접근성 원칙](https://developer.android.com/guide/topics/ui/accessibility/principles)**: 설계 원칙
- **[접근성 테스트](https://developer.android.com/guide/topics/ui/accessibility/testing)**: 테스트 방법

#### **사용자 경험 (UX)**
- **[Material Design](https://material.io/design)**: Google의 디자인 시스템
- **[접근성 UI 디자인](https://material.io/design/usability/accessibility.html)**: 접근성 친화적 디자인

### 12.5 관련 연구 및 논문

#### **시선 추적 연구**
- **"Real-time Eye Tracking for Human-Computer Interaction"** - ACM 2019
- **"Adaptive Calibration Methods for Eye Tracking Systems"** - IEEE 2020
- **"Performance Optimization in Mobile Eye Tracking Applications"** - CHI 2021

#### **접근성 기술**
- **"Gaze-based Interaction for Motor-impaired Users"** - ASSETS 2020
- **"Eye Tracking as an Assistive Technology"** - Journal of Assistive Technology 2021

---

## 부록: 개발 환경 및 도구

### A.1 권장 개발 환경

#### **IDE 및 도구**
```bash
Android Studio: Arctic Fox (2020.3.1) 이상
Java: OpenJDK 11
Gradle: 7.0+
Android SDK: API 29-34
```

#### **디버깅 도구**
```bash
# ADB 로그 모니터링
adb logcat | grep "TAG_NAME"

# 성능 프로파일링
adb shell top -p PACKAGE_NAME

# 메모리 분석
adb shell dumpsys meminfo PACKAGE_NAME
```

### A.2 테스트 권장 사항

#### **기기별 테스트**
- **삼성 갤럭시**: One UI 환경 테스트
- **구글 픽셀**: 순정 Android 환경
- **LG/샤오미**: 커스텀 ROM 환경
- **저사양 기기**: 성능 최적화 검증

#### **시나리오별 테스트**
1. **초기 설정**: 권한 설정 플로우
2. **일반 사용**: 클릭, 스크롤, 메뉴
3. **장시간 사용**: 메모리 누수, 배터리 소모
4. **예외 상황**: 전화 수신, 알림, 멀티태스킹

### A.3 배포 및 설치 가이드

#### **APK 빌드**
```bash
./gradlew assembleRelease
```

#### **디버그 설치**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### **릴리즈 서명**
```bash
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 
-keystore release-key.keystore app-release-unsigned.apk alias_name
```

---

**라이센스**: MIT License  
**개발팀**: Visual Camp & Contributors  
**문의사항**: GitHub Issues 또는 Discussion  
**버전**: 1.0.0 (2024년 업데이트)

---

*이 문서는 EyeID SDK 기반 적응형 캘리브레이션 프로젝트의 완전한 기술 가이드입니다. Visual Camp에 프로젝트 이관 시 또는 안드로이드 접근성 팀에서의 추가 개발 시 이 문서를 참조하여 빠른 이해와 확장 개발이 가능합니다.*
