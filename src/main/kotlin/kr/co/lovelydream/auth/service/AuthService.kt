package kr.co.lovelydream.auth.service

import kr.co.lovelydream.auth.dto.ReqEmailDTO
import kr.co.lovelydream.auth.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.auth.dto.ReqLoginDTO
import kr.co.lovelydream.auth.dto.TokenDTO

interface AuthService {
    fun login(request: ReqLoginDTO): TokenDTO

    fun reissue(refreshToken: String): TokenDTO

    fun logout(token: String)

    fun sendEmailCode(emailDTO : ReqEmailDTO) : String

    fun verifyEmailCode(emailVerifyDTO: ReqEmailVerifyDTO)
}