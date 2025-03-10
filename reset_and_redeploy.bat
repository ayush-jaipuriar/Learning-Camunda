@echo off
echo Resetting database and redeploying process...

REM Stop any running application
taskkill /F /IM java.exe 2>nul

REM Set PostgreSQL connection details
set PGUSER=postgres
set PGPASSWORD=root
set PGDATABASE=disputedb
set PGHOST=localhost
set PGPORT=5432

echo.
echo Clearing Camunda tables...
psql -c "SET session_replication_role = 'replica';"
psql -c "DELETE FROM act_ru_incident;"
psql -c "DELETE FROM act_ru_job;"
psql -c "DELETE FROM act_ru_jobdef;"
psql -c "DELETE FROM act_ru_task;"
psql -c "DELETE FROM act_ru_identitylink;"
psql -c "DELETE FROM act_ru_variable;"
psql -c "DELETE FROM act_ru_event_subscr;"
psql -c "DELETE FROM act_ru_execution;"
psql -c "DELETE FROM act_ru_ext_task;"
psql -c "DELETE FROM act_ru_batch;"
psql -c "DELETE FROM act_ru_meter_log;"
psql -c "DELETE FROM act_ru_authorization WHERE resource_type_ != 1;"
psql -c "DELETE FROM act_hi_taskinst;"
psql -c "DELETE FROM act_hi_actinst;"
psql -c "DELETE FROM act_hi_varinst;"
psql -c "DELETE FROM act_hi_detail;"
psql -c "DELETE FROM act_hi_comment;"
psql -c "DELETE FROM act_hi_attachment;"
psql -c "DELETE FROM act_hi_identitylink;"
psql -c "DELETE FROM act_hi_procinst;"
psql -c "DELETE FROM act_hi_incident;"
psql -c "DELETE FROM act_hi_job_log;"
psql -c "DELETE FROM act_hi_batch;"
psql -c "DELETE FROM act_hi_op_log;"
psql -c "SET session_replication_role = 'origin';"

echo.
echo Clean and rebuild the application...
call ./gradlew clean build -x test

echo.
echo Starting the application...
start mvn spring-boot:run

echo.
echo Application rebuild and restart initiated!
echo.
echo Please wait for the application to start up...
timeout /t 15

echo.
echo Opening the application...
start http://localhost:8080/camunda/app/tasklist/

echo Done!
pause 