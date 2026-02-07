/*
 * Threema for Android
 * Copyright (c) 2025 Threema GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.threema.app.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.threema.app.di.awaitAppFullyReadyWithTimeout
import ch.threema.app.services.SynchronizeContactsService
import ch.threema.app.utils.WorkManagerUtil
import ch.threema.base.utils.getThreemaLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val logger = getThreemaLogger("ContactBookSyncWorker")

class ContactBookSyncWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters), KoinComponent {

    private val synchronizeContactsService: SynchronizeContactsService by inject()

    override suspend fun doWork(): Result {
        logger.info("Starting periodic contact book sync")

        awaitAppFullyReadyWithTimeout(timeout = 20.seconds)
            ?: return Result.failure()

        val started = synchronizeContactsService.instantiateSynchronizationAndRun()
        return if (started) {
            logger.info("Contact book sync started")
            Result.success()
        } else {
            logger.warn("Could not start contact book sync (maybe already running or no network)")
            // We return success here because failing would retry, and we don't necessarily need immediate retry
            // if it's just because it's already running or offline (though constraints should handle offline).
            // If it failed because of no account, we also can't do much.
            Result.success()
        }
    }

    companion object {
        private val SYNC_INTERVAL = 24.hours

        @JvmStatic
        fun schedulePeriodicSync(context: Context) {
            val schedulePeriodMs = SYNC_INTERVAL.inWholeMilliseconds

            logger.info("Initializing contact book sync. Schedule period: $schedulePeriodMs ms")

            try {
                val workManager = WorkManager.getInstance(context)

                if (WorkManagerUtil.shouldScheduleNewWorkManagerInstance(
                        workManager,
                        WorkerNames.WORKER_CONTACT_BOOK_SYNC,
                        schedulePeriodMs,
                    )
                ) {
                    logger.debug("Scheduling new job")

                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()

                    val workRequest = PeriodicWorkRequest.Builder(
                        ContactBookSyncWorker::class.java,
                        schedulePeriodMs,
                        TimeUnit.MILLISECONDS,
                    )
                        .setConstraints(constraints)
                        .addTag(schedulePeriodMs.toString())
                        .setInitialDelay(1000, TimeUnit.MILLISECONDS)
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        WorkerNames.WORKER_CONTACT_BOOK_SYNC,
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest,
                    )
                }
            } catch (e: Exception) {
                logger.error("Unable to schedule ContactBookSyncWorker", e)
            }
        }
    }
}
