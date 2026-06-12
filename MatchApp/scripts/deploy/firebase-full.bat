@echo off
:: Full Firebase deploy: Firestore rules + indexes + Storage rules + Functions.
:: Run from MatchApp\ directory.
::
:: Prerequisites:
::   npm install -g firebase-tools
::   firebase login
::   firebase use <your-project-id>
::
:: Usage: scripts\deploy\firebase-full.bat

echo [1/4] Deploying Firestore rules ...
firebase deploy --only firestore:rules
if errorlevel 1 goto :err

echo [2/4] Deploying Firestore indexes ...
firebase deploy --only firestore:indexes
if errorlevel 1 goto :err

echo [3/4] Deploying Storage rules ...
firebase deploy --only storage
if errorlevel 1 goto :err

echo [4/4] Building + deploying Cloud Functions ...
firebase deploy --only functions
if errorlevel 1 goto :err

echo.
echo ==============================
echo  Firebase deploy complete!
echo ==============================
goto :eof

:err
echo.
echo [ERROR] Deploy step failed. Check output above.
exit /b 1
