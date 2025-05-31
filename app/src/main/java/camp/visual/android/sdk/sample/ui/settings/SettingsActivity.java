package camp.visual.android.sdk.sample.ui.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import camp.visual.android.sdk.sample.R;
import camp.visual.android.sdk.sample.data.settings.SettingsRepository;
import camp.visual.android.sdk.sample.data.settings.SharedPrefsSettingsRepository;
import camp.visual.android.sdk.sample.domain.model.OneEuroFilterPreset;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.service.tracking.GazeTrackingService;

public class SettingsActivity extends AppCompatActivity {

    private SettingsRepository settingsRepository;
    private UserSettings currentSettings;

    // 🎯 새로운 캘리브레이션 전략 UI 요소들
    private RadioGroup calibrationStrategyRadioGroup;
    private RadioButton radioQuickStart;
    private RadioButton radioBalancedCal;  // 이름 충돌 방지
    private RadioButton radioPrecision;
    private Switch backgroundLearningSwitch;
    private TextView strategyDescriptionText;

    // 기존 UI 요소들
    private SeekBar fixationDurationBar;
    private TextView fixationDurationText;
    private SeekBar aoiRadiusBar;
    private TextView aoiRadiusText;
    private SeekBar edgeTriggerTimeBar;
    private TextView edgeTriggerTimeText;
    private SeekBar scrollCountBar;
    private TextView scrollCountText;

    // 커서 오프셋 UI 요소
    private SeekBar cursorOffsetXBar;
    private TextView cursorOffsetXText;
    private SeekBar cursorOffsetYBar;
    private TextView cursorOffsetYText;

    // OneEuroFilter 프리셋 UI 요소
    private RadioGroup filterPresetRadioGroup;
    private RadioButton radioStability;
    private RadioButton radioBalancedStability;
    private RadioButton radioBalanced;
    private RadioButton radioBalancedResponsive;
    private RadioButton radioResponsive;
    private RadioButton radioCustom;
    private LinearLayout customFilterLayout;

    // OneEuroFilter 커스텀 UI 요소
    private SeekBar oneEuroFreqBar;
    private TextView oneEuroFreqText;
    private SeekBar oneEuroMinCutoffBar;
    private TextView oneEuroMinCutoffText;
    private SeekBar oneEuroBetaBar;
    private TextView oneEuroBetaText;
    private SeekBar oneEuroDCutoffBar;
    private TextView oneEuroDCutoffText;

