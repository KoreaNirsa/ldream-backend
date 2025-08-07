package kr.co.lovelydream.member.service

import kr.co.lovelydream.member.dto.ReqCreateProfileDTO

interface MemberService {
    fun createProfile(reqCreateProfileDTO : ReqCreateProfileDTO)
}