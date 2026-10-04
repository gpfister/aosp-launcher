package org.gpfister.aosp.gplauncher.data

/**
 * Reads per-partition build versions from the `ro.<partition>.build.*` properties.
 * The `system` partition falls back to the unprefixed `ro.build.*` properties.
 */
class ImageVersionReader(private val props: SystemProps) {

    fun read(): List<ImageVersion> = PARTITIONS.mapNotNull { (name, prefix) ->
        val version = props.get("$prefix.version.incremental")
            ?: if (name == "system") props.get("ro.build.version.incremental") else null
        val fingerprint = props.get("$prefix.fingerprint")
            ?: if (name == "system") props.get("ro.build.fingerprint") else null
        if (version == null && fingerprint == null) null else ImageVersion(name, version, fingerprint)
    }

    fun activeSlot(): String? = props.get("ro.boot.slot_suffix")?.trimStart('_')

    private companion object {
        val PARTITIONS = listOf(
            "boot" to "ro.bootimage.build",
            "system" to "ro.system.build",
            "system_ext" to "ro.system_ext.build",
            "vendor" to "ro.vendor.build",
            "product" to "ro.product.build",
            "odm" to "ro.odm.build",
            "vendor_dlkm" to "ro.vendor_dlkm.build",
            "odm_dlkm" to "ro.odm_dlkm.build",
        )
    }
}
