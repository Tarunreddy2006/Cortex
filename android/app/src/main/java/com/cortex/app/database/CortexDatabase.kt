package com.cortex.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cortex.app.database.dao.*
import com.cortex.app.database.entities.*
import com.cortex.app.privacy.FinancialExclusionPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        // Phase 1 Foundations
        AppSelectionEntity::class,
        FinancialExclusionEntity::class,
        CollectedRecordEntity::class,
        SyncHistoryEntity::class,

        // Phase 2 Personal Memory Engine
        MemoryEntity::class,

        // Phase 2 Knowledge Vault
        DocumentEntity::class,
        CollectionEntity::class,
        NoteEntity::class,

        // Phase 2 Academic Intelligence
        AcademicProfileEntity::class,
        AcademicSubjectEntity::class,
        TimetableEntity::class,
        AcademicAssignmentEntity::class,
        AcademicExaminationEntity::class,

        // Phase 2 Life & Daily Management
        TaskEntity::class,
        GoalEntity::class,

        // Phase 2 Project Intelligence
        ProjectEntity::class,
        ProjectMilestoneEntity::class,
        ProjectDecisionEntity::class,

        // Phase 2 Personal Timeline
        TimelineEventEntity::class,

        // Phase 3 Intelligence, Reasoning & Automation
        ConversationEntity::class,
        ConversationMessageEntity::class,
        ProposedActionEntity::class,
        DecisionRecordEntity::class,
        ProactiveAlertEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CortexDatabase : RoomDatabase() {
    // Phase 1 DAOs
    abstract fun appSelectionDao(): AppSelectionDao
    abstract fun financialExclusionDao(): FinancialExclusionDao
    abstract fun collectedRecordDao(): CollectedRecordDao
    abstract fun syncHistoryDao(): SyncHistoryDao

    // Phase 2 DAOs
    abstract fun memoryDao(): MemoryDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun academicDao(): AcademicDao
    abstract fun lifeManagementDao(): LifeManagementDao
    abstract fun projectDao(): ProjectDao
    abstract fun timelineDao(): TimelineDao

    // Phase 3 DAOs
    abstract fun conversationDao(): ConversationDao
    abstract fun proposedActionDao(): ProposedActionDao
    abstract fun decisionRecordDao(): DecisionRecordDao
    abstract fun proactiveAlertDao(): ProactiveAlertDao

    companion object {
        @Volatile
        private var INSTANCE: CortexDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Memories
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `memories` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `content` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `sourceConnector` TEXT NOT NULL,
                        `sourceRecordId` TEXT,
                        `confidenceStatus` TEXT NOT NULL,
                        `approvalStatus` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `relatedMemoryIdsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `reviewDate` INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_category` ON `memories` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_sourceConnector` ON `memories` (`sourceConnector`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_isPinned` ON `memories` (`isPinned`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_approvalStatus` ON `memories` (`approvalStatus`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_createdAt` ON `memories` (`createdAt`)")

                // Documents & Collections & Notes
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `documents` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `storagePath` TEXT NOT NULL,
                        `extractedText` TEXT,
                        `summary` TEXT,
                        `category` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `collectionId` TEXT,
                        `sourceConnector` TEXT NOT NULL,
                        `version` INTEGER NOT NULL,
                        `importedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_category` ON `documents` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_fileType` ON `documents` (`fileType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_collectionId` ON `documents` (`collectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_importedAt` ON `documents` (`importedAt`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `collections` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `colorHex` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `collectionId` TEXT,
                        `tagsJson` TEXT NOT NULL,
                        `linkedDocumentIdsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_collectionId` ON `notes` (`collectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_createdAt` ON `notes` (`createdAt`)")

                // Academic Intelligence
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `academic_profile` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `institution` TEXT NOT NULL,
                        `degree` TEXT NOT NULL,
                        `department` TEXT NOT NULL,
                        `semester` INTEGER NOT NULL,
                        `academicYear` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `academic_subjects` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `subjectCode` TEXT NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `facultyName` TEXT,
                        `credits` INTEGER NOT NULL,
                        `colorHex` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `timetables` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `dayOfWeek` INTEGER NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `room` TEXT,
                        `scheduleType` TEXT NOT NULL,
                        `verificationStatus` TEXT NOT NULL,
                        `notes` TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetables_dayOfWeek` ON `timetables` (`dayOfWeek`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetables_subjectId` ON `timetables` (`subjectId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `academic_assignments` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `subjectId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `dueDateMs` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `sourceConnector` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_assignments_subjectId` ON `academic_assignments` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_assignments_dueDateMs` ON `academic_assignments` (`dueDateMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_assignments_status` ON `academic_assignments` (`status`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `academic_examinations` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `subjectId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `examDateMs` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `room` TEXT,
                        `syllabusCoverage` TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_examinations_subjectId` ON `academic_examinations` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_examinations_examDateMs` ON `academic_examinations` (`examDateMs`)")

                // Life Management (Tasks & Goals)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `priority` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `dueDateMs` INTEGER,
                        `isRecurring` INTEGER NOT NULL,
                        `recurrenceRule` TEXT,
                        `subtasksJson` TEXT NOT NULL,
                        `projectId` TEXT,
                        `completedAtMs` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_priority` ON `tasks` (`priority`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_category` ON `tasks` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDateMs` ON `tasks` (`dueDateMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId` ON `tasks` (`projectId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `goalType` TEXT NOT NULL,
                        `targetDateMs` INTEGER,
                        `progressPercent` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_status` ON `goals` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_goalType` ON `goals` (`goalType`)")

                // Projects
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `projects` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `objective` TEXT,
                        `status` TEXT NOT NULL,
                        `techStackJson` TEXT NOT NULL,
                        `progressPercent` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_createdAt` ON `projects` (`createdAt`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `project_milestones` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `projectId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetDateMs` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAtMs` INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_milestones_projectId` ON `project_milestones` (`projectId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `project_decisions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `projectId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `rationale` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `decidedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_decisions_projectId` ON `project_decisions` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_decisions_decidedAt` ON `project_decisions` (`decidedAt`)")

                // Timeline
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `timeline_events` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `timestampMs` INTEGER NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `summary` TEXT,
                        `sourceConnector` TEXT NOT NULL,
                        `referenceId` TEXT,
                        `isSignificant` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_timestampMs` ON `timeline_events` (`timestampMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_eventType` ON `timeline_events` (`eventType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_sourceConnector` ON `timeline_events` (`sourceConnector`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_isSignificant` ON `timeline_events` (`isSignificant`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Conversations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_updatedAt` ON `conversations` (`updatedAt`)")

                // Conversation Messages
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversation_messages` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `conversationId` TEXT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `sourcesJson` TEXT NOT NULL,
                        `intent` TEXT NOT NULL,
                        `referencedProject` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_messages_conversationId` ON `conversation_messages` (`conversationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_messages_createdAt` ON `conversation_messages` (`createdAt`)")

                // Proposed Actions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `proposed_actions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `actionType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `sourceContext` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `executedAt` INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_proposed_actions_status` ON `proposed_actions` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_proposed_actions_actionType` ON `proposed_actions` (`actionType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_proposed_actions_createdAt` ON `proposed_actions` (`createdAt`)")

                // Decision Records
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `decision_records` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `statement` TEXT NOT NULL,
                        `alternativesJson` TEXT NOT NULL,
                        `advantagesJson` TEXT NOT NULL,
                        `disadvantagesJson` TEXT NOT NULL,
                        `risksJson` TEXT NOT NULL,
                        `unknownInfoJson` TEXT NOT NULL,
                        `assumptionsJson` TEXT NOT NULL,
                        `recommendation` TEXT NOT NULL,
                        `conditionsJson` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_decision_records_status` ON `decision_records` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_decision_records_createdAt` ON `decision_records` (`createdAt`)")

                // Proactive Alerts
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `proactive_alerts` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `alertKey` TEXT NOT NULL,
                        `alertType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `referenceId` TEXT,
                        `actionSuggestion` TEXT,
                        `isDismissed` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_proactive_alerts_alertKey` ON `proactive_alerts` (`alertKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_proactive_alerts_isDismissed` ON `proactive_alerts` (`isDismissed`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_proactive_alerts_priority` ON `proactive_alerts` (`priority`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_proactive_alerts_createdAt` ON `proactive_alerts` (`createdAt`)")
            }
        }

        fun getInstance(context: Context): CortexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CortexDatabase::class.java,
                    "cortex_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).financialExclusionDao()
                                val defaults = FinancialExclusionPolicy.PERMANENT_FINANCIAL_PACKAGES.map { pkg ->
                                    FinancialExclusionEntity(
                                        packageName = pkg,
                                        appName = FinancialExclusionPolicy.getKnownAppName(pkg),
                                        isPermanent = true,
                                        addedByUser = false,
                                        reason = "Permanent financial security denylist"
                                    )
                                }
                                dao.insertAll(defaults)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
