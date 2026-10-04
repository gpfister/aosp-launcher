package org.gpfister.aosp.gplauncher.data

/** Read access to Android system properties (hidden API, available to platform apps). */
fun interface SystemProps {
    fun get(key: String): String?
}

object ReflectiveSystemProps : SystemProps {
    private val getter by lazy {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java, String::class.java)
    }

    override fun get(key: String): String? =
        runCatching { getter.invoke(null, key, "") as String }.getOrNull()?.takeIf { it.isNotBlank() }
}
