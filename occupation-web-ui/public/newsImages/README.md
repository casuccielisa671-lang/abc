# 新闻页面图片目录

将新闻配图放在本目录，前端引用路径格式：

```text
/newsImages/你的文件名.jpg
```

示例：

```text
occupation-web-ui/public/newsImages/banner-backend.jpg
→ 代码中写：/newsImages/banner-backend.jpg
```

开发阶段可使用网络占位图；下载本地图后保持文件名与 `src/views/Home/jobNewsData.js` 中一致即可自动切换。
