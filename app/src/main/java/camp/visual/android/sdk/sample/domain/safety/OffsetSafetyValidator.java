package camp.visual.android.sdk.sample.domain.safety;

import android.util.Log;

/**
 * 🔒 오프셋 안전성 검증기
 * - 극한 오프셋 값 방지
 * - 화면 크기 대비 적정 오프셋 검증
 * - 안전하지 않은 오프셋 자동 보정
 */
public class OffsetSafetyValidator {
    private static final String TAG = "OffsetSafety";
    private static final float MAX_SAFE_OFFSET = 100.0f; // 최대 100px
    private static final float EXTREME_OFFSET_THRESHOLD = 200.0f; // 극한 200px
    private static final float SCREEN_RATIO_THRESHOLD = 0.1f; // 화면의 10%
    
    public static class OffsetValidationResult {
        public final boolean isValid;
        public final String warningMessage;
        public final float recommendedX;
        public final float recommendedY;
        public final ValidationLevel level;
        
        public enum ValidationLevel {
            SAFE,        // 완전 안전
            WARNING,     // 경고 (사용 가능하지만 권장하지 않음)
            DANGEROUS,   // 위험 (자동 보정 필요)
            EXTREME      // 극한 (강제 제한)
        }
        
        public OffsetValidationResult(boolean valid, String message, float recX, float recY, ValidationLevel level) {
            this.isValid = valid;
            this.warningMessage = message;
            this.recommendedX = recX;
            this.recommendedY = recY;
            this.level = level;
        }
    }
    
    /**
     * 오프셋 안전성 종합 검증
     */
    public static OffsetValidationResult validateOffset(float offsetX, float offsetY, 
                                                       float screenWidth, float screenHeight) {
        
        float magnitude = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        
        Log.d(TAG, String.format("오프셋 검증: X=%.1f, Y=%.1f, 크기=%.1f (화면: %.0fx%.0f)", 
              offsetX, offsetY, magnitude, screenWidth, screenHeight));
        
        // 1. 극한값 체크 (절대 허용 불가)
        if (Math.abs(offsetX) > EXTREME_OFFSET_THRESHOLD || Math.abs(offsetY) > EXTREME_OFFSET_THRESHOLD) {
            float safeX = Math.signum(offsetX) * MAX_SAFE_OFFSET;
            float safeY = Math.signum(offsetY) * MAX_SAFE_OFFSET;
            
            Log.w(TAG, "극한 오프셋 감지 - 강제 제한: (" + offsetX + "," + offsetY + 
                  ") -> (" + safeX + "," + safeY + ")");
                  
            return new OffsetValidationResult(false, 
                "오프셋이 너무 큽니다. 캘리브레이션을 다시 시도해보세요.", 
                safeX, safeY, OffsetValidationResult.ValidationLevel.EXTREME);
        }
        
        // 2. 화면 비율 기준 체크
        float maxScreenRatio = Math.min(screenWidth, screenHeight) * SCREEN_RATIO_THRESHOLD;
        if (magnitude > maxScreenRatio) {
            float scale = maxScreenRatio / magnitude;
            float adjustedX = offsetX * scale;
            float adjustedY = offsetY * scale;
            
            Log.w(TAG, "화면 비율 초과 - 스케일 조정: 비율=" + scale + 
                  ", 조정후=(" + adjustedX + "," + adjustedY + ")");
                  
            return new OffsetValidationResult(false,
                "오프셋이 화면 크기에 비해 큽니다. 자동 조정됩니다.",
                adjustedX, adjustedY, OffsetValidationResult.ValidationLevel.DANGEROUS);
        }
        
        // 3. 일반적인 안전 범위 체크
        if (magnitude > MAX_SAFE_OFFSET) {
            Log.w(TAG, "권장 범위 초과 - 경고 발생: 크기=" + magnitude);
            
            return new OffsetValidationResult(true,
                "큰 오프셋입니다. 캘리브레이션 재시도를 권장합니다.",
                offsetX, offsetY, OffsetValidationResult.ValidationLevel.WARNING);
        }
        
        // 4. 중간 범위 체크 (주의)
        if (magnitude > MAX_SAFE_OFFSET * 0.7f) { // 70px 이상
            Log.i(TAG, "중간 범위 오프셋 - 주의 필요: 크기=" + magnitude);
            
            return new OffsetValidationResult(true,
                "오프셋이 다소 큽니다. 정상 동작하지만 확인해보세요.",
                offsetX, offsetY, OffsetValidationResult.ValidationLevel.WARNING);
        }
        
        // 5. 완전 안전 범위
        Log.d(TAG, "안전한 오프셋 범위: 크기=" + magnitude);
        return new OffsetValidationResult(true, null, offsetX, offsetY, 
                                        OffsetValidationResult.ValidationLevel.SAFE);
    }
    
