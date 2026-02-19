const { Client } = require('pg');
const crypto = require('crypto');
require('dotenv').config();

const client = new Client({
    connectionString: process.env.DATABASE_URL || "postgresql://matrimony_user:matrimony_dev_pass@localhost:5432/matrimony_db"
});

const professions = ['Software Engineer', 'Doctor', 'Teacher', 'Architect', 'Business Analyst', 'Consultant', 'Chef', 'Data Scientist'];
const educations = ['bachelors', 'masters', 'phd'];
const religions = ['Hindu', 'Muslim', 'Christian', 'Sikh', 'Jain'];
const castes = ['Brahmin', 'Kshatriya', 'Vaniya', 'Sunni', 'Catholic', 'Other'];
const cities = ['Mumbai', 'Delhi', 'Bangalore', 'Chennai', 'Hyderabad', 'Pune'];
const incomeBands = ['5-10L', '10-25L', '25-50L', '50L+'];

async function main() {
    try {
        await client.connect();
        console.log('Connected to DB');

        for (let i = 0; i < 50; i++) {
            const gender = i % 2 === 0 ? 'female' : 'male';
            const firstName = gender === 'female' ? `Priya${i}` : `Rahul${i}`;
            const email = `user${i}_${Date.now()}@example.com`;
            const phone = `98765432${i.toString().padStart(2, '0')}`;

            // 1. Insert User
            const userValues = [
                crypto.randomUUID(),
                email,
                'hash',
                phone,
                i % 3 === 0,
                'user'
            ];
            const userRes = await client.query(
                `INSERT INTO "User" (id, email, "passwordHash", phone, "isVerified", role, "updatedAt") 
             VALUES ($1, $2, $3, $4, $5, $6, NOW()) RETURNING id`,
                userValues
            );
            const userId = userRes.rows[0].id;

            // 2. Insert Profile
            const profileValues = [
                crypto.randomUUID(),
                userId,
                firstName,
                'Kumar',
                new Date(1990 + Math.floor(Math.random() * 10), 0, 1),
                gender,
                educations[Math.floor(Math.random() * educations.length)],
                professions[Math.floor(Math.random() * professions.length)],
                religions[Math.floor(Math.random() * religions.length)],
                castes[Math.floor(Math.random() * castes.length)],
                cities[Math.floor(Math.random() * cities.length)],
                'State',
                'India',
                incomeBands[Math.floor(Math.random() * incomeBands.length)],
                `I am a ${professions[Math.floor(Math.random() * professions.length)]} looking for a partner.`,
                'never_married',
                160 + Math.floor(Math.random() * 30)
            ];
            await client.query(
                `INSERT INTO "Profile" 
            (id, "userId", "firstName", "lastName", "dateOfBirth", gender, "educationLevel", profession, religion, caste, city, state, country, "incomeBand", bio, "maritalStatus", "heightCm", "updatedAt")
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, NOW())`,
                profileValues
            );

            // 3. Insert Photo
            const photoValues = [
                crypto.randomUUID(),
                userId,
                `https://randomuser.me/api/portraits/${gender === 'male' ? 'men' : 'women'}/${i % 99}.jpg`,
                `https://randomuser.me/api/portraits/${gender === 'male' ? 'men' : 'women'}/${i % 99}.jpg`,
                true
            ];
            await client.query(
                `INSERT INTO "Photo" (id, "userId", "photoUrl", "thumbnailUrl", "isPrimary")
             VALUES ($1, $2, $3, $4, $5)`,
                photoValues
            );

            console.log(`Seeded user ${email}`);
        }
    } catch (e) {
        console.error(e);
    } finally {
        await client.end();
    }
}

main();
