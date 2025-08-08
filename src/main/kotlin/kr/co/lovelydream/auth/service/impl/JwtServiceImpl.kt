package kr.co.lovelydream.auth.service.impl

import kr.co.lovelydream.auth.jwt.JwtTokenProvider
import kr.co.lovelydream.auth.service.JwtService
import org.springframework.stereotype.Service

@Service
class JwtServiceImpl(
    private val jwtTokenProvider: JwtTokenProvider
) : JwtService {
    override fun generateAccessToken(email: String) = jwtTokenProvider.generateAccessToken(email)
    override fun generateRefreshToken(email: String) = jwtTokenProvider.generateRefreshToken(email)
    override fun isValid(token: String) = jwtTokenProvider.isValid(token)
    override fun getEmail(token: String) = jwtTokenProvider.getEmail(token)
    override fun getJti(token: String) = jwtTokenProvider.getJti(token)
    override fun getTyp(token: String) = jwtTokenProvider.getTyp(token)
    override fun getExpiration(token: String) = jwtTokenProvider.getExpiration(token)
    override fun getRemainingTtlSeconds(token: String) = jwtTokenProvider.getRemainingTtlSeconds(token)
}