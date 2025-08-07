package kr.co.lovelydream.auth.constants

object JwtConstants {
    const val REFRESH_PREFIX = "refresh:"
    const val BLACKLIST_PREFIX = "blacklist:"
// Base64-encoded
    const val ACCESS_EXPIRATION_MS = 15 * 60 * 1000L        // 15분
    const val REFRESH_EXPIRATION_MS = 1000 * 60 * 60 * 24 * 7L // 7일
}