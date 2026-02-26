# Android Native SillyTavern

全新原生重写的安卓酒馆项目，当前已完成 M1 + 批次1 + 批次2 + 批次3 + 批次4 + 批次5（可交付基线）+ 批次6-阶段A（解耦重构）。

## 已完成功能

### M1 基础

- 多模块工程骨架
- 基础聊天页（可配置 `baseUrl / apiKey / model`）
- 用户 Persona 编辑
- 新聊天
- OpenAI 兼容流式输出
- 控制台页（请求日志 + 原始增量输出）
- GitHub Actions 自动构建 Debug APK

### 批次1：导入兼容 + 聊天联动

- 导入中心（文件选择导入）
- 兼容导入
  - PNG 角色卡（读取 `tEXt/iTXt` `chara` 元数据，支持 base64 JSON）
  - 世界书 JSON
  - 正则 JSON
  - 预设 JSON
- 聊天联动
  - 选择已导入角色参与系统提示拼装
  - 选择已导入预设并覆盖模型/温度
  - 世界书关键词命中后注入上下文
  - 正则对助手输出后处理
- 控制台增强
  - 导入成功/失败日志
  - 解析错误日志

### 批次2：基础扩展 + 主题 + 控制台增强

- 扩展基础
  - 扩展 `manifest` JSON 导入
  - 权限声明读取
  - 启用/停用开关
- 主题与 UI 美化
  - 主题 JSON 导入（`tokens`）
  - 主题应用（Material 颜色方案动态切换）
- 控制台增强
  - 日志导出文本能力（内存拼接）
  - 日志上限提升与稳定 key 渲染

### 批次3：扩展钩子 + 权限网关 + 日志落盘

- 扩展钩子与权限网关
  - `beforeSend` / `afterReceive` 钩子执行
  - `chatWrite` / `chatRead` 权限校验与拒绝日志
- 聊天链路接入
  - 发送前应用扩展前缀处理
  - 回复后应用扩展后缀处理
- 控制台导出增强
  - 导出到本地文件：`files/exports/console-*.log`
- 导入页增强
  - 扩展权限与钩子信息展示

### 批次4：前端显示 + 冲突策略可选 + 事件总线补齐

- 前端显示能力
  - 内置 WebView 页面（URL 输入/加载）
  - 调试开关（JS 开关）
- 导入冲突策略
  - 支持 `重命名` 与 `覆盖` 两种模式
  - 导入页可切换冲突策略
- 扩展事件总线
  - 已接入 `onAppStart` / `onSettingsOpen` 分发
- 控制台增强
  - 关键字过滤后查看与导出

## 模块说明

- `app`：应用入口、导航、主题应用、前端 WebView
- `core-model`：统一数据模型与导入冲突策略枚举
- `core-network`：Provider 网络与流式解析/控制台日志
- `core-storage`：导入解析、扩展/主题状态、扩展钩子网关、冲突策略
- `feature-chat`：聊天 UI 与编排联动
- `feature-import`：导入 UI 与管理列表（含扩展/主题/冲突策略）
- `feature-console`：控制台日志查看、过滤、落盘导出

## 批次6-阶段A 解耦结果

- 存储层已从单体对象解耦为接口边界：`LibraryRepository / ImportRepository / ExtensionRepository / ThemeRepository / ChatRepository`。
- 新增 `StorageProvider` 作为依赖装配入口，默认提供 `InMemoryAppStore` 实现。
- `AppRepository` 保留为兼容门面（delegation），旧调用不立即中断。
- `ChatViewModel` 与 `ImportViewModel` 已改为依赖接口而非直接耦合单体仓库实现。
- `MainActivity` 的主题与扩展事件分发已改为通过 `StorageProvider` 访问接口层。

## 批次7-阶段B 继续解耦

- 网络客户端已抽象为 `ChatClient` 接口，`OpenAiCompatibleClient` 改为接口实现。
- 新增 `ChatUseCase`，将发送链路（提示组装/扩展处理/流式回复后处理）从 `ChatViewModel` 中下沉。
- 新增 `ImportUseCase`，将导入/冲突策略/扩展开关/主题应用从 `ImportViewModel` 中下沉。
- `MainActivity` 不再直接依赖 `StorageProvider`，主题与启动事件改为经由 ViewModel 暴露与触发。

## 批次8-阶段C 继续解耦

- 新增应用级协调器 [`AppCoordinatorViewModel`](app/src/main/java/com/sillyandroid/app/AppCoordinatorViewModel.kt)，统一主题读取与应用启动事件桥接。
- [`MainActivity`](app/src/main/java/com/sillyandroid/app/MainActivity.kt) 仅依赖协调器 + 功能页面，进一步收敛跨 Feature 直接依赖。
- 为用例层增加端口接口：[`ChatUseCasePort`](feature-chat/src/main/java/com/sillyandroid/feature/chat/ChatUseCasePort.kt) 与 [`ImportUseCasePort`](feature-import/src/main/java/com/sillyandroid/feature/importer/ImportUseCasePort.kt)。
- [`ChatUseCase`](feature-chat/src/main/java/com/sillyandroid/feature/chat/ChatUseCase.kt) / [`ImportUseCase`](feature-import/src/main/java/com/sillyandroid/feature/importer/ImportUseCase.kt) 改为实现对应端口；ViewModel/Coordinator 依赖端口而非具体实现。

## 批次9-阶段D 继续解耦

- 新增应用依赖组装 [`AppGraph`](app/src/main/java/com/sillyandroid/app/AppGraph.kt)，集中装配 Repository/UseCase/ViewModel 依赖。
- 新增 [`ViewModelFactories`](app/src/main/java/com/sillyandroid/app/ViewModelFactories.kt) ，通过 Factory 从 `AppGraph` 注入 ViewModel。
- [`MainActivity`](app/src/main/java/com/sillyandroid/app/MainActivity.kt) 已接入 Factory，避免 ViewModel 隐式 new 具体实现。
- [`ChatUseCase`](feature-chat/src/main/java/com/sillyandroid/feature/chat/ChatUseCase.kt) 与 [`ImportUseCase`](feature-import/src/main/java/com/sillyandroid/feature/importer/ImportUseCase.kt) 移除默认 `StorageProvider` 参数，改为纯构造注入。

## 本地构建

1. 使用 Android Studio 打开 `android-native-sillytavern`
2. 等待 Gradle 同步
3. 运行 `app` 模块

## GitHub 构建 APK

- 工作流文件：`.github/workflows/android-build.yml`
- push 或手动触发后，在 Artifacts 下载 Debug APK

## 下一阶段（批次5）

- 扩展脚本沙箱（受限运行时）
- 扩展事件总线标准化（更多 hook 与参数）
- 导入事务回滚与批量失败恢复
- release 签名与发布流水线固化
