# 用户版压缩包方案

本项目支持生成一个面向普通用户的 Windows 压缩包：`dist/MindMapCourseDesign.zip`。

压缩包采用“优先使用本机 Java，缺失时使用内置运行时”的策略：

- 启动脚本先检查 `JAVA_HOME` 是否指向 Java 17 或更新版本。
- 然后检查 `PATH` 中的 `java` 是否是 Java 17 或更新版本。
- 如果用户本机没有可用 Java，则使用压缩包内的 `runtime\bin\java.exe`。
- 压缩包不会把 Java 安装到用户系统里，因此不需要管理员权限，也不会污染开发者环境。

## 构建方式

在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-user-package.ps1
```

构建机器需要安装：

- Maven
- JDK 17，并且包含 `jdeps` 和 `jlink`

生成后交付给用户：

```text
dist\MindMapCourseDesign.zip
```

## 用户使用方式

用户解压后双击：

```text
Start.bat
```

如果用户电脑已经安装 Java 17 或更新版本，程序会直接使用用户本机的 Java。否则程序会自动使用压缩包里附带的运行时。
