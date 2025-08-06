package kr.co.lovelydream.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import kr.co.lovelydream.member.entity.Gender
import kr.co.lovelydream.member.entity.Member
import java.time.LocalDate


@Schema(description = "회원가입 요청 회원정보 DTO")
data class ReqSignupMemberDTO(

    @field:Email(message = "올바른 이메일 형식을 입력하세요.")
    @field:NotBlank(message = "이메일은 필수 값입니다.")
    @get:Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "이메일 인증 여부는 필수 값입니다.")
    @field:Pattern(
        regexp = "^Y$",
        message = "이메일 인증이 완료되지 않았습니다."
    )
    @get:Schema(description = "이메일 인증 여부 (Y만 허용)", example = "Y")
    val emailVerified : String,

    @field:NotBlank(message = "이름을 입력하세요.")
    @field:Pattern(
        regexp = "^[가-힣]{2,10}$",
        message = "이름은 2~10자의 한글만 입력 가능합니다."
    )
    @get:Schema(description = "사용자 이름(한글만 가능)", example = "홍길동")
    val name: String,

    @field:NotBlank(message = "별명을 입력하세요.")
    @field:Pattern(
        regexp = "^[가-힣a-zA-Z0-9]{2,12}$",
        message = "별명은 2~12자의 한글, 영어, 숫자만 입력 가능합니다."
    )
    @get:Schema(description = "상대방이 부를 별명", example = "길동이123")
    val nickname: String,

    @field:NotBlank(message = "생년월일은 필수 값입니다.")
    @field:Pattern(
        regexp = "\\d{4}-\\d{2}-\\d{2}",
        message = "생년월일은 YYYY-MM-DD 형식이어야 합니다."
    )
    @get:Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-05-21")
    val birthDate: String,

    @field:NotBlank(message = "성별을 선택하세요.")
    @get:Schema(description = "성별 (M: 남성, F: 여성)", example = "M")
    val gender: String,

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
) {
    /**
     * ReqSignupDTO → Member 엔티티 변환
     * @param encodedPassword BCrypt 등으로 암호화된 비밀번호
     */
    fun toMemberEntity(encodedPassword: String): Member {
        return Member(
            email = this.email,
            name = this.name,
            nickname = this.nickname,
            birthDate = LocalDate.parse(this.birthDate),
            gender = Gender.valueOf(this.gender),
            password = encodedPassword
        )
    }
}
