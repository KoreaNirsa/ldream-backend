package kr.co.lovelydream.auth.service.impl

import kr.co.lovelydream.auth.jwt.JwtTokenProvider
import kr.co.lovelydream.auth.service.JwtService
import org.springframework.stereotype.Service

@Service
class JwtServiceImpl(
    private val jwtTokenProvider: JwtTokenProvider
) : JwtService {
    override fun generateAccessToken(memberId: String) = jwtTokenProvider.generateAccessToken(memberId)
    override fun generateRefreshToken(memberId: String) = jwtTokenProvider.generateRefreshToken(memberId)
    override fun isValid(token: String) = jwtTokenProvider.isValid(token)
    override fun getMemberId(token: String) = jwtTokenProvider.getMemberId(token)
    override fun getJti(token: String) = jwtTokenProvider.getJti(token)
    override fun getTyp(token: String) = jwtTokenProvider.getTyp(token)
    override fun getTier(token: String): String = jwtTokenProvider.getTier(token)
    override fun getRoles(token: String): List<String> = jwtTokenProvider.getRoles(token)
    override fun getExpiration(token: String) = jwtTokenProvider.getExpiration(token)
    override fun getRemainingTtlSeconds(token: String) = jwtTokenProvider.getRemainingTtlSeconds(token)
}