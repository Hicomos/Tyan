package edu.zjut.androiddeveloper_520_4.tyan;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/*
  悬浮窗服务，负责：
 1. 创建和管理悬浮窗视图
 2. 处理悬浮窗的拖动和点击事件
 3. 启动截屏服务
 4. 保持前台服务状态
 */
public class FloatingWindowService extends Service {
    // 日志标签
    private static final String TAG = "FloatingWindowService";
    
    // 窗口管理相关
    private WindowManager windowManager;  // 窗口管理器实例
    private View floatingView;           // 悬浮窗视图
    private WindowManager.LayoutParams params; // 窗口布局参数
    
    // 触摸事件相关变量
    private int initialX;          // 悬浮窗初始X坐标
    private int initialY;          // 悬浮窗初始Y坐标
    private float initialTouchX;   // 触摸点初始X坐标
    private float initialTouchY;   // 触摸点初始Y坐标
    
    // 通知相关变量
    // 通知相关常量
    private static final int NOTIFICATION_ID = 2001;       // 通知ID
    private static final String CHANNEL_ID = "floating_window_channel"; // 通知渠道ID

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FloatingWindowService onStartCommand");
        // 必须在服务创建后立即调用 startForeground
        
        return START_STICKY;//服务被异常终止后会自动重启
    }


    /*
     创建通知
     */
    private Notification createNotification() {
        // 创建通知内容
        String title = getString(R.string.app_name);
        String content = getString(R.string.floating_window);

        // 创建通知
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        // 设置通知内容
        builder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_floating_bubble)
                .setOngoing(true);

        return builder.build();
    }

    /*
     服务创建时调用，主要初始化工作：
     1. 创建通知渠道和前台通知
     2. 初始化窗口管理器和悬浮窗视图
     3. 设置触摸事件监听器
     */
    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.d(TAG, "FloatingWindowService onCreate");

        // 立即启动前台服务，必须在 onCreate 中就调用
        startForeground(NOTIFICATION_ID, createNotification());
        
        // 初始化窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 加载悬浮窗布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window_layout, null);
        
        // 设置窗口管理器参数
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );//PixelFormat.TRANSLUCENT表示透明像素格式
        
        // 初始位置
        params.gravity = Gravity.TOP | Gravity.START;//Gravity是一个用于设置视图位置的常量
        params.x = 0;
        params.y = 100;
        
        // 设置触摸监听器，处理拖动和点击
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 记录初始位置
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        // 计算移动距离
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        
                        // 更新悬浮窗位置
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        // 如果移动距离很小，则视为点击
                        int DeltaX = (int) (event.getRawX() - initialTouchX);
                        int DeltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(DeltaX) < 10 && Math.abs(DeltaY) < 10) {
                            // 处理点击事件 - 启动截屏服务
                            Intent intent = new Intent(FloatingWindowService.this, ScreenshotService.class);
                            // 使用 startForegroundService 而不是 startService
                            // 这样可以确保服务在前台运行，不会被系统轻易杀死
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
        
        // 将视图添加到窗口
        windowManager.addView(floatingView, params);
    }

    /*
      服务销毁时调用，清理资源：
     1. 移除悬浮窗视图
     2. 释放相关资源
     */
    @Override
    public void onDestroy() {
        android.util.Log.d(TAG, "FloatingWindowService onDestroy");
        super.onDestroy();
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView);
                floatingView = null;
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error removing floating view: " + e.getMessage());
            }
        }
    }
}
