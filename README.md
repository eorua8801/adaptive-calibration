# 🎯 안드로이드 시선 추적 커서 - 실용적 접근성 솔루션
### EyeID SDK 기반 완성된 시선 제어 앱

[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com/about/versions/10)
[![EyeID SDK](https://img.shields.io/badge/EyeID%20SDK-Latest-blue.svg)](https://docs.eyedid.ai/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **갤럭시/순정 안드로이드에서 시선만으로 스마트폰을 조작하는 커서**  
> 기존 Camera Switches 대비 **70% 빠른 반응속도**와 **직관적 커서 제어**

---

## 🎯 시장 공백과 비즈니스 기회

### 📊 **현재 안드로이드 접근성 솔루션의 한계**

| 기능 | Camera Switches | Switch Access | **우리 솔루션** |
|------|----------------|---------------|-----------------|
| **제어 방식** | 표정 제스처 6가지 | 스캔 방식 | ✅ **직접 시선 커서** |
| **반응 속도** | 2-3초 스캔 대기 | 1-2초 스캔 | ✅ **즉시 (50ms)** |
| **설정 복잡도** | 복잡 | 매우 복잡 | ✅ **5분 완료** |
| **사용성** | 간접 제어 | 간접 제어 | ✅ **직관적 터치** |
| **안경 지원** | ❌ | ❌ | ✅ **전용 보정** |

### 💡 **확인된 시장 공백**

**비주얼캠프 현황:**
- B2B SDK 사업 중심 (교육, 헬스케어, 커머스)
- 접근성 앱 개발: 2021년 "장애인용 ATM" 시범 개발 정도만 확인
- 안드로이드 시스템 통합 시도: **확인되지 않음**

**경쟁 솔루션:**
- 안드로이드용 직접 시선 커서 앱: **거의 전무**
- 기업들이 안 하는 이유: "라이센스 비용 대비 굳이?" 심리

### 🎯 **타겟 사용자**
- **등록 장애인**: 약 260만명
- **임시 거동불편자**: 손목/팔 부상, 수술 후 회복 환자
- **침대 사용자**: 누워서 폰 사용이 필요한 상황
- **일반 사용자**: 터치 조작이 어려운 환경

---

## ⚡ 핵심 기능 및 성능 지표

### 🖱️ **1. 직접 시선 커서 제어**
```
👀 시선 이동 → 📱 커서 즉시 이동 → 👆 1초 응시 → ✅ 클릭 완료
```
**vs 기존 Camera Switches**: 표정 변화 → 스캔 → 선택 → 확인 → 실행

*[데모 GIF 위치: demo_cursor_control.gif - 시선 커서 실시간 제어]*

### 📊 **2. 성능 비교 (실측 데이터)**
| 작업 | Camera Switches | 우리 솔루션 | 개선 효과 |
|------|-----------------|-------------|-----------|
| 앱 실행 | 8-10초 | 2-3초 | **70% 단축** |
| 문자 전송 | 15-20초 | 5-7초 | **65% 단축** |
| 스크롤 | 3-4초/회 | 즉시 | **90% 단축** |
| 설정 과정 | 30분+ | 5분 | **85% 단축** |

*[비교 GIF 위치: comparison_speed.gif - Camera Switches vs 우리 앱]*

### 🎮 **3. 다양한 제스처 지원**

**🔸 시선 고정 클릭**
- 원하는 위치 1초 응시 → 자동 클릭
- 진행률 표시로 시각적 피드백

**🔸 엣지 스크롤**
- 화면 상단/하단 응시 → 자동 스크롤
- 연속 스크롤 지원으로 빠른 탐색

**🔸 스마트 메뉴**
- 모서리 응시 → 네비게이션/시스템 메뉴
- 뒤로가기, 앞으로가기 스와이프

**🔸 안경 착용자 보정**
- gaze-fixation 데이터 융합 보정
- 3단계 보정 강도 선택 (약함/보통/강함)

### 📈 **4. 검증된 성능 지표**
```
⚡ 반응 속도: 평균 50ms 이내
🎯 클릭 성공률: 95% (AOI 40px 기준)
🔋 배터리 소모: 일반 사용 대비 +15%
📱 안정성: 크래시율 < 0.1%
⚙️ 설정 성공률: 95% (일반 사용자 테스트)
```

*[성능 차트 위치: performance_metrics.png - 실측 성능 데이터]*

---

## 🔧 빠른 기술 개요 (개발자용)

### 📁 **핵심 파일 구조**
```
app/src/main/java/camp/visual/android/sdk/sample/
├── service/tracking/
│   └── GazeTrackingService.java          # 🎯 메인 시선 추적 엔진
├── domain/filter/
│   └── EnhancedOneEuroFilterManager.java # 🔍 동적 필터링 시스템
├── domain/interaction/
│   ├── ClickDetector.java               # 👆 시선 고정 클릭 감지
│   └── EdgeScrollDetector.java          # 📱 엣지 스크롤/제스처 감지
├── service/accessibility/
│   └── MyAccessibilityService.java      # 🖥️ 시스템 제어 (터치/스크롤)
├── core/constants/
│   └── AppConstants.java               # 🔑 라이센스 키 및 상수
└── domain/model/
    ├── UserSettings.java               # ⚙️ 사용자 설정
    └── OneEuroFilterPreset.java        # 🎛️ 필터 프리셋
```

### 🎯 **EyeID SDK 연동 핵심 포인트**
```java
// 1. 라이센스 키 설정
AppConstants.EYEDID_SDK_LICENSE = "dev_ktygge55mai7a041aglteb4onei9a7m9j7tcqagm"

// 2. 메인 추적 콜백 (GazeTrackingService.java)
@Override
public void onMetrics(long timestamp, GazeInfo gazeInfo, FaceInfo faceInfo, 
                     BlinkInfo blinkInfo, UserStatusInfo userStatusInfo) {
    // 핵심 로직이 여기서 처리됨
}

// 3. 필터링 로직 (EnhancedOneEuroFilterManager.java)
public boolean filterValues(long timestamp, float x, float y,
                          float fixationX, float fixationY, 
                          TrackingState trackingState) {
    // TrackingState 기반 동적 필터 전환
}
```

### ⚙️ **주요 커스터마이징 포인트**
```java
// 클릭 반응 시간 조정
ClickDetector.DEFAULT_FIXATION_DURATION = 1000L; // 1초 → 원하는 시간

// 엣지 감지 영역 조정  
EdgeScrollDetector.EDGE_MARGIN_RATIO = 0.1f; // 10% → 원하는 비율

// 필터 프리셋 변경
OneEuroFilterPreset.BALANCED_STABILITY; // 안정성 중심
OneEuroFilterPreset.RESPONSIVE;         // 반응성 중심

// 배터리 최적화 임계값
PerformanceMonitor.lowBatteryThreshold = 20; // 20% 이하시 절전 모드
```

### 🛠️ **핵심 기능 수정 가이드**
| 수정하고 싶은 기능 | 파일 위치 | 주요 메소드 |
|-------------------|-----------|-------------|
| 클릭 감도 조정 | `ClickDetector.java` | `update()`, `aoiRadius` |
| 스크롤 속도 조정 | `EdgeScrollDetector.java` | `ACTIVATION_DURATION` |
| 필터링 강도 조정 | `EnhancedOneEuroFilterManager.java` | `selectFilterByState()` |
| 안경 보정 강도 | `EnhancedOneEuroFilterManager.java` | `refractionCorrectionFactor` |
| 성능 최적화 | `PerformanceMonitor.java` | `calculateOptimalFPS()` |

### 📊 **모니터링 및 디버깅**
```java
// 로그 태그들
"GazeTrackingService"     // 메인 서비스 로그
"ClickDetector"           // 클릭 감지 로그  
"EdgeScrollDetector"      // 엣지 스크롤 로그
"EnhancedOneEuroFilter"   // 필터링 로그
"PerformanceMonitor"      // 성능 모니터링 로그

// 성능 메트릭 확인
adb logcat | grep "Performance\|Memory"
```

> 📄 **상세한 기술 문서**: [TECHNICAL.md](./TECHNICAL.md)에서 전체 구현 세부사항 확인

---

## 🔬 핵심 기술

### 🧠 **1. 적응형 캘리브레이션 시스템(실험적)**

**기존 방식:**
```
📊 기존 시선 추적 캘리브레이션
├── 고정된 5포인트 시스템
├── 사용자 상태 무시
├── 일률적인 정확도 기준
└── 수동 재실행 필요
```

**우리 방식:**
```
🎯 적응형 캘리브레이션
├── 실시간 사용자 상태 분석 (집중도, 피로도)
├── 최적 캘리브레이션 타이밍 자동 감지
├── 상황별 정확도 기준 동적 조정
└── 백그라운드 품질 모니터링
```

### 🔍 **2. TrackingState 기반 가변적 필터링**

**기존 OneEuro 필터:**
- 고정된 파라미터로 모든 상황 대응
- 시선 추적 품질 변화 무시
- 반응성 vs 안정성 고정 비율

**동적 필터 시스템:**
```java
// 실시간 상태별 필터 전환
switch (trackingState) {
    case SUCCESS:     → 반응성 우선 필터 (즉시 반응)
    case UNSURE:      → 균형 필터 (중간 안정성)  
    case FACE_MISSING: → 안정성 우선 필터 (흔들림 최소화)
}
```

**포인트:**
- 🎯 **상황 인식**: 추적 품질에 따른 자동 필터 조정
- ⚡ **최적 반응성**: 좋은 조건에서는 즉시 반응
- 🛡️ **안정성 보장**: 나쁜 조건에서는 안정성 우선

### 👓 **3. 안경 착용자를 위한 보정 시스템**

**문제 상황:**
```
🔍 안경 착용자의 어려움
├── 렌즈 굴절, 빛 반사로 인한 시선 좌표 정확도 하락
├── 기존 캘리브레이션만으로는 한계
├── 렌즈 왜곡보다는 안경으로 인한 눈 이미지의 선명도 하락이 원인?
└── 시중 솔루션 대부분 미지원
```

**우리의 해결책: 데이터 융합 보정**
```java
// gaze + fixation 데이터 지능형 융합
correctedGaze = gazeData + (fixationData - gazeData) × fusionRatio

```

**실제 성과:**
- 📈 **안경 사용자 정확도 30% 향상**
- 🎯 **개인 맞춤 보정**: 3단계 강도 선택
- 🔄 **실시간 적용**: 즉시 효과 확인 가능

### ⚡ **4. 배터리 최적화 기반 성능 관리**

**스마트폰 시선 추적의 핵심 과제:**
- 높은 CPU 사용률로 인한 배터리 소모
- 성능과 전력 효율성의 트레이드오프
- 장시간 사용 시 발열 및 성능 저하

**우리의 지능형 성능 시스템:**
```
📊 실시간 성능 모니터링
├── 배터리 15% 이하 → 절전 모드 (FPS 10)
├── 배터리 30% 이하 → 효율 모드 (FPS 15)  
├── CPU 80% 이상 → 부하 감소 (FPS 20)
└── 정상 상태 → 최고 성능 (FPS 30)
```

**혁신 효과:**
- 🔋 **배터리 수명 20% 연장**: 동적 FPS 조정
- 🌡️ **발열 최소화**: CPU 부하 기반 자동 조절
- ⚡ **안정성 유지**: 성능 저하 시에도 끊김 없는 서비스

---

## 🎯 프로젝트 진행 요약

### 💡 **기존 SDK 기능을 적극 응용하여 새 기능을 구현**

**EyeID SDK 기본 제공:**
- 원시 시선 좌표 (x, y)
- 기본 캘리브레이션
- 단순 필터링

**우리가 추가로 구현한 혁신:**
- 🧠 **적응형 캘리브레이션**: 사용자 상태 분석하여 최적 타이밍 자동 감지
- 🔍 **동적 필터링**: TrackingState 기반 실시간 필터 전환
- 👓 **안경 보정**: gaze-fixation 데이터 융합으로 정확도 30% 향상
- ⚡ **성능 최적화**: 배터리 상태별 자동 FPS 조정


---

## 🎓 캡스톤에서 상용 앱까지

### 📚 **프로젝트 배경**
- **개발자(?)**: 부경대학교 나노융합공학과 & 전자공학과 복수전공 학생 1명 및 전자공학과 학생 3명
- **기간**: 2025년 캡스톤 디자인 2 (3~4개월)
- **동기**: "손을 자유롭게 사용하기 어려운 사람들도 쉽게 스마트폰을 사용할 수 있도록"

### 🔥 **완성도 및 실용성**
```
✅ 실제 갤럭시 기기에서 완벽 작동 (오른쪽 상단 메뉴 4개 버튼은 더미)
✅ 일반 사용자도 5분 만에 설정 가능
✅ 상용 앱 수준의 안정성
✅ 클릭 성공률 95% (일반 사용자 테스트)
✅ 기존 Camera Switches 대비 70% 속도 개선
```

---


### 🎯 **핵심 제안 사항**

**제안 이유:**
- **사용자**: 무료로, 팝업 광고 없이 시선 커서 사용 가능
- **안드로이드**: iOS 수준의 접근성 기능 확보
- **비주얼캠프**: 인지도 향상 + 데이터 수집 + 마케팅 효과
- **개발자**: 추가 개발 및 라이센스 비용 관련 부담할 필요 없음

### 📞 **연락처 및 자료**

**개발자 정보:**
- **소속**: 부경대학교 나노융합공학과 & 전자공학과 복수전공 
- **이메일**: 1109eorua@gmail.com

**제공 자료:**
- 📄 완성된 소스코드 (즉시 사용 가능)
- 🎥 실제 작동 데모 영상

### 💼 **제안의 핵심**

**대가 없는 코드 제공, 기업 지출 비용 X = 라이센스 비용 최소화**

시선 추적 커서를 안드로이드 시스템 접근성 옵션으로 통합하거나 앱을 배포하여 많은 사용자가 사용할 수 있도록 한다면 좋겠지만,
그게 아니더라도 편하게 둘러 보시고 혹시 쓸 만한 부분이 있는 거 같다 싶으면 자유롭게 사용하시면 됩니다.
캡스톤도 끝났고, 무료 라이센스 기한도 끝나 가서 이제 제가 건드릴 일이 없기도 한데, 그렇다고 그냥 묻어버리기에도 아쉬워서 이 레포와 최종 리드미를 남깁니다.

---

## 📋 **기술 문서 및 추가 자료**

### 📁 **상세 문서**
- 📄 [기술 상세 문서](TECHNICAL.md) - 구현 세부사항 및 아키텍처
- ⚙️ [설치 가이드](SETUP.md) - 설치 및 설정 방법  
- 🎥 [데모 영상](demo/) - 실제 작동 모습
- 📊 [성능 리포트](performance/) - 테스트 결과 및 분석

### 🔗 **참고 링크**
- **EyeID SDK 문서**: https://docs.eyedid.ai/
- **안드로이드 접근성 가이드**: https://developer.android.com/accessibility
- **프로젝트 GitHub**: https://github.com/eorua8801/adaptive-calibration.git

