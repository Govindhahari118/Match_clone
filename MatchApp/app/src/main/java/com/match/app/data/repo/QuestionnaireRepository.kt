package com.match.app.data.repo

import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.entity.QuestionnaireEntity
import com.match.app.domain.questionnaire.Questionnaire
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionnaireRepository @Inject constructor(
    private val dao: QuestionnaireDao
) {

    suspend fun save(
        userId: Long,
        selfLikert: Map<Int, Int>, selfInterests: Map<Int, Set<String>>,
        partnerLikert: Map<Int, Int>, partnerInterests: Map<Int, Set<String>>
    ) {
        val self = Questionnaire.encode(selfLikert, selfInterests)
        val partner = Questionnaire.encode(partnerLikert, partnerInterests)
        dao.upsert(
            QuestionnaireEntity(
                userId = userId,
                selfVector = Vec.encode(self),
                partnerVector = Vec.encode(partner)
            )
        )
    }

    suspend fun hasQuestionnaire(userId: Long): Boolean = dao.forUser(userId) != null

    suspend fun vectorsFor(userId: Long): Pair<FloatArray, FloatArray>? {
        val q = dao.forUser(userId) ?: return null
        return Vec.decode(q.selfVector) to Vec.decode(q.partnerVector)
    }
}
