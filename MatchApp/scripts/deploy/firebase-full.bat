@echo off
setlocal
:: Production Firebase deploy. Run from MatchApp\.
:: Never relies on "firebase use"; the exact target project and source SHA must be explicit.

if "%MATREE_RELEASE_SHA%"=="" (
  echo [ERROR] MATREE_RELEASE_SHA is required.
  exit /b 1
)
python scripts\ci\release_source_guard.py --sha "%MATREE_RELEASE_SHA%"
if errorlevel 1 exit /b 1

if "%MATREE_FIREBASE_PROJECT_ID%"=="" (
  echo [ERROR] MATREE_FIREBASE_PROJECT_ID is required.
  exit /b 1
)

echo Target Firebase project: %MATREE_FIREBASE_PROJECT_ID%

echo [1/4] Deploying Firestore rules ...
firebase deploy --non-interactive --project "%MATREE_FIREBASE_PROJECT_ID%" --only firestore:rules
if errorlevel 1 goto :err

echo [2/4] Deploying Firestore indexes ...
firebase deploy --non-interactive --project "%MATREE_FIREBASE_PROJECT_ID%" --only firestore:indexes
if errorlevel 1 goto :err

echo [3/4] Deploying Storage rules ...
firebase deploy --non-interactive --project "%MATREE_FIREBASE_PROJECT_ID%" --only storage
if errorlevel 1 goto :err

echo [4/4] Building and deploying Cloud Functions ...
firebase deploy --non-interactive --project "%MATREE_FIREBASE_PROJECT_ID%" --only functions
if errorlevel 1 goto :err

if /I "%MATREE_DEPLOY_HOSTING%"=="1" (
  echo Deploying operator Hosting because MATREE_DEPLOY_HOSTING=1 ...
  firebase deploy --non-interactive --project "%MATREE_FIREBASE_PROJECT_ID%" --only hosting
  if errorlevel 1 goto :err
)

echo Firebase deploy complete for %MATREE_FIREBASE_PROJECT_ID%.
exit /b 0

:err
echo [ERROR] Firebase deploy failed.
exit /b 1
