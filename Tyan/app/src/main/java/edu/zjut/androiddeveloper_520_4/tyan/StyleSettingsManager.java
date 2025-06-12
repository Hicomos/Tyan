package edu.zjut.androiddeveloper_520_4.tyan;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 保存和管理应用的样式设置
 */
public class StyleSettingsManager {
    private static final String PREFS_NAME = "TyanStyleSettings";
    
    // Preference keys
    private static final String KEY_SCENE = "scene";
    private static final String KEY_TONE = "tone";
    private static final String KEY_TARGET = "target";
    private static final String KEY_OTHER_REQUIREMENTS = "other_requirements";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_URL = "url";
    private static final String KEY_MODEL_NAME = "model_name";
    
    // Default values
    private static final String DEFAULT_SCENE = "工作交流";
    private static final String DEFAULT_TONE = "专业、友好";
    private static final String DEFAULT_TARGET = "客户";
    private static final String DEFAULT_OTHER_REQUIREMENTS = "无";
    private static final String DEFAULT_KEY = "并行智算云的大模型api-key";
    private static final String DEFAULT_URL = "https://llmapi.paratera.com/";
    private static final String DEFAULT_MODEL_NAME = "Qwen2.5-VL-72B-Instruct-P003";
    
    private final SharedPreferences preferences;
    
    public StyleSettingsManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 获取当前设置的对话场景
     * @return 当前设置的场景，如未设置则返回默认值
     */
    public String getScene() {
        return preferences.getString(KEY_SCENE, DEFAULT_SCENE);
    }
    
    /**
     * 获取当前设置的回复语气
     * @return 当前设置的语气风格，如未设置则返回默认值
     */
    public String getTone() {
        return preferences.getString(KEY_TONE, DEFAULT_TONE);
    }
    
    /**
     * 获取当前设置的回复目标对象
     * @return 当前设置的目标对象，如未设置则返回默认值
     */
    public String getTarget() {
        return preferences.getString(KEY_TARGET, DEFAULT_TARGET);
    }
    
    /**
     * 获取其他特殊要求设置
     * @return 当前设置的其他要求，如未设置则返回默认值
     */
    public String getOtherRequirements() {
        return preferences.getString(KEY_OTHER_REQUIREMENTS, DEFAULT_OTHER_REQUIREMENTS);
    }
    
    /**
     * 获取API访问密钥
     * @return 当前设置的API密钥，如未设置则返回默认值
     */
    public String getKey() {
        return preferences.getString(KEY_API_KEY, DEFAULT_KEY);
    }
    
    /**
     * 获取API服务端点URL
     * @return 当前设置的API地址，如未设置则返回默认值
     */
    public String getUrl() {
        return preferences.getString(KEY_URL, DEFAULT_URL);
    }
    
    /**
     * 获取当前使用的AI模型名称
     * @return 当前设置的模型名称，如未设置则返回默认值
     */
    public String getModelName() {
        return preferences.getString(KEY_MODEL_NAME, DEFAULT_MODEL_NAME);
    }
    
    /**
     * 设置对话场景配置
     * @param scene 新的对话场景设置
     */
    public void setScene(String scene) {
        preferences.edit().putString(KEY_SCENE, scene).apply();
    }
    
    /**
     * 设置回复语气配置
     * @param tone 新的语气风格设置
     */
    public void setTone(String tone) {
        preferences.edit().putString(KEY_TONE, tone).apply();
    }
    
    /**
     * 设置回复目标对象
     * @param target 新的目标对象设置
     */
    public void setTarget(String target) {
        preferences.edit().putString(KEY_TARGET, target).apply();
    }
    
    /**
     * 设置其他特殊要求
     * @param otherRequirements 新的其他要求设置
     */
    public void setOtherRequirements(String otherRequirements) {
        preferences.edit().putString(KEY_OTHER_REQUIREMENTS, otherRequirements).apply();
    }
    
    /**
     * 设置API访问密钥
     * @param key 新的API密钥
     */
    public void setKey(String key) {
        preferences.edit().putString(KEY_API_KEY, key).apply();
    }
    
    /**
     * 设置API服务端点URL
     * @param url 新的API地址
     */
    public void setUrl(String url) {
        preferences.edit().putString(KEY_URL, url).apply();
    }
    
    /**
     * 设置AI模型名称
     * @param modelName 新的模型名称
     */
    public void setModelName(String modelName) {
        preferences.edit().putString(KEY_MODEL_NAME, modelName).apply();
    }
    
    /**
     * 重置所有设置为默认值
     * 包括：场景、语气、目标对象、其他要求、API密钥、URL和模型名称
     */
    public void resetToDefaults() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SCENE, DEFAULT_SCENE);
        editor.putString(KEY_TONE, DEFAULT_TONE);
        editor.putString(KEY_TARGET, DEFAULT_TARGET);
        editor.putString(KEY_OTHER_REQUIREMENTS, DEFAULT_OTHER_REQUIREMENTS);
        editor.putString(KEY_API_KEY, DEFAULT_KEY);
        editor.putString(KEY_URL, DEFAULT_URL);
        editor.putString(KEY_MODEL_NAME, DEFAULT_MODEL_NAME);
        editor.apply();
    }
}
