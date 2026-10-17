package id.riko.hub.ui.timeline

import id.riko.hub.domain.logic.TimelineStop

internal fun TimelineStop.isCurrentEvent(now: Long): Boolean =
    event != null && at <= now && (endAt ?: at) > now
