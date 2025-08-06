package kr.co.lovelydream.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "프로필 신규 등록 요청 DTO")
data class ReqCreateProfileDTO (
    @field:Min(1)
    @get:Schema(description = "회원 고유 ID", example = "10")
    val memberId: Long,

    @field:NotBlank
    @get:Schema(description = "MBTI", example = "INFP")
    val mbti: String,

    @field:NotBlank
    @get:Schema(description = "선호 지역", example = "대구")
    val preferredLocation: String,

    @field:NotBlank
    @get:Schema(description = "선호 시간대", example = "저녁")
    val preferredTime: String,

    @field:NotBlank
    @get:Schema(description = "예산 범위", example = "10-20만원")
    val budgetRange: String,

    @field:NotBlank
    @get:Schema(description = "관계 상태", example = "연인과")
    val relationshipStatus: String,

    @field:NotEmpty
    @get:Schema(description = "선호 요일", example = "[화요일, 금요일, 토요일]")
    val preferredDays: List<String>,

    @field:NotEmpty
    @get:Schema(description = "데이트 분위기", example = "[인스타 감성, 신나는, 차분한]")
    val dateMoods: List<String>,

    @field:NotEmpty
    @get:Schema(description = "관심사", example = "[카페, 사진, 독서, 음악]")
    val interests: List<String>,

    @field:NotEmpty
    @get:Schema(description = "음식 취향", example = "[중식, 양식, 일식, 한식]")
    val foodPreferences: List<String>,

    @field:NotEmpty
    @get:Schema(description = "이동 수단", example = "[도보, 자동차, 대중교통]")
    val transportation: List<String>
)