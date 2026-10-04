package org.gpfister.aosp.gplauncher.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.net.Inet4Address

class DeviceInfoRepository(
    private val context: Context,
    private val props: SystemProps = ReflectiveSystemProps,
    private val netRoot: File = File("/sys/class/net"),
) {
    private val imageReader = ImageVersionReader(props)

    fun load(): DeviceInfo = DeviceInfo(
        ethernetMac = ethernetMac(),
        ethernetIp = ethernetIp(),
        launcherVersion = versionOf(context.packageName).orEmpty(),
        kioskVersion = versionOf(KIOSK_PACKAGE),
        activeSlot = imageReader.activeSlot(),
        images = imageReader.read(),
    )

    /** Reads the MAC from sysfs: `NetworkInterface.getHardwareAddress` is blocked for apps on Android 11+. */
    internal fun ethernetMac(): String? =
        netRoot.listFiles { f -> f.name.startsWith("eth") }
            ?.sortedBy { it.name }
            ?.firstNotNullOfOrNull { runCatching { File(it, "address").readText().trim() }.getOrNull() }
            ?.takeIf { it.isNotEmpty() }

    private fun ethernetIp(): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        return cm.allNetworks
            .filter { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true }
            .firstNotNullOfOrNull { network ->
                cm.getLinkProperties(network)?.linkAddresses
                    ?.map { it.address }
                    ?.firstOrNull { it is Inet4Address }
                    ?.hostAddress
            }
    }

    private fun versionOf(packageName: String): String? = try {
        context.packageManager.getPackageInfo(packageName, 0).versionName
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
