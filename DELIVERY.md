# 交付说明（可交付版本）

## 1. 版本与产物

- 版本：`1.0.0`
- 产物：
  - Debug APK（持续集成）
  - Release APK（tag 发布构建）
  - SHA256 校验文件

## 2. GitHub Secrets（Release 必需）

在仓库 `Settings -> Secrets and variables -> Actions` 配置：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## 3. 发布流程

1. 推送 tag（例如 `v1.0.0`）
2. CI 触发 release job
3. 生成并上传：
   - `app-release.apk`
   - `SHA256SUMS.txt`

工作流文件：`.github/workflows/android-build.yml`

## 4. 验收清单

- 聊天链路
  - 可配置 API 并进行流式对话
  - 可查看 AI 原始输出与网络日志
- 导入链路
  - PNG 角色卡 / 世界书 / 正则 / 预设导入成功
  - 冲突策略可切换（重命名/覆盖）
- 扩展链路
  - 可导入 manifest
  - 可启停扩展
  - beforeSend/afterReceive 钩子生效（含权限网关）
- 主题与前端
  - 主题可导入与应用
  - 前端 WebView 可加载 URL 且支持调试开关
- 控制台
  - 支持过滤关键字
  - 支持导出到本地文件

## 5. 已知限制

- 扩展脚本沙箱尚未实现（当前为配置驱动钩子）
- 事件总线为基础版本，未覆盖全部高级事件
- 回滚机制为后续增强项
