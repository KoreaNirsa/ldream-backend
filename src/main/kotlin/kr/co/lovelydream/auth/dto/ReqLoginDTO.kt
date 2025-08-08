package kr.co.lovelydream.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "로그인 요청 DTO")
data class ReqLoginDTO(
    @field:Email(message = "올바른 이메일 형식을 입력하세요.")
    @field:NotBlank(message = "이메일은 필수 값입니다.")
    @get:Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "비밀번호를 입력하세요.")
    @field:Pattern(
        regexp = """^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#\$%^&*(),.?":{}|<>]).{8,}$""",
        message = "비밀번호는 특수문자 1개, 대문자 1개, 소문자 1개, 숫자를 포함해 8자 이상이어야 합니다."
    )
    @get:Schema(
        description = "비밀번호 (특수문자, 대소문자, 숫자 포함 8자 이상)",
        example = "Password123!"
    )
    val password: String
)
