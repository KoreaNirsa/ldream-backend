package kr.co.lovelydream.auth.service

import java.util.*

interface JwtService {
    fun generateAccessToken(email: String): String
    fun generateRefreshToken(email: String): String
    fun isValid(token: String): Boolean
    fun getEmail(token: String): String
    fun getJti(token: String): String
    fun getTyp(token: String): String
    fun getExpiration(token: String): Date
    fun getRemainingTtlSeconds(token: String): Long
}