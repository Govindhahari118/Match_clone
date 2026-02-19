# Matrimony App

## Project Structure
- `backend/`: Node.js + Express + Prisma
- `frontend/`: Next.js 14 Web App
- `mobile/`: React Native (Expo)
- `docs/`: Documentation

## Prerequisites
- Docker & Docker Compose
- Node.js 20+

## Getting Started

### 1. Start Infrastructure (DB, Redis)
Run the database and redis containers:
```bash
docker-compose up -d postgres redis
```

### 2. Backend Setup
Navigate to the backend folder:
```bash
cd backend
npm install
# Set up the database schema
npx prisma migrate dev --name init
# Start the server
npm run dev
```
The backend will run on http://localhost:5000.

### 3. Frontend Setup
Navigate to the frontend folder:
```bash
cd frontend
npm install
npm run dev
```
The frontend will run on http://localhost:3000.

## API Documentation
See `docs/matrimony-api-postman.json` for Postman collection.
See `docs/matrimony-app-prd.md` for full requirements.
