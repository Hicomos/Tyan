package edu.zjut.androiddeveloper_520_4.tyan;

/*
聊天消息数据模型类，包含以下信息：
1. 消息类型（用户、AI、系统等）
2. 消息文本内容
3. 图片消息的URL（如果是图片消息）
 */
public class ChatMessage {
    // 消息类型常量定义
    public static final int TYPE_USER = 1;    // 用户发送的消息
    public static final int TYPE_AI = 2;      // AI回复的消息
    public static final int TYPE_SYSTEM = 3; // 系统通知消息
    public static final int TYPE_LOADING = 4;// 加载中状态
    public static final int TYPE_IMAGE = 5;  // 图片消息
    
    private final int type;       // 消息类型，使用上述常量定义
    private final String text;   // 消息文本内容，对于图片消息可能为null
    private final String imageUrl; // 图片URL，仅图片消息有效

    public ChatMessage(int type, String text, String imageUrl) {
        this.type = type;
        this.text = text;
        this.imageUrl = imageUrl;
    }
    
    //获取消息类型
    public int getType() {
        return type;
    }
    
    //获取消息文本内容
    public String getText() {
        return text;
    }
    
    //获取图片URL
    public String getImageUrl() {
        return imageUrl;
    }
}
