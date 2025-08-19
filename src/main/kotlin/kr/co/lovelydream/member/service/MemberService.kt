package kr.co.lovelydream.member.service

import kr.co.lovelydream.member.dto.ReqCreateProfileDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper
import kr.co.lovelydream.member.dto.ResInitDataDTO

interface MemberService {
    fun signup(
        reqSignupWrapper : ReqSignupWrapper
    ) : Long

    fun createProfile(reqCreateProfileDTO : ReqCreateProfileDTO)

    fun selectMemberData(memberId : Long) : ResInitDataDTO
}