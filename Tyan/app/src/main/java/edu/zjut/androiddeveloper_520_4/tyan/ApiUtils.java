package edu.zjut.androiddeveloper_520_4.tyan;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/*
 API工具类，提供API通信和剪贴板操作的通用方法
 */
public class ApiUtils {
    private static final String TAG = "ApiUtils";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
    }
    
    /*
     将文本复制到系统剪贴板
     */
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(context.getString(R.string.app_name), text);
        clipboard.setPrimaryClip(clip);
    }
    
    /*
     创建大模型API请求体
     */
    public static String createApiRequestBody(StyleSettingsManager settingsManager, String userContent, String base64Image) throws JSONException {
        JSONObject requestBody = new JSONObject();
        
        // 添加模型信息
        String modelName = settingsManager.getModelName();
        requestBody.put("model", modelName);
        
        // 创建消息数组
        JSONArray messagesArray = new JSONArray();
        
        // 系统消息
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一位社交达人，请你智能识别内容，帮我做出回复，只需要给我回复的内容，不需要给我其他多余的内容。");
        messagesArray.put(systemMessage);
        
        // 用户消息
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        
        // 判断是否有图片
        if (base64Image != null) {
            // 有图片，创建包含文本和图片的消息
            // 创建内容数组
            JSONArray contentArray = new JSONArray();
            
            // 添加文本部分
            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", userContent);
            contentArray.put(textPart);
            
            // 添加图片部分
            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "image_url");
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
            imagePart.put("image_url", imageUrl);
            contentArray.put(imagePart);
            
            // 设置用户消息内容
            userMessage.put("content", contentArray);
        } else {
            // 没有图片，只发送文本内容
            userMessage.put("content", userContent);
        }
        
        messagesArray.put(userMessage);
        
        // 添加消息数组到请求体
        requestBody.put("messages", messagesArray);
        
        // 根据示例代码添加必要参数
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 512);
        requestBody.put("temperature", 0.6);
        
        return requestBody.toString();
    }
    
    /*
     发送API请求
     */
    public static void sendApiRequest(StyleSettingsManager settingsManager, String requestBodyJson, ApiCallback callback) {
        OkHttpClient client = new OkHttpClient();
        
        // 获取API配置
        String baseUrl = settingsManager.getUrl();
        String apiKey = settingsManager.getKey();
        
        // 确保URL格式正确
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        // 构建API URL
        String apiUrl = baseUrl + "chat/completions";
        
        Log.d(TAG, "正在发送请求到: " + apiUrl);
        
        RequestBody body = RequestBody.create(requestBodyJson, JSON);
        
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            //client请求回调，enqueue方法会异步执行请求，防止网络阻塞UI线程
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "请求失败: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");// 获取choices数组
                        if (choices.length() > 0) {
                            JSONObject firstChoice = choices.getJSONObject(0);
                            JSONObject message = firstChoice.getJSONObject("message");// 获取第一个choice的message，即大模型的回复
                            String content = message.getString("content");
                            
                            if (callback != null) {
                                callback.onSuccess(content);
                            }
                        } else {
                            throw new JSONException("No choices in response");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析响应失败: " + e.getMessage(), e);
                        if (callback != null) {
                            callback.onError(response.code(), responseBody, e);
                        }
                    }
                } else {
                    try {
                        // 获取错误响应内容
                        String errorBody = "";
                        if (response.body() != null) {
                            errorBody = response.body().string();
                        }
                        
                        Log.e(TAG, "请求错误: " + response.code() + ", 错误响应体: " + errorBody);
                        
                        if (callback != null) {
                            callback.onError(response.code(), errorBody, null);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "读取错误响应失败: " + e.getMessage(), e);
                        if (callback != null) {
                            callback.onError(response.code(), "", e);
                        }
                    }
                }
            }
        });
    }
    
    /*
     API回调接口
     */
    public interface ApiCallback {
        void onSuccess(String content);// 成功，大模型回复的内容
        void onFailure(Exception e);
        void onError(int statusCode, String errorBody, Exception e);
    }
}
