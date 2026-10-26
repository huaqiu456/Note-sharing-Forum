# 前端 xiaohashu-web

## 基本信息

| 项 | 值 |
| --- | --- |
| 容器内端口 | 80 |
| 对外端口 | 8087 |
| 技术栈 | Vue3 + TypeScript + Vite + Element Plus + Pinia + Vue Router + Axios |
| 运行方式 | Nginx 托管静态资源 + 反向代理 |

## 目录结构

```
xiaohashu-web/src/
├── api/          auth.ts / user.ts / note.ts / file.ts
├── utils/        request.ts（axios 实例与拦截器）
├── stores/       auth.ts（token 状态）
├── router/       路由与登录守卫
├── layouts/      MainLayout.vue
├── types/        接口类型定义
└── views/        Login / Home / NotePublish / NoteDetail / Search / Profile
```

## 页面与功能

| 页面 | 路径 | 功能 |
| --- | --- | --- |
| 登录 | `/login` | 验证码登录、密码登录 |
| 首页 | `/home` | 笔记列表分页（按发布时间倒序） |
| 发布/编辑 | `/publish` | 图/视频上传、发布与编辑 |
| 详情 | `/note/:id` | 展示标题、正文、图片/视频 |
| 搜索 | `/search` | 关键词搜索笔记 |
| 我的 | `/profile` | 个人资料展示与修改、修改密码、我的笔记 |

## 请求封装

- `request.ts`：请求自动带 `Authorization`；响应统一处理 `success=false`。
- **401**：清 token 并跳登录页。
- **403**：仅提示「权限不足」，不清 token、不跳登录页。

## Nginx 配置（容器内）

| 路径 | 转发目标 |
| --- | --- |
| `/` | 本地静态资源（history 模式回退 `index.html`） |
| `/auth/`、`/user/`、`/note/`、`/file/` | `http://xiaohashu-gateway:8000` |
| `/images/` | `http://host.docker.internal:9000/xiaohashu/`（内部 MinIO） |

## 构建

- 镜像采用多阶段构建：`node:20-alpine` 构建 `dist` → `nginx:alpine` 托管。
- 生产构建命令：`npm run build`。
