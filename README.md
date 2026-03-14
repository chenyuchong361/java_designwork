# 思维导图绘制工具

这是一个基于 Java Swing 实现的课程设计项目，对应题目中的“思维导图绘制工具”。程序支持单中心节点思维导图的创建、编辑、保存打开、自定义 `.dt` 文件格式存储，以及导出 `PNG/JPG` 图片。

## 已实现功能

- Swing 图形界面，包含工具栏、绘图区、结构显示区和状态栏
- 新建思维导图，初始自动生成一个中心节点
- 鼠标单击节点选中，双击节点快速重命名
- 为选中节点添加子节点
- 为非中心节点添加兄弟节点
- 删除非中心节点，并同时删除该节点的整棵子树
- 保存/另存为自定义 `.dt` 文件
- 打开已保存的 `.dt` 文件继续编辑
- 导出为 `PNG` 或 `JPG` 图片
- 自动布局、左侧布局、右侧布局三种结构布局方式
- 右侧树形结构区与画布选中状态联动

## 项目结构

- `src/main/java/com/course/mindmap/model`：思维导图数据模型
- `src/main/java/com/course/mindmap/layout`：节点布局算法
- `src/main/java/com/course/mindmap/io`：`.dt` 文件读写
- `src/main/java/com/course/mindmap/ui`：Swing 界面与交互

## 运行方式

### 方式一：使用 Maven 直接运行

```bash
mvn exec:java
```

### 方式二：先打包再运行

```bash
mvn clean package
java -jar target/mindmap-course-design-1.0-SNAPSHOT.jar
```

### Windows 快速启动

```bat
run.bat
```

## 文件格式说明

程序使用自定义扩展名 `.dt`，实际内容为 UTF-8 编码的 XML，保存了：

- 导图标题
- 布局方式
- 每个节点的 `id`
- 节点文本
- 节点层级结构

## 交互说明

- 单击空白区域：取消选中
- 单击节点：选中节点
- 双击节点：重命名节点
- 选中中心节点：可添加子节点、切换布局
- 选中普通节点：可添加子节点、兄弟节点、删除节点

## 测试

项目包含两项基础测试：

- 文件保存/读取回归测试
- 自动布局结果测试

可执行：

```bash
mvn test
```

