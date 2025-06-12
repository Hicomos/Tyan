package edu.zjut.androiddeveloper_520_4.tyan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

//权限管理类，用于统一处理应用所需的所有权限
public class PermissionManager {
    private static final String PREFS_NAME = "TyanPermissions";
    private static final String KEY_MEDIA_PROJECTION_RESULT_CODE = "media_projection_result_code";
    private static final String KEY_MEDIA_PROJECTION_DATA = "media_projection_data";
    
    private static PermissionManager instance;//单例模式
    
    private Context context;//应用上下文
    private SharedPreferences preferences;//存储权限请求结果
    private MediaProjection mediaProjection;//媒体投影对象
    private int mediaProjectionResultCode = -1;//媒体投影请求结果码，-1表示未授权
    private Intent mediaProjectionData;//媒体投影请求数据
    
    private PermissionManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager(context);
        }
        return instance;
    }//获取单例
    
    //检查并请求所有应用所需的权限
    public void checkAndRequestAllPermissions(AppCompatActivity activity, PermissionCallback callback) {
        //AppCompatActivity接收一个Activity上下文，用于显示权限请求对话框
        //PermissionCallback是自定义的回调接口，用于处理权限请求结果
        // 检查存储权限
        boolean hasStoragePermission = checkStoragePermission(activity);
        
        // 检查悬浮窗权限
        boolean hasOverlayPermission = checkOverlayPermission(activity);
        
        // 检查通知权限（Android 13 及以上需要）
        boolean hasNotificationPermission = checkNotificationPermission(activity);
        
        // 如果所有权限都已授予，直接回调成功
        if (hasStoragePermission && hasOverlayPermission && hasNotificationPermission) {
            callback.onPermissionResult(true);
        } else {
            // 请求存储权限
            if (!hasStoragePermission) {
                requestStoragePermission(activity);
            }//调用requestStoragePermission方法请求存储权限
            
            // 请求悬浮窗权限
            if (!hasOverlayPermission) {
                requestOverlayPermission(activity);
            }
            
            // 请求通知权限
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission(activity);
            }
            
            // 这里不直接回调，因为权限请求是异步的
            // 在 MainActivity 中需要在 onResume 中再次检查权限状态
        }
    }
    
    //检查存储权限
    public boolean checkStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {//Build.VERSION_CODES.TIRAMISU是Android 13的SDK版本号
        //Build.VERSION.SDK_INT：获取当前设备的Android版本号
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) 
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    //检查通知权限
    public boolean checkNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13 以下版本不需要该权限
    }
    
    //请求通知权限
    public void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    103);//new String[]{Manifest.permission.POST_NOTIFICATIONS}：要请求的权限数组103：自定义请求码，用于在回调中识别这次权限请求
        }
    }
    
    //请求存储权限
    public void requestStoragePermission(Activity activity) {
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }//没有存储权限，则添加存储权限到权限列表
        
        ActivityCompat.requestPermissions(activity, 
                permissions.toArray(new String[0]), 
                100);//转为stirng数组，100：自定义请求码，用于在回调中识别这次权限请求
    }
    
    //检查悬浮窗权限
    public boolean checkOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(activity);//Settings.canDrawOverlays()：检查当前应用是否具有悬浮窗权限
        }
        return true;
    }
    
    //请求悬浮窗权限
    public void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, 101);
            //Intent()：创建显式Intent。Settings.ACTION_MANAGE_OVERLAY_PERMISSION：系统预定义的权限管理动作。Uri.parse()：构建指向当前应用的URI
                //startActivityForResult()：启动带回调的Activity
        }
    }
    
    //请求媒体投影权限
    public void requestMediaProjectionPermission(Activity activity, int requestCode) {
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(projectionManager.createScreenCaptureIntent(), requestCode);
        //通过createScreenCaptureIntent()创建系统标准的截图权限请求Intent
    }


    //保存媒体投影权限结果
    public void saveMediaProjectionResult(int resultCode, Intent data) {
        this.mediaProjectionResultCode = resultCode;
        this.mediaProjectionData = data;
        
        // 释放旧的 MediaProjection
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        // 保存到 SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_MEDIA_PROJECTION_RESULT_CODE, resultCode);
        // 无法直接保存 Intent，这里只是标记已获取权限
        editor.putBoolean(KEY_MEDIA_PROJECTION_DATA, data != null);
        editor.apply();
    }
    
    //检查是否已有媒体投影权限
    public boolean hasMediaProjectionPermission() {
        return mediaProjectionResultCode == Activity.RESULT_OK && mediaProjectionData != null;
    }
    
    //获取媒体投影对象
    public MediaProjection getMediaProjection(Context context) {
        if (hasMediaProjectionPermission() && mediaProjection == null) {
            try {
                MediaProjectionManager projectionManager = 
                        (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                mediaProjection = projectionManager.getMediaProjection(mediaProjectionResultCode, mediaProjectionData);
                //projectionManager.getMediaProjection()：根据结果码和数据获取MediaProjection对象,相当于工厂类
                
                // 注册回调以检测媒体投影的终止
                if (mediaProjection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mediaProjection.registerCallback(new MediaProjection.Callback() {
                        // 当媒体投影停止时，也就是截屏结束时，清理资源
                        @Override
                        public void onStop() {
                            mediaProjection = null;
                        }
                    }, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                mediaProjection = null;
            }
        }
        return mediaProjection;
    }
    
    //释放媒体投影资源
    public void releaseMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
    
    //权限请求结果回调接口
    public interface PermissionCallback {
        void onPermissionResult(boolean allGranted);
    }
}
