package edu.zjut.androiddeveloper_520_4.tyan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

/*
 截屏工具类，用于处理截屏操作
 */
public class ScreenshotUtil {
    private static final String TAG = "ScreenshotUtil";
    
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;// 用于显示截屏内容的虚拟显示
    private ImageReader imageReader;// 用于接收截屏数据，属于安卓中间件
    private int width;
    private int height;
    private int density;// DPI，即每英寸像素数
    private Handler handler;// 用于处理回调的主线程Handler
    
    public interface ScreenshotCallback {
        void onScreenshotTaken(Bitmap bitmap);
    }
    
    public ScreenshotUtil(Context context) {
        this.handler = new Handler(Looper.getMainLooper());//Looper.getMainLooper() 获取主线程的 Looper，Looper 是 Android 中的消息循环机制，用于处理 UI 线程的消息和事件
        
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);// 获取窗口管理服务
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        this.width = metrics.widthPixels;
        this.height = metrics.heightPixels;
        this.density = metrics.densityDpi;
    }//屏幕尺寸和密度获取
    
    /*
     设置 MediaProjection
     @param mediaProjection MediaProjection 对象
     */
    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }
    
    /*
     开始截屏
     @param callback 截屏完成后的回调
     */
    public void takeScreenshot(final ScreenshotCallback callback) {
        if (mediaProjection == null) {
            return;
        }
        
        // 确保先停止之前的截屏，释放资源
        stopScreenshot();
        
        // 重新创建 ImageReader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);//PixelFormat.RGBA_8888 表示每个像素使用 4 个字节（红、绿、蓝、透明通道），2 表示最多保留 2 张图片
        
        // 重新创建 VirtualDisplay
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                VIRTUAL_DISPLAY_FLAGS,
                imageReader.getSurface(),
                null,
                handler
        );
        
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();// 获取图像的平面数据
                    ByteBuffer buffer = planes[0].getBuffer();// 获取第一个平面的字节缓冲区
                    int pixelStride = planes[0].getPixelStride();// 每个像素的字节数
                    int rowStride = planes[0].getRowStride();// 每行的字节数
                    int rowPadding = rowStride - pixelStride * width;// 计算行填充字节数
                    
                    Bitmap bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                    );// 创建一个新的 Bitmap 对象，宽度加上行填充字节数，使用 ARGB_8888 配置
                    bitmap.copyPixelsFromBuffer(buffer);
                    
                    // 裁剪到正确的宽度
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                    bitmap.recycle();// 回收原始 Bitmap，避免内存泄漏
                    
                    // 单次截屏，直接回调
                    if (callback != null) {
                        callback.onScreenshotTaken(croppedBitmap);
                    }
                    stopScreenshot();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }, handler);
    }
    
    
    /*
     停止截屏
     */
    public void stopScreenshot() {
        if (virtualDisplay != null) {
            try {
                virtualDisplay.release();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error releasing virtualDisplay: " + e.getMessage());
            } finally {
                virtualDisplay = null;
            }
        }
        if (imageReader != null) {
            try {
                // 确保先移除监听器，再关闭imageReader
                imageReader.setOnImageAvailableListener(null, null);
                imageReader.close();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error closing imageReader: " + e.getMessage());
            } finally {
                imageReader = null;
            }
        }
    }
    
    /*
     释放资源
     */
    public void release() {
        stopScreenshot();
        // 不要在这里停止MediaProjection，因为它是由PermissionManager管理的
        // 只需要清除引用
        mediaProjection = null;
    }
}
