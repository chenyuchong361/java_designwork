# Script: build-user-package.ps1
# Purpose: Build a Windows user ZIP that launches with installed Java 17+ when available and falls back to a bundled runtime.
# Author: Codex
# Created: 2026-05-11
# Last Updated: 2026-05-11
# Dependencies: Maven, JDK 17 with jdeps and jlink
# Usage: powershell -ExecutionPolicy Bypass -File scripts\build-user-package.ps1
#
# Changelog:
# - 2026-05-11 Codex: Initial creation.
# - 2026-05-11 Codex: Created release packaging script with an installed-Java-first launcher and bundled runtime fallback. Reason: Support both developer and non-developer users. Impact: backward compatible.

[CmdletBinding()]
param(
    [string]$PackageName = "MindMapCourseDesign",
    [string]$JarName = "MindMapCourseDesign.jar"
)

$ErrorActionPreference = "Stop"

function Get-RequiredCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $command) {
        throw "Required command '$Name' was not found. Install JDK 17 and Maven before building the user package."
    }

    return $command.Source
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory
    )

    Push-Location $WorkingDirectory
    try {
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
        }
    }
    finally {
        Pop-Location
    }
}

function Remove-PathInside {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Parent
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    $resolvedPath = (Resolve-Path -LiteralPath $Path).Path
    $resolvedParent = (Resolve-Path -LiteralPath $Parent).Path

    if (-not $resolvedPath.StartsWith($resolvedParent, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the release directory: $resolvedPath"
    }

    Remove-Item -LiteralPath $resolvedPath -Recurse -Force
}

function Get-RuntimeModules {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JdepsPath,

        [Parameter(Mandatory = $true)]
        [string]$JarPath
    )

    $jdepsOutput = & $JdepsPath "--multi-release" "17" "--ignore-missing-deps" "--print-module-deps" $JarPath 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "jdeps could not infer modules. Falling back to the modules needed by this Swing application."
        Write-Warning ($jdepsOutput -join [Environment]::NewLine)
        $jdepsOutput = @("java.base,java.desktop,java.xml")
    }

    $modules = $jdepsOutput |
        Where-Object { $_ -match "^[a-zA-Z0-9_.]+(,[a-zA-Z0-9_.]+)*$" } |
        Select-Object -Last 1

    if ([string]::IsNullOrWhiteSpace($modules)) {
        $modules = "java.base,java.desktop,java.xml"
    }

    $moduleMap = @{}
    foreach ($module in ($modules -split ",")) {
        $trimmed = $module.Trim()
        if ($trimmed.Length -gt 0) {
            $moduleMap[$trimmed] = $true
        }
    }

    $moduleMap["java.desktop"] = $true
    $moduleMap["java.xml"] = $true
    $moduleMap["jdk.charsets"] = $true

    return (($moduleMap.Keys | Sort-Object) -join ",")
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot
$distRoot = Join-Path $repoRoot "dist"
$packageRoot = Join-Path $distRoot $PackageName
$appRoot = Join-Path $packageRoot "app"
$runtimeRoot = Join-Path $packageRoot "runtime"
$zipPath = Join-Path $distRoot "$PackageName.zip"

$mavenPath = Get-RequiredCommand "mvn"
$jdepsPath = Get-RequiredCommand "jdeps"
$jlinkPath = Get-RequiredCommand "jlink"

New-Item -ItemType Directory -Force -Path $distRoot | Out-Null
Remove-PathInside -Path $packageRoot -Parent $distRoot
Remove-PathInside -Path $zipPath -Parent $distRoot
New-Item -ItemType Directory -Force -Path $appRoot | Out-Null

Write-Host "Building project jar..."
Invoke-Checked -FilePath $mavenPath -Arguments @("-q", "package") -WorkingDirectory $repoRoot

$sourceJar = Join-Path $repoRoot "target\mindmap-course-design-1.0-SNAPSHOT.jar"
if (-not (Test-Path -LiteralPath $sourceJar)) {
    throw "Expected jar was not generated: $sourceJar"
}

Copy-Item -LiteralPath $sourceJar -Destination (Join-Path $appRoot $JarName) -Force

