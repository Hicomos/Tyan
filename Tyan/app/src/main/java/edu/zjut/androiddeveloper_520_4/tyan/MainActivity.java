package edu.zjut.androiddeveloper_520_4.tyan;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/*
 应用的主活动类，负责：
 1. 管理应用的主要界面和功能入口
 2. 处理各种权限请求（悬浮窗、存储、屏幕截图等）
 3. 启动悬浮窗服务和其他功能活动
 */
public class MainActivity extends AppCompatActivity {
    //AppCompatActivity是Android的一个基类，用于支持向后兼容的活动
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 100;//悬浮窗权限
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 102;//截屏权限
    
    // 主界面按钮视图
    private CardView floatingWindowButton;  // 悬浮窗功能按钮
    private CardView styleSettingsButton;  // 样式设置按钮
    private CardView localImageReplyButton; // 本地图片回复按钮
    
    // 权限管理工具类实例
    private PermissionManager permissionManager;
    
    // 悬浮窗权限请求的结果处理器
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), 
                    result -> {
                        // 检查悬浮窗权限是否被授予，请求返回后检查lambda逻辑
                        //->是lambda表达式
                        if (Settings.canDrawOverlays(this)) {//如果悬浮窗权限被授予
                            startFloatingWindowService();//启动悬浮窗服务
                        } else {
                            Toast.makeText(this, R.string.floating_window_permission, Toast.LENGTH_SHORT).show();
                        }//否则显示权限未授予的提示，Toast是Android的一个简易提示框
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);//Bundle是Android的一个数据存储类，用于保存活动状态
        setContentView(R.layout.activity_main);
        // 初始化权限管理器
        permissionManager = PermissionManager.getInstance(this);
        
        // 初始化视图
        floatingWindowButton = findViewById(R.id.floatingWindowButton);
        styleSettingsButton = findViewById(R.id.styleSettingsButton);
        localImageReplyButton = findViewById(R.id.localImageReplyButton);
        
        
        // 请求所有必要的权限
        requestAllPermissions();
        // 设置点击监听器
        setupClickListeners();
    }
    
    
    //设置所有按钮的点击事件监听器
    private void setupClickListeners() {
        //所有按钮都是先检查权限然后执行相应操作
        //也就是启动对应的activity或服务
        // 悬浮窗按钮点击事件
        floatingWindowButton.setOnClickListener(v -> checkAndRequestOverlayPermission());
        
        styleSettingsButton.setOnClickListener(v -> {
            // 样式设置按钮点击事件
            Intent intent = new Intent(this, StyleSettingsActivity.class);
            startActivity(intent);
        });
        
        localImageReplyButton.setOnClickListener(v -> {
            // 本地图片回复按钮点击事件
            Intent intent = new Intent(this, LocalImageReplyActivity.class);
            startActivity(intent);
        });
    }
    
    //检查并请求悬浮窗权限
    private void checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();//先显示悬浮窗权限请求对话框
        } else {
            startFloatingWindowService();
        }//如果已经有悬浮窗权限，则直接启动悬浮窗服务
    }
    
    //显示悬浮窗权限请求对话框
    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.floating_window_permission)
                .setMessage(R.string.floating_window_permission_description)
                .setPositiveButton(R.string.grant, (dialog, which) -> {
                    //AlertDialog是系统提供的对话框组件
                    // 用户点击授权按钮后，跳转到悬浮窗权限设置页面
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                    );
                    overlayPermissionLauncher.launch(intent);
                })
                .setNegativeButton(R.string.cancel, null)//设置否定权限按钮
                .show();
    }
    
    //启动悬浮窗服务
    private void startFloatingWindowService() {
        Intent intent = new Intent(this, FloatingWindowService.class);//创建一个Intent对象，指向FloatingWindowService服务
        
        // 在 Android 8.0 及以上版本使用 startForegroundService
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        android.util.Log.d("MainActivity", "Started floating window service");
        
        // 最小化应用
        moveTaskToBack(true);
    }
    
    //请求所有必要的权限
    private void requestAllPermissions() {
        // 检查并请求存储和悬浮窗权限
        permissionManager.checkAndRequestAllPermissions(this, allGranted -> {
            //lamada表达式，前面是返回的参数
            if (allGranted) {
                // 如果还没有媒体投影权限，请求它
                if (!permissionManager.hasMediaProjectionPermission()) {
                    requestMediaProjectionPermission();
                }
            }
        });
    }
    
     //请求媒体投影权限（截屏权限）

     private void requestMediaProjectionPermission() {
         MediaProjectionManager projectionManager =
                 (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
         startActivityForResult(projectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
         //createScreenCaptureIntent()方法用于创建一个intent，启动系统的屏幕捕获界面
     }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //onActivityResult()方法用于处理权限请求的结果
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            // 处理媒体投影权限结果
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 保存媒体投影权限结果
                permissionManager.saveMediaProjectionResult(resultCode, data);
                Toast.makeText(this, R.string.screenshot_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.screenshot_permission_denied, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            // 处理悬浮窗权限结果
            if (Settings.canDrawOverlays(this)) {
                startFloatingWindowService();
            } else {
                Toast.makeText(this, R.string.floating_window_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }
}