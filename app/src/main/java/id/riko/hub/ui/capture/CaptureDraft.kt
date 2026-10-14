package id.riko.hub.ui.capture

import id.riko.hub.domain.model.Task

/** Preserve the entire capture; long text belongs in notes rather than a truncated title. */
internal fun captureTaskDraft(raw: String): Task? {
    val clean = raw.trim()
    if (clean.isEmpty()) return Task(title = "")
    val firstLine = clean.substringBefore('\n').trim()
    val title = firstLine.takeIf { it.length <= 120 }.orEmpty()
    val notes = if (title.isEmpty()) clean else clean.substringAfter('\n', "").trim()
    if (notes.length > 2000) return null
    return Task(title = title, notes = notes.takeIf { it.isNotEmpty() })
}
