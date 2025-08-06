package kr.co.lovelydream.member.service

import kr.co.lovelydream.member.dto.ReqEmailDTO
import kr.co.lovelydream.member.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.member.dto.ReqSignupMemberDTO
import kr.co.lovelydream.member.dto.ReqSignupTermsDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper

interface AuthService {
    fun signup(
        reqSignupWrapper : ReqSignupWrapper
    ) : Long

    fun sendEmailCode(emailDTO : ReqEmailDTO) : String

    fun verifyEmailCode(emailVerifyDTO: ReqEmailVerifyDTO)
}