@echo off
echo Starting Matrimony App Database...

:: Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo Docker is NOT running. Attempting to start Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo Please wait for Docker to start (may take a minute)...
    timeout /t 60
)

:: Run Docker Compose
echo Starting Containers...
docker-compose up -d

:: Check status
docker ps

echo if "db" is running, you can now run "npm run seed" in backend folder.
pause
