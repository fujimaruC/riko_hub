package id.riko.hub.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskWidgetPresentationTest {
    @Test fun capacityUsesHeightBudgetsAndFontScale() {
        assertEquals(1, widgetTaskCapacity(259, 1f))
        assertEquals(1, widgetTaskCapacity(260, 1f))
        assertEquals(1, widgetTaskCapacity(321, 1f))
        assertEquals(2, widgetTaskCapacity(322, 1f))
        assertEquals(2, widgetTaskCapacity(385, 1f))
        assertEquals(3, widgetTaskCapacity(386, 1f))
        assertEquals(1, widgetTaskCapacity(321, 1.25f))
        assertEquals(2, widgetTaskCapacity(403, 1.25f))
        assertEquals(3, widgetTaskCapacity(483, 1.25f))
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
