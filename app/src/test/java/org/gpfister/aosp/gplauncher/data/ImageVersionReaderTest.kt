package org.gpfister.aosp.gplauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageVersionReaderTest {
    private fun reader(vararg props: Pair<String, String>) = ImageVersionReader(SystemProps { props.toMap()[it] })

    @Test
    fun `system falls back to unprefixed properties`() {
        val images = reader("ro.build.version.incremental" to "123", "ro.build.fingerprint" to "fp").read()
        assertEquals(listOf(ImageVersion("system", "123", "fp")), images)
    }

    @Test
    fun `prefixed properties win and missing partitions are omitted`() {
        val images = reader(
            "ro.system.build.version.incremental" to "10",
            "ro.build.version.incremental" to "99",
            "ro.vendor.build.version.incremental" to "20",
        ).read()
        assertEquals(listOf("system", "vendor"), images.map { it.partition })
        assertEquals("10", images.first().version)
    }

    @Test
    fun `active slot drops underscore`() {
        assertEquals("a", reader("ro.boot.slot_suffix" to "_a").activeSlot())
        assertNull(reader().activeSlot())
    }
}
