@echo off
:: First-time developer setup for the MatchApp project.
:: Run from MatchApp\ directory.
::
:: Installs: Firebase CLI, Node deps for Cloud Functions.
::
:: Usage: scripts\setup\dev-setup.bat

echo [1/4] Checking Node.js ...
node --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js not found. Install from https://nodejs.org (LTS).
    exit /b 1
)
node --version

echo [2/4] Installing Firebase CLI ...
npm install -g firebase-tools
if errorlevel 1 (
    echo [WARN] Firebase CLI install failed. Try: npm install -g firebase-tools
)

echo [3/4] Installing Cloud Functions dependencies ...
cd functions
npm install
cd ..

echo [4/4] Setting up keystore template ...
if not exist keystore.properties (
    copy keystore.properties.template keystore.properties
    echo [INFO] keystore.properties created from template. Fill in your passwords.
) else (
    echo [INFO] keystore.properties already exists.
)

echo.
echo ==============================
echo  Dev setup complete!
echo  Next steps:
echo    1. firebase login
echo    2. firebase use <your-project-id>
echo    3. Fill in keystore.properties
echo    4. Run scripts\setup\generate-keystore.bat
echo ==============================
