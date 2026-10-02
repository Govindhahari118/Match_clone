@echo off
setlocal
:: Production release build. Run from MatchApp\.
:: Exact release source, version and signing inputs are supplied outside source control.

if "%MATREE_RELEASE_SHA%"=="" (
  echo [ERROR] MATREE_RELEASE_SHA is required for a production build.
  exit /b 1
)
python scripts\ci\release_source_guard.py --sha "%MATREE_RELEASE_SHA%"
if errorlevel 1 exit /b 1

if "%MATREE_APPLICATION_ID%"=="" (
  echo [ERROR] MATREE_APPLICATION_ID is required for a production build.
  exit /b 1
)
if /I "%MATREE_APPLICATION_ID%"=="com.match.app" (
  echo [ERROR] MATREE_APPLICATION_ID must be the final production package, not com.match.app.
  exit /b 1
)

if "%MATREE_APP_LINK_HOST%"=="" (
  echo [ERROR] MATREE_APP_LINK_HOST is required for a production build.
  exit /b 1
)
if /I "%MATREE_APP_LINK_HOST%"=="invalid.matree.local" (
  echo [ERROR] MATREE_APP_LINK_HOST must be the verified production hostname.
  exit /b 1
)

if "%MATREE_VERSION_CODE%"=="" (
  echo [ERROR] MATREE_VERSION_CODE is required for a production build.
  exit /b 1
)
if "%MATREE_VERSION_NAME%"=="" (
  echo [ERROR] MATREE_VERSION_NAME is required for a production build.
  exit /b 1
)

if not exist app\src\release\google-services.json if not exist app\google-services.json (
  echo [ERROR] Production google-services.json is required. Do not use the debug placeholder.
  exit /b 1
)

if not exist keystore.properties (
  if "%MATREE_KEYSTORE_PATH%"=="" goto :missing_signing
  if "%MATREE_KEYSTORE_PASSWORD%"=="" goto :missing_signing
  if "%MATREE_KEY_ALIAS%"=="" goto :missing_signing
  if "%MATREE_KEY_PASSWORD%"=="" goto :missing_signing
  if not exist "%MATREE_KEYSTORE_PATH%" (
    echo [ERROR] MATREE_KEYSTORE_PATH does not exist.
    exit /b 1
  )
)

echo Running release tests, lint and bundle build ...
call gradlew.bat --no-daemon testReleaseUnitTest lintRelease bundleRelease
if errorlevel 1 goto :err

python scripts\ci\release_candidate_scan.py --merged-manifest app\build\intermediates\merged_manifests\release\processReleaseManifest\AndroidManifest.xml
if errorlevel 1 goto :err

for %%F in (app\build\outputs\bundle\release\*.aab) do (
  python scripts\ci\release_artifact_audit.py "%%F"
  if errorlevel 1 goto :err
)

echo.
echo =========================================
echo Production release bundle validated.
echo Version: %MATREE_VERSION_NAME% (%MATREE_VERSION_CODE%)
echo AAB: app\build\outputs\bundle\release\
echo =========================================
exit /b 0

:missing_signing
echo [ERROR] Configure keystore.properties or all MATREE_KEYSTORE_* environment variables.
exit /b 1

:err
echo [ERROR] Production release build failed.
exit /b 1
