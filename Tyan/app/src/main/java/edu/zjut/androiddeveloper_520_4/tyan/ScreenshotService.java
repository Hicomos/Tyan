package edu.zjut.androiddeveloper_520_4.tyan;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;

import okhttp3.OkHttpClient;

/*
   截屏服务
  1. 处理截屏操作
  2. 将截屏图片发送到AI大模型进行处理
  3. 管理截屏权限和媒体投影
  4. 保持前台服务状态，防止被系统回收
 */
public class ScreenshotService extends Service {
    // 日志标签
    private static final String TAG = "ScreenshotService";
    
    // 依赖组件
    private PermissionManager permissionManager;  // 权限管理工具
    private ScreenshotUtil screenshotUtil;       // 截屏工具类
    private Handler mainHandler;                  // 主线程Handler
    private OkHttpClient client;                  // HTTP客户端
    private StyleSettingsManager settingsManager; // 样式设置管理器
    // 使用ApiUtils中的JSON MediaType

    // 通知相关常量
    private static final int NOTIFICATION_ID = 1001; // 通知ID
    private static final String CHANNEL_ID = "screenshot_service_channel"; // 通知渠道ID

    @Override
    public void onCreate() {
        super.onCreate();
        permissionManager = PermissionManager.getInstance(this);
        screenshotUtil = new ScreenshotUtil(this);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化OkHttpClient
        client = new OkHttpClient();
        
        // 初始化StyleSettingsManager
        settingsManager = new StyleSettingsManager(this);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 需要指定前台服务类型
                startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            Log.d(TAG, "ScreenshotService created as foreground service successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service: " + e.getMessage());
            e.printStackTrace();
        }//前台服务和activity的区别：前者是为了在后台运行时保持服务活跃，防止被系统回收；后者是为了与用户交互，显示界面。
        //前台服务可见，后台service不可见，前台服务需要显示通知，后台服务不需要。
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.d(TAG, "Starting screenshot service");
            
            // 显示提示消息
            Toast.makeText(this, R.string.taking_screenshot, Toast.LENGTH_SHORT).show();
            
            // 检查是否有媒体投影权限
            if (permissionManager.hasMediaProjectionPermission()) {
                // 已有权限，开始截屏
                MediaProjection mediaProjection = permissionManager.getMediaProjection(this);
                if (mediaProjection != null) {
                    screenshotUtil.setMediaProjection(mediaProjection);
                    
                    // 使用延迟截屏，给用户时间看到提示
                    mainHandler.postDelayed(() -> {
                        takeScreenshot();
                    }, 300); // 延迟300毫秒再截屏
                } else {
                    Log.e(TAG, "MediaProjection is null");
                    Toast.makeText(this, R.string.screenshot_failed, Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
            } else {
                // 没有权限，提示用户
                Log.e(TAG, "No media projection permission");
                Toast.makeText(this, R.string.screenshot_permission_denied, Toast.LENGTH_SHORT).show();
                // 打开主活动请求权限
                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);
                stopSelf();
            }
        } else {
            Log.e(TAG, "Intent is null");
            stopSelf();
        }
        
