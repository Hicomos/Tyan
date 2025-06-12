package edu.zjut.androiddeveloper_520_4.tyan;

/*
 聊天风格设置界面Activity
 */

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

public class StyleSettingsActivity extends AppCompatActivity {

    private EditText sceneEditText;
    private EditText toneEditText;
    private EditText targetEditText;
    private EditText otherRequirementsEditText;
    private EditText keyEditText;
    private EditText urlEditText;
    private EditText modelNameEditText;
    private Button saveButton;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_style_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.style_settings_layout),null);
        initViews();//设置UI控件
        loadSettings();//加载已保存的设置

        setupClickListeners();//设置按钮点击事件监听
    }

    private void initViews() {// 初始化UI控件
        sceneEditText = findViewById(R.id.scene_edit_text);
        toneEditText = findViewById(R.id.tone_edit_text);
        targetEditText = findViewById(R.id.target_edit_text);
        otherRequirementsEditText = findViewById(R.id.other_requirements_edit_text);
        keyEditText = findViewById(R.id.key_edit_text);
        urlEditText = findViewById(R.id.url_edit_text);
        modelNameEditText = findViewById(R.id.model_name_edit_text);
        saveButton = findViewById(R.id.save_button);
        backButton = findViewById(R.id.back_button);
    }

    private void loadSettings() {
        // 加载已保存的设置
        StyleSettingsManager settingsManager = new StyleSettingsManager(this);
        
        sceneEditText.setText(settingsManager.getScene());
        toneEditText.setText(settingsManager.getTone());
        targetEditText.setText(settingsManager.getTarget());
        otherRequirementsEditText.setText(settingsManager.getOtherRequirements());
        keyEditText.setText(settingsManager.getKey());
        urlEditText.setText(settingsManager.getUrl());
        modelNameEditText.setText(settingsManager.getModelName());
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveSettings());
        
        backButton.setOnClickListener(v -> finish());
    }

    private void saveSettings() {
        // 保存设置到StyleSettingsManager
        StyleSettingsManager settingsManager = new StyleSettingsManager(this);
        
        settingsManager.setScene(sceneEditText.getText().toString());
        settingsManager.setTone(toneEditText.getText().toString());
        settingsManager.setTarget(targetEditText.getText().toString());
        settingsManager.setOtherRequirements(otherRequirementsEditText.getText().toString());
        settingsManager.setKey(keyEditText.getText().toString());
        settingsManager.setUrl(urlEditText.getText().toString());
        settingsManager.setModelName(modelNameEditText.getText().toString());
        
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
