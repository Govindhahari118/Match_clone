package com.match.app.data.repo

import com.match.app.data.local.dao.NoteDao
import com.match.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val dao: NoteDao) {
    fun observe(me: Long, target: Long): Flow<NoteEntity?> = dao.observe(me, target)
    suspend fun save(me: Long, target: Long, text: String) {
        if (text.isBlank()) dao.delete(me, target)
        else dao.upsert(NoteEntity(ownerId = me, targetId = target, note = text))
    }
    suspend fun delete(me: Long, target: Long) = dao.delete(me, target)
}
