@echo off
:: Builds a signed release APK / AAB and runs all unit tests.
:: Run from MatchApp\ directory.
::
:: Requires keystore.properties to be filled in (see keystore.properties.template).
::
:: Usage: scripts\deploy\build-release.bat

echo Checking keystore.properties ...
if not exist keystore.properties (
    echo [ERROR] keystore.properties not found.
    echo Copy keystore.properties.template to keystore.properties and fill in values.
    exit /b 1
)

echo Running unit tests ...
call gradlew.bat testReleaseUnitTest --no-daemon
if errorlevel 1 (
    echo [ERROR] Unit tests failed. Fix before releasing.
    exit /b 1
)

echo Building release APK ...
call gradlew.bat assembleRelease --no-daemon
if errorlevel 1 goto :err

echo Building release AAB (for Play Store) ...
call gradlew.bat bundleRelease --no-daemon
if errorlevel 1 goto :err

echo.
echo ==============================
echo  Release build complete!
echo  APK: app\build\outputs\apk\release\
echo  AAB: app\build\outputs\bundle\release\
echo ==============================
goto :eof

:err
echo [ERROR] Build failed.
exit /b 1