    private Switch clickEnabledSwitch;
    private Switch scrollEnabledSwitch;
    private Switch edgeScrollEnabledSwitch;
    private Switch blinkDetectionSwitch;
    private Switch autoOnePointCalibrationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 액션바 설정
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("시선 추적 설정");
        }

        // 설정 저장소 초기화
        settingsRepository = new SharedPrefsSettingsRepository(this);
        currentSettings = settingsRepository.getUserSettings();

        // UI 초기화
        initViews();
        loadSettings();
        setupListeners();

        // 🎯 개선된 안내 시스템
        showCursorOffsetInfo();
    }

    private void initViews() {
        // 🎯 캘리브레이션 전략 UI 초기화 (새로 추가)
        calibrationStrategyRadioGroup = findViewById(R.id.radio_group_calibration_strategy);
        radioQuickStart = findViewById(R.id.radio_quick_start);
        radioBalancedCal = findViewById(R.id.radio_balanced_cal);
        radioPrecision = findViewById(R.id.radio_precision);
        backgroundLearningSwitch = findViewById(R.id.switch_background_learning);
        strategyDescriptionText = findViewById(R.id.text_strategy_description);

        // 기존 SeekBar와 TextView 초기화
        fixationDurationBar = findViewById(R.id.seekbar_fixation_duration);
        fixationDurationText = findViewById(R.id.text_fixation_duration);
        aoiRadiusBar = findViewById(R.id.seekbar_aoi_radius);
        aoiRadiusText = findViewById(R.id.text_aoi_radius);
        edgeTriggerTimeBar = findViewById(R.id.seekbar_edge_trigger_time);
        edgeTriggerTimeText = findViewById(R.id.text_edge_trigger_time);
        scrollCountBar = findViewById(R.id.seekbar_scroll_count);
        scrollCountText = findViewById(R.id.text_scroll_count);

        // 커서 오프셋 UI 초기화
        cursorOffsetXBar = findViewById(R.id.seekbar_cursor_offset_x);
        cursorOffsetXText = findViewById(R.id.text_cursor_offset_x);
        cursorOffsetYBar = findViewById(R.id.seekbar_cursor_offset_y);
        cursorOffsetYText = findViewById(R.id.text_cursor_offset_y);

        // OneEuroFilter 프리셋 UI 초기화
        filterPresetRadioGroup = findViewById(R.id.radio_group_filter_preset);
        radioStability = findViewById(R.id.radio_stability);
        radioBalancedStability = findViewById(R.id.radio_balanced_stability);
        radioBalanced = findViewById(R.id.radio_balanced);
        radioBalancedResponsive = findViewById(R.id.radio_balanced_responsive);
        radioResponsive = findViewById(R.id.radio_responsive);
        radioCustom = findViewById(R.id.radio_custom);
        customFilterLayout = findViewById(R.id.layout_custom_filter);

        // OneEuroFilter 커스텀 UI 초기화
        oneEuroFreqBar = findViewById(R.id.seekbar_one_euro_freq);
        oneEuroFreqText = findViewById(R.id.text_one_euro_freq);
        oneEuroMinCutoffBar = findViewById(R.id.seekbar_one_euro_min_cutoff);
        oneEuroMinCutoffText = findViewById(R.id.text_one_euro_min_cutoff);
        oneEuroBetaBar = findViewById(R.id.seekbar_one_euro_beta);
        oneEuroBetaText = findViewById(R.id.text_one_euro_beta);
        oneEuroDCutoffBar = findViewById(R.id.seekbar_one_euro_d_cutoff);
        oneEuroDCutoffText = findViewById(R.id.text_one_euro_d_cutoff);

        // Switch 초기화
        clickEnabledSwitch = findViewById(R.id.switch_click_enabled);
        scrollEnabledSwitch = findViewById(R.id.switch_scroll_enabled);
        edgeScrollEnabledSwitch = findViewById(R.id.switch_edge_scroll_enabled);
        blinkDetectionSwitch = findViewById(R.id.switch_blink_detection);
        autoOnePointCalibrationSwitch = findViewById(R.id.switch_auto_one_point_calibration);

        // SeekBar 범위 설정
        fixationDurationBar.setMax(30); // 300ms ~ 3000ms
        aoiRadiusBar.setMax(60); // 10 ~ 70
        edgeTriggerTimeBar.setMax(40); // 1000ms ~ 5000ms
        scrollCountBar.setMax(4); // 1 ~ 5

        // 커서 오프셋 범위 설정: -50px ~ +50px (0~100으로 매핑)
        cursorOffsetXBar.setMax(100);
        cursorOffsetYBar.setMax(100);

        // OneEuroFilter 범위 설정
        oneEuroFreqBar.setMax(90); // 10 ~ 100 Hz
        oneEuroMinCutoffBar.setMax(50); // 0.0 ~ 5.0
        oneEuroBetaBar.setMax(20); // 0.0 ~ 2.0
        oneEuroDCutoffBar.setMax(50); // 0.0 ~ 5.0
    }

    private void loadSettings() {
        // 🎯 캘리브레이션 전략 설정 로드
        UserSettings.CalibrationStrategy strategy = currentSettings.getCalibrationStrategy();
        switch (strategy) {
            case QUICK_START:
                radioQuickStart.setChecked(true);
                break;
            case BALANCED:
                radioBalancedCal.setChecked(true);
                break;
            case PRECISION:
                radioPrecision.setChecked(true);
                break;
        }
        updateStrategyDescription(strategy);

        // 🧠 백그라운드 학습 설정 로드
        backgroundLearningSwitch.setChecked(currentSettings.isBackgroundLearningEnabled());

        // 기존 SeekBar 설정
        fixationDurationBar.setProgress((int)((currentSettings.getFixationDurationMs() - 300) / 100));
        updateFixationDurationText();

        aoiRadiusBar.setProgress((int)(currentSettings.getAoiRadius() - 10));
        updateAoiRadiusText();

        edgeTriggerTimeBar.setProgress((int)((currentSettings.getEdgeTriggerMs() - 1000) / 100));
        updateEdgeTriggerTimeText();

        scrollCountBar.setProgress(currentSettings.getContinuousScrollCount() - 1);
        updateScrollCountText();

        // 커서 오프셋 설정 (-50~+50을 0~100으로 변환)
        cursorOffsetXBar.setProgress((int)(currentSettings.getCursorOffsetX() + 50));
        cursorOffsetYBar.setProgress((int)(currentSettings.getCursorOffsetY() + 50));
        updateCursorOffsetTexts();

        // OneEuroFilter 프리셋 설정
        OneEuroFilterPreset preset = currentSettings.getOneEuroFilterPreset();
        switch (preset) {
            case STABILITY:
                radioStability.setChecked(true);
                break;
            case BALANCED_STABILITY:
                radioBalancedStability.setChecked(true);
                break;
            case BALANCED:
                radioBalanced.setChecked(true);
                break;
            case BALANCED_RESPONSIVE:
                radioBalancedResponsive.setChecked(true);
                break;
            case RESPONSIVE:
                radioResponsive.setChecked(true);
                break;
            case CUSTOM:
                radioCustom.setChecked(true);
                break;
        }

        // OneEuroFilter 커스텀 설정 (항상 로드하되, 커스텀 모드일 때만 표시)
        oneEuroFreqBar.setProgress((int)(currentSettings.getOneEuroFreq() - 10));
        oneEuroMinCutoffBar.setProgress((int)(currentSettings.getOneEuroMinCutoff() * 10));
        oneEuroBetaBar.setProgress((int)(currentSettings.getOneEuroBeta() * 1000)); // 0.007 같은 작은 값 처리
        oneEuroDCutoffBar.setProgress((int)(currentSettings.getOneEuroDCutoff() * 10));
        updateOneEuroTexts();

        // 커스텀 레이아웃 표시/숨김
        updateCustomFilterVisibility();

        // Switch 설정
        clickEnabledSwitch.setChecked(currentSettings.isClickEnabled());
        scrollEnabledSwitch.setChecked(currentSettings.isScrollEnabled());
        edgeScrollEnabledSwitch.setChecked(currentSettings.isEdgeScrollEnabled());
        blinkDetectionSwitch.setChecked(currentSettings.isBlinkDetectionEnabled());
        autoOnePointCalibrationSwitch.setChecked(currentSettings.isAutoOnePointCalibrationEnabled());

        // 스크롤 관련 설정의 활성화 상태 업데이트
        updateScrollSettingsState();
    }

    private void setupListeners() {
        // 🎯 개선된 리스너들 설정
        setupStrategyListeners();
        setupBackgroundLearningSwitch();
        setupAutoCalibrationSwitch();

        // 프리셋 라디오 그룹 리스너
        filterPresetRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateCustomFilterVisibility();
            saveSettings();
        });

        // 기존 SeekBar 리스너들...
        fixationDurationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFixationDurationText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        aoiRadiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateAoiRadiusText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        edgeTriggerTimeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateEdgeTriggerTimeText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        scrollCountBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateScrollCountText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        // 커서 오프셋 SeekBar 리스너
        cursorOffsetXBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCursorOffsetTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        cursorOffsetYBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCursorOffsetTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        // OneEuroFilter 커스텀 SeekBar 리스너
        oneEuroFreqBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateOneEuroTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        oneEuroMinCutoffBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateOneEuroTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        oneEuroBetaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateOneEuroTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        oneEuroDCutoffBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateOneEuroTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        // Switch 리스너
        clickEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());

        scrollEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateScrollSettingsState();
            saveSettings();
        });

        edgeScrollEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        blinkDetectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
    }

    // 🎯 개선된 캘리브레이션 전략 리스너
    private void setupStrategyListeners() {
        calibrationStrategyRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            UserSettings.CalibrationStrategy selectedStrategy = getSelectedStrategy();
            updateStrategyDescription(selectedStrategy);

            // 전략별 특별 안내
            switch (selectedStrategy) {
                case QUICK_START:
                    Toast.makeText(this, "⚠️ 빠른 시작 모드 선택됨 - 정확도 주의", Toast.LENGTH_LONG).show();
                    break;
                case BALANCED:
                    Toast.makeText(this, "⚖️ 균형 모드 선택됨 - 정밀 보정 권장", Toast.LENGTH_SHORT).show();
                    break;
                case PRECISION:
                    Toast.makeText(this, "🎯 정밀 모드 선택됨 - 최고 정확도 보장", Toast.LENGTH_SHORT).show();
                    break;
            }

            // 자동으로 백그라운드 학습 설정 조정
            if (selectedStrategy == UserSettings.CalibrationStrategy.PRECISION) {
                if (backgroundLearningSwitch.isChecked()) {
                    backgroundLearningSwitch.setChecked(false);
                    Toast.makeText(this, "💡 정밀 모드에서는 백그라운드 학습이 자동 비활성화됩니다", Toast.LENGTH_LONG).show();
                }
            }

            saveSettings();
        });
    }

    // 🧠 개선된 백그라운드 학습 스위치
    private void setupBackgroundLearningSwitch() {
        backgroundLearningSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettings();

            if (isChecked) {
                // 백그라운드 학습 활성화시 중요한 경고
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ 백그라운드 학습 주의사항")
                        .setMessage("백그라운드 학습을 활성화하면:\n\n" +
                                "✅ 장점:\n" +
                                "• 사용하며 자동으로 미세 조정\n" +
                                "• 시간이 지날수록 더 정확해질 수 있음\n\n" +
                                "⚠️ 위험:\n" +
                                "• 잘못된 패턴을 학습할 수 있음\n" +
                                "• 억지로 눈을 움직이면 더 부정확해짐\n" +
                                "• 정밀 모드에서는 자동 비활성화됨\n\n" +
                                "💡 권장사항:\n" +
                                "정확한 정밀 보정 후에만 활성화하세요!")
                        .setPositiveButton("✅ 이해했습니다", null)
                        .setNegativeButton("❌ 비활성화", (dialog, which) -> {
                            backgroundLearningSwitch.setChecked(false);
                        })
                        .show();
            } else {
                Toast.makeText(this, "✅ 안전 모드: 수동 미세 조정만 사용됩니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🎯 개선된 자동 캘리브레이션 스위치
    private void setupAutoCalibrationSwitch() {
        autoOnePointCalibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettings();

            if (!isChecked) {
                // 자동 캘리브레이션 비활성화시 강한 경고
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ 자동 보정 비활성화 주의")
                        .setMessage("자동 보정을 비활성화하면:\n\n" +
                                "❌ 문제점:\n" +
                                "• 앱 시작 시 보정이 실행되지 않음\n" +
                                "• 수동으로 보정해야 함\n" +
                                "• 보정 없이 사용하면 매우 부정확\n\n" +
                                "💡 권장사항:\n" +
                                "특별한 이유가 없다면 활성화 상태를 유지하세요!")
                        .setPositiveButton("✅ 이해했습니다", null)
                        .setNegativeButton("🔄 다시 활성화", (dialog, which) -> {
                            autoOnePointCalibrationSwitch.setChecked(true);
                        })
                        .show();
            } else {
                Toast.makeText(this, "✅ 앱 시작 시 자동으로 보정됩니다", Toast.LENGTH_SHORT).show();
            }

            // 서비스에 설정 변경 알림
            if (GazeTrackingService.getInstance() != null) {
                GazeTrackingService.getInstance().refreshSettings();
            }
        });
    }

    // 🎯 캘리브레이션 전략 관련 메서드들 개선
    private UserSettings.CalibrationStrategy getSelectedStrategy() {
        int checkedId = calibrationStrategyRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_quick_start) return UserSettings.CalibrationStrategy.QUICK_START;
        if (checkedId == R.id.radio_balanced_cal) return UserSettings.CalibrationStrategy.BALANCED;
        if (checkedId == R.id.radio_precision) return UserSettings.CalibrationStrategy.PRECISION;
        return UserSettings.CalibrationStrategy.PRECISION; // 기본값을 PRECISION으로 변경
    }

    private void updateStrategyDescription(UserSettings.CalibrationStrategy strategy) {
        String description = "";
        switch (strategy) {
            case QUICK_START:
                description = "⚠️ 빠른 시작 모드 (정확도 주의)\n" +
                        "• 2초 빠른 보정으로 즉시 시작\n" +
                        "• 사용하며 자동 학습 (위험할 수 있음)\n" +
                        "• 시선이 맞지 않으면 정밀 보정 권장\n" +
                        "• 바로 시작하고 싶을 때만 선택";
                break;
            case BALANCED:
                description = "⚖️ 균형 모드 (표준)\n" +
                        "• 빠른 기본 보정 후 선택적 정밀 보정\n" +
                        "• 제한적 자동 학습\n" +
                        "• 필요시 정밀 보정을 적극 권장\n" +
                        "• 적당한 속도와 정확도의 균형";
                break;
            case PRECISION:
                description = "🎯 정밀 모드 (적극 권장)\n" +
                        "• 5포인트 정밀 보정으로 높은 정확도\n" +
                        "• 자동 학습 없이 안전한 사용\n" +
                        "• 가장 정확하고 안정적인 시선 추적\n" +
                        "• 정밀 작업이나 장시간 사용에 최적";
                break;
        }

        if (strategyDescriptionText != null) {
            strategyDescriptionText.setText(description);
        }
    }

    // 🎯 커서 오프셋 안내 개선
    private void showCursorOffsetInfo() {
        // 설정 화면 진입시 한 번만 표시
        if (isFirstTimeOffset()) {
            new AlertDialog.Builder(this)
                    .setTitle("💡 커서 위치 미세 조정 안내")
                    .setMessage("커서 위치 미세 조정 기능:\n\n" +
                            "✅ 올바른 사용법:\n" +
                            "• 정밀 보정을 먼저 실행\n" +
                            "• 미세한 차이만 조정 (±10px 이내 권장)\n" +
                            "• 큰 차이는 재보정으로 해결\n\n" +
                            "❌ 잘못된 사용법:\n" +
                            "• 보정 없이 오프셋으로만 맞추기\n" +
                            "• 큰 차이를 오프셋으로 해결\n" +
                            "• 억지로 많이 조정하기\n\n" +
                            "🎯 기억하세요: 정확한 보정이 우선입니다!")
                    .setPositiveButton("✅ 이해했습니다", null)
                    .show();

            markOffsetInfoShown();
        }
    }

    private boolean isFirstTimeOffset() {
        return getSharedPreferences("settings_info", MODE_PRIVATE)
                .getBoolean("offset_info_shown", false) == false;
    }

    private void markOffsetInfoShown() {
        getSharedPreferences("settings_info", MODE_PRIVATE)
                .edit()
                .putBoolean("offset_info_shown", true)
                .apply();
    }

    private void updateCustomFilterVisibility() {
        boolean isCustom = radioCustom.isChecked();
        customFilterLayout.setVisibility(isCustom ? View.VISIBLE : View.GONE);
    }

    private OneEuroFilterPreset getSelectedPreset() {
        int checkedId = filterPresetRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_stability) return OneEuroFilterPreset.STABILITY;
        if (checkedId == R.id.radio_balanced_stability) return OneEuroFilterPreset.BALANCED_STABILITY;
        if (checkedId == R.id.radio_balanced) return OneEuroFilterPreset.BALANCED;
        if (checkedId == R.id.radio_balanced_responsive) return OneEuroFilterPreset.BALANCED_RESPONSIVE;
        if (checkedId == R.id.radio_responsive) return OneEuroFilterPreset.RESPONSIVE;
        if (checkedId == R.id.radio_custom) return OneEuroFilterPreset.CUSTOM;
        return OneEuroFilterPreset.BALANCED; // 기본값
    }

    private void updateFixationDurationText() {
        float value = 300 + (fixationDurationBar.getProgress() * 100);
        fixationDurationText.setText(String.format("%.1f초", value / 1000));
    }

    private void updateAoiRadiusText() {
        float value = 10 + aoiRadiusBar.getProgress();
        aoiRadiusText.setText(String.format("%.0f 픽셀", value));
    }

    private void updateEdgeTriggerTimeText() {
        float value = 1000 + (edgeTriggerTimeBar.getProgress() * 100);
        edgeTriggerTimeText.setText(String.format("%.1f초", value / 1000));
    }

    private void updateScrollCountText() {
        int value = scrollCountBar.getProgress() + 1;
        scrollCountText.setText(String.format("%d회", value));
    }

    private void updateCursorOffsetTexts() {
        // 0~100을 -50~+50으로 변환
        float offsetX = cursorOffsetXBar.getProgress() - 50;
        float offsetY = cursorOffsetYBar.getProgress() - 50;

        cursorOffsetXText.setText(String.format("%.0f px", offsetX));
        cursorOffsetYText.setText(String.format("%.0f px", offsetY));
    }

    private void updateOneEuroTexts() {
        double freq = 10 + oneEuroFreqBar.getProgress();
        double minCutoff = oneEuroMinCutoffBar.getProgress() / 10.0;
        double beta = oneEuroBetaBar.getProgress() / 1000.0; // 0.001 단위로 조정
        double dCutoff = oneEuroDCutoffBar.getProgress() / 10.0;

        oneEuroFreqText.setText(String.format("%.0f Hz", freq));
        oneEuroMinCutoffText.setText(String.format("%.1f", minCutoff));
        oneEuroBetaText.setText(String.format("%.3f", beta)); // 소수점 3자리까지 표시
        oneEuroDCutoffText.setText(String.format("%.1f", dCutoff));
    }

    private void updateScrollSettingsState() {
        boolean scrollEnabled = scrollEnabledSwitch.isChecked();
        edgeScrollEnabledSwitch.setEnabled(scrollEnabled);
        edgeTriggerTimeBar.setEnabled(scrollEnabled);
        scrollCountBar.setEnabled(scrollEnabled);
    }

    private void saveSettings() {
        UserSettings.Builder builder = new UserSettings.Builder()
                .fixationDurationMs(300 + (fixationDurationBar.getProgress() * 100))
                .aoiRadius(10 + aoiRadiusBar.getProgress())
                .scrollEnabled(scrollEnabledSwitch.isChecked())
                .edgeMarginRatio(0.01f) // 고정 값 사용
                .edgeTriggerMs(1000 + (edgeTriggerTimeBar.getProgress() * 100))
                .continuousScrollCount(scrollCountBar.getProgress() + 1)
                .clickEnabled(clickEnabledSwitch.isChecked())
                .edgeScrollEnabled(edgeScrollEnabledSwitch.isChecked())
                .blinkDetectionEnabled(blinkDetectionSwitch.isChecked())
                .autoOnePointCalibrationEnabled(autoOnePointCalibrationSwitch.isChecked())
                .cursorOffsetX(cursorOffsetXBar.getProgress() - 50) // 0~100을 -50~+50으로 변환
                .cursorOffsetY(cursorOffsetYBar.getProgress() - 50) // 0~100을 -50~+50으로 변환
                .oneEuroFilterPreset(getSelectedPreset())
                .oneEuroFreq(10 + oneEuroFreqBar.getProgress())
                .oneEuroMinCutoff(oneEuroMinCutoffBar.getProgress() / 10.0)
                .oneEuroBeta(oneEuroBetaBar.getProgress() / 1000.0) // 0.001 단위
                .oneEuroDCutoff(oneEuroDCutoffBar.getProgress() / 10.0)
                // 🎯 새로운 설정들 추가
                .calibrationStrategy(getSelectedStrategy())
                .backgroundLearningEnabled(backgroundLearningSwitch.isChecked());

        UserSettings newSettings = builder.build();
        settingsRepository.saveUserSettings(newSettings);
        currentSettings = newSettings;

        // 서비스에 설정 변경 알림 (OneEuroFilter 설정도 실시간 반영)
        if (GazeTrackingService.getInstance() != null) {
            GazeTrackingService.getInstance().refreshSettings();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 설정 화면이 다시 보일 때마다 최신 설정 로드
        currentSettings = settingsRepository.getUserSettings();
        loadSettings();

        Log.d("SettingsActivity", "설정 새로고침 - 현재 커서 오프셋: X=" +
                currentSettings.getCursorOffsetX() + ", Y=" + currentSettings.getCursorOffsetY());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}