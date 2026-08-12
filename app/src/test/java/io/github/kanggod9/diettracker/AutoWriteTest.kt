package io.github.kanggod9.diettracker

import io.github.kanggod9.diettracker.ui.shouldAutoWrite
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoWriteTest {
    @Test fun autoWriteRunsOnlyForNewLogsWhenEnabled() {
        assertTrue(shouldAutoWrite(isNew = true, setting = "true"))
        assertFalse(shouldAutoWrite(isNew = false, setting = "true"))
        assertFalse(shouldAutoWrite(isNew = true, setting = "false"))
        assertFalse(shouldAutoWrite(isNew = true, setting = null))
    }
}