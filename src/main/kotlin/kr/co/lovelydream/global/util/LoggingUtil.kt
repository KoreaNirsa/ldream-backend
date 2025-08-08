package kr.co.lovelydream.global.util

object LoggingUtil {
    /**
     * 이메일 앞 한 글자만 남기고 *** 처리
     * 예: test@example.com -> t***@example.com
     */
    fun maskEmail(email: String): String {
        return email.replace(Regex("(^.).*(@.*$)"), "$1***$2")
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
}