package kr.co.lovelydream.auth.service

import kr.co.lovelydream.auth.dto.ReqEmailDTO
import kr.co.lovelydream.auth.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.auth.dto.ReqLoginDTO
import kr.co.lovelydream.auth.dto.TokenDTO

interface AuthService {
    fun login(reqLoginDTO: ReqLoginDTO, deviceId: String): TokenDTO

    fun reissue(refreshToken: String, deviceId : String): TokenDTO

    fun logout(accessToken: String?, refreshToken: String?, deviceId : String?)

    fun blacklistAccessIfValid(at: String)

    fun sendEmailCode(emailDTO : ReqEmailDTO) : String

    fun verifyEmailCode(emailVerifyDTO: ReqEmailVerifyDTO)
}