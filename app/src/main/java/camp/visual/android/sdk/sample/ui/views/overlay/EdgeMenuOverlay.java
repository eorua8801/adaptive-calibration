package camp.visual.android.sdk.sample.ui.views.overlay;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public abstract class EdgeMenuOverlay extends View {
    
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
    
    // 메뉴 설정
    protected static final int BUTTON_COUNT = 4; // 기본 버튼 개수
    protected static final float MENU_RADIUS = 120f; // 메뉴 반지름 (dp) - 대폭 축소!
    protected static final float BUTTON_RADIUS = 42f; // 버튼 반지름 (dp) - 더 크게
    protected static final float CORNER_MARGIN = 100f; // 모서리 여백 (dp)
    protected static final float TRIGGER_CENTER_OFFSET = 60f; // 트리거 영역 중심 오프셋 (dp)
    protected static final float MIN_DISTANCE_FROM_EDGE = 50f; // 화면 가장자리로부터 최소 거리 (dp)
    
    protected float menuRadiusPx;
    protected float buttonRadiusPx;
    protected float cornerMarginPx;
    
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
    }
    
    protected abstract void initMenuButtons();
    
    protected void addMenuButton(String icon, String label, Runnable action) {
        menuButtons.add(new MenuButton(icon, label, action));
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
        // 실제 트리거 영역의 중심점을 계산
        float centerX, centerY;
        
        // 🔍 디버깅: 임시로 중심점을 화면 중앙 근처로 이동해서 테스트
        if (corner == Corner.LEFT_TOP) {
            // 좌상단: 임시로 더 안쪽으로 이동
            centerX = getWidth() * 0.3f;  // 30% 위치로 테스트
            centerY = getHeight() * 0.3f; // 30% 위치로 테스트
        } else {
            // 우상단: 임시로 더 안쪽으로 이동
            centerX = getWidth() * 0.7f;  // 70% 위치로 테스트
            centerY = getHeight() * 0.3f; // 30% 위치로 테스트
        }
        
        // 반투명 원형 배경 그리기 (더 부드러운 느낌)
        float backgroundRadius = menuRadiusPx * menuProgress * 0.8f;
        
        // 그라데이션 효과를 위한 여러 원 그리기
        for (int i = 0; i < 3; i++) {
            float radius = backgroundRadius * (1f - i * 0.15f);
            int alpha = (int)(120 * (1f - i * 0.3f) * menuProgress);
            backgroundPaint.setColor(Color.argb(alpha, 0, 0, 0));
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint);
        }
    }
    
    private void drawMenuButtons(Canvas canvas) {
        // 실제 트리거 영역의 중심점을 계산
        float centerX, centerY;
        
        // 🔍 디버깅: 임시로 중심점을 화면 중앙 근처로 이동해서 테스트
        if (corner == Corner.LEFT_TOP) {
            // 좌상단: 임시로 더 안쪽으로 이동
            centerX = getWidth() * 0.3f;  // 30% 위치로 테스트
            centerY = getHeight() * 0.3f; // 30% 위치로 테스트
        } else {
            // 우상단: 임시로 더 안쪽으로 이동
            centerX = getWidth() * 0.7f;  // 70% 위치로 테스트
            centerY = getHeight() * 0.3f; // 30% 위치로 테스트
        }
        
        float minDistancePx = MIN_DISTANCE_FROM_EDGE * getResources().getDisplayMetrics().density;
        
        for (int i = 0; i < menuButtons.size(); i++) {
            MenuButton button = menuButtons.get(i);
            
            // 버튼 위치 계산 (더 넓은 간격으로 원형 배치)
            float angle = calculateButtonAngle(i);
            float distance = menuRadiusPx * 0.45f * 1.0f; // 🔍 디버깅: menuProgress를 1.0으로 고정해서 테스트
            
            float buttonX = centerX + (float) Math.cos(Math.toRadians(angle)) * distance;
            float buttonY = centerY + (float) Math.sin(Math.toRadians(angle)) * distance;
            
            // 🔍 디버깅: 버튼 위치 로그
            android.util.Log.d("EdgeMenuOverlay", String.format(
                "Button[%d] %s: angle=%.1f°, center=(%.1f,%.1f), distance=%.1f, pos=(%.1f,%.1f)", 
                i, button.label, angle, centerX, centerY, distance, buttonX, buttonY));
            
            // 화면 경계 체크 및 조정 (강력한 안전장치)
            float safeMargin = minDistancePx + buttonRadiusPx;
            buttonX = Math.max(safeMargin, Math.min(getWidth() - safeMargin, buttonX));
            buttonY = Math.max(safeMargin, Math.min(getHeight() - safeMargin - 80f, buttonY)); // 라벨 공간 충분히 확보
            
            // 버튼 배경 그리기 (더 세련된 스타일)
            float currentButtonRadius = buttonRadiusPx * 1.0f; // 🔍 디버깅: menuProgress를 1.0으로 고정해서 테스트
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
            
            // 라벨 그리기 (버튼 아래, 더 적절한 간격)
            float labelY = buttonY + currentButtonRadius + textPaint.getTextSize() + 15f;
            if (button == hoveredButton && hoverProgress > 0f) {
                // 호버 중일 때 라벨 색상 변화
                int alpha = (int)(255 * (0.8f + 0.2f * hoverProgress));
                textPaint.setColor(Color.argb(alpha, 100, 150, 255));
            } else {
                textPaint.setColor(Color.argb(200, 255, 255, 255));
            }
            canvas.drawText(button.label, buttonX, labelY, textPaint);
            
            // 버튼 위치 저장 (클릭 감지용)
            button.centerX = buttonX;
            button.centerY = buttonY;
            button.radius = currentButtonRadius;
        }
    }
    
    private float calculateButtonAngle(int buttonIndex) {
        // 종료 버튼(135도)을 기준으로 반구를 따라 더 넓은 간격으로 배치
        if (corner == Corner.LEFT_TOP) {
            // 좌상단: 종료 버튼(135도)을 기준으로 더 넓은 간격
            switch (buttonIndex) {
                case 0: return 30f;  // 뒤로: 105도 차이
                case 1: return 60f;  // 홈: 75도 차이
                case 2: return 90f;  // 최근: 45도 차이
                case 3: return 135f; // 종료: 기준점
                default: return 45f;
            }
        } else {
            // 우상단: 대칭적 배치
            switch (buttonIndex) {
                case 0: return 150f; // 뒤로
                case 1: return 120f; // 홈
                case 2: return 90f;  // 최근
                case 3: return 45f;  // 종료
                default: return 135f;
            }
        }
    }
    
    private void drawCancelProgress(Canvas canvas) {
        // 실제 트리거 영역의 중심점을 계산
        float centerX, centerY;
        
        // 🔍 디버깅: 임시로 중심점을 화면 중앙 근처로 이동해서 테스트
        if (corner == Corner.LEFT_TOP) {
            // 좌상단: 임시로 더 안쪽으로 이동
            centerX = getWidth() * 0.3f;  // 30% 위치로 테스트
            centerY = getHeight() * 0.3f; // 30% 위치로 테스트
        } else {
            // 우상단: 임시로 더 안쪽으로 이동
            centerX = getWidth() * 0.7f;  // 70% 위치로 테스트
            centerY = getHeight() * 0.3f; // 30% 위치로 테스트
        }
        
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
