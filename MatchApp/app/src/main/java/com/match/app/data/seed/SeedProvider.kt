package com.match.app.data.seed

import com.match.app.core.security.Passwords
import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.QuestionnaireEntity
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.local.entity.LikeEntity
import com.match.app.data.local.entity.MessageEntity
import com.match.app.domain.model.MatchMode
import com.match.app.domain.questionnaire.Questionnaire
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Generates 40 demo users the first time the app runs, so recommendations aren't empty. */
@Singleton
class SeedProvider @Inject constructor(
    private val userDao: UserDao,
    private val qDao: QuestionnaireDao,
    private val likeDao: com.match.app.data.local.dao.LikeDao,
    private val messageDao: com.match.app.data.local.dao.MessageDao,
    private val session: com.match.app.data.session.SessionStore
) {

    suspend fun seedIfNeeded() {
        if (userDao.seedCount() > 0) return
        val pwHash = Passwords.hash("Password@123")
        val rnd = Random(42)

        // Pre-create main demo user for easy tester login
        val demo1Id = userDao.insert(
            UserEntity(
                email = "demo1@match.app",
                passwordHash = pwHash,
                displayName = "Arjun Reddy",
                age = 28,
                gender = "MALE",
                lookingFor = "FEMALE",
                city = "Hyderabad",
                bio = "Software Architect. Love travel and photography.",
                rasi = "Mesha",
                nakshatra = "Ashwini",
                isVerified = true,
                isPremium = true,
                religion = "Hindu",
                motherTongue = "Telugu",
                education = "M.Tech",
                profession = "Software Engineer",
                maritalStatus = "Never Married",
                heightCm = 180,
                state = "Telangana",
                incomeBand = "25-50L",
                diet = "Veg",
                familyType = "Nuclear",
                nativeState = "Andhra Pradesh",
                lastActiveAt = System.currentTimeMillis()
            )
        )
        session.setHasQuestionnaire(true) // Mark as completed for demo

        val firstNames = listOf(
            "Rahul","Vikram","Siddharth","Karan","Rohan","Aditya","Nikhil",
            "Priya","Ananya","Meera","Kavya","Isha","Neha","Riya","Sneha",
            "Aarav","Dev","Aisha","Zara","Varun","Manish","Pooja","Swati",
            "Tushar","Nisha","Deepak","Anjali","Rajesh","Divya","Kunal","Shruti",
            "Mohit","Tanvi","Harsh","Sakshi","Gaurav","Bhavna","Akash","Preeti"
        )
        // ... (rest of pools)
        val cities = listOf(
            "Bangalore","Mumbai","Hyderabad","Chennai","Pune","Delhi","Kolkata",
            "Vijayawada","Guntur","Ahmedabad","Jaipur","Lucknow","Chandigarh",
            "Coimbatore","Visakhapatnam","Kochi","Indore","Nagpur"
        )
        val states = listOf(
            "Telangana","Andhra Pradesh","Karnataka","Tamil Nadu","Maharashtra",
            "Delhi","West Bengal","Gujarat","Rajasthan","Kerala","Punjab","Uttar Pradesh"
        )
        val castes = listOf(
            "Reddy","Kamma","Brahmin","Kapu","Velama","Naidu","Yadav","Maratha",
            "Rajput","Nair","Iyer","Iyengar","Agarwal","Patel","Lingayat",""
        )
        val religions = listOf("Hindu","Hindu","Hindu","Hindu","Muslim","Christian","Sikh","Jain","Buddhist")
        val tongues = listOf("Telugu","Tamil","Hindi","Kannada","Malayalam","Marathi","Gujarati","Bengali","Punjabi")
        val educations = listOf("Graduate","Post-Graduate","Doctorate","Diploma","B.Tech","M.Tech","MBA","CA","MBBS","BDS")
        val professions = listOf(
            "Software Engineer","Doctor","Teacher","Entrepreneur","Banker","Lawyer",
            "Designer","Data Scientist","Product Manager","Civil Servant","Architect",
            "Chartered Accountant","Dentist","Scientist","Pilot","Pharmacist"
        )
        val maritalStatuses = listOf("Never Married","Never Married","Never Married","Never Married","Divorced","Widowed","Awaiting Divorce")
        val heights = listOf(152,155,158,160,163,165,168,170,173,175,178,180,183,185)
        val diets = listOf("Veg","Non-Veg","Eggetarian","Jain","Veg","Non-Veg","Vegan")
        val incomeBands = listOf("3-6L","6-10L","10-15L","15-25L","25-50L","50L+")
        val familyTypes = listOf("Nuclear","Joint","Extended","Nuclear","Joint")
        
        val bios = listOf(
            "Love books, long walks and good food. Family-oriented and grounded.",
            "Engineer who enjoys travel and music. Looking for someone who values growth.",
            "Calm, family-oriented, ambitious. Believe in mutual respect and understanding.",
            "Hiker, foodie, looking for a partner in crime. Let's explore the world together.",
            "Passionate about technology and startups. Weekend trekker and chai lover.",
            "Traditional values with a modern outlook. Looking for a life partner with similar mindset.",
            "Working in healthcare, passionate about helping others. Love cooking and gardening.",
            "Creative soul — love art, music, and dance. Looking for someone who appreciates the arts.",
            "MBA grad working in finance. Fitness enthusiast who loves early morning runs.",
            "Moved back from the US, rooted in Indian values. Looking for someone genuine.",
            "Love to travel, cook, and spend time with family. Believe in simple living.",
            "Software professional based in Bangalore. Weekend photographer and coffee addict.",
            "Doctor by profession, musician by heart. Looking for a compassionate partner.",
            "Government officer, believe in discipline and dedication. Love reading and writing.",
            "NRI in Canada, planning to settle in India soon. Looking for someone from a good family.",
            "Dentist with a passion for social work. Love animals and nature.",
            "Chartered accountant, love numbers and puzzles. Looking for someone smart and fun.",
            "Architect who loves design in all forms. Enjoy traveling to heritage sites.",
            "Teacher who believes in making a difference. Love kids and storytelling.",
            "Entrepreneur running a startup. Work hard, play harder. Looking for an equal partner."
        )
        val rasis = com.match.app.core.matching.Astrology.RASIS
        val nakshatras = com.match.app.core.matching.Astrology.NAKSHATRAS

        val userIds = mutableListOf<Long>()
        for (i in 0 until 40) {
            val name = firstNames[i % firstNames.size]
            val genderStr = if (i % 2 == 0) "FEMALE" else "MALE"
            val wants = if (genderStr == "MALE") "FEMALE" else "MALE"
            val seedAge = 22 + rnd.nextInt(12)
            val seedState = states.random(rnd)
            val uid = userDao.insert(
                UserEntity(
                    email = "demo${i + 2}@match.app",
                    passwordHash = pwHash,
                    displayName = name,
                    age = seedAge,
                    gender = genderStr,
                    lookingFor = wants,
                    city = cities.random(rnd),
                    bio = "Hi, I'm $name — " + bios[i % bios.size],
                    rasi = rasis.random(rnd),
                    nakshatra = nakshatras.random(rnd),
                    isSeed = true,
                    religion = religions.random(rnd),
                    motherTongue = tongues.random(rnd),
                    education = educations.random(rnd),
                    profession = professions.random(rnd),
                    maritalStatus = maritalStatuses.random(rnd),
                    heightCm = heights.random(rnd),
                    isVerified = i % 3 != 2,
                    isPremium = i < 10,
                    caste = castes.random(rnd),
                    state = seedState,
                    incomeBand = incomeBands.random(rnd),
                    diet = diets.random(rnd),
                    familyType = familyTypes.random(rnd),
                    lastActiveAt = System.currentTimeMillis() - rnd.nextLong(0L, 7L * 24 * 60 * 60 * 1000L)
                )
            )
            userIds.add(uid)
            // Seed questionnaire for every demo user
            val likertSelf    = (1..10).associateWith { rnd.nextInt(1, 6) }
            val likertPartner = (1..10).associateWith { rnd.nextInt(1, 6) }
            val interestsSelf    = Questionnaire.INTERESTS.associate { q -> q.id to q.options.shuffled(rnd).take(3).toSet() }
            val interestsPartner = Questionnaire.INTERESTS.associate { q -> q.id to q.options.shuffled(rnd).take(3).toSet() }
            qDao.upsert(
                QuestionnaireEntity(
                    userId = uid,
                    selfVector = Vec.encode(Questionnaire.encode(likertSelf, interestsSelf)),
                    partnerVector = Vec.encode(Questionnaire.encode(likertPartner, interestsPartner))
                )
            )
        }

        // ── Seed Interactions for Demo User 1 ──────────────────────────────────
        // 1. Incoming Interests (People who liked Arjun)
        userIds.take(5).forEach { from ->
            likeDao.like(LikeEntity(fromUserId = from, toUserId = demo1Id))
        }
        // 2. Mutual Matches (Arjun liked them back)
        userIds.slice(5..7).forEach { other ->
            likeDao.like(LikeEntity(fromUserId = demo1Id, toUserId = other))
            likeDao.like(LikeEntity(fromUserId = other, toUserId = demo1Id))
            // Start a conversation
            messageDao.insert(MessageEntity(fromUserId = other, toUserId = demo1Id, body = "Hi Arjun, saw your profile. Let's connect!"))
            messageDao.insert(MessageEntity(fromUserId = demo1Id, toUserId = other, body = "Hey! Sure, would love to know more about you."))
        }
    }
}
