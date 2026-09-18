# 安装 Android SDK v2 —— 可靠的许可应答（Start-Process 标准输入重定向）
$ErrorActionPreference = 'Continue'
$sdkRoot = "D:\applications\Android\Sdk"
$sdkmanager = "$sdkRoot\cmdline-tools\latest\bin\sdkmanager.bat"

# 1) 若 cmdline-tools 不存在则下载解压（上次已完成会跳过）
if (-not (Test-Path $sdkmanager)) {
  New-Item -ItemType Directory -Force $sdkRoot | Out-Null
  $zip = "$env:TEMP\android-cmdline-tools.zip"
  Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile $zip -UseBasicParsing
  Expand-Archive -Path $zip -DestinationPath "$sdkRoot\_tmp" -Force
  New-Item -ItemType Directory -Force "$sdkRoot\cmdline-tools\latest" | Out-Null
  Copy-Item "$sdkRoot\_tmp\cmdline-tools\*" "$sdkRoot\cmdline-tools\latest\" -Recurse -Force
  Remove-Item "$sdkRoot\_tmp" -Recurse -Force
}

# 2) 准备 500 行 "y" 应答文件
$answers = "$env:TEMP\sdk-licenses-yes.txt"
1..500 | ForEach-Object { "y" } | Set-Content -Path $answers -Encoding ASCII -Force

Write-Host "==> 接受许可（--licenses）..."
Start-Process -FilePath $sdkmanager -ArgumentList "--sdk_root=`"$sdkRoot`"", "--licenses" -RedirectStandardInput $answers -NoNewWindow -Wait

Write-Host "==> 安装组件..."
Start-Process -FilePath $sdkmanager -ArgumentList "--sdk_root=`"$sdkRoot`"", "platform-tools", "platforms;android-36", "build-tools;36.0.0" -RedirectStandardInput $answers -NoNewWindow -Wait

Write-Host "==> 已安装组件："
& $sdkmanager --sdk_root="$sdkRoot" --list_installed
Write-Host "==> 完成"
