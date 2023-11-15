package sc2.syrup.common.util

import java.net.InetAddress
import javax.servlet.http.HttpServletRequest

fun ipParser(request: HttpServletRequest): String {
    var ip = request.getHeader("X-Forwarded-For")

    if (ip == null || ip.isEmpty() || ip.equals("unknown", ignoreCase = true)) {
        ip = request.getHeader("Proxy-Client-IP")
    }
    if (ip == null || ip.isEmpty() || ip.equals("unknown", ignoreCase = true)) {
        ip = request.getHeader("WL-Proxy-Client-IP")
    }
    if (ip == null || ip.isEmpty() || ip.equals("unknown", ignoreCase = true)) {
        ip = request.getHeader("HTTP_CLIENT_IP")
    }
    if (ip == null || ip.isEmpty() || ip.equals("unknown", ignoreCase = true)) {
        ip = request.getHeader("HTTP_X_FORWARDED_FOR")
    }
    if (ip == null || ip.isEmpty() || ip.equals("unknown", ignoreCase = true)) {
        ip = request.getHeader("X-Real-IP")
    }
    if (ip == null || ip.isEmpty() || ip.equals("unknown", ignoreCase = true)) {
        ip = request.getHeader("REMOTE_ADDR")
    }
    if (ip == null || ip.isEmpty() || ip.equals("unknown", ignoreCase = true)) {
        ip = request.remoteAddr
    }

    if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1")) {
        val address = InetAddress.getLocalHost()
        ip = address.hostAddress
    }

    return ip
}
