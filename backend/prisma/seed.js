const { PrismaClient } = require('@prisma/client');
require('dotenv').config(); // Load .env file
const prisma = new PrismaClient();

const professions = ['Software Engineer', 'Doctor', 'Teacher', 'Architect', 'Business Analyst', 'Consultant', 'Chef', 'Data Scientist'];
const educations = ['bachelors', 'masters', 'phd'];
const religions = ['Hindu', 'Muslim', 'Christian', 'Sikh', 'Jain'];
const castes = ['Brahmin', 'Kshatriya', 'Vaniya', 'Sunni', 'Catholic', 'Other'];
const cities = ['Mumbai', 'Delhi', 'Bangalore', 'Chennai', 'Hyderabad', 'Pune'];
const incomeBands = ['5-10L', '10-25L', '25-50L', '50L+'];

async function main() {
    console.log('Seeding database...');

    // Clear existing profiles (Optional: usually safer to upsert or just append if dev)
    // For now, I'll just create new ones to ensure volume.

    for (let i = 0; i < 50; i++) {
        const gender = i % 2 === 0 ? 'female' : 'male';
        const firstName = gender === 'female' ? `Priya${i}` : `Rahul${i}`;

        // Create User
        const user = await prisma.user.create({
            data: {
                email: `user${i}_${Date.now()}@example.com`,
                passwordHash: 'hashed_password_placeholder', // In real app, hash this
                phone: `98765432${i.toString().padStart(2, '0')}`,
                isVerified: i % 3 === 0, // Random verification
                role: 'user',
                profile: {
                    create: {
                        firstName: firstName,
                        lastName: 'Kumar',
                        dateOfBirth: new Date(1990 + Math.floor(Math.random() * 10), 0, 1),
                        gender: gender,
                        educationLevel: educations[Math.floor(Math.random() * educations.length)],
                        profession: professions[Math.floor(Math.random() * professions.length)],
                        religion: religions[Math.floor(Math.random() * religions.length)],
                        caste: castes[Math.floor(Math.random() * castes.length)],
                        city: cities[Math.floor(Math.random() * cities.length)],
                        state: 'State',
                        country: 'India',
                        incomeBand: incomeBands[Math.floor(Math.random() * incomeBands.length)],
                        bio: `I am a ${professions[Math.floor(Math.random() * professions.length)]} looking for a compatible partner. I enjoy traveling and reading.`,
                        maritalStatus: 'never_married',
                        heightCm: 160 + Math.floor(Math.random() * 30),
                        photos: {
                            create: [
                                {
                                    photoUrl: `https://randomuser.me/api/portraits/${gender === 'male' ? 'men' : 'women'}/${i % 99}.jpg`,
                                    thumbnailUrl: `https://randomuser.me/api/portraits/${gender === 'male' ? 'men' : 'women'}/${i % 99}.jpg`,
                                    isPrimary: true
                                },
                                {
                                    photoUrl: `https://randomuser.me/api/portraits/${gender === 'male' ? 'men' : 'women'}/${(i + 50) % 99}.jpg`,
                                    thumbnailUrl: `https://randomuser.me/api/portraits/${gender === 'male' ? 'men' : 'women'}/${(i + 50) % 99}.jpg`,
                                    isPrimary: false
                                }
                            ]
                        }
                    }
                }
            }
        });
        console.log(`Created user ${user.email}`);
    }
}

main()
    .catch((e) => {
        console.error(e);
        process.exit(1);
    })
    .finally(async () => {
        await prisma.$disconnect();
    });
