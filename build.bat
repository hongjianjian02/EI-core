@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
call "D:\Java in send\gradle\gradle-9.2.1\bin\gradle.bat" build -g ".gradle-home" --no-daemon --console=plain > build.log 2>&1
echo EXIT=%ERRORLEVEL% >> build.log