        // 返回 START_STICKY 确保服务不会被系统轻易杀死
        return START_STICKY;
    }
    
    /**
     * 1. 获取媒体投影权限
     * 2. 延迟300ms后执行截屏(给用户时间看到提示)
     * 3. 截屏成功后发送给大模型处理
     */
    private void takeScreenshot() {
        Log.d(TAG, "Taking screenshot");
        
        try {
            // 确保每次截屏前重新获取MediaProjection
            MediaProjection mediaProjection = permissionManager.getMediaProjection(this);
            if (mediaProjection != null) {
                Log.d(TAG, "MediaProjection successfully obtained");
                screenshotUtil.setMediaProjection(mediaProjection);
                
                // 添加延迟以确保MediaProjection完全初始化
                mainHandler.postDelayed(() -> {
                    Log.d(TAG, "Starting screenshot capture after delay");
                    screenshotUtil.takeScreenshot(bitmap -> {
                        if (bitmap != null) {
                            Log.d(TAG, "Screenshot taken successfully, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                            //bitmap是截屏的Bitmap对象
                            // 直接发送给大模型，不保存到相册
                            Toast.makeText(ScreenshotService.this, R.string.sending_to_model, Toast.LENGTH_SHORT).show();
                            sendImageToLargeModel(bitmap);
                        } else {
                            Log.e(TAG, "Screenshot bitmap is null");
                            mainHandler.post(() -> {
                                Toast.makeText(ScreenshotService.this, R.string.screenshot_failed, Toast.LENGTH_SHORT).show();
                                stopSelf();
                            });//如果bitmap为null，说明截屏失败
                        }
                    });
                }, 100); // 添加100毫秒延迟，确保MediaProjection初始化完成
            } else {
                Log.e(TAG, "MediaProjection is null - permission might be revoked or not granted");
                mainHandler.post(() -> {
                    Toast.makeText(ScreenshotService.this, R.string.screenshot_failed, Toast.LENGTH_SHORT).show();
                    stopSelf();
                });//如果MediaProjection为null，说明权限被撤销或未授予
            }
        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot: " + e.getMessage(), e);
            e.printStackTrace();
            mainHandler.post(() -> {
                Toast.makeText(ScreenshotService.this, "Screenshot failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                stopSelf();
            });
        }
    }
    

    /*
     创建通知
     */
    private android.app.Notification createNotification() {
        // 创建通知内容
        String title = getString(R.string.app_name);
        String content = getString(R.string.taking_screenshot);

        // 创建通知
        android.app.Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new android.app.Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new android.app.Notification.Builder(this);
        }

        // 设置通知内容，内容是应用名称和截屏提示
        builder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_floating_bubble)
                .setOngoing(true);

        return builder.build();
    }

    /*
     服务销毁时调用，清理资源：
     1. 停止前台服务
     2. 释放截屏工具资源
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ScreenshotService destroyed");
        
        // 停止前台服务
        stopForeground(true);
        
        // 释放资源，但不停止MediaProjection
        if (screenshotUtil != null) {
            try {
                screenshotUtil.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing screenshotUtil: " + e.getMessage());
            } finally {
                screenshotUtil = null;
            }
        }
        
        // 不在这里释放 permissionManager 的 MediaProjection
        // 因为它是全局共享的，需要在应用退出时才释放
    }
    
    /*
     将截图发送给大模型
     @param bitmap 要发送的截图
     */
    private void sendImageToLargeModel(Bitmap bitmap) {
        try {
            Log.d(TAG, "Preparing to send screenshot to large model");
            
            // 转换图片为Base64
            String base64Image = ApiUtils.bitmapToBase64(bitmap);
            Log.d(TAG, "Image converted to Base64, length: " + base64Image.length());
            
            // 用户消息文本内容
            String scene = settingsManager.getScene();
            String tone = settingsManager.getTone();
            String target = settingsManager.getTarget();
            String otherRequirements = settingsManager.getOtherRequirements();
            
            String userContent = "场景: " + scene + "\n" +
                                "语气: " + tone + "\n" +
                                "回复对象: " + target + "\n" +
                                "其他要求: " + otherRequirements + "\n\n" +
                                "请根据图片内容给出合适的回复。";
            
            Log.d(TAG, "Preparing user message with settings - Scene: " + scene + ", Tone: " + tone + ", Target: " + target);
            
            // 使用ApiUtils创建请求体
            String requestBodyJson = ApiUtils.createApiRequestBody(settingsManager, userContent, base64Image);
            
            Log.d(TAG, "Request body prepared, sending API request");
            // 发送API请求
            ApiUtils.sendApiRequest(settingsManager, requestBodyJson, new ApiUtils.ApiCallback() {
                @Override
                public void onSuccess(String content) {
                    mainHandler.post(() -> {
                        // 复制内容到系统剪贴板
                        ApiUtils.copyToClipboard(ScreenshotService.this, content);
                        
                        // 显示复制成功的提示
                        Toast.makeText(ScreenshotService.this, "大模型回复已复制到剪贴板", Toast.LENGTH_LONG).show();
                        
                        // 延迟停止服务
                        mainHandler.postDelayed(() -> stopSelf(), 1000);
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    mainHandler.post(() -> {
                        Toast.makeText(ScreenshotService.this, "请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        stopSelf();
                    });
                }
                
                @Override
                public void onError(int statusCode, String errorBody, Exception e) {
                    mainHandler.post(() -> {
                        String errorMessage = "请求错误: " + statusCode;
                        if (e != null) {
                            errorMessage += " - " + e.getMessage();
                        }
                        Toast.makeText(ScreenshotService.this, errorMessage, Toast.LENGTH_SHORT).show();
                        stopSelf();
                    });
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request: " + e.getMessage(), e);
            e.printStackTrace();
            mainHandler.post(() -> {
                Toast.makeText(ScreenshotService.this, "发送请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                stopSelf();
            });
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory error processing image: " + e.getMessage(), e);
            e.printStackTrace();
            mainHandler.post(() -> {
                Toast.makeText(ScreenshotService.this, "内存不足，请尝试较小的截图", Toast.LENGTH_SHORT).show();
                stopSelf();
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error sending image to model: " + e.getMessage(), e);
            e.printStackTrace();
            mainHandler.post(() -> {
                Toast.makeText(ScreenshotService.this, "发送图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                stopSelf();
            });
        }
    }
}
