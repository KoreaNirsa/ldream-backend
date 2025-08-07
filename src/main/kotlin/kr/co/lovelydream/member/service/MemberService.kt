package kr.co.lovelydream.member.service

import kr.co.lovelydream.member.dto.ReqCreateProfileDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper

interface MemberService {
    fun signup(
        reqSignupWrapper : ReqSignupWrapper
    ) : Long

    fun createProfile(reqCreateProfileDTO : ReqCreateProfileDTO)
}