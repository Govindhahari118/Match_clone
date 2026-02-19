# How to Run Matrimony App (Production Ready)

The app appears broken because the Database is offline and empty. Follow these steps to fix it.

## 1. Start the Database
The app requires PostgreSQL.
- **Option A (Docker):** Run `start_db.bat` from the desktop folder.
- **Option B (Manual):** Ensure PostgreSQL Service is running on port 5432.

## 2. Setup the Database (Important!)
To ensure all features (like Shortlisting) work, you must update the database schema:
```bash
cd backend
npx prisma migrate dev --name add_shortlist
```

## 3. Seed the Database
An empty app looks broken. Run this command to create 50 realistic profiles:
```bash
npm run seed
```

## 3. Run the App
Open two terminals:

**Terminal 1 (Backend):**
```bash
cd backend
npm run dev
```

**Terminal 2 (Frontend):**
```bash
cd frontend
npm run dev
```

## 4. Test the App
1. Open http://localhost:3000
2. Search for "Female, 20-30, Hindu".
3. You will be redirected to `/matches` with those filters pre-applied.
4. "Shortlist" button will work (once backend is running).
5. "View Profile" will show detailed pages.

If you see "Network Error" or "Connection Refused", your Database is still down.
