package edu.zjut.androiddeveloper_520_4.tyan;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/*
聊天消息列表适配器，支持多种消息类型：
每种消息类型对应不同的布局和ViewHolder
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    //聊天页面使用列表布局recycleview，实现view复用，滚动最新的视图时，支持重新绑定view
    //继承自RecyclerView.Adapter，泛化参数是<RecyclerView.ViewHolder>
    // 聊天消息数据列表
    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    @Override
    /*
     获取指定位置的消息类型
     */
    public int getItemViewType(int position) {
        return messages.get(position).getType();//position指定当前消息
    }
    //获取消息的具体类型
    @NonNull
    @Override
    /*
     创建对应消息类型的ViewHolder，保存组件的缓存，避免重复引用，提升视图性能，其实是个容器
     */
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        /*
        LayoutInflater: Android系统服务，负责将XML布局转换为View对象,动态设置部分视图
         */
        
        switch (viewType) {
            case ChatMessage.TYPE_USER:
                View userView = inflater.inflate(R.layout.item_user_message, parent, false);
                return new UserMessageViewHolder(userView);
                
            case ChatMessage.TYPE_AI:
                View aiView = inflater.inflate(R.layout.item_ai_message, parent, false);
                return new AIMessageViewHolder(aiView);
                
            case ChatMessage.TYPE_SYSTEM:
                View systemView = inflater.inflate(R.layout.item_system_message, parent, false);
                return new SystemMessageViewHolder(systemView);
                
            case ChatMessage.TYPE_LOADING:
                View loadingView = inflater.inflate(R.layout.item_loading_message, parent, false);
                return new LoadingMessageViewHolder(loadingView);
                
            case ChatMessage.TYPE_IMAGE:
                View imageView = inflater.inflate(R.layout.item_image_message, parent, false);
                return new ImageMessageViewHolder(imageView);
                
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }
    
    @Override
    //绑定数据到ViewHolder
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        //从消息列表 messages 中根据位置 position 获取对应的聊天消息对象，position是逻辑索引，不是真实的屏幕位置
        switch (holder.getItemViewType()) {
            case ChatMessage.TYPE_USER:
                ((UserMessageViewHolder) holder).bind(message);//绑定数据
                break;
                
            case ChatMessage.TYPE_AI:
                ((AIMessageViewHolder) holder).bind(message);
                break;
                
            case ChatMessage.TYPE_SYSTEM:
                ((SystemMessageViewHolder) holder).bind(message);
                break;
                
            case ChatMessage.TYPE_LOADING:
                // No binding needed for loading
                break;
                
            case ChatMessage.TYPE_IMAGE:
                ((ImageMessageViewHolder) holder).bind(message);
                break;
        }
    }
    
    @Override
    //获取消息总数
    public int getItemCount() {
        return messages.size();
    }

    //用户消息ViewHolder，显示用户发送的文本消息
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;//其实这里就是一个文本框
        
        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }//根据xml文件中的组件初始化viewholder
        
        void bind(ChatMessage message) {//所谓数据绑定就是把内容设置到文本组件中
            messageText.setText(message.getText());
        }
    }

    //I消息ViewHolder，显示AI回复的文本消息
    static class AIMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        
        AIMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }
        
        void bind(ChatMessage message) {
            messageText.setText(message.getText());
        }
    }

    //系统消息ViewHolder，显示系统通知类消息
    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        
        SystemMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }
        
        void bind(ChatMessage message) {
            messageText.setText(message.getText());
        }
    }

    //加载状态ViewHolder，显示加载进度条和提示文本

    static class LoadingMessageViewHolder extends RecyclerView.ViewHolder {
        private final ProgressBar progressBar;
        private final TextView loadingText;
        
        LoadingMessageViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            loadingText = itemView.findViewById(R.id.loading_text);
        }
    }

    //图片消息ViewHolder，显示图片消息
    static class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        
        ImageMessageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.message_image);
        }
        
        void bind(ChatMessage message) {
            if (message.getImageUrl() != null) {//图片可能为空
                imageView.setImageURI(Uri.parse(message.getImageUrl()));
            }
        }
    }
}
