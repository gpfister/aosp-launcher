package org.gpfister.aosp.gplauncher.data

data class ImageVersion(
    val partition: String,
    val version: String?,
    val fingerprint: String?,
)

data class DeviceInfo(
    val ethernetMac: String?,
    val ethernetIp: String?,
    val launcherVersion: String,
    val kioskVersion: String?,
    val activeSlot: String?,
    val images: List<ImageVersion>,
)
