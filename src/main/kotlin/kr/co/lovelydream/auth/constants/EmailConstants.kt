package kr.co.lovelydream.auth.constants

object EmailConstants {
    const val SENDER_EMAIL = "islandtim-project@naver.com"
    const val EMAIL_SUBJECT = "[LovelyDrme] 이메일 인증 코드"
    const val EMAIL_BODY_TEMPLATE = "요청하신 인증 코드는 [%s] 입니다.\n5분 내에 입력해주세요."
    val CODE_RANGE = 100000..999999
}