# AGENTS.md

> 开始协作前，请先在 `Collaborators，author name` 中添加自己的名字。  
> 这样 Agent 在生成脚本头注释、Changelog、Git commit、PR 描述时，可以自动带出正确的协作者名字。

## Collaborators

- Codex
- chenyuchong


---

 Script Header Template

```text
Script: <file name>
Purpose: <what this script does>
Author: <chenyuchong>
Created: <YYYY-MM-DD>
Last Updated: <YYYY-MM-DD>
Dependencies: <deps or None>
Usage: <usage or N/A>

Changelog:
- <YYYY-MM-DD> <author>: Initial creation.
- <YYYY-MM-DD> <modifier>: Modified <specific change>. Reason: <reason>. Impact: <backward compatible / breaking change / no behavior change>.

```


## 1. Scope

本规范适用于本仓库所有：

- 新增脚本
- 修改脚本
- 自动化脚本
- 核心模块
- Agent 生成或修改的代码
- Agent 帮助生成的 Git commit / PR 描述

目标：

1. 可读
2. 可追溯
3. 可协作

---

## 2. Must Follow

### 2.1 脚本头注释强制要求

所有新建或修改的脚本，文件最上方必须包含头注释，至少写明：

- Script
- Purpose
- Author
- Created
- Last Updated
- Dependencies
- Usage
- Changelog

### 2.2 修改别人脚本时强制要求

如果修改的是别人写的脚本，`Changelog` 必须明确写出：

- 修改人
- 原作者
- 修改了什么
- 为什么改
- 是否影响兼容性

禁止模糊描述：

- updated
- optimized
- fixed bug
- modified
- 调整
- 优化一下

### 2.3 Agent 协作要求

AI Agent 参与时必须：

- 保留原作者信息
- 不删除历史 Changelog
- 追加本次修改记录
- 不伪造历史
- 不在未说明影响时做大规模重构
- 输出内容默认遵循本文件规范
- 帮协作者生成 Git commit 时必须使用本文件中的提交规范

---

## 3. Git Commit Rules

feat：新增用户可感知功能                          
add：新增脚本 / 模板 / 配置 / 辅助资源
fix：修复缺陷
refactor：重构实现，不改业务功能
docs：文档修改
test：测试新增或修改
style：仅格式调整，不改逻辑
chore：杂项维护
perf：性能优化
ci：CI/CD 修改
build：构建或发布流程修改
revert：回滚提交

Agent Git Commit Policy：

当协作者要求 Agent 帮忙生成、整理或代写 Git 提交说明时，Agent 必须：

先根据改动内容判断提交类型
优先输出符合规范的 commit message
提交说明必须具体，不得使用模糊词
如果改动包含多个独立主题，优先建议拆分多个 commit
如果只能合并成一个 commit，使用最主要的改动类型
提交说明应与脚本头部 Changelog 保持语义一致
输出 commit 时优先使用以下格式：

Commit: <type>(<scope>): <summary>

示例：

Commit: fix(config): handle missing env file gracefully
Commit: refactor(sync): split validation and persistence logic

---


## 4. . PR Rules

PR 描述至少写明：

Purpose
Changes
Risk
Compatibility

模板：
```text
## Purpose
Why this change is needed.

## Changes
- item 1
- item 2

## Risk
Low / Medium / High

## Compatibility
Backward compatible / Breaking change
```
