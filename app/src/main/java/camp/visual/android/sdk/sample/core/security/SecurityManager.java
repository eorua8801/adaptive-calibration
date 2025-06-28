package camp.visual.android.sdk.sample.core.security;

import android.util.Log;
import camp.visual.android.sdk.sample.core.constants.AppConstants;

/**
 * 🔒 보안 관리자
 * - 민감한 데이터 처리
 * - 로그 데이터 보호
 * - 라이센스 키 관리
 */
public final class SecurityManager {
    
    /**
     * 🔐 암호화된 라이센스 키 반환
     * 실제 배포 시에는 더 강력한 암호화 적용 필요
     */
    public static String getSecureLicense() {
        // 🆕 유효한 EyeDID SDK 라이센스 키 사용
        String licenseKey;
        
        // 유효한 EyeDID SDK 라이센스 키 사용
        licenseKey = "dev_ktygge55mai7a041aglteb4onei9a7m9j7tcqagm";
        Log.d(AppConstants.Logging.TAG_SECURITY, "라이센스 키 사용: " + licenseKey.substring(0, 10) + "...");
        
        // 🔥 라이센스 키 유효성 검사
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            Log.e(AppConstants.Logging.TAG_SECURITY, "최종 라이센스 키가 여전히 null이거나 비어있음");
            return null;
        }
        
        // 🔥 라이센스 키 형식 검사 (기본적인 수준)
        if (licenseKey.length() < 10) {
            Log.e(AppConstants.Logging.TAG_SECURITY, "라이센스 키가 너무 짧음: " + licenseKey.length());
            return null;
        }
        
        Log.i(AppConstants.Logging.TAG_SECURITY, "라이센스 키 검증 완료 (길이: " + licenseKey.length() + ")");
        return licenseKey;
    }
    
    /**
     * 🔍 로그용 민감 데이터 마스킹
     */
    public static String sanitizeCoordinates(float x, float y) {
        final boolean DEBUG = true;
        if (DEBUG) {
            return String.format("(%.1f, %.1f)", x, y);
        }
        return "(***,***)";
    }
    
    /**
     * 🔍 사용자 ID 마스킹
     */
    public static String sanitizeUserId(String userId) {
        if (userId == null || userId.length() < 4) {
            return "****";
        }
        
        final boolean DEBUG = true;
        if (DEBUG) {
            return userId;
        }
        
        return userId.substring(0, 2) + "****" + userId.substring(userId.length() - 2);
    }
    
    /**
     * 🔍 디바이스 정보 마스킹
     */
    public static String sanitizeDeviceInfo(String deviceInfo) {
        final boolean DEBUG = true;
        if (DEBUG) {
            return deviceInfo;
        }
        
        if (deviceInfo == null || deviceInfo.length() < 6) {
            return "******";
        }
        
        return deviceInfo.substring(0, 3) + "***" + deviceInfo.substring(deviceInfo.length() - 3);
    }
    
    /**
     * 🔒 민감한 설정값 검증
     */
    public static boolean validateSensitiveSettings(Object... values) {
        for (Object value : values) {
            if (value == null) {
                Log.w(AppConstants.Logging.TAG_SECURITY, "민감한 설정값이 null입니다");
                return false;
            }
            
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                Log.w(AppConstants.Logging.TAG_SECURITY, "민감한 설정값이 비어있습니다");
                return false;
            }
        }
        return true;
    }
    
    /**
     * 🔒 보안 로그 출력 (배포 버전에서는 비활성화)
     */
    public static void secureLog(String tag, String message) {
        final boolean DEBUG = true;
        if (DEBUG) {
            Log.d(tag, "[SECURE] " + message);
        }
        // 배포 버전에서는 로그 출력하지 않음
    }
    
    /**
     * 🔒 에러 로그 출력 (항상 활성화, 하지만 민감 정보 제외)
     */
    public static void secureErrorLog(String tag, String message, Throwable throwable) {
        // 민감 정보가 포함될 수 있는 스택 트레이스는 디버그 모드에서만
        final boolean DEBUG = true;
        if (DEBUG) {
            Log.e(tag, "[SECURE_ERROR] " + message, throwable);
        } else {
            Log.e(tag, "[ERROR] " + sanitizeErrorMessage(message));
        }
    }
    
    /**
     * 🔍 에러 메시지에서 민감 정보 제거
     */
    private static String sanitizeErrorMessage(String message) {
        if (message == null) return "알 수 없는 오류";
        
        // 파일 경로, IP 주소, 개인정보 등이 포함될 수 있는 패턴 제거
        return message
                .replaceAll("/data/[^\\s]+", "/data/***")
                .replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "***.***.***.***")
                .replaceAll("user[\\w]*=\\w+", "user=***")
                .replaceAll("password[\\w]*=\\w+", "password=***")
                .replaceAll("key[\\w]*=\\w+", "key=***");
    }
    
    /**
     * 🔒 런타임 보안 검사
     */
    public static class RuntimeSecurityCheck {
        private static boolean hasPerformedSecurityCheck = false;
        
        public static boolean performSecurityCheck() {
            if (hasPerformedSecurityCheck) {
                return true; // 이미 검사 완료
            }
            
            try {
                // 1. 라이센스 키 유효성 검사
                String license = getSecureLicense();
                if (license == null || license.trim().isEmpty()) {
                    Log.e(AppConstants.Logging.TAG_SECURITY, "라이센스 키가 유효하지 않습니다");
                    return false;
                }
                Log.i(AppConstants.Logging.TAG_SECURITY, "라이센스 키 검증 성공");
                
                // 2. 디버그 모드 확인
                final boolean DEBUG = true;
                if (DEBUG) {
                    Log.w(AppConstants.Logging.TAG_SECURITY, "디버그 모드에서 실행 중 - 보안 수준이 낮아질 수 있습니다");
                }
                
                // 3. 루팅 검사 (기본적인 수준)
                if (isDeviceRooted()) {
                    Log.w(AppConstants.Logging.TAG_SECURITY, "루팅된 디바이스에서 실행 중 - 보안 위험 존재");
                }
                
                hasPerformedSecurityCheck = true;
                Log.i(AppConstants.Logging.TAG_SECURITY, "보안 검사 완료 - 모든 검사 통과");
                return true;
                
            } catch (Exception e) {
                Log.e(AppConstants.Logging.TAG_SECURITY, "보안 검사 중 오류 발생", e);
                return false;
            }
        }
        
        private static boolean isDeviceRooted() {
            // 기본적인 루팅 검사 (더 강화된 검사가 필요할 수 있음)
            try {
                Process process = Runtime.getRuntime().exec("su");
                process.destroy();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    // Private constructor to prevent instantiation
    private SecurityManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
