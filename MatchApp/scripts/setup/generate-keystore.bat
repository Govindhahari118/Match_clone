@echo off
:: Generates the release signing keystore for Play Store.
:: Run once before your first release build.
::
:: Usage: scripts\setup\generate-keystore.bat
::
:: After running, copy keystore.properties.template → keystore.properties
:: and fill in the passwords you used here.

set KEYSTORE_FILE=match-release.jks
set KEY_ALIAS=match

if exist "%KEYSTORE_FILE%" (
    echo [WARN] %KEYSTORE_FILE% already exists. Delete it first if you want to regenerate.
    goto :eof
)

echo Generating release keystore ...
keytool -genkeypair -v ^
    -keystore %KEYSTORE_FILE% ^
    -alias %KEY_ALIAS% ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity 10000

echo.
echo Done. Now create keystore.properties from keystore.properties.template
echo and set your storePassword / keyPassword.
echo IMPORTANT: Never commit keystore.properties or match-release.jks to Git.
