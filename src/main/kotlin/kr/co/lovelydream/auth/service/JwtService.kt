package kr.co.lovelydream.auth.service

import java.util.*

interface JwtService {
    fun generateAccessToken(memberId: String): String
    fun generateRefreshToken(memberId: String): String
    fun isValid(token: String): Boolean
    fun getMemberId(token: String): String
    fun getJti(token: String): String
    fun getTyp(token: String): String
    fun getTier(token: String): String
    fun getRoles(token: String): List<String>
    fun getExpiration(token: String): Date
    fun getRemainingTtlSeconds(token: String): Long
}