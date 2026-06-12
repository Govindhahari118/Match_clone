@echo off
:: Encodes match-release.jks to Base64 so it can be stored as a GitHub secret.
:: Run this from the MatchApp\ directory.
::
:: Usage: scripts\setup\encode-keystore.bat
::
:: Copy the output value into GitHub → Settings → Secrets → KEYSTORE_BASE64

set KEYSTORE_FILE=match-release.jks

if not exist "%KEYSTORE_FILE%" (
    echo [ERROR] %KEYSTORE_FILE% not found. Run generate-keystore.bat first.
    exit /b 1
)

echo Base64-encoded keystore (paste into GitHub secret KEYSTORE_BASE64):
echo.
powershell -NoProfile -Command "[Convert]::ToBase64String([IO.File]::ReadAllBytes('%KEYSTORE_FILE%'))"
echo.
