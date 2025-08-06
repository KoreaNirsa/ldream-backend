package kr.co.lovelydream.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "이메일 인증 요청 DTO")
data class ReqEmailVerifyDTO (
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 아닙니다.")
    @get:Schema(description = "이메일 주소", example = "islandtim@naver.com")
    val email: String,

    @field:NotBlank(message = "인증 코드는 필수입니다.")
    @get:Schema(description = "인증 코드", example = "230411")
    val code: String
)
