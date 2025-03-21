package org.sagebionetworks.bridge.kmm.shared.repo

import co.touchlab.stately.ensureNeverFrozen
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.sagebionetworks.bridge.kmm.shared.apis.SchedulesV2Api
import org.sagebionetworks.bridge.kmm.shared.cache.ResourceDatabaseHelper
import org.sagebionetworks.bridge.kmm.shared.cache.ResourceResult
import org.sagebionetworks.bridge.kmm.shared.cache.ResourceStatus
import org.sagebionetworks.bridge.kmm.shared.cache.ResourceType
import org.sagebionetworks.bridge.kmm.shared.models.*
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

class ScheduleTimelineRepo(internal val adherenceRecordRepo: AdherenceRecordRepo,
                           internal val assessmentConfigRepo: AssessmentConfigRepo,
                           httpClient: HttpClient,
                           databaseHelper: ResourceDatabaseHelper,
                           backgroundScope: CoroutineScope,
                           var scheduleMutator: ParticipantScheduleMutator? = null) :
    AbstractResourceRepo(databaseHelper, backgroundScope) {

    internal companion object {
        const val SCHEDULE_TIMELINE_ID = "ScheduleTimelineId"
    }

    init {
        ensureNeverFrozen()
    }

    private var scheduleApi = SchedulesV2Api(
        httpClient = httpClient
    )

    private fun getTimeline(studyId: String): Flow<ResourceResult<ParticipantSchedule>> {
        return getResourceById(
            SCHEDULE_TIMELINE_ID + studyId,
            resourceType = ResourceType.PARTICIPANT_SCHEDULE,
            remoteLoad = { loadRemoteTimeline(studyId) },
            studyId = studyId
        )
    }

    private suspend fun loadRemoteTimeline(studyId: String): String {
        var schedule = scheduleApi.getParticipantScheduleForSelf(studyId)
        schedule = scheduleMutator?.mutateParticipantSchedule(schedule) ?: schedule
        backgroundScope.launch() {
            schedule.assessments?.let {
                assessmentConfigRepo.loadAndCacheConfigs(it)
            }
        }
        return Json.encodeToString(schedule)
    }

    internal fun clearCachedSchedule(studyId: String) {
        database.removeAllResources(ResourceType.PARTICIPANT_SCHEDULE, studyId)
    }

    /**
     * Get all the scheduled sessions for today that have not expired.
     * @param studyId Study identifier
     * @param includeAllNotifications Include all [ScheduledNotification]s
     * @param alwaysIncludeNextDay Include the next day containing a [ScheduledSessionWindow]
     */
    fun getSessionsForToday(studyId: String,
                            includeAllNotifications: Boolean = false,
                            alwaysIncludeNextDay: Boolean = true): Flow<ResourceResult<ScheduledSessionTimelineSlice>> {
        return getSessionsForDay(
            studyId = studyId,
            instantInDay = Clock.System.now(),
            includeAllNotifications,
            alwaysIncludeNextDay,
        )
    }

    /**
     * Get all sessions with a start time in the past.
     */
    fun getPastSessions(studyId: String, now: Instant = Clock.System.now()): Flow<ResourceResult<ScheduledSessionTimelineSlice>> {
        return combine(
            getTimeline(studyId),
            //Need to include call to get AdherenceRecords as part of the combine.
            //This will trigger the flow to emit a new value anytime the AdherenceRecords change.
            adherenceRecordRepo.getAllCachedAdherenceRecords(studyId)
        ) { timeLineResource, _ ->
            extractPastSessionsFromResults(
                timeLineResource,
                studyId,
                now,
            )
        }
    }

    private fun extractPastSessionsFromResults(
        timelineResult: ResourceResult<ParticipantSchedule>,
        studyId: String,
        instantInDay: Instant,
    ): ResourceResult<ScheduledSessionTimelineSlice> {
        if (timelineResult is ResourceResult.Success) {
            return ResourceResult.Success(extractPastSessions(
                timeline = timelineResult.data,
                studyId = studyId,
                instantInDay = instantInDay
            ), ResourceStatus.SUCCESS)
        } else if (timelineResult is ResourceResult.Failed) {
            return ResourceResult.Failed(ResourceStatus.FAILED)
        } else {
            return ResourceResult.InProgress
        }
    }

    private fun extractPastSessions(
        timeline: ParticipantSchedule,
        studyId: String,
        instantInDay: Instant,
        timezone: TimeZone = TimeZone.currentSystemDefault(),
    ): ScheduledSessionTimelineSlice {

        // Extract sessions
        val windows = extractSessionWindows(
            timeline,
            instantInDay,
            studyId,
            WindowFilterType.Past,
            false,
            timezone
        ).sortedBy { it.startDateTime }

        // Build the timeline slice
        val today = instantInDay.toLocalDateTime(timezone)
        val schedules = mutableListOf<ScheduledSessionWindow>()

        windows.forEach { window ->
             if (window.startDateTime <= today) {
                 schedules.add(window)
             }
        }

        return ScheduledSessionTimelineSlice(
            instantInDay = instantInDay,
            timezone = timezone,
            scheduledSessionWindows = schedules,
            notifications = emptyList()
        )
    }

    /**
     * Used for testing so that we can specify a consistent point in time.
     */
    internal fun getSessionsForDay(
        studyId: String,
        instantInDay: Instant,
        includeAllNotifications: Boolean = false,
        alwaysIncludeNextDay: Boolean = false,
    ): Flow<ResourceResult<ScheduledSessionTimelineSlice>> {
        return combine(
            getTimeline(studyId),
            //Need to include call to get AdherenceRecords as part of the combine.
            //This will trigger the flow to emit a new value anytime the AdherenceRecords change.
            adherenceRecordRepo.getAllCachedAdherenceRecords(studyId)
        ) { timeLineResource, _ ->
            extractSessionsForDayFromResults(
                timeLineResource,
                studyId,
                instantInDay,
                includeAllNotifications,
                alwaysIncludeNextDay,
            )
        }
    }

    private fun extractSessionsForDayFromResults(
        timelineResult: ResourceResult<ParticipantSchedule>,
        studyId: String,
        instantInDay: Instant,
        includeAllNotifications: Boolean,
        alwaysIncludeNextDay: Boolean,
    ): ResourceResult<ScheduledSessionTimelineSlice> {
        if (timelineResult is ResourceResult.Success) {
            return ResourceResult.Success(extractSessionsForDay(
                timelineResult.data,
                instantInDay,
                studyId,
                includeAllNotifications,
                alwaysIncludeNextDay,
            ), ResourceStatus.SUCCESS)
        } else if (timelineResult is ResourceResult.Failed) {
            return ResourceResult.Failed(ResourceStatus.FAILED)
        } else {
            return ResourceResult.InProgress
        }

    }

    /**
     * Extract the scheduled sessions from the [Timeline] for the day specified by [instantInDay].
     * The returned list of [ScheduledSessionWindow]s will include all sessions that are active on the
     * day specified by [instantInDay]. Sessions that expire before [instantInDay] will be excluded.
     */
    @OptIn(ExperimentalTime::class)
    private fun extractSessionsForDay(
        timeline: ParticipantSchedule,
        instantInDay: Instant,
        studyId: String,
        includeAllNotifications: Boolean,
        alwaysIncludeNextDay: Boolean,
        timezone: TimeZone = TimeZone.currentSystemDefault(),
    ): ScheduledSessionTimelineSlice {

        // Extract for today
        val windows = extractSessionWindows(
            timeline,
            instantInDay,
            studyId,
            if (includeAllNotifications) WindowFilterType.TodayAndNotifications else WindowFilterType.Today,
            alwaysIncludeNextDay,
            timezone
        ).sortedBy { it.startDateTime }

        // Build the timeline slice
        var upNext: LocalDate? = null
        val today = instantInDay.toLocalDateTime(timezone)
        val schedules = mutableListOf<ScheduledSessionWindow>()
        val notifications = mutableListOf<ScheduledNotification>()

        run loop@ {
            windows.forEach { window ->

                when {
                    // Include in available now if already started.
                    (window.startDateTime <= today) ||
                            (alwaysIncludeNextDay && window.startDateTime.date == today.date) -> {
                        schedules.add(window)
                    }

                    // else include in up next if this is for the next window of time
                    // (either later today or next day with a schedule).
                    upNext == null || upNext == window.startDateTime.date -> {
                        upNext = window.startDateTime.date
                        schedules.add(window)
                    }

                    // If we aren't adding notifications then we have found everything
                    // we need so exit the loop.
                    else -> if (!includeAllNotifications) return@loop
                }
                // Add the notifications
                notifications.addAll(window.notifications ?: emptyList())
            }
        }
        notifications.sortBy { it.scheduleOn }

        return ScheduledSessionTimelineSlice(
            instantInDay = instantInDay,
            timezone = timezone,
            scheduledSessionWindows = schedules,
            notifications = notifications
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun extractSessionWindows(
        timeline: ParticipantSchedule,
        instantInDay: Instant,
        studyId: String,
        filterType: WindowFilterType,
        alwaysIncludeNextDay: Boolean,
        timezone: TimeZone,
    ): List<ScheduledSessionWindow> {

        // Set up maps so that we are iterating through each set only once before switching
        // to using a hashmap.
        val assessmentInfoMap = timeline.assessments?.associateBy({ it.key }, { it }) ?: return emptyList()
        val scheduleSessionMap = timeline.schedule?.groupBy { it.refGuid } ?: return emptyList()
        val eventMap = timeline.eventTimestamps
        val sessions = timeline.sessions ?: emptyList()

        // Get date portion of instantInDay
        val day: LocalDate = instantInDay.toLocalDateTime(timezone).date

        val foundState = FoundWindowState()
        return sessions.mapNotNull { session ->
            val scheduledSessionList = scheduleSessionMap[session.guid]
            if (scheduledSessionList == null) {
                null
            } else {
                //Map the schedules to windows
                scheduledSessionList.mapNotNull { scheduledSession ->
                    createScheduledSessionWindow(
                        scheduledSession,
                        filterType,
                        alwaysIncludeNextDay,
                        day,
                        instantInDay,
                        studyId,
                        assessmentInfoMap,
                        session,
                        eventMap,
                        timezone,
                        foundState
                    )
                }
            }
        }.flatten()
    }

    // Work-around for Kotlin not having inout variables. syoung 06/25/2021
    private class FoundWindowState(var foundToday: Boolean = false, var nextDay: LocalDate? = null)

    internal enum class WindowFilterType {
        Future, Past, Today, Notifications, TodayAndNotifications;

        fun isToday() = listOf(Today, TodayAndNotifications).contains(this)
        fun includeCompleted() = listOf(Today, TodayAndNotifications, Past).contains(this)
        fun isNotifications() = listOf(Notifications, TodayAndNotifications).contains(this)
        fun includeExpired() = listOf(Past).contains(this)
    }

    /**
     * This is pulled out into a function to make it a little easier to read and to
     * allow for using explicit returns in the `mapNotNull` method. I find mapping
     * functions in Kotlin with early exits difficult to grok. syoung 06/24/2021
     */
    @ExperimentalTime
    private fun createScheduledSessionWindow(
        scheduledSession: ScheduledSession,
        filterType: WindowFilterType,
        alwaysIncludeNextDay: Boolean,
        day: LocalDate,
        instantInDay: Instant,
        studyId: String,
        assessmentInfoMap: Map<String, AssessmentInfo>,
        session: SessionInfo,
        eventMap: Map<String,Instant>?,
        timezone: TimeZone,
        foundState: FoundWindowState,
    ): ScheduledSessionWindow? {
        val eventTimestamp = eventMap?.get(scheduledSession.startEventId) ?: return null

        fun rulesForNotifications() =
            scheduledSession.startDate >= day && !session.notifications.isNullOrEmpty()
        fun rulesForAvailableToday() =
            (scheduledSession.startDate <= day && scheduledSession.endDate >= day)
        fun rulesForToday(startDate: LocalDate? = null) = when {
            foundState.foundToday && !alwaysIncludeNextDay -> rulesForAvailableToday()
            startDate != null && foundState.nextDay != null -> startDate <= foundState.nextDay!!
            else -> scheduledSession.endDate >= day
        }
        // Exit early if this schedule is not in the range that we are looking for
        if (!when (filterType) {
            WindowFilterType.Notifications -> rulesForNotifications()
            WindowFilterType.Future -> scheduledSession.startDate > day
            WindowFilterType.Past -> scheduledSession.startDate <= day
            WindowFilterType.Today -> rulesForToday()
            WindowFilterType.TodayAndNotifications -> rulesForToday() || rulesForNotifications()
        }) {
            return null
        }

        // LocalTime support is hopefully coming: https://github.com/Kotlin/kotlinx-datetime/issues/57#issuecomment-800287971
        // Construct a startDateTime from today and startTime
        val startDateTimeString =
            scheduledSession.startDate.toString() + "T" + scheduledSession.startTime
        val startDateTime = LocalDateTime.parse(startDateTimeString)

        val startInstant = startDateTime.toInstant(timezone)
        val expirationInstant = startInstant.plus(scheduledSession.expiration, timezone)

        val endDateTime = expirationInstant.toLocalDateTime(timezone)
        val isExpired = (expirationInstant < instantInDay)

        // Exit early if all the assessments are completed and this filtering does
        // not include them.
        val assessments = mapAssessments(scheduledSession, studyId, assessmentInfoMap)
        val isCompleted = assessments.all { it.isCompleted || it.isDeclined }
        if (!filterType.includeCompleted() && isCompleted) {
            return null
        }

        // Exit early if expired and not completed.
        if (!filterType.includeExpired() && isExpired && !isCompleted) {
            return null
        }

        // If we are filtering for today schedules and one was found that is available today
        // but *not* available NOW then track that.
        if (rulesForAvailableToday() && startInstant > instantInDay) {
            foundState.foundToday = true
        } else if (alwaysIncludeNextDay && scheduledSession.startDate > day &&
            (foundState.nextDay == null || foundState.nextDay!! > scheduledSession.startDate)) {
            foundState.nextDay = scheduledSession.startDate
        }

        // Convert notifications
        val notifications = session.notifications?.mapNotNull {
            it.scheduleAt(scheduledSession.instanceGuid, instantInDay, startDateTime, endDateTime, scheduledSession.expiration.days == 0)
        }
        // Exit early if this is only getting future notifications and there aren't any.
        if (notifications.isNullOrEmpty() &&
            filterType.isNotifications() &&
            (!filterType.isToday() || !rulesForToday(scheduledSession.startDate))) {
            return null
        }

        return ScheduledSessionWindow(
            scheduledSession = scheduledSession,
            sessionInfo = session,
            assessments = assessments,
            eventTimestamp = eventTimestamp,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            notifications = if (notifications.isNullOrEmpty() || isCompleted) null else notifications.sortedBy { it.scheduleOn }
        )
    }

    private fun mapAssessments(
        scheduledSession: ScheduledSession,
        studyId: String,
        assessmentInfoMap: Map<String, AssessmentInfo>
    ): List<ScheduledAssessmentReference> {

        val adherenceRecords = adherenceRecordRepo.getCachedAdherenceRecords(
            scheduledSession.assessments.map { it.instanceGuid },
            studyId
        )

        return scheduledSession.assessments.map { assessment ->
            val recordsList = adherenceRecords[assessment.instanceGuid]
            ScheduledAssessmentReference(
                instanceGuid = assessment.instanceGuid,
                studyId = studyId,
                assessmentInfo = assessmentInfoMap[assessment.refKey]!!,
                adherenceRecordList = recordsList ?: emptyList()
            )
        }
    }
}

@ExperimentalTime
internal fun NotificationInfo.scheduleAt(instanceGuid: String,
                                         instantInDay: Instant,
                                         startDateTime: LocalDateTime,
                                         endDateTime: LocalDateTime,
                                         isTimeSensitive: Boolean,
): ScheduledNotification? {

    // Get the first notification trigger
    val timeZone = TimeZone.currentSystemDefault()
    val period = this.offset?.let { DateTimePeriod.parse(it) } ?: DateTimePeriod()
    val lastInstant = endDateTime.toInstant(timeZone)
    var firstInstant = if (notifyAt == NotificationType.AFTER_WINDOW_START) {
        startDateTime.toInstant(timeZone).plus(period, timeZone)
    } else {
        lastInstant.minus(period, timeZone)
    }

    // If there is an interval, move the firstInstant forward to after the current time
    val intervalPeriod = this.interval?.let { DateTimePeriod.parse(it) }
    if (intervalPeriod != null) {
        val endInstant = endDateTime.toInstant(timeZone)
        while(firstInstant < instantInDay && firstInstant < endInstant) {
            firstInstant = firstInstant.plus(intervalPeriod, timeZone)
        }
    }

    // Return null if the first instant isn't in the future
    return if (firstInstant < instantInDay) null
    else ScheduledNotification(
        instanceGuid,
        firstInstant.toLocalDateTime(timeZone),
        intervalPeriod,
        if (intervalPeriod ==null) null else lastInstant.toLocalDateTime(timeZone),
        allowSnooze ?: false,
        message,
        isTimeSensitive
    )
}

/**
 * A parsed instance of the schedule calculated at a given instant in time and split into
 * which schedules are available now, up next, or require scheduling a notification.
 */
data class ScheduledSessionTimelineSlice (
    val instantInDay: Instant,
    val timezone: TimeZone,
    val scheduledSessionWindows: List<ScheduledSessionWindow>,
    val notifications: List<ScheduledNotification>,
)

data class ScheduledSessionWindow (
    val scheduledSession: ScheduledSession,
    val eventTimestamp: Instant,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val assessments: List<ScheduledAssessmentReference>,
    val sessionInfo: SessionInfo,
    val notifications: List<ScheduledNotification>?,
) {
    val instanceGuid = scheduledSession.instanceGuid
    val hasStartTimeOfDay = startDateTime.let { it.hour > 0 || it.minute > 0 }
    val hasEndTimeOfDay = scheduledSession.expiration.let { it.hours > 0 || it.minutes > 0 }
    val persistent = scheduledSession.persistent
    val isCompleted = assessments.all { it.isCompleted || it.isDeclined }

    fun isAvailableNow(now: Instant = Clock.System.now()): Boolean {
        val timeZone = TimeZone.currentSystemDefault()
        return startDateTime.toInstant(timeZone) <= now && now < endDateTime.toInstant(timeZone)
    }

    fun isInFuture(now: Instant = Clock.System.now()): Boolean {
        val timeZone = TimeZone.currentSystemDefault()
        return startDateTime.toInstant(timeZone) > now
    }

    fun isInPast(now: Instant = Clock.System.now()): Boolean {
        val timeZone = TimeZone.currentSystemDefault()
        return endDateTime.toInstant(timeZone) < now
    }
}

data class ScheduledAssessmentReference (
    val instanceGuid: String,
    val studyId: String,
    val assessmentInfo: AssessmentInfo,
    val adherenceRecordList: List<AdherenceRecord>,
) {
    val isCompleted = adherenceRecordList.any { it.finishedOn != null }
    val isDeclined = !isCompleted && adherenceRecordList.any { it.declined == true }

    fun history(): List<AssessmentHistoryRecord> {
        return adherenceRecordList.mapNotNull { record ->
            if (record.startedOn != null && record.finishedOn != null && !record.declined)  {
                AssessmentHistoryRecord(instanceGuid, assessmentInfo, record.startedOn, record.finishedOn, record.clientTimeZone, record.clientData)
            } else {
                null
            }
        }
    }
}

@Serializable
data class ScheduledNotification(
    val instanceGuid: String,
    val scheduleOn: LocalDateTime,
    val repeatInterval: DateTimePeriod?,
    val repeatUntil: LocalDateTime?,
    val allowSnooze: Boolean,
    val message: NotificationMessage?,
    val isTimeSensitive: Boolean = false,
)

data class AssessmentHistoryRecord (
    val instanceGuid: String,
    val assessmentInfo: AssessmentInfo,
    val startedOn: Instant,
    val finishedOn: Instant,
    val clientTimeZone: String? = null,
    val clientData: kotlinx.serialization.json.JsonElement? = null) {

    //TODO: Switch to using Instant math once we migrate to Kotlin 1.5 -nbrown 07/27/21
    val minutes: Int = ((finishedOn.epochSeconds - startedOn.epochSeconds) / 60).toInt()
//    @OptIn(ExperimentalTime::class)
//    val minutes = finishedOn.minus(startedOn).inMinutes.roundToInt()
}

interface ParticipantScheduleMutator {

    fun mutateParticipantSchedule(participantSchedule: ParticipantSchedule) : ParticipantSchedule

}