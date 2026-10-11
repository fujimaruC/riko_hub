package id.riko.hub.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskWidgetPresentationTest {
    @Test fun capacityUsesHeightBudgetsAndFontScale() {
        assertEquals(1, widgetTaskCapacity(250, 272, 1f))
        assertEquals(3, widgetTaskCapacity(250, 500, 1f))
        assertEquals(1, widgetTaskCapacity(299, 343, 1f))
        assertEquals(2, widgetTaskCapacity(299, 344, 1f))
        assertEquals(2, widgetTaskCapacity(299, 419, 1f))
        assertEquals(3, widgetTaskCapacity(299, 420, 1f))
        assertEquals(1, widgetTaskCapacity(300, 359, 1f))
        assertEquals(2, widgetTaskCapacity(300, 360, 1f))
        assertEquals(2, widgetTaskCapacity(300, 435, 1f))
        assertEquals(3, widgetTaskCapacity(300, 436, 1f))
        assertEquals(1, widgetTaskCapacity(300, 399, 1.25f))
        assertEquals(2, widgetTaskCapacity(300, 400, 1.25f))
        assertEquals(2, widgetTaskCapacity(300, 494, 1.25f))
        assertEquals(3, widgetTaskCapacity(300, 495, 1.25f))
        assertEquals(1, widgetTaskCapacity(299, 394, 1.3f))
        assertEquals(2, widgetTaskCapacity(299, 395, 1.3f))
        assertEquals(1, widgetTaskCapacity(300, 428, 1.4f))
        assertEquals(2, widgetTaskCapacity(300, 429, 1.4f))
        assertEquals(2, widgetTaskCapacity(300, 534, 1.4f))
        assertEquals(3, widgetTaskCapacity(300, 535, 1.4f))
    }

    @Test fun mascotStaysVisibleWhileItsDateColumnFits() {
        assertEquals(true, widgetShowsMiyu(250, 1f))
        assertEquals(false, widgetShowsMiyu(250, 1.1f))
        assertEquals(true, widgetShowsMiyu(300, 1f))
        assertEquals(false, widgetShowsMiyu(300, 1.26f))
    }

    @Test fun emptyTaskDateFallbackRefreshesAtNextZonedMidnight() {
        val zone = ZoneId.of("America/New_York")
        val now = LocalDate.of(2026, 3, 8).atTime(0, 30).atZone(zone).toInstant().toEpochMilli()
        val nextMidnight = LocalDate.of(2026, 3, 9).atStartOfDay(zone).toInstant().toEpochMilli()
        val clock = object : id.riko.hub.core.time.Clock {
            override fun now() = now
            override fun zone() = zone
        }
        assertNull(nextWidgetRefreshAt(emptyList(), clock))
        assertEquals(nextMidnight, nextDateRefreshAt(now, zone))
    }
}
