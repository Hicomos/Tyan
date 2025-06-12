# Tyan - Android 智能回复应用

Tyan 是一款基于Android平台的智能对话应用，具备本地图片识别回复、浮动窗口聊天等创新功能。目前支持Android 9

## ✨ 功能特点

- 📷 本地图片智能识别与回复
- 🪟 浮动窗口聊天模式
- 📸 截图识别与自动回复
- 🎨 可自定义的聊天界面样式
- 🤖 AI智能对话引擎

## 🛠️ 技术栈

- Android SDK
- Java
- Material Design
- 浮动窗口服务
- 截图识别技术

## 📦 安装指南

1. 克隆仓库：
```bash
git clone https://github.com/yourusername/tyan.git
```

2. 使用Android Studio打开项目

3. 构建并运行：
```bash
./gradlew assembleDebug
```

## 🖼️ 截图预览

![主界面截图](app/src/main/图片1.png)
![悬浮窗截图](app/src/main/图片2.png)
![本地上传截图](app/src/main/图片3.png)
![图标](app/src/main/tubiao-playstore.png)

## 🏗️ 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/edu/zjut/androiddeveloper_xxx/tyan/
│   │   │   ├── LocalImageReplyActivity.java - 本地图片回复功能
│   │   │   ├── FloatingWindowService.java - 浮动窗口服务
│   │   │   ├── ScreenshotService.java - 截图服务
│   │   │   └── MainActivity.java - 主界面
│   │   └── res/ - 资源文件
├── build.gradle - 模块构建配置
```

## 📝 使用说明

1. 启动应用后授予必要权限
2. 使用浮动窗口按钮开启悬浮聊天
3. 点击图片图标选择本地图片进行识别
4. 在设置中调整界面样式

## 📜 许可证

本项目采用 [MIT License](LICENSE)