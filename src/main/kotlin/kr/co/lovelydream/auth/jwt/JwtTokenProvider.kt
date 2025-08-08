package kr.co.lovelydream.auth.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import kr.co.lovelydream.auth.constants.JwtConstants.ACCESS_EXPIRATION_MS
import kr.co.lovelydream.auth.constants.JwtConstants.REFRESH_EXPIRATION_MS
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

    fun generateAccessToken(email: String): String {
        val now = Date()
        val expiry = Date(now.time + ACCESS_EXPIRATION_MS)
        val jti = UUID.randomUUID().toString()

        return Jwts.builder()
            .subject(email)
            .claim("typ", "access")
            .id(jti)
            .issuedAt(Date())
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(email: String): String {
        val now = Date()
        val expiry = Date(now.time + REFRESH_EXPIRATION_MS)
        val jti = UUID.randomUUID().toString()

        return Jwts.builder()
            .subject(email)
            .claim("typ", "refresh")
            .id(jti)
            .issuedAt(Date())
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun getEmail(token: String): String =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject

    fun getJti(token: String): String =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.id

    fun isValid(token: String): Boolean = try {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        !claims.payload.expiration.before(Date())
    } catch (e: Exception) {
        false
    }

    fun getExpiration(token: String): Date =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.expiration

    fun getRemainingTtlSeconds(token: String): Long {
        val exp = getExpiration(token).time
        val now = System.currentTimeMillis()
        val diff = (exp - now) / 1000
        return if (diff > 0) diff else 0
    }

    fun getTyp(token: String): String =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.get("typ", String::class.java)
}