$modules = Get-RuntimeModules -JdepsPath $jdepsPath -JarPath $sourceJar
Write-Host "Creating bundled runtime with modules: $modules"
Invoke-Checked -FilePath $jlinkPath -Arguments @(
    "--add-modules", $modules,
    "--strip-debug",
    "--no-header-files",
    "--no-man-pages",
    "--compress=2",
    "--output", $runtimeRoot
) -WorkingDirectory $repoRoot

$today = Get-Date -Format "yyyy-MM-dd"
$launcherTemplate = @'
@echo off
REM Script: Start.bat
REM Purpose: Launch MindMapCourseDesign with installed Java 17+ when available, otherwise use the bundled runtime.
REM Author: Codex
REM Created: __TODAY__
REM Last Updated: __TODAY__
REM Dependencies: Java 17+ or bundled runtime
REM Usage: Double-click Start.bat
REM
REM Changelog:
REM - __TODAY__ Codex: Initial creation.
REM - __TODAY__ Codex: Created launcher to prefer local Java 17+ and fall back to bundled runtime. Reason: Support both developer and non-developer users. Impact: backward compatible.

setlocal EnableExtensions
set "APP_HOME=%~dp0"
set "APP_JAR=%APP_HOME%app\__JAR_NAME__"
set "BUNDLED_JAVA=%APP_HOME%runtime\bin\java.exe"
set "JAVA_EXE="

if not "%JAVA_HOME%"=="" call :try_java "%JAVA_HOME%\bin\java.exe"
if defined JAVA_EXE goto run

call :try_java "java"
if defined JAVA_EXE goto run

if exist "%BUNDLED_JAVA%" (
    set "JAVA_EXE=%BUNDLED_JAVA%"
    goto run
)

echo Unable to find Java 17 or newer, and the bundled runtime is missing.
echo Keep the runtime folder next to this file, or install Java 17+.
pause
exit /b 1

:run
if not exist "%APP_JAR%" (
    echo Application jar was not found:
    echo %APP_JAR%
    pause
    exit /b 1
)

echo Starting MindMapCourseDesign...
"%JAVA_EXE%" -jar "%APP_JAR%"
if errorlevel 1 (
    echo Application exited with an error.
    pause
)
exit /b %errorlevel%

:try_java
set "CANDIDATE=%~1"
if "%CANDIDATE%"=="" exit /b 1

"%CANDIDATE%" -version >nul 2>&1
if errorlevel 1 exit /b 1

set "JAVA_VERSION="
for /f "tokens=3" %%v in ('"%CANDIDATE%" -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.version ="') do set "JAVA_VERSION=%%v"
if "%JAVA_VERSION%"=="" exit /b 1

for /f "tokens=1 delims=." %%m in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%m"
if "%JAVA_MAJOR%"=="1" for /f "tokens=2 delims=." %%m in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%m"

set /a JAVA_MAJOR_NUM=%JAVA_MAJOR% >nul 2>&1
if errorlevel 1 exit /b 1

if %JAVA_MAJOR_NUM% GEQ 17 (
    set "JAVA_EXE=%CANDIDATE%"
    exit /b 0
)

exit /b 1
'@

$launcher = $launcherTemplate.Replace("__TODAY__", $today).Replace("__JAR_NAME__", $JarName)
Set-Content -LiteralPath (Join-Path $packageRoot "Start.bat") -Value $launcher -Encoding ASCII

$readme = @"
MindMapCourseDesign user package
Generated: $today

How to run
1. Unzip this package.
2. Double-click Start.bat.

Runtime strategy
- Start.bat first checks JAVA_HOME for Java 17 or newer.
- Then it checks java from PATH.
- If neither is available, it uses runtime\bin\java.exe from this package.

Notes
- Do not remove the app or runtime folders.
- This package does not install Java globally on the user's computer.
- Developers can keep using their installed Java; non-developer users can run with the bundled runtime.
"@
Set-Content -LiteralPath (Join-Path $packageRoot "README.txt") -Value $readme -Encoding UTF8

Compress-Archive -Path $packageRoot -DestinationPath $zipPath -Force
Write-Host "User package created: $zipPath"
