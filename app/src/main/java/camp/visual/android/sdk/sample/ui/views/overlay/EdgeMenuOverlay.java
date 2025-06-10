package camp.visual.android.sdk.sample.ui.views.overlay;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public abstract class EdgeMenuOverlay extends View {
    
    private static final String TAG = "EdgeMenuOverlay";
    
    public enum MenuState {
        HIDDEN, SHOWING, CANCELING
    }
    
    public enum Corner {
        LEFT_TOP, RIGHT_TOP
    }
    
    protected final Paint backgroundPaint;
    protected final Paint buttonPaint;
    protected final Paint iconPaint;
    protected final Paint textPaint;
    protected final Paint progressPaint;
    
    protected MenuState currentState = MenuState.HIDDEN;
    protected Corner corner;
    protected List<MenuButton> menuButtons = new ArrayList<>();
    protected MenuButton hoveredButton = null;
    protected float menuProgress = 0f; // 0.0 = 완전 숨김, 1.0 = 완전 표시
    protected float cancelProgress = 0f; // 취소 진행률
    protected float hoverProgress = 0f; // 호버 클릭 진행률 (0.0~1.0)
    
    // 메뉴 설정 (HTML과 동일한 값들)
    protected static final int BUTTON_COUNT = 4; // 기본 버튼 개수
    protected static final float MENU_RADIUS = 80f; // 메뉴 반지름 (dp) - HTML과 동일
    protected static final float BUTTON_SIZE = 50f; // 버튼 크기 (dp) - HTML과 동일
    protected static final float BUTTON_RADIUS = 25f; // 버튼 반지름 (dp) - BUTTON_SIZE/2
    protected static final float CORNER_MARGIN = 50f; // 모서리 여백 (dp)
    protected static final float TRIGGER_CENTER_OFFSET = 60f; // 트리거 영역 중심 오프셋 (dp)
    protected static final float MIN_DISTANCE_FROM_EDGE = 50f; // 화면 가장자리로부터 최소 거리 (dp)
    
    // 🆕 원형 메뉴 특수 설정
    protected static final boolean ENABLE_SAFE_MARGIN = false; // 원형 메뉴에서는 safeMargin 비활성화
    
    protected float menuRadiusPx;
    protected float buttonRadiusPx;
    protected float cornerMarginPx;
    protected float minDistancePx;
    
    public EdgeMenuOverlay(Context context, Corner corner) {
        super(context);
        this.corner = corner;
        
        // Paint 객체들 초기화
        // 반투명 배경
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.argb(120, 0, 0, 0));
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        // 버튼 배경
        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setColor(Color.argb(200, 255, 255, 255));
        buttonPaint.setStyle(Paint.Style.FILL);
        
        // 아이콘
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(Color.argb(255, 60, 60, 60));
        iconPaint.setTextAlign(Paint.Align.CENTER);
        iconPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics()));
        
        // 텍스트
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.argb(180, 255, 255, 255));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        
        // 진행률 (취소용)
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.argb(150, 255, 100, 100));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(8f);
        
        initDimensions();
        initMenuButtons();
    }
    
    private void initDimensions() {
        float density = getResources().getDisplayMetrics().density;
        menuRadiusPx = MENU_RADIUS * density;
        buttonRadiusPx = BUTTON_RADIUS * density;
        cornerMarginPx = CORNER_MARGIN * density;
        minDistancePx = MIN_DISTANCE_FROM_EDGE * density;
    }
    
    protected abstract void initMenuButtons();
    
    protected void addMenuButton(String icon, String label, Runnable action) {
        menuButtons.add(new MenuButton(icon, label, action));
    }
    
    // 공통 메뉴 중심점 계산 (HTML과 정확히 동일하게 수정)
    private float[] getMenuCenter() {
        float centerX, centerY;
        
        if (corner == Corner.LEFT_TOP) {
            // 좌상단: 화면 왼쪽 가장자리, 높이의 25% 지점
            centerX = 0f;
            centerY = getHeight() / 4f;
        } else {
            // 우상단: 화면 오른쪽 가장자리, 높이의 25% 지점  
            centerX = getWidth();
            centerY = getHeight() / 4f;
        }
        
        return new float[]{centerX, centerY};
    }

    // 반원 효과를 위한 배경 중심점 (회색 원이 화면 테두리에 위치)
    private float[] getBackgroundCenter() {
        if (corner == Corner.LEFT_TOP) {
            return new float[]{0f, getHeight() / 4f}; // 왼쪽 테두리
        } else {
            return new float[]{getWidth(), getHeight() / 4f}; // 오른쪽 테두리
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (currentState == MenuState.HIDDEN || menuProgress <= 0f) {
            return;
        }
        
        drawBackground(canvas);
        drawMenuButtons(canvas);
        
        if (currentState == MenuState.CANCELING) {
            drawCancelProgress(canvas);
        }
        
        // 호버 진행률 표시
        if (hoveredButton != null && hoverProgress > 0f) {
            drawHoverProgress(canvas);
        }
    }
    
    private void drawBackground(Canvas canvas) {
        float[] bgCenter = getBackgroundCenter();
        float centerX = bgCenter[0];
        float centerY = bgCenter[1];
        
        // MD 가이드: 반투명 회색 원이 화면 테두리에서 반만 나타남
        float radius = menuRadiusPx * menuProgress;
        
        // 메인 배경 원 (반투명 회색)
        Paint mainBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainBgPaint.setColor(Color.argb(120, 128, 128, 128)); // MD 가이드: 반투명 회색
        canvas.drawCircle(centerX, centerY, radius, mainBgPaint);
        
        // 더 부드러운 테두리 효과
        Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setColor(Color.argb(60, 128, 128, 128));
        canvas.drawCircle(centerX, centerY, radius * 1.1f, edgePaint);
        
        // 안쪽 그라데이션 효과
        Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setColor(Color.argb(80, 160, 160, 160));
        canvas.drawCircle(centerX, centerY, radius * 0.7f, innerPaint);
    }
    
    // MD 가이드의 정확한 삼각함수 값 테이블 구현
    private AngleData calculatePreciseButtonPosition(int buttonIndex) {
        if (corner == Corner.LEFT_TOP) {
            // 좌상단 메뉴: 1시~5시 방향 배치 (정확한 cos/sin 값)
            switch (buttonIndex) {
                case 0: return new AngleData(-67.5f, 0.383f, -0.924f);  // 12.5시 방향
                case 1: return new AngleData(-22.5f, 0.924f, -0.383f);  // 1.5시 방향  
                case 2: return new AngleData(22.5f, 0.924f, 0.383f);    // 2.5시 방향
                case 3: return new AngleData(67.5f, 0.383f, 0.924f);    // 5.5시 방향
                default: return new AngleData(0f, 1f, 0f);
            }
        } else {
            // 우상단 메뉴: 7시~11시 방향 배치 (대칭)
            switch (buttonIndex) {
                case 0: return new AngleData(-112.5f, -0.383f, -0.924f); // 11.5시 방향
                case 1: return new AngleData(-157.5f, -0.924f, 0.383f);  // 9.5시 방향
                case 2: return new AngleData(-202.5f, -0.924f, -0.383f); // 10.5시 방향
                case 3: return new AngleData(112.5f, -0.383f, 0.924f);   // 7.5시 방향
                default: return new AngleData(180f, -1f, 0f);
            }
        }
    }

    private static class AngleData {
        final float angle;
        final float cosValue;
        final float sinValue;
        
        AngleData(float angle, float cosValue, float sinValue) {
            this.angle = angle;
            this.cosValue = cosValue;
            this.sinValue = sinValue;
        }
    }
    
    private void drawMenuButtons(Canvas canvas) {
        float[] center = getMenuCenter();
        float centerX = center[0];
        float centerY = center[1];
        
        for (int i = 0; i < menuButtons.size(); i++) {
            MenuButton button = menuButtons.get(i);
            
            // 정확한 버튼 위치 계산
            AngleData angleData = calculatePreciseButtonPosition(i);
            
            // HTML과 동일한 고정 거리 사용 (80dp, 애니메이션 무관)
            float distance = MENU_RADIUS * getResources().getDisplayMetrics().density; // 80dp 고정
            
            // MD 가이드 공식: center + (radius * cos(angle), radius * sin(angle))
            float buttonX = centerX + distance * angleData.cosValue;
            float buttonY = centerY + distance * angleData.sinValue;
            
            // 버튼은 중심점 기준으로 그려지므로 추가 오프셋 불필요
            // (HTML과 달리 Android Canvas는 중심점 기준 드로잉)
            
            // 🔧 중요: 원형 메뉴에서는 안전장치 비활성화 (반원 효과를 위해)
            if (ENABLE_SAFE_MARGIN) {
                // 일반 UI에서는 안전장치 적용
                float safeMargin = minDistancePx + buttonRadiusPx;
                buttonX = Math.max(safeMargin, Math.min(getWidth() - safeMargin, buttonX));
                buttonY = Math.max(safeMargin, Math.min(getHeight() - safeMargin - 80f, buttonY));
            } else {
                // 원형 메뉴에서는 안전장치 비활성화
                Log.d(TAG, "원형 메뉴 모드: 안전장치 비활성화");
            }
            
            drawSingleButton(canvas, button, buttonX, buttonY, i);
        }
    }
    
    private void drawSingleButton(Canvas canvas, MenuButton button, float buttonX, float buttonY, int index) {
        // 버튼 배경 그리기 (더 세련된 스타일)
        float currentButtonRadius = buttonRadiusPx * menuProgress;
        float shadowOffset = 4f * getResources().getDisplayMetrics().density;
        
        if (button == hoveredButton) {
            // 호버 시 그림자 효과
            Paint shadowPaint = new Paint(buttonPaint);
            shadowPaint.setColor(Color.argb(80, 0, 0, 0));
            canvas.drawCircle(buttonX + shadowOffset, buttonY + shadowOffset, 
                            currentButtonRadius * 1.3f, shadowPaint);
            
            buttonPaint.setColor(Color.argb(255, 100, 150, 255)); // 호버 시 진한 파란색
            currentButtonRadius *= 1.3f; // 크기 확대
        } else {
            // 일반 상태 그림자
            Paint shadowPaint = new Paint(buttonPaint);
            shadowPaint.setColor(Color.argb(50, 0, 0, 0));
            canvas.drawCircle(buttonX + shadowOffset/2, buttonY + shadowOffset/2, 
                            currentButtonRadius, shadowPaint);
            
            buttonPaint.setColor(Color.argb(230, 255, 255, 255)); // 기본 흰색
        }
        
        canvas.drawCircle(buttonX, buttonY, currentButtonRadius, buttonPaint);
        
        // 아이콘 그리기 (크기 조정)
        float iconSize = iconPaint.getTextSize();
        if (button == hoveredButton) {
            iconPaint.setTextSize(iconSize * 1.1f);
        }
        float iconY = buttonY + iconPaint.getTextSize() * 0.35f;
        canvas.drawText(button.icon, buttonX, iconY, iconPaint);
        iconPaint.setTextSize(iconSize); // 원래 크기로 복원
        
        // 라벨 그리기 (위치별 배치)
        drawButtonLabel(canvas, button, buttonX, buttonY, currentButtonRadius);
        
        // 버튼 위치 저장 (클릭 감지용)
        button.centerX = buttonX;
        button.centerY = buttonY;
        button.radius = currentButtonRadius;
    }
    
    private void drawButtonLabel(Canvas canvas, MenuButton button, float buttonX, float buttonY, float buttonRadius) {
        float textGapPx = 8f * getResources().getDisplayMetrics().density; // MD 가이드 텍스트 간격
        
        float labelX, labelY;
        
        if (corner == Corner.LEFT_TOP) {
            // 좌측 메뉴: 텍스트를 버튼 오른쪽에 배치 (MD 가이드)
            labelX = buttonX + buttonRadius + textGapPx;
            labelY = buttonY + textPaint.getTextSize() / 3f;
            textPaint.setTextAlign(Paint.Align.LEFT);
        } else {
            // 우측 메뉴: 텍스트를 버튼 왼쪽에 배치 (MD 가이드)
            labelX = buttonX - buttonRadius - textGapPx;
            labelY = buttonY + textPaint.getTextSize() / 3f;
            textPaint.setTextAlign(Paint.Align.RIGHT);
        }
        
        // 화면 경계 체크
        if (labelX < 20f || labelX > getWidth() - 100f) {
            // 텍스트가 화면 밖으로 나가면 버튼 아래에 배치
            labelX = buttonX;
            labelY = buttonY + buttonRadius + textPaint.getTextSize() + textGapPx;
            textPaint.setTextAlign(Paint.Align.CENTER);
        }
        
        // 호버 중일 때 라벨 색상 변화
        if (button == hoveredButton && hoverProgress > 0f) {
            int alpha = (int)(255 * (0.8f + 0.2f * hoverProgress));
            textPaint.setColor(Color.argb(alpha, 100, 150, 255));
        } else {
            textPaint.setColor(Color.argb(200, 255, 255, 255));
        }
        
        canvas.drawText(button.label, labelX, labelY, textPaint);
    }
    
    private void drawCancelProgress(Canvas canvas) {
        float[] center = getMenuCenter();
        float centerX = center[0];
        float centerY = center[1];
        
        // 취소 진행률을 원형으로 표시 (더 세련된 스타일)
        float progressRadius = menuRadiusPx * 0.85f;
        RectF oval = new RectF(centerX - progressRadius, centerY - progressRadius,
                               centerX + progressRadius, centerY + progressRadius);
        
        // 360도 전체 원으로 취소 진행률 표시
        float sweepAngle = 360 * cancelProgress;
        
        // 진행률에 따라 색상 변화 (빨간색으로 점진적 변화)
        int alpha = (int)(200 * cancelProgress);
        int red = 255;
        int green = (int)(255 * (1f - cancelProgress));
        int blue = (int)(100 * (1f - cancelProgress));
        
        progressPaint.setColor(Color.argb(alpha, red, green, blue));
        progressPaint.setStrokeWidth(12f * getResources().getDisplayMetrics().density);
        
        canvas.drawArc(oval, -90, sweepAngle, false, progressPaint);
    }
    
    private void drawHoverProgress(Canvas canvas) {
        if (hoveredButton == null || hoverProgress <= 0f) return;
        
        // 호버된 버튼 주위에 진행률 원 그리기
        float centerX = hoveredButton.centerX;
        float centerY = hoveredButton.centerY;
        float progressRadius = hoveredButton.radius + 8f;
        
        // 호버 진행률 원형 게이지
        RectF oval = new RectF(centerX - progressRadius, centerY - progressRadius,
                               centerX + progressRadius, centerY + progressRadius);
        
        float sweepAngle = 360 * hoverProgress;
        
        // 진행률에 따라 색상 변화 (파란색 → 초록색)
        int alpha = (int)(200 * hoverProgress);
        int red = (int)(100 * (1f - hoverProgress));
        int green = (int)(150 + 105 * hoverProgress);
        int blue = (int)(255 * (1f - hoverProgress * 0.3f));
        
        Paint hoverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hoverPaint.setColor(Color.argb(alpha, red, green, blue));
        hoverPaint.setStyle(Paint.Style.STROKE);
        hoverPaint.setStrokeWidth(6f * getResources().getDisplayMetrics().density);
        hoverPaint.setStrokeCap(Paint.Cap.ROUND);
        
        canvas.drawArc(oval, -90, sweepAngle, false, hoverPaint);
    }
    
    public void showMenu() {
        if (currentState != MenuState.HIDDEN) return;
        
        currentState = MenuState.SHOWING;
        
        ObjectAnimator progressAnimator = ObjectAnimator.ofFloat(this, "menuProgress", 0f, 1f);
        progressAnimator.setDuration(300);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.start();
    }
    
    public void hideMenu() {
        if (currentState == MenuState.HIDDEN) return;
        
        ObjectAnimator progressAnimator = ObjectAnimator.ofFloat(this, "menuProgress", menuProgress, 0f);
        progressAnimator.setDuration(200);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                currentState = MenuState.HIDDEN;
                cancelProgress = 0f;
                hoveredButton = null;
            }
        });
        progressAnimator.start();
    }
    
    public void startCanceling() {
        if (currentState == MenuState.SHOWING) {
            currentState = MenuState.CANCELING;
            cancelProgress = 0f;
        }
    }
    
    public void updateCancelProgress(float progress) {
        if (currentState == MenuState.CANCELING) {
            cancelProgress = Math.max(0f, Math.min(1f, progress));
            invalidate();
            
            if (cancelProgress >= 1f) {
                hideMenu();
            }
        }
    }
    
    public void cancelCanceling() {
        if (currentState == MenuState.CANCELING) {
            currentState = MenuState.SHOWING;
            cancelProgress = 0f;
            invalidate();
        }
    }
    
    public MenuButton getButtonAt(float x, float y) {
        if (currentState != MenuState.SHOWING) return null;
        
        for (MenuButton button : menuButtons) {
            float distance = (float) Math.sqrt(
                Math.pow(x - button.centerX, 2) + Math.pow(y - button.centerY, 2)
            );
            if (distance <= button.radius) {
                return button;
            }
        }
        return null;
    }
    
    public void setHoveredButton(MenuButton button) {
        if (hoveredButton != button) {
            hoveredButton = button;
            invalidate();
        }
    }
    
    public boolean isVisible() {
        return currentState != MenuState.HIDDEN;
    }
    
    public MenuState getCurrentState() {
        return currentState;
    }
    
    // 애니메이션용 setter
    public void setMenuProgress(float progress) {
        this.menuProgress = progress;
        invalidate();
    }
    
    public float getMenuProgress() {
        return menuProgress;
    }
    
    // 호버 진행률 설정 (EdgeMenuManager에서 호출)
    public void setHoverProgress(float progress) {
        this.hoverProgress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }
    
    public float getHoverProgress() {
        return hoverProgress;
    }
    
    public static class MenuButton {
        public String icon;
        public String label;
        public Runnable action;
        
        // 클릭 감지용 (그리기 후 설정됨)
        public float centerX;
        public float centerY;
        public float radius;
        
        public MenuButton(String icon, String label, Runnable action) {
            this.icon = icon;
            this.label = label;
            this.action = action;
        }
        
        public void execute() {
            if (action != null) {
                action.run();
            }
        }
    }
}
