package kr.co.lovelydream.global.util

object LoggingUtil {
    /**
     * 이메일 앞 4글자 + ***** + @도메인 형태로 마스킹
     * 예: abcdef@example.com -> abcd*****@example.com
     */
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email // 형식이 아니면 그대로 반환

        val namePart = parts[0]
        val domainPart = parts[1]

        val visible = if (namePart.length >= 4) namePart.substring(0, 4) else namePart
        return "$visible*****@$domainPart"
    }

    /**
     * 이름 마스킹 (성만 남기고 * 처리)
     * 예: 김재섭 -> 김**
     */
    fun maskName(name: String): String {
        return if (name.length > 1) name.first() + "*".repeat(name.length - 1) else name
    }

    /**
     * 전화번호 마스킹 (중간 4자리 *)
     * 예: 010-1234-5678 -> 010-****-5678
     */
    fun maskPhone(phone: String): String {
        return phone.replace(Regex("(\\d{3})-(\\d{4})-(\\d{4})"), "$1-****-$3")
    }

    /**
     * JTI 마스킹
     * - 앞 4글자 + *** + 뒤 4글자
     */
    fun maskJti(jti: String): String =
        if (jti.length <= 8) jti.take(2) + "***"
        else jti.take(4) + "***" + jti.takeLast(4)

    /**
     * 디바이스 ID 마스킹
     * - 앞 4글자 + ***
     */
    fun maskDevice(deviceId: String): String =
        if (deviceId.length <= 6) deviceId.take(2) + "***"
        else deviceId.take(4) + "***"
}