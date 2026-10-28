@echo off
setlocal
if exist "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\javac.exe" (
    set "PATH=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin;%PATH%"
)
echo [Malibu] Compiling CasinoApp.java...
javac -cp "lib/mysql-connector-j-26.10.0.jar" -d bin src/CasinoApp.java
if %errorlevel% equ 0 (
    echo [Malibu] Launching Malibu Casino Floor & Cage Management System...
    java -cp "bin;lib/mysql-connector-j-26.10.0.jar" CasinoApp
) else (
    echo [ERROR] Compilation failed.
    pause
)
endlocal
