package com.todoroo.astrid.service

import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import org.tasks.caldav.VtodoCache
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.DeletionDao
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskDao
import org.tasks.data.GoogleTaskList
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.db.QueryUtils
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import javax.inject.Inject

class TaskDeleter @Inject constructor(
        private val deletionDao: DeletionDao,
        private val workManager: WorkManager,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val googleTaskDao: GoogleTaskDao,
        private val preferences: Preferences,
        private val syncAdapters: SyncAdapters,
        private val vtodoCache: VtodoCache,
    ) {

    suspend fun markDeleted(item: Task) = markDeleted(listOf(item.id))

    suspend fun markDeleted(taskIds: List<Long>): List<Task> {
        val ids: MutableSet<Long> = HashSet(taskIds)
        ids.addAll(taskIds.chunkedMap(googleTaskDao::getChildren))
        ids.addAll(taskIds.chunkedMap(taskDao::getChildren))
        deletionDao.markDeleted(ids)
        workManager.cleanup(ids)
        syncAdapters.sync()
        localBroadcastManager.broadcastRefresh()
        return ids.chunkedMap(taskDao::fetch)
    }

    suspend fun clearCompleted(filter: Filter): Int {
        val deleteFilter = Filter(null, null)
        deleteFilter.setFilterQueryOverride(
                QueryUtils.removeOrder(QueryUtils.showHiddenAndCompleted(filter.originalSqlQuery)))
        val completed = taskDao.fetchTasks(preferences, deleteFilter)
                .filter(TaskContainer::isCompleted)
                .map(TaskContainer::getId)
                .toMutableList()
        completed.removeAll(deletionDao.hasRecurringAncestors(completed))
        completed.removeAll(googleTaskDao.hasRecurringParent(completed))
        markDeleted(completed)
        return completed.size
    }

    suspend fun delete(task: Task) = delete(task.id)

    suspend fun delete(task: Long) = delete(listOf(task))

    suspend fun delete(tasks: List<Long>) {
        deletionDao.delete(tasks)
        workManager.cleanup(tasks)
        localBroadcastManager.broadcastRefresh()
    }

    fun delete(list: GoogleTaskList) = runBlocking {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    suspend fun delete(list: GoogleTaskAccount) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    suspend fun delete(list: CaldavCalendar) {
        vtodoCache.delete(list)
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    suspend fun delete(list: CaldavAccount) {
        vtodoCache.delete(list)
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }
}