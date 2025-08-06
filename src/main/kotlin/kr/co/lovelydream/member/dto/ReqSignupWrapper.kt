package kr.co.lovelydream.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "회원가입 요청 Wrapper")
data class ReqSignupWrapper(
    @field:Valid val member: ReqSignupMemberDTO,
    @field:Valid val terms: ReqSignupTermsDTO
)