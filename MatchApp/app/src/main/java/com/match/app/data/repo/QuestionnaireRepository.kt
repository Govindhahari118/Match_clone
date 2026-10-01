package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.entity.QuestionnaireEntity
import com.match.app.domain.questionnaire.Questionnaire
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionnaireRepository @Inject constructor(
    private val dao: QuestionnaireDao
) {
    data class Answers(
        val selfLikert: Map<Int, Int>,
        val selfInterests: Map<Int, Set<String>>,
        val partnerLikert: Map<Int, Int>,
        val partnerInterests: Map<Int, Set<String>>
    )

    private val functions = FirebaseFunctions.getInstance()

    suspend fun save(
        userId: Long,
        selfLikert: Map<Int, Int>, selfInterests: Map<Int, Set<String>>,
        partnerLikert: Map<Int, Int>, partnerInterests: Map<Int, Set<String>>
    ) {
        val self = Questionnaire.encode(selfLikert, selfInterests)
        val partner = Questionnaire.encode(partnerLikert, partnerInterests)

        functions.getHttpsCallable("saveQuestionnaire")
            .call(
                mapOf(
                    "selfVector" to self.toList(),
                    "partnerVector" to partner.toList()
                )
            )
            .await()

        dao.upsert(
            QuestionnaireEntity(
                userId = userId,
                selfVector = Vec.encode(self),
                partnerVector = Vec.encode(partner)
            )
        )
    }

    suspend fun load(userId: Long): Answers? {
        val result = functions.getHttpsCallable("getMyQuestionnaire").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid questionnaire response")
        if (data["completed"] != true) {
            val legacy = dao.forUser(userId) ?: return null
            val legacySelf = Vec.decode(legacy.selfVector)
            val legacyPartner = Vec.decode(legacy.partnerVector)
            functions.getHttpsCallable("saveQuestionnaire")
                .call(
                    mapOf(
                        "selfVector" to legacySelf.toList(),
                        "partnerVector" to legacyPartner.toList()
                    )
                )
                .await()
            return decodeAnswers(legacySelf, legacyPartner)
        }

        val self = numberVector(data["selfVector"]) ?: error("Invalid saved self questionnaire")
        val partner = numberVector(data["partnerVector"]) ?: error("Invalid saved partner questionnaire")
        dao.upsert(
            QuestionnaireEntity(
                userId = userId,
                selfVector = Vec.encode(self),
                partnerVector = Vec.encode(partner)
            )
        )
        return decodeAnswers(self, partner)
    }

    suspend fun hasQuestionnaire(userId: Long): Boolean = load(userId) != null

    suspend fun vectorsFor(userId: Long): Pair<FloatArray, FloatArray>? {
        val q = dao.forUser(userId) ?: return null
        return Vec.decode(q.selfVector) to Vec.decode(q.partnerVector)
    }

    private fun numberVector(value: Any?): FloatArray? {
        val values = (value as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() } ?: return null
        return values.toFloatArray().takeIf { it.size == Questionnaire.VECTOR_LENGTH }
    }

    private fun decodeAnswers(self: FloatArray, partner: FloatArray): Answers {
        val selfDecoded = Questionnaire.decode(self) ?: error("Invalid self questionnaire vector")
        val partnerDecoded = Questionnaire.decode(partner) ?: error("Invalid partner questionnaire vector")
        return Answers(
            selfLikert = selfDecoded.likert,
            selfInterests = selfDecoded.interests,
            partnerLikert = partnerDecoded.likert,
            partnerInterests = partnerDecoded.interests
        )
    }
}
