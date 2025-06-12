package edu.zjut.androiddeveloper_520_4.tyan;

/*
 本地图片回复功能Activity
 主要功能包括：
 1. 图片上传功能：
 2. 智能回复功能：
 3. 剪贴板集成：
 */

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class LocalImageReplyActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 101;
    // 使用ApiUtils中的JSON MediaType
    
    private ImageView uploadImageView;
    private LinearLayout uploadImageContainer;
    private TextView uploadPromptText;
    private RecyclerView chatRecyclerView;// 消息列表的RecyclerView
    private EditText messageEditText;
    private ImageButton sendButton;
    
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private Bitmap selectedImage;
    private StyleSettingsManager settingsManager;
    
    private final ActivityResultLauncher<Intent> imagePickerLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), 
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri selectedImageUri = result.getData().getData();//两个getData不一样，前者是ActivityResultContracts的getData，后者是Intent的getData
                            try {
                                selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                                //getContentResolver()获取ContentResolver对象，用于访问应用的内容提供者
                                uploadImageView.setImageBitmap(selectedImage);
                                uploadImageView.setVisibility(View.VISIBLE);
                                uploadPromptText.setText(R.string.image_ready_prompt);
                                //设置上传图片容器的可见性
                                uploadImageContainer.setVisibility(View.VISIBLE);

                                // 添加图片消息到聊天记录
                                addMessage(new ChatMessage(ChatMessage.TYPE_IMAGE, null, selectedImageUri.toString()));
                            } catch (IOException e) {
                                Toast.makeText(this, R.string.image_load_error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });//ActivityResultLauncher是一个用于处理Activity结果的接口，这里是选择相册图片

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_image_reply);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.local_image_reply_layout), null);
        
        // 初始化控件
        initViews();
        setupRecyclerView();//设置消息列表的适配器和布局管理器

        setupClickListeners();//设置点击监听器

        // 初始化设置管理器
        settingsManager = new StyleSettingsManager(this);

        // 注意：在输入框获得焦点时自动粘贴剪贴板内容
        // 不需要在这里调用 getClipboardContent()
    }
    
    private void initViews() {
        uploadImageContainer = findViewById(R.id.upload_image_container);
        uploadImageView = findViewById(R.id.upload_image_view);
        uploadPromptText = findViewById(R.id.upload_prompt_text);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        
        // 为输入框添加焦点变化监听器
        messageEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 当输入框获得焦点时，自动粘贴剪贴板内容
                getClipboardContent();
            }
        });
        
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());// 返回按钮点击事件，结束当前Activity
    }
    
    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));// 设置RecyclerView的布局管理器为线性布局
        chatRecyclerView.setAdapter(chatAdapter);
    }
    
    private void setupClickListeners() {
        uploadImageContainer.setOnClickListener(v -> checkAndRequestStoragePermission());
        
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(message)) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

    }
    
    private void checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    REQUEST_STORAGE_PERMISSION);
        } else {
            openImagePicker();// 如果已经有权限，直接打开图片选择器
        }
    }

    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    
    private void sendMessage(String message) {
        // 添加用户消息到聊天记录
        addMessage(new ChatMessage(ChatMessage.TYPE_USER, message, null));
        addMessage(new ChatMessage(ChatMessage.TYPE_LOADING, getString(R.string.loading), null));// 添加加载消息
        
        try {
            // 用户消息文本内容
            String userContent = "场景: " + settingsManager.getScene() + "\n" +
                                "语气: " + settingsManager.getTone() + "\n" +
                                "回复对象: " + settingsManager.getTarget() + "\n" +
                                "其他要求: " + settingsManager.getOtherRequirements() + "\n\n" +
                                "用户消息: " + message;
            
            // 判断是否有图片
            String base64Image = null;
            if (selectedImage != null) {
                // 有图片，添加提示
                userContent += "\n\n请根据图片内容给出合适的回复。";
                base64Image = ApiUtils.bitmapToBase64(selectedImage);
            } else {
                // 没有图片，只发送文本内容
                userContent += "\n\n请给出合适的回复。";
            }
            
            // 使用ApiUtils创建请求体
            String requestBodyJson = ApiUtils.createApiRequestBody(settingsManager, userContent, base64Image);
            
            // 添加调试信息
            addMessage(new ChatMessage(ChatMessage.TYPE_SYSTEM, 
                    "正在发送请求到: " + settingsManager.getUrl() + "\n模型: " + settingsManager.getModelName(), null));
            
            // 发送API请求
            ApiUtils.sendApiRequest(settingsManager, requestBodyJson, new ApiUtils.ApiCallback() {
                @Override
                public void onSuccess(String content) {
                    runOnUiThread(() -> {//runOnUiThread用于在主线程中执行UI更新操作
                        // 移除加载消息
                        removeLoadingMessage();
                        // 添加AI回复
                        addMessage(new ChatMessage(ChatMessage.TYPE_AI, content, null));
                        
                        // 复制内容到系统剪贴板
                        ApiUtils.copyToClipboard(LocalImageReplyActivity.this, content);
                        
                        // 显示复制成功的提示
                        Toast.makeText(LocalImageReplyActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        // 移除加载消息
                        removeLoadingMessage();
                        // 添加错误消息
                        addMessage(new ChatMessage(ChatMessage.TYPE_SYSTEM, 
                                getString(R.string.network_error) + "\n错误详情: " + e.getMessage(), null));
                    });
                }
                
                @Override
                public void onError(int statusCode, String errorBody, Exception e) {
                    runOnUiThread(() -> {
                        // 移除加载消息
                        removeLoadingMessage();
                        // 添加详细错误消息
                        addMessage(new ChatMessage(ChatMessage.TYPE_SYSTEM,
                                getString(R.string.api_error) + " " + statusCode +
                                "\n错误详情: " + errorBody, null));
                        
                        // 对于403错误，提供更具体的建议
                        if (statusCode == 403) {
                            addMessage(new ChatMessage(ChatMessage.TYPE_SYSTEM, 
                                    "403错误通常表示授权问题。请检查:\n" +
                                    "1. API密钥是否正确\n" +
                                    "2. API密钥是否已过期\n" +
                                    "3. 是否有权限访问此API\n" +
                                    "4. 请求格式是否符合API要求", null));
                        }
                    });
                }
            });
            
        } catch (JSONException e) {
            // 移除加载消息
            removeLoadingMessage();
            // 添加错误消息
            addMessage(new ChatMessage(ChatMessage.TYPE_SYSTEM, getString(R.string.json_error), null));
        }
    }

    
    private void addMessage(ChatMessage message) {
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);// 通知适配器有新消息插入，参数是插入的位置
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);// 滚动到最新消息位置
    }
    
    private void removeLoadingMessage() {
        for (int i = 0; i < chatMessages.size(); i++) {
            if (chatMessages.get(i).getType() == ChatMessage.TYPE_LOADING) {
                chatMessages.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    
    /*
     获取剪贴板内容并粘贴到输入框
     */
    private void getClipboardContent() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            
            // 检查剪贴板是否有内容
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                //先判断剪贴板是否有内容，再获取内容
                ClipData clipData = clipboard.getPrimaryClip();
                
                // 检查剪贴板内容是否是文本
                if (clipData.getItemCount() > 0 && 
                    clipData.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    
                    // 获取文本内容
                    CharSequence text = clipData.getItemAt(0).getText();//CharSequence是一个可以包含多个字符的序列，通常用于表示文本内容
                    if (text != null && !TextUtils.isEmpty(text)) {
                        // 设置到输入框
                        messageEditText.setText(text);
                        // 将光标移动到文本结尾
                        messageEditText.setSelection(text.length());
                        
                        // 显示提示
                        Toast.makeText(this, R.string.clipboard_content_pasted, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            // 异常处理
            Log.e("LocalImageReplyActivity", "Error getting clipboard content: " + e.getMessage());
        }
    }
}
