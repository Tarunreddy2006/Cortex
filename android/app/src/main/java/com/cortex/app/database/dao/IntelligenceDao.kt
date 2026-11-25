package com.cortex.app.database.dao

import androidx.room.*
import com.cortex.app.database.entities.*

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversations(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversation(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("SELECT * FROM conversation_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessages(conversationId: String): List<ConversationMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessageEntity)

    @Query("DELETE FROM conversation_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessages(conversationId: String)

    @Query("SELECT * FROM conversation_messages WHERE text LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT 50")
    suspend fun searchMessages(query: String): List<ConversationMessageEntity>
}

@Dao
interface ProposedActionDao {
    @Query("SELECT * FROM proposed_actions ORDER BY createdAt DESC")
    suspend fun getAllActions(): List<ProposedActionEntity>

    @Query("SELECT * FROM proposed_actions WHERE status = 'PROPOSED' ORDER BY createdAt DESC")
    suspend fun getPendingActions(): List<ProposedActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ProposedActionEntity)

    @Query("UPDATE proposed_actions SET status = :status, executedAt = :executedAt WHERE id = :id")
    suspend fun updateActionStatus(id: String, status: String, executedAt: Long? = System.currentTimeMillis())

    @Query("DELETE FROM proposed_actions WHERE id = :id")
    suspend fun deleteAction(id: String)
}

@Dao
interface DecisionRecordDao {
    @Query("SELECT * FROM decision_records ORDER BY createdAt DESC")
    suspend fun getAllDecisions(): List<DecisionRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionRecordEntity)

    @Query("DELETE FROM decision_records WHERE id = :id")
    suspend fun deleteDecision(id: String)
}

@Dao
interface ProactiveAlertDao {
    @Query("SELECT * FROM proactive_alerts WHERE isDismissed = 0 ORDER BY createdAt DESC")
    suspend fun getActiveAlerts(): List<ProactiveAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: ProactiveAlertEntity)

    @Query("UPDATE proactive_alerts SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissAlert(id: String)

    @Query("UPDATE proactive_alerts SET isDismissed = 1 WHERE alertKey = :alertKey")
    suspend fun dismissAlertByKey(alertKey: String)
}
