@echo off
echo Restarting the application...

REM Stop any running application (adjust the command as needed)
taskkill /F /IM java.exe

REM Start the application (adjust the command as needed)
start mvn spring-boot:run

echo Application restart initiated!
echo.
echo Please wait for the application to start up...
timeout /t 10

echo Opening the application in your browser...
start http://localhost:8080/api/security-test/public

echo Done!
pause 