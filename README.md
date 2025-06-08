# EyeID Tracker - Adaptive Calibration

[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com/about/versions/10)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen.svg)](https://android-arsenal.com/api?level=29)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 안드로이드 시선 추적 기반 커서 제어 시스템  
> EyeID SDK를 활용하여 실제 사용 가능한 완전한 시스템을 구현

## 목차
1. [프로젝트 소개](#1-프로젝트-소개)
2. [핵심 기술적 기여](#2-핵심-기술적-기여)
3. [최신 개발 성과](#3-최신-개발-성과)
4. [설치 및 설정](#4-설치-및-설정)
5. [앱 구조 개요](#5-앱-구조-개요)
6. [주요 컴포넌트 상세 설명](#6-주요-컴포넌트-상세-설명)
7. [좌표계 통합 및 캘리브레이션 시스템](#7-좌표계-통합-및-캘리브레이션-시스템)
8. [통합 커서 오프셋 시스템](#8-통합-커서-오프셋-시스템)
9. [설정 파라미터 가이드](#9-설정-파라미터-가이드)
10. [기능 수정 및 확장 가이드](#10-기능-수정-및-확장-가이드)
11. [문제 해결 및 디버깅](#11-문제-해결-및-디버깅)
12. [참고 자료](#12-참고-자료)

---

## 1. 프로젝트 소개

### 1.1 개요
본 프로젝트는 Eyedid(이전 SeeSo) SDK를 활용한 안드로이드 시선 추적 애플리케이션입니다. 사용자가 손으로 기기를 조작하지 않고도 시선만으로 스마트폰을 제어할 수 있는 완전한 시스템을 구현했습니다. 적응형 캘리브레이션 시스템을 통해 안전하고 정확한 시선 추적 경험을 제공하며, 안경 착용자를 포함한 다양한 사용자 환경을 지원합니다.

### 1.2 주요 기능
- **시선 고정 클릭**: 특정 위치를 일정 시간 응시하면 해당 위치를 클릭
- **화면 가장자리 스크롤**: 화면 상단 또는 하단을 일정 시간 응시하면 자동 스크롤
- **자동 캘리브레이션**: 앱 시작 시 자동 1포인트 캘리브레이션으로 시선 정확도 향상
- **통합 커서 오프셋 시스템**: 사용자별 미세 조정과 자동 보정을 통합한 정밀 위치 제어
- **정밀 캘리브레이션**: 5포인트 캘리브레이션을 통한 고정밀 시선 추적
- **안경 굴절 보정**: fixation 데이터를 활용한 안경 착용자 특화 보정 시스템
- **지능형 필터링**: TrackingState 기반 동적 필터 전환
- **성능 최적화**: 배터리 상태 기반 동적 FPS 조정
- **설정 화면**: 사용자별 최적화를 위한 다양한 파라미터 조정 기능
- **시각적 피드백**: 시선 위치, 진행 상태 등을 표시하는 커서 및 UI

### 1.3 아키텍처 특징
- **서비스 우선 아키텍처**: 백그라운드 지속 추적과 UI 분리로 안정성 확보
- **모듈화된 구조**: 역할별로 분리된 컴포넌트 구조로 확장성 및 유지보수성 향상
- **다중 레이어 아키텍처**: 데이터, 도메인, UI 레이어 분리를 통한 관심사 분리
- **설정 관리**: 사용자 설정을 효율적으로 관리하는 저장소 패턴 적용
- **안전성 우선 설계**: 잘못된 학습 방지를 위한 다중 검증 시스템

---

## 2. 핵심 기술적 기여

### 2.1 SDK 기능 vs 자체 구현 기능 구분

#### EyeID SDK가 제공하는 기능 (기본 인프라)
- 카메라 기반 시선 좌표 계산 (x, y 픽셀 좌표)
- AI 기반 시선 추적 알고리즘 (얼굴/눈 인식 및 시선 벡터 계산)
- 기본 캘리브레이션 시스템 (1포인트, 5포인트)
- OneEuro 필터 (기본 노이즈 제거)
- TrackingState, FaceInfo, BlinkInfo 등 메타데이터 제공

#### 자체 구현한 핵심 기능들

**1. 안경 굴절 보정 시스템**
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

**2. 안드로이드 좌표계 통합**
```java
public PointF convertGazeToSystemCoordinates(float gazeX, float gazeY) {
    int statusBarHeight = getStatusBarHeight(); // 85px 오프셋
    float systemY = gazeY + statusBarHeight;
    return new PointF(gazeX, systemY);
}
```

**3. 시선 기반 제스처 시스템**
```java
// 시선 고정 클릭
if (isWithinFixationRadius(x, y) && fixationDuration >= 1000) {
    triggerClick(x, y);
    showClickFeedback();
}

// 가장자리 스크롤
if ((gazeY < topEdge || gazeY > bottomEdge) && edgeDuration >= 3000) {
    performScroll(gazeY < topEdge ? SCROLL_UP : SCROLL_DOWN);
}
```

**4. 적응형 학습 안전 시스템**
```java
private boolean validateLearningData(float gazeX, float gazeY, float touchX, float touchY) {
    float distance = calculateDistance(gazeX, gazeY, touchX, touchY);
    return distance > 100 && distance < 300 &&
           !isOutlierPattern(gazeX, gazeY) &&
           !isConsecutiveError() &&
           !isUnrealisticMovement() &&
           !isForcedEyeMovement();
}
```

**5. 지능형 필터링 전환**
```java
if (trackingState == TrackingState.SUCCESS) {
    activeFilter = normalFilter;      // 반응성 중심
} else {
    activeFilter = confidenceFilter;  // 안정성 중심
}
```

**6. 시스템 통합 서비스**
```java
public class GazeTrackingService extends Service {
    // 백그라운드 시선 추적 및 제스처 감지
}

public class MyAccessibilityService extends AccessibilityService {
    // 시스템 제어 (클릭, 스크롤) 통합
}
```

### 2.2 해결한 주요 기술적 문제들

1. **85px 좌표 오프셋 문제**: 앱 영역과 전체 화면 좌표계 불일치 해결
2. **SDK 인스턴스 충돌**: MainActivity와 Service 간 SDK 동시 사용 문제 해결
3. **적응형 학습 악순환**: 잘못된 학습으로 인한 정확도 저하 방지
4. **안경 착용자 지원**: fixation 데이터를 활용한 굴절 보정 시스템 구현
5. **시스템 권한 통합**: 접근성 서비스와 오버레이 권한의 안정적 연동

---

## 3. 최신 개발 성과

### 3.1 주요 개발 성과

#### 완료된 develop 브랜치 수정사항
1. **앱 시작 시 자동 1포인트 캘리브레이션** - 서비스 시작 3초 후 자동 실행
2. **시선-커서 오프셋 자동 정렬** - 1포인트 캘리브레이션 후 자동 오프셋 계산
3. **사용자 설정에서 자동 캘리브레이션 끄기/켜기** - 설정 UI 및 저장소 구현
4. **Android 14 FOREGROUND_SERVICE_CAMERA 권한 추가** - AndroidManifest.xml 반영
5. **메인 화면 버튼을 "정밀 캘리브레이션"으로 변경** - 5포인트 캘리브레이션 구분

#### 새로 구현된 기능

**통합 커서 오프셋 시스템**
- **사용자 커서 오프셋 설정**: -50px ~ +50px 범위에서 좌우/상하 미세 조정
- **통합 오프셋 계산**: 사용자 설정 + 자동 보정 오프셋을 하나로 통합
- **실시간 반영**: 설정 변경 시 즉시 커서 위치에 반영
- **클릭 정확도 보장**: 커서 위치 = 클릭 위치로 일치

**개선된 캘리브레이션 UX**
- **중복 점 표시 문제 해결**: 기존 보라색 + 주황색 점 → 주황색 점만 표시
- **명확한 안내**: "잠시 후 나타나는 점을 응시해주세요" 메시지
- **사용 팁 제공**: 설정 화면에 "자동 보정 후 미세 조정하시면 더 정확합니다" 안내

### 3.2 수정된 파일 목록

#### 도메인 모델
- `UserSettings.java` - 커서 오프셋 필드 추가 (cursorOffsetX, cursorOffsetY)

#### 데이터 저장소
- `SharedPrefsSettingsRepository.java` - 커서 오프셋 저장/로드 로직 및 통합 오프셋 저장 메서드 추가

#### 핵심 서비스
- `GazeTrackingService.java` - 통합 오프셋 시스템 구현, 캘리브레이션 UX 개선

#### 설정 화면
- `SettingsActivity.java` - 커서 오프셋 조정 UI 추가, 실시간 반영 로직
- `activity_settings.xml` - 커서 위치 미세 조정 섹션 및 사용 팁 안내 추가

---

## 4. 설치 및 설정

### 4.1 시스템 요구사항
- Android 10.0 (API 레벨 29) 이상
- 전면 카메라가 있는 안드로이드 기기
- Android Studio Arctic Fox (2020.3.1) 이상

### 4.2 필요 권한
- `CAMERA`: 시선 추적을 위한 카메라 사용
- `SYSTEM_ALERT_WINDOW`: 다른 앱 위에 오버레이 표시
- `BIND_ACCESSIBILITY_SERVICE`: 시스템 제어(클릭, 스크롤) 기능
- `FOREGROUND_SERVICE`: 백그라운드 실행을 위한 포그라운드 서비스
- `FOREGROUND_SERVICE_CAMERA`: Android 14+ 카메라 포그라운드 서비스 권한

### 4.3 프로젝트 설정 방법

1. **저장소 클론하기**
   ```bash
   git clone https://github.com/YOUR_USERNAME/EyedidTracker-Refactored.git
   cd EyedidTracker-Refactored
   ```

2. **Android Studio에서 프로젝트 열기**
   - Android Studio 실행 > Open an existing Android Studio project > 프로젝트 폴더 선택

3. **Eyedid SDK 키 설정**
   - `EyedidTrackingRepository.java` 파일에서 LICENSE_KEY 값을 본인의 라이센스 키로 변경:
   ```java
   private static final String LICENSE_KEY = "your_license_key_here";
   ```

4. **빌드 및 실행**
   ```bash
   ./gradlew clean build
   ```

### 4.4 앱 설정 방법 (최초 실행 시)

1. **권한 설정 (순서대로)**
   - **카메라 권한**: 시선 추적용
   - **오버레이 권한**: 시선 커서 표시용  
   - **접근성 서비스**: 시선 터치/스크롤용

2. **자동 캘리브레이션 확인**
   - 앱 시작 3초 후 자동으로 1포인트 캘리브레이션이 실행됩니다.
   - 화면에 나타나는 주황색 점을 응시하여 캘리브레이션을 완료합니다.

3. **권장 사용 순서**
   ```
   1단계: 앱 실행 → 자동 1포인트 캘리브레이션 (3초 후)
   2단계: 대부분 사용자 - 자동 보정으로 충분, 바로 사용 가능
   3단계: 필요한 경우 - 설정에서 커서 위치 미세 조정
   4단계: 정밀도가 필요한 경우 - "정밀 캘리브레이션" 버튼으로 5포인트 실행
   ```

---

## 5. 앱 구조 개요

### 5.1 프로젝트 디렉토리 구조
```
app/src/main/java/camp/visual/android/sdk/sample/
├── data/                           # 데이터 레이어
│   ├── repository/                 # 데이터 접근 저장소
│   │   ├── EyeTrackingRepository.java    # 시선 추적 저장소 인터페이스
│   │   └── EyedidTrackingRepository.java # Eyedid SDK 구현체
│   └── settings/                   # 설정 관리
│       ├── SettingsRepository.java       # 설정 저장소 인터페이스
│       └── SharedPrefsSettingsRepository.java # SharedPreference 구현체
├── domain/                         # 비즈니스 로직 레이어
│   ├── model/                      # 도메인 모델
│   │   ├── BlinkData.java               # 눈 깜빡임 데이터 모델 (미구현)
│   │   └── UserSettings.java            # 사용자 설정 모델
│   └── interaction/                # 상호작용 로직
│       ├── ClickDetector.java           # 시선 고정 클릭 감지기
│       └── EdgeScrollDetector.java      # 가장자리 스크롤 감지기
├── service/                        # 안드로이드 서비스
│   ├── tracking/                   # 시선 추적 서비스
│   │   └── GazeTrackingService.java     # 핵심 시선 추적 서비스
│   └── accessibility/              # 접근성 서비스
│       └── MyAccessibilityService.java  # 시스템 제어 서비스
├── ui/                             # 프레젠테이션 레이어
│   ├── main/                       # 메인 화면
│   │   └── MainActivity.java            # 메인 액티비티
│   ├── settings/                   # 설정 화면
│   │   └── SettingsActivity.java        # 설정 액티비티
│   └── views/                      # 커스텀 뷰
│       ├── CalibrationViewer.java       # 캘리브레이션 화면
│       ├── OverlayCursorView.java       # 시선 커서 오버레이
│       └── PointView.java               # 시선 포인트 표시 뷰
└── AndroidManifest.xml             # 앱 매니페스트
```

### 5.2 시스템 아키텍처

```
사용자 시선
    ↓
EyeID SDK (시선 좌표 제공)
    ↓
자체 구현 시스템들 ↓
    ↓
┌─────────────────────────────────┐
│ 좌표 변환 & 안경 굴절 보정       │
├─────────────────────────────────┤
│ 지능형 필터링 & 제스처 감지      │
├─────────────────────────────────┤
│ 적응형 학습 & 안전 검증         │
├─────────────────────────────────┤
│ 시스템 통합 & 접근성 서비스      │
└─────────────────────────────────┘
    ↓
실제 클릭/스크롤 실행
```

---

## 6. 주요 컴포넌트 상세 설명

### 6.1 GazeTrackingService

이 서비스는 앱의 핵심으로, Eyedid SDK를 이용한 시선 추적, 시선 데이터 처리, 제스처 감지 등을 담당합니다.

#### 주요 역할
1. Eyedid SDK 초기화 및 시선 추적 시작/중지
2. 시선 데이터 필터링 및 처리
3. 통합 커서 오프셋 시스템 관리
4. 자동 캘리브레이션 및 정밀 캘리브레이션 처리
5. 시선 기반 제스처 감지 및 이벤트 처리
6. 안경 굴절 보정 및 지능형 필터링 적용
7. 오버레이 UI 관리 (커서, 캘리브레이션)
8. 진동 피드백 제공

#### 핵심 메서드
- **onCreate()**: 서비스 초기화, 컴포넌트 설정
- **initGazeTracker()**: Eyedid SDK 초기화
- **trackingCallback.onMetrics()**: 시선 데이터 수신 및 처리 (핵심 로직)
- **performAutoCalibration()**: 자동 1포인트 캘리브레이션 실행
- **triggerCalibration()**: 정밀 5포인트 캘리브레이션 시작
- **applyGlassesCorrection()**: 안경 굴절 보정 적용
- **updateFilters()**: TrackingState 기반 동적 필터 전환
- **scrollUp(), scrollDown()**: 스크롤 기능 구현
- **performClick()**: 클릭 동작 실행 (통합 오프셋 적용)
- **saveIntegratedCursorOffset()**: 통합 오프셋 저장
- **resetAll()**: 상태 초기화

#### 핵심 코드 분석: trackingCallback.onMetrics()
```java
@Override
public void onMetrics(long timestamp, GazeInfo gazeInfo, FaceInfo faceInfo, BlinkInfo blinkInfo, UserStatusInfo userStatusInfo) {
    if (gazeInfo.trackingState == TrackingState.SUCCESS) {
        // 1. 안경 굴절 보정 적용
        PointF corrected = applyGlassesCorrection(gazeInfo.x, gazeInfo.y, 
                                                gazeInfo.fixationX, gazeInfo.fixationY);
        
        // 2. 원-유로 필터링 적용
        if (oneEuroFilterManager.filterValues(timestamp, corrected.x, corrected.y)) {
            float[] filtered = oneEuroFilterManager.getFilteredValues();
            
            // 3. TrackingState 기반 지능형 필터링
            updateFilters(gazeInfo.trackingState);
            
            // 4. 통합 오프셋 적용
            filteredX += userSettings.getCursorOffsetX();
            filteredY += userSettings.getCursorOffsetY();
            
            // 5. 제스처 감지 및 처리
            edgeScrollDetector.update(filteredX, filteredY);
            clickDetector.update(filteredX, filteredY);
        }
    }
}
```

### 6.2 EdgeScrollDetector

화면 가장자리 응시를 감지하여 스크롤 동작을 트리거하는 클래스입니다.

#### 주요 역할
1. 상단/하단 가장자리 응시 감지
2. 응시 지속 시간 측정 및 피드백 제공
3. 스크롤 동작 트리거

#### 주요 메서드
- **update()**: 현재 시선 위치가 가장자리인지 확인
- **processTopEdge()**: 상단 가장자리 응시 처리
- **processBottomEdge()**: 하단 가장자리 응시 처리
- **getEdgeStateText()**: 현재 응시 상태에 대한 텍스트 반환

#### 핵심 변수 설명
- **EDGE_THRESHOLD_FRAMES**: 연속 프레임 감지 임계값 (기본값: 5)
- **edgeMarginRatio**: 가장자리 인식 영역 비율 (기본값: 0.01, 화면 높이의 1%)
- **edgeTriggerMs**: 스크롤 트리거까지의 응시 시간 (기본값: 3000ms)

### 6.3 ClickDetector

특정 영역을 일정 시간 응시하면 클릭으로 인식하는 클래스입니다.

#### 주요 역할
1. 특정 영역 응시 감지
2. 응시 지속 시간 측정
3. 클릭 동작 트리거

#### 주요 메서드
- **update()**: 현재 시선 위치 업데이트 및 클릭 판단
- **getProgress()**: 현재 응시 진행도 반환 (0.0-1.0)
- **reset()**: 상태 초기화

#### 핵심 변수 설명
- **aoiRadius**: 관심 영역(Area of Interest) 반경 (기본값: 40 픽셀)
- **fixationDurationMs**: 클릭으로 인식할 응시 시간 (기본값: 1000ms)

### 6.4 MyAccessibilityService

시스템 제어(클릭, 스크롤 등)를 담당하는 접근성 서비스입니다.

#### 주요 역할
1. 화면 어디서나 클릭 동작 실행
2. 다양한 스크롤 동작 지원 (방향, 크기, 연속 스크롤)

#### 주요 메서드
- **performClickAt()**: 지정 위치 클릭 실행
- **performScroll()**: 단일 스크롤 실행
- **performContinuousScroll()**: 연속 스크롤 실행

#### 핵심 변수 설명
- **SCROLL_AMOUNT_SMALL/MEDIUM/LARGE**: 스크롤 이동 거리 (화면 높이 대비 비율)
- **Direction**: 스크롤 방향 열거형 (UP, DOWN)
- **ScrollAmount**: 스크롤 양 열거형 (SMALL, MEDIUM, LARGE)

### 6.5 UserSettings

사용자 설정 정보를 담는 모델 클래스입니다. 빌더 패턴을 사용하여 구현되었습니다.

#### 주요 속성
- **fixationDurationMs**: 고정 클릭 인식 시간
- **aoiRadius**: 관심 영역 반경
- **scrollEnabled**: 스크롤 기능 활성화 여부
- **edgeMarginRatio**: 가장자리 인식 영역 비율
- **edgeTriggerMs**: 스크롤 트리거 응시 시간
- **continuousScrollCount**: 연속 스크롤 횟수
- **clickEnabled**: 클릭 기능 활성화 여부
- **edgeScrollEnabled**: 가장자리 스크롤 활성화 여부
- **autoCalibrationEnabled**: 자동 캘리브레이션 활성화 여부
- **cursorOffsetX**: 커서 X축 오프셋
- **cursorOffsetY**: 커서 Y축 오프셋
- **blinkDetectionEnabled**: 눈 깜빡임 감지 활성화 여부 (미구현)

---

## 7. 좌표계 통합 및 캘리브레이션 시스템

### 7.1 좌표계 통합 가이드

#### 7.1.1 초기 문제 상황
개발 과정에서 다음과 같은 좌표계 관련 문제들이 발생했습니다:
- **캘리브레이션 문제**: 버튼 클릭은 감지되지만 실제 캘리브레이션이 실행되지 않음
- **터치 위치 불일치**: 시선 커서의 중심이 아닌 위쪽 85px 지점에서 터치가 발생

#### 7.1.2 캘리브레이션 시스템 해결 방법

**문제 원인**: MainActivity와 GazeTrackingService 간 SDK 인스턴스 충돌

**해결 코드**:
```java
// MainActivity에서 서비스 상태 확인 후 적절한 곳에서 캘리브레이션 실행
private void startCalibration() {
    if (isServiceRunning()) {
        // 서비스에서 실행
        GazeTrackingService.getInstance().triggerCalibration();
    } else {
        // MainActivity에서 실행
        gazeTracker.startCalibration(calibrationType);
    }
}
```

**서비스-액티비티 연동**:
- MainActivity에서 서비스 실행 상태 확인
- 서비스가 실행 중이면 MainActivity의 tracker 해제
- 캘리브레이션은 서비스를 우선으로 실행

### 7.2 안드로이드 좌표계 분석

안드로이드 시선 추적 앱에서는 **세 가지 주요 좌표계**가 상호작용합니다:

#### 전체 화면 좌표계 (Hardware Screen)
```
┌─────────────────┐ Y=0 (물리적 화면 최상단)
│   상태바 (85px)   │
├─────────────────┤ Y=85
│                 │
│   앱 영역        │ (시선 추적이 실제 작동하는 영역)
│  (2069px)       │
├─────────────────┤ Y=2154
│ 네비게이션바      │
│  (126px)        │  
└─────────────────┘ Y=2280 (물리적 화면 최하단)
```

#### 앱 영역 좌표계 (App Window)
```
┌─────────────────┐ Y=0 (앱 영역 최상단)
│                 │
│   앱 컨텐츠      │ (일반적인 앱 UI가 그려지는 영역)
│                 │
└─────────────────┘ Y=2069 (앱 영역 최하단)
```

#### 접근성 서비스 좌표계 (Accessibility)
```
┌─────────────────┐ Y=0 (접근성 기준 최상단)
│                 │
│  제스처 감지 영역  │ (터치/클릭 이벤트 발생 영역)
│                 │  
└─────────────────┘ Y=2280 (접근성 기준 최하단)
```

### 7.3 좌표계 불일치 해결

#### 7.3.1 문제 발견
개발자 옵션의 터치 포인트 표시로 확인한 결과, 시선 커서 중심이 아닌 **위쪽 85px 지점**에서 터치가 발생함을 발견했습니다.

#### 7.3.2 좌표계별 측정 결과
로그 분석을 통해 확인된 화면 크기:
```
앱 영역: 1080 x 2069px
전체 화면: 1080 x 2280px  
상태바: 85px
네비게이션바: 126px
계산 검증: 2069 + 85 + 126 = 2280 ✅
```

#### 7.3.3 좌표계 불일치 원인

**시선 추적 SDK**:
- 앱 영역 기준으로 좌표 제공 (0~2069)
- 예: (650, 1413)

**접근성 서비스**:
- 전체 화면 기준으로 좌표 해석 (0~2280)
- 동일 좌표 (650, 1413)을 받으면 실제로는 (650, 1413-85) 위치에 터치

**결과**:
- 85px만큼 위에서 터치 발생
- 시선 커서와 실제 터치 위치 불일치

### 7.4 최종 해결 방법

#### 7.4.1 터치 좌표 변환
```java
private void performClick(float x, float y) {
    // 시선 좌표(앱 영역) → 접근성 서비스(전체 화면)
    float adjustedX = x;
    float adjustedY = y + statusBarHeight;  // +85px 보정
    
    MyAccessibilityService.performClickAt(adjustedX, adjustedY);
}
```

#### 7.4.2 캘리브레이션 포인트 처리
```java
private void showCalibrationPointView(float x, float y) {
    // SDK에서 제공하는 캘리브레이션 좌표는 이미 전체 화면 기준
    // 오버레이도 전체 화면에 그려지므로 변환하지 않음
    calibrationViewer.setPointPosition(x, y);
}
```

#### 7.4.3 시선 커서 표시
```java
// 오버레이 커서는 전체 화면에 그려지므로 변환 불필요
overlayCursorView.updatePosition(gazeX, gazeY);
```

### 7.5 변환 매트릭스 요약

| 요소 | 입력 좌표계 | 출력 좌표계 | 변환 공식 |
|------|------------|------------|-----------|
| **시선 → 터치** | 앱 영역 | 접근성 (전체) | Y + 85px |
| **시선 → 커서** | 앱 영역 | 오버레이 (전체) | 변환 없음* |
| **캘리브레이션** | 전체 화면 | 오버레이 (전체) | 변환 없음 |

*시선 커서는 앱 영역 좌표를 받지만 오버레이에 그릴 때는 SDK가 내부적으로 처리

---

## 8. 통합 커서 오프셋 시스템

### 8.1 기술적 구현 세부사항

#### 통합 오프셋 계산 로직
```java
// 1포인트 캘리브레이션 완료 시
float newAutoOffsetX = targetX - avgGazeX;
float newAutoOffsetY = targetY - avgGazeY;

// 기존 사용자 오프셋과 새로운 자동 오프셋 통합
float integratedOffsetX = userSettings.getCursorOffsetX() + newAutoOffsetX;
float integratedOffsetY = userSettings.getCursorOffsetY() + newAutoOffsetY;

// 통합 오프셋을 설정에 저장하여 일관성 유지
saveIntegratedCursorOffset(integratedOffsetX, integratedOffsetY);
```

#### 커서-클릭 위치 일치 보장
```java
// 커서 표시: 원본 시선 + 통합 오프셋
filteredX += userSettings.getCursorOffsetX();
filteredY += userSettings.getCursorOffsetY();

// 클릭 실행: 커서가 표시된 정확한 위치에서 클릭
float adjustedX = cursorX;
float adjustedY = cursorY + statusBarHeight;
```

#### 설정 UI 구현
- **슬라이더 범위**: 0~100 (내부적으로 -50px ~ +50px로 변환)
- **실시간 반영**: 슬라이더 조정 시 즉시 서비스에 설정 변경 알림
- **직관적 레이블**: "← 왼쪽 | 오른쪽 →", "↑ 위쪽 | 아래쪽 ↓"

### 8.2 자동 보정과 미세 보정의 관계

#### 통합 방식 채택
- **독립적이지 않음**: 사용자 설정 + 자동 보정이 하나로 통합
- **장점**: 개인적 선호도가 자동 보정에 반영되어 더 정확한 결과
- **사용 시나리오**: 
  ```
  초기(0, 0) → 사용자 조정(+5, +3) → 자동 보정(+10, -2) → 통합(+15, +1)
  ```

### 8.3 검증된 기능

- ✅ 1포인트 캘리브레이션이 Eyedid SDK 공식 문서 방식(`CalibrationModeType.ONE_POINT`) 준수
- ✅ 커서와 터치 위치 완벽 일치
- ✅ 설정 변경 시 실시간 반영
- ✅ 기존 좌표계 변환 로직과 호환성 유지
- ✅ 사용자 친화적인 설정 UI 및 안내 메시지

---

## 9. 설정 파라미터 가이드

### 9.1 고정 클릭 설정

| 파라미터 | 설명 | 기본값 | 권장 범위 | 영향 |
|---------|------|--------|----------|------|
| fixationDurationMs | 클릭 인식 시간(ms) | 1000 | 500-2000 | 값이 작을수록 빠르게 인식되지만 오탐지 가능성 증가 |
| aoiRadius | 관심 영역 반경(px) | 40 | 20-70 | 값이 클수록 넓은 영역에서 클릭 인식, 정밀도 감소 |
| clickEnabled | 클릭 기능 활성화 | true | - | 기능 자체의 활성화/비활성화 |

### 9.2 스크롤 설정

| 파라미터 | 설명 | 기본값 | 권장 범위 | 영향 |
|---------|------|--------|----------|------|
| edgeMarginRatio | 가장자리 인식 영역 비율 | 0.01 | 0.005-0.05 | 값이 클수록 넓은 영역에서 가장자리 인식 |
| edgeTriggerMs | 스크롤 트리거 시간(ms) | 3000 | 1000-5000 | 값이 작을수록 빠르게 스크롤 트리거 |
| continuousScrollCount | 연속 스크롤 횟수 | 2 | 1-5 | 한 번 트리거 시 연속 실행될 스크롤 횟수 |
| scrollEnabled | 스크롤 기능 활성화 | true | - | 스크롤 기능 자체의 활성화/비활성화 |
| edgeScrollEnabled | 가장자리 스크롤 활성화 | true | - | 가장자리 스크롤 기능의 활성화/비활성화 |

### 9.3 캘리브레이션 및 오프셋 설정

| 파라미터 | 설명 | 기본값 | 권장 범위 | 영향 |
|---------|------|--------|----------|------|
| autoCalibrationEnabled | 자동 캘리브레이션 활성화 | true | - | 앱 시작 시 1포인트 캘리브레이션 실행 여부 |
| cursorOffsetX | 커서 X축 오프셋(px) | 0 | -50 ~ +50 | 커서 좌우 위치 미세 조정 |
| cursorOffsetY | 커서 Y축 오프셋(px) | 0 | -50 ~ +50 | 커서 상하 위치 미세 조정 |

---

## 10. 기능 수정 및 확장 가이드

### 10.1 고급 사용 테크닉: 눈 감기를 통한 정밀 클릭

개발 과정에서 발견된 흥미로운 사용 테크닉으로, 눈을 감아 커서를 고정하는 방법이 있습니다. 이 방법은 정밀한 클릭이 필요한 상황에서 매우 유용합니다.

#### 작동 원리
- 사용자가 눈을 감으면 시선 추적이 일시적으로 중단됩니다.
- 시선 추적이 중단되면 마지막으로 감지된 위치에 커서가 고정됩니다.
- 고정된 커서 위치에서 시간이 경과하면 클릭이 발생합니다.
- 클릭이 발생할 때 진동 피드백이 제공되므로, 사용자는 눈을 감고 있어도 클릭 여부를 감지할 수 있습니다.

#### 사용 방법
1. 시선을 원하는 클릭 위치로 이동시킵니다.
2. 커서가 원하는 위치에 도달하면 눈을 감습니다.
3. 고정 클릭 시간(기본 1초)동안 눈을 감은 상태를 유지합니다.
4. 진동 피드백이 느껴지면 눈을 뜹니다.

### 10.2 새로운 제스처 추가하기

새로운 시선 기반 제스처를 추가하려면 다음 단계를 따르세요:

1. **제스처 감지기 클래스 생성**
   - `domain/interaction` 패키지에 새 감지기 클래스 생성
   - 기존 `ClickDetector` 또는 `EdgeScrollDetector`를 참고

2. **GazeTrackingService에 통합**
   - `GazeTrackingService` 클래스에 감지기 인스턴스 추가
   - `initDetectors()` 메서드에서 초기화
   - `onMetrics()` 콜백에서 제스처 감지 로직 호출

3. **설정 파라미터 추가 (선택 사항)**
   - `UserSettings` 클래스에 새 설정 파라미터 추가
   - `SettingsActivity`에 UI 요소 추가
   - `SharedPrefsSettingsRepository`에 저장/로드 로직 추가

### 10.3 필터링 최적화하기

시선 추적 데이터 필터링을 최적화하려면:

1. **원-유로 필터 파라미터 조정**
   ```java
   oneEuroFilterManager = new OneEuroFilterManager(2);
   oneEuroFilterManager.setMinCutoff(0.5f);  // 기본값보다 낮게 설정하면 더 부드러움
   oneEuroFilterManager.setBeta(0.05f);      // 낮은 beta는 속도 변화에 덜 민감
   ```

2. **추가 평활화 적용**
   ```java
   // 이동 평균 필터 (마지막 N개 샘플의 평균)
   private final int WINDOW_SIZE = 5;
   private float[] xHistory = new float[WINDOW_SIZE];
   private float[] yHistory = new float[WINDOW_SIZE];
   
   private float[] applyMovingAverage(float x, float y) {
       // 이동 평균 계산 로직
   }
   ```

---

## 11. 문제 해결 및 디버깅

### 11.1 일반적인 문제 및 해결 방법

| 문제 | 가능한 원인 | 해결 방법 |
|-----|-----------|---------|
| 시선 추적이 시작되지 않음 | SDK 라이센스 키 오류 | EyedidTrackingRepository.java의 LICENSE_KEY 확인 |
| 시선 커서가 떨림 | 필터링 부족 | OneEuroFilterManager 파라미터 조정 |
| 클릭이 실행되지 않음 | 접근성 서비스 비활성화 | 접근성 설정에서 앱 활성화 확인 |
| 터치 위치가 부정확함 | 좌표계 변환 문제 | performClick() 메서드에서 상태바 높이 보정 확인 |
| 캘리브레이션이 작동하지 않음 | 서비스-액티비티 간 SDK 인스턴스 충돌 | 서비스 실행 중일 때 서비스에서 캘리브레이션 실행 |
| 자동 캘리브레이션이 실행되지 않음 | 자동 캘리브레이션 설정 비활성화 | 설정에서 "앱 시작 시 자동 빠른 보정" 활성화 |
| 커서 오프셋 설정이 반영되지 않음 | 서비스 재시작 필요 | 설정 변경 후 앱 재시작 또는 서비스 재시작 |
| 앱 충돌 발생 | 권한 문제 | Logcat에서 오류 확인 및 권한 설정 확인 |

### 11.2 좌표계 관련 디버깅

좌표계 문제를 디버깅하려면:

1. **개발자 옵션 활용**
   - 설정 > 개발자 옵션 > 터치 포인트 표시 활성화
   - 시선 클릭 시 터치 지점과 커서 위치 비교

2. **로그 출력으로 확인**
   ```java
   // 터치 실행 시 로그 출력
   private void performClick(float x, float y) {
       float adjustedY = y + statusBarHeight;
       Log.d(TAG, String.format("Original: (%.1f, %.1f), Adjusted: (%.1f, %.1f)", 
               x, y, x, adjustedY));
       MyAccessibilityService.performClickAt(x, adjustedY);
   }
   ```

3. **화면 정보 확인**
   ```java
   // 앱 시작 시 화면 정보 로그 출력
   private void printScreenInfo() {
       Log.d(TAG, "App Height: " + dm.heightPixels);
       Log.d(TAG, "Real Height: " + realHeight);
       Log.d(TAG, "Status Bar: " + statusBarHeight);
       Log.d(TAG, "Navigation Bar: " + (realHeight - dm.heightPixels - statusBarHeight));
   }
   ```

### 11.3 오프셋 시스템 디버깅

통합 커서 오프셋 시스템 관련 문제 해결:

1. **오프셋 값 확인**
   ```java
   // 오프셋 적용 상태 로그 출력
   Log.d(TAG, String.format("User Offset: (%.1f, %.1f), Applied: (%.1f, %.1f)", 
           userSettings.getCursorOffsetX(), userSettings.getCursorOffsetY(),
           filteredX, filteredY));
   ```

2. **캘리브레이션 후 오프셋 통합 확인**
   ```java
   // 1포인트 캘리브레이션 완료 후 로그
   Log.d(TAG, String.format("Auto Offset: (%.1f, %.1f), Integrated: (%.1f, %.1f)",
           newAutoOffsetX, newAutoOffsetY, integratedOffsetX, integratedOffsetY));
   ```

### 11.4 로그 활용 가이드

각 클래스에는 로깅 코드가 포함되어 있습니다. `TAG` 필터를 사용하여 특정 컴포넌트의 로그만 확인할 수 있습니다.

```
// 로그캣에서 다음 태그 필터 사용
GazeTrackingService
ClickDetector
EdgeScrollDetector
MyAccessibilityService
CoordinateManager
AutoCalibrationManager
OffsetManager
```

### 11.5 성능 프로파일링

앱의 성능을 모니터링하려면:

1. **Android Studio의 CPU Profiler 사용**
2. **각 메서드에 성능 측정 로그 추가**
3. **메모리 누수 확인**: 오버레이 뷰가 제대로 제거되는지 확인

### 11.6 성능 및 리소스 최적화

#### 리소스 사용 분석
- **주요 소모원**: Eyedid SDK의 시선 추적 알고리즘 (핵심 기능으로 불가피)
- **적절한 수준**: 시선 추적 앱으로서는 합리적인 리소스 사용량
- **최적화 여지**: 카메라 해상도/FPS 조정, 배터리 모드 추가 가능

#### 메모리 관리
- **통합 오프셋**: 단일 설정값으로 관리하여 메모리 효율성 향상
- **오버레이 뷰**: 적절한 생명주기 관리로 메모리 누수 방지

---

## 12. 참고 자료

### 12.1 Eyedid SDK 문서
- [SDK 개요](https://docs.eyedid.ai/docs/document/eyedid-sdk-overview)
- [안드로이드 퀵 스타트 가이드](https://docs.eyedid.ai/docs/quick-start/android-quick-start)
- [API 문서](https://docs.eyedid.ai/docs/api/android-api-docs/)
- [캘리브레이션 가이드](https://docs.eyedid.ai/docs/document/calibration-overview)
- [1포인트 캘리브레이션 문서](https://docs.eyedid.ai/docs/document/calibration-overview#one-point-calibration)

### 12.3 안드로이드 개발 관련
- [안드로이드 접근성 서비스 가이드](https://developer.android.com/guide/topics/ui/accessibility/service)
- [시스템 오버레이 가이드](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
- [DisplayMetrics 문서](https://developer.android.com/reference/android/util/DisplayMetrics)
- [원-유로 필터 논문](https://cristal.univ-lille.fr/~casiez/1euro/)
- [Android 14 포그라운드 서비스 변경사항](https://developer.android.com/about/versions/14/changes/fgs-types-required)

### 12.4 좌표계 및 UI 개발
- [안드로이드 화면 좌표계 이해](https://developer.android.com/guide/topics/graphics/2d-graphics)
- [접근성 서비스 좌표 시스템](https://developer.android.com/guide/topics/ui/accessibility/principles)
- [윈도우 매니저 레이아웃 파라미터](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)

---

## 부록: 개발 과정에서 학습한 팁

### A.1 좌표계 변환 검증 방법
1. 개발자 옵션의 터치 포인트 표시 활용
2. 각 좌표계별 화면 정보 로깅
3. 단계별 좌표 변환 확인

### A.2 성능 최적화 팁
1. 시선 데이터 필터링 파라미터 조정
2. UI 업데이트 빈도 최적화
3. 메모리 누수 방지 (오버레이 뷰 관리)

### A.3 사용자 경험 개선
1. 진동 피드백을 통한 명확한 상호작용 표시
2. 시각적 진행 표시기로 사용자 안내
3. 설정 화면을 통한 개인화 지원
4. 자동 캘리브레이션으로 초기 설정 부담 감소
5. 통합 오프셋 시스템으로 정밀도 향상

### A.4 디버깅 및 테스트 팁
1. 로그캣 필터링을 통한 효율적 디버깅
2. 개발자 옵션의 다양한 도구 활용
3. 다양한 기기에서의 테스트 중요성
4. 사용자 피드백을 통한 지속적 개선

---

이 통합 가이드는 Eyedid SDK를 사용한 안드로이드 시선 추적 앱 개발의 모든 측면을 다룹니다. 최신 기능인 통합 커서 오프셋 시스템과 자동 캘리브레이션을 포함하여, 좌표계 문제부터 고급 최적화 기법까지 실제 개발 과정에서 마주칠 수 있는 다양한 상황에 대한 해결책을 제시합니다.

**라이센스**: MIT 라이센스 하에 배포됩니다.  
**기여**: 시선 추적 기술의 안전하고 정확한 사용을 목표로 기여해 주시기 바랍니다.  
**문의**: GitHub Issues 또는 Discussion을 통해 문의해 주시기 바랍니다.
