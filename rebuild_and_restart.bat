@echo off
echo Rebuilding and restarting the application...

REM Stop any running application
taskkill /F /IM java.exe 2>nul

REM Clean and rebuild the application
call ./gradlew clean build -x test

REM Start the application
start mvn spring-boot:run

echo Application rebuild and restart initiated!
echo.
echo Please wait for the application to start up...
timeout /t 15

echo.
echo Opening the application...
start http://localhost:8080/camunda/app/tasklist/

echo.
echo Done!
pause 