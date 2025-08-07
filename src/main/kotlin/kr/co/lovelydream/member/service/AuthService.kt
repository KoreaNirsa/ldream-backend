package kr.co.lovelydream.member.service

import kr.co.lovelydream.member.dto.ReqEmailDTO
import kr.co.lovelydream.member.dto.ReqEmailVerifyDTO

interface AuthService {
    fun sendEmailCode(emailDTO : ReqEmailDTO) : String

    fun verifyEmailCode(emailVerifyDTO: ReqEmailVerifyDTO)
}