package com.firefly.kotlin.ext.common

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * @author Pengtao Qiu
 */
fun getHost(): String {
    val allNetInterfaces = NetworkInterface.getNetworkInterfaces()
    var ip: InetAddress
    var host = ""
    while (allNetInterfaces.hasMoreElements()) {
        val netInterface = allNetInterfaces.nextElement() as NetworkInterface
        val addresses = netInterface.inetAddresses
        while (addresses.hasMoreElements()) {
            ip = addresses.nextElement() as InetAddress
            if (ip is Inet4Address) {
                host = ip.hostAddress
                if (!host.startsWith("127.0.0.1")) {
                    return host
                }
            }
        }
    }
    return host
}

enum class PathType(val value: String) {
    CLASS_PATH("classpath:"), FILE("file:");

    companion object {
        fun parse(path: String) = when {
            path.startsWith(CLASS_PATH.value) -> ConfigPath(path.substring(CLASS_PATH.value.length), CLASS_PATH)
            path.startsWith(FILE.value) -> ConfigPath(path.substring(FILE.value.length), FILE)
            else -> throw IllegalArgumentException("the path format error")
        }
    }

}

data class ConfigPath(val path: String, val type: PathType)