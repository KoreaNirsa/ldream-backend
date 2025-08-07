package kr.co.lovelydream.auth.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
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
        return Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun generateRefreshToken(email: String): String {
        val now = Date()
        val expiry = Date(now.time + REFRESH_EXPIRATION_MS)
        return Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getEmail(token: String): String =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject

    fun isValid(token: String): Boolean = try {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        !claims.payload.expiration.before(Date())
    } catch (e: Exception) {
        false
    }

    fun getExpiration(token: String): Date =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.expiration
}