package com.example.firenotes.data.service

import java.util.UUID

// Sync Queue & Conflict Resolution Architecture (V4 Offline First logic stubs)
data class SyncTask(
    val id: String = UUID.randomUUID().toString(),
    val actionType: String, // INSERT, UPDATE, DELETE
    val tableName: String,
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface ConflictResolver {
    suspend fun <T> resolve(local: T, remote: T): T
}

class DefaultConflictResolver : ConflictResolver {
    override suspend fun <T> resolve(local: T, remote: T): T {
        // Default strategy: Last Write Wins
        return remote
    }
}

class SyncQueue {
    private val queue = mutableListOf<SyncTask>()

    fun enqueue(task: SyncTask) {
        queue.add(task)
    }

    fun getPendingTasks(): List<SyncTask> = queue.toList()

    fun dequeue(taskId: String) {
        queue.removeAll { it.id == taskId }
    }

    fun clear() {
        queue.clear()
    }
}
