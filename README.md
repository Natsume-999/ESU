# 界限（ESU）

基于 **Paper 1.21.4** 与 **PluginBase** 框架的 RPG 服务器插件项目。

> **命名说明**：对外服务器/Wiki 品牌名为「**界限**」；`ESU` 为底层技术工程名（仓库名、插件 jar、数据目录 `plugins/ESU/`）。两者并存。

本仓库包含两个独立的 Wiki 站点，**默认繁体中文**（台湾），可切换简体中文（繁简均自动生成）。

## 双 Wiki 结构

| Wiki | 目录 | 受众 | 可见性 |
|------|------|------|--------|
| 玩家 Wiki | `wiki-player/` | 玩家 | 公开 |
| 策划 Wiki | `wiki-planning/` | 策划团队 | 私有（内部） |

- **玩家 Wiki**：怎么玩、武器流派、技能、攻略，面向玩家的轻松向导。
- **策划 Wiki**：设计愿景、系统机制、数值平衡，团队内部策划文档（GDD）。

## 本地查看

```bash
npm install

# 玩家 Wiki
npm run player:dev

# 策划 Wiki
npm run planning:dev
```

浏览器打开终端提示的地址（默认 `http://localhost:5173`）。右上角可切换简体/繁体。

## 简繁双语机制

- **只维护简体**。繁体由 `scripts/convert-tw.mjs` 用 OpenCC 自动生成到各 Wiki 的 `zh-tw/` 目录。
- 转换在 `dev`/`build` 前自动运行（`npm run convert` 也可手动触发）。
- 繁体目录不入 git（已加入 `.gitignore`），随构建自动生成，永不与简体脱节。
- OpenCC 使用台湾正体词库，自带用词本地化（如「软件」→「軟體」）。

::: tip 新增/修改文档
只改简体 Markdown，无需碰繁体。下次 `dev`/`build` 会自动重新生成繁体。
:::

## 构建

```bash
# 玩家 Wiki（含简繁转换）
npm run player:build      # 产物在 wiki-player/.vitepress/dist

# 策划 Wiki
npm run planning:build    # 产物在 wiki-planning/.vitepress/dist
```

## 插件构建

```bash
./gradlew clean build
```

产物：`build/libs/ESU-<版本>-all.jar`，复制到服务端 `plugins/` 即可。

## 环境要求

- JDK 21、Paper 1.21.4（插件）
- Node.js（Wiki）

## 部署

### 玩家 Wiki（公开）

`.github/workflows/deploy-docs.yml` 自动部署玩家 Wiki 到 GitHub Pages。推送 `wiki-player/` 改动到 `main`/`master` 即触发。需在仓库 Settings → Pages 将来源设为 **GitHub Actions**。

### 策划 Wiki（私有）

策划 Wiki 为内部资料，**不公开部署**。团队成员本地查看：

1. 加为仓库协作者（仓库 Settings → Collaborators）
2. `clone` 后运行 `npm install && npm run planning:dev`

::: tip 子路径部署
若玩家 Wiki 部署到 `用户名.github.io/仓库名/`，需修改 `wiki-player/.vitepress/config.mjs` 的 `base` 为 `'/仓库名/'`。
:::