    /**
     * 오프셋 변화량 검증 (급격한 변화 감지)
     */
    public static boolean isOffsetChangeSafe(float oldX, float oldY, float newX, float newY) {
        float changeX = Math.abs(newX - oldX);
        float changeY = Math.abs(newY - oldY);
        float changeMagnitude = (float) Math.sqrt(changeX * changeX + changeY * changeY);
        
        final float MAX_SAFE_CHANGE = 50.0f; // 한 번에 50px 이상 변화는 위험
        
        if (changeMagnitude > MAX_SAFE_CHANGE) {
            Log.w(TAG, "급격한 오프셋 변화 감지: " + changeMagnitude + "px - 안전하지 않음");
            return false;
        }
        
        Log.d(TAG, "안전한 오프셋 변화: " + changeMagnitude + "px");
        return true;
    }
    
    /**
     * 추천 오프셋 계산 (현재 오프셋을 안전 범위로 조정)
     */
    public static float[] getRecommendedOffset(float offsetX, float offsetY, 
                                             float screenWidth, float screenHeight) {
        OffsetValidationResult result = validateOffset(offsetX, offsetY, screenWidth, screenHeight);
        return new float[]{result.recommendedX, result.recommendedY};
    }
    
    /**
     * 오프셋 품질 점수 계산 (0-100)
     */
    public static int calculateOffsetQualityScore(float offsetX, float offsetY, 
                                                float screenWidth, float screenHeight) {
        float magnitude = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        
        // 기본 점수 100에서 차감 방식
        int score = 100;
        
        // 크기에 따른 차감
        if (magnitude > MAX_SAFE_OFFSET) {
            score -= 40; // 권장 범위 초과 시 40점 차감
        } else if (magnitude > MAX_SAFE_OFFSET * 0.7f) {
            score -= 20; // 70% 초과 시 20점 차감
        } else if (magnitude > MAX_SAFE_OFFSET * 0.5f) {
            score -= 10; // 50% 초과 시 10점 차감
        }
        
        // 화면 비율에 따른 차감
        float screenRatio = magnitude / Math.min(screenWidth, screenHeight);
        if (screenRatio > 0.1f) {
            score -= 30; // 화면의 10% 초과 시 30점 차감
        } else if (screenRatio > 0.05f) {
            score -= 15; // 화면의 5% 초과 시 15점 차감
        }
        
        // 비대칭성 확인 (X, Y 불균형)
        float asymmetry = Math.abs(Math.abs(offsetX) - Math.abs(offsetY));
        if (asymmetry > 30.0f) {
            score -= 15; // 심한 비대칭 시 15점 차감
        } else if (asymmetry > 15.0f) {
            score -= 5; // 약간의 비대칭 시 5점 차감
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * 오프셋 상태 설명 생성
     */
    public static String getOffsetStatusDescription(float offsetX, float offsetY, 
                                                   float screenWidth, float screenHeight) {
        OffsetValidationResult result = validateOffset(offsetX, offsetY, screenWidth, screenHeight);
        int qualityScore = calculateOffsetQualityScore(offsetX, offsetY, screenWidth, screenHeight);
        
        StringBuilder description = new StringBuilder();
        description.append("오프셋 상태: ");
        
        switch (result.level) {
            case SAFE:
                description.append("✅ 안전 (품질: ").append(qualityScore).append("점)");
                break;
            case WARNING:
                description.append("⚠️ 주의 (품질: ").append(qualityScore).append("점)");
                break;
            case DANGEROUS:
                description.append("⚠️ 위험 (품질: ").append(qualityScore).append("점)");
                break;
            case EXTREME:
                description.append("🚨 극한 (품질: ").append(qualityScore).append("점)");
                break;
        }
        
        float magnitude = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        description.append("\n크기: ").append(String.format("%.1f", magnitude)).append("px");
        
        if (result.warningMessage != null) {
            description.append("\n").append(result.warningMessage);
        }
        
        return description.toString();
    }
    
    /**
     * 디버그 정보 출력
     */
    public static void logOffsetDebugInfo(float offsetX, float offsetY, 
                                        float screenWidth, float screenHeight) {
        OffsetValidationResult result = validateOffset(offsetX, offsetY, screenWidth, screenHeight);
        int qualityScore = calculateOffsetQualityScore(offsetX, offsetY, screenWidth, screenHeight);
        
        Log.d(TAG, "=== 오프셋 디버그 정보 ===");
        Log.d(TAG, "원본 오프셋: (" + offsetX + ", " + offsetY + ")");
        Log.d(TAG, "권장 오프셋: (" + result.recommendedX + ", " + result.recommendedY + ")");
        Log.d(TAG, "검증 결과: " + result.level + " (" + (result.isValid ? "유효" : "무효") + ")");
        Log.d(TAG, "품질 점수: " + qualityScore + "/100");
        Log.d(TAG, "화면 크기: " + screenWidth + "x" + screenHeight);
        if (result.warningMessage != null) {
            Log.d(TAG, "경고 메시지: " + result.warningMessage);
        }
        Log.d(TAG, "========================");
    }
}
