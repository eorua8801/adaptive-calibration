// 📝 EdgeMenuOverlay.java 수정 제안 사항
// 다음 메서드들을 수정하여 버튼 배치 문제를 해결할 수 있습니다.

// 1. 메뉴 중심점을 화면 중앙으로 이동 (현재: 화면 높이 1/4 → 수정: 화면 높이 중앙)
private float[] getMenuCenter() {
    float centerX, centerY;
    
    if (corner == Corner.LEFT_TOP) {
        // 좌상단: 화면 왼쪽 가장자리, 높이의 중앙 지점으로 수정
        centerX = 0f;
        centerY = getHeight() / 2f; // 🔧 1/4f → 1/2f로 변경
    } else {
        // 우상단: 화면 오른쪽 가장자리, 높이의 중앙 지점으로 수정  
        centerX = getWidth();
        centerY = getHeight() / 2f; // 🔧 1/4f → 1/2f로 변경
    }
    
    return new float[]{centerX, centerY};
}

// 2. 배경 중심점도 동일하게 수정
private float[] getBackgroundCenter() {
    if (corner == Corner.LEFT_TOP) {
        return new float[]{0f, getHeight() / 2f}; // 🔧 1/4f → 1/2f로 변경
    } else {
        return new float[]{getWidth(), getHeight() / 2f}; // 🔧 1/4f → 1/2f로 변경
    }
}

// 3. 버튼 배치 각도를 더 중앙 중심으로 조정 (2시~10시 범위로 변경)
private AngleData calculatePreciseButtonPosition(int buttonIndex) {
    if (corner == Corner.LEFT_TOP) {
        // 좌상단 메뉴: 2시~8시 방향 배치로 수정 (더 중앙 중심적)
        switch (buttonIndex) {
            case 0: return new AngleData(-30f, 0.866f, -0.5f);    // 2시 방향
            case 1: return new AngleData(0f, 1f, 0f);             // 3시 방향  
            case 2: return new AngleData(30f, 0.866f, 0.5f);      // 4시 방향
            case 3: return new AngleData(60f, 0.5f, 0.866f);      // 5시 방향
            default: return new AngleData(0f, 1f, 0f);
        }
    } else {
        // 우상단 메뉴: 4시~10시 방향 배치로 수정 (대칭)
        switch (buttonIndex) {
            case 0: return new AngleData(-150f, -0.866f, -0.5f);  // 10시 방향
            case 1: return new AngleData(-180f, -1f, 0f);         // 9시 방향
            case 2: return new AngleData(-210f, -0.866f, 0.5f);   // 8시 방향
            case 3: return new AngleData(-240f, -0.5f, 0.866f);   // 7시 방향
            default: return new AngleData(180f, -1f, 0f);
        }
    }
}

// 🎯 추가 제안: 메뉴 반지름을 약간 줄여서 버튼들이 화면 안에 잘 들어오도록 조정
protected static final float MENU_RADIUS = 70f; // 기존 80f → 70f로 축소

// 🎯 선택사항: 메뉴 위치를 더 세밀하게 조정하고 싶다면
private float[] getMenuCenter() {
    float centerX, centerY;
    
    if (corner == Corner.LEFT_TOP) {
        centerX = 0f;
        // 화면 높이의 40~60% 지점 중 선택 (현재는 중앙인 50%)
        centerY = getHeight() * 0.45f; // 약간 위쪽으로 조정
    } else {
        centerX = getWidth();
        centerY = getHeight() * 0.45f; // 약간 위쪽으로 조정
    }
    
    return new float[]{centerX, centerY};
}
