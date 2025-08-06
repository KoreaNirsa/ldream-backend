package kr.co.lovelydream.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import kr.co.lovelydream.member.entity.Gender
import kr.co.lovelydream.member.entity.Member
import kr.co.lovelydream.member.entity.MemberTerms
import kr.co.lovelydream.member.entity.Terms
import kr.co.lovelydream.member.entity.TermsType
import java.time.LocalDate


@Schema(description = "회원가입 요청 약관 동의 DTO")
data class ReqSignupTermsDTO(
    @field:AssertTrue(message = "[필수] 서비스 이용약관에 동의해야 합니다.")
    @get:Schema(description = "[필수] 서비스 이용약관 동의 여부", example = "true")
    val agreeTerms: Boolean,

    @field:AssertTrue(message = "[필수] 개인정보 수집 및 이용에 동의해야 합니다.")
    @get:Schema(description = "[필수] 개인정보 수집 및 이용 동의 여부", example = "true")
    val agreePrivacy: Boolean,

    @field:AssertTrue(message = "[필수] 위치정보 수집 및 이용에 동의해야 합니다.")
    @get:Schema(description = "[필수] 위치정보 수집 및 이용 동의 여부", example = "true")
    val agreeLocation: Boolean,

    @field:AssertTrue(message = "[필수] 결제/환불 정책에 동의해야 합니다.")
    @get:Schema(description = "[필수] 결제/환불 정책 동의 여부", example = "true")
    val agreePaymentPolicy: Boolean,

    @get:Schema(description = "[선택] 마케팅 정보 수신 동의 여부", example = "false")
    val agreeMarketing: Boolean = false,

    @get:Schema(description = "[선택] 맞춤형 추천/개인화 서비스 동의 여부", example = "false")
    val agreePersonalized: Boolean = false
) {
    fun toMemberTermsEntity(member: Member, termsMap: Map<TermsType, Terms>): List<MemberTerms> {
        val list = mutableListOf<MemberTerms>()

        if (agreeTerms) list.add(MemberTerms(member = member, terms = termsMap[TermsType.SERVICE]!!))
        if (agreePrivacy) list.add(MemberTerms(member = member, terms = termsMap[TermsType.PRIVACY]!!))
        if (agreeLocation) list.add(MemberTerms(member = member, terms = termsMap[TermsType.LOCATION]!!))
        if (agreePaymentPolicy) list.add(MemberTerms(member = member, terms = termsMap[TermsType.PAYMENT]!!))
        if (agreeMarketing) list.add(MemberTerms(member = member, terms = termsMap[TermsType.MARKETING]!!))
        if (agreePersonalized) list.add(MemberTerms(member = member, terms = termsMap[TermsType.PERSONALIZATION]!!))

        return list
    }
}