package kr.co.lovelydream.member.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.response.ResultResponse
import kr.co.lovelydream.global.vo.ResultVO
import kr.co.lovelydream.member.dto.ReqCreateProfileDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper
import kr.co.lovelydream.member.service.MemberService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/member")
@Tag(name = "Member", description = "회원 관련 API")
class MemberController(
    private val memberService: MemberService
) {

    @Operation(summary = "회원가입", description = "신규 유저를 등록합니다.")
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody reqSignupWrapper: ReqSignupWrapper
    ): ResponseEntity<ResultVO<Long>> {
        val memberId : Long = memberService.signup(reqSignupWrapper)
        return ResultResponse.success(memberId, ResponseCode.SUCCESS)
    }

    @Operation(summary = "프로필 신규 등록", description = "프로필 설정을 신규 등록합니다.")
    @PostMapping("/profile")
    fun createProfile(
        @Valid @RequestBody reqCreateProfileDTO: ReqCreateProfileDTO
    ): ResponseEntity<ResultVO<Nothing>> {
        memberService.createProfile(reqCreateProfileDTO)
        return ResultResponse.success(null, ResponseCode.SUCCESS)
    }

    @Operation(summary = "초기 데이터 정보 조회", description = "로그인 후 필요한 데이터를 조회합니다.")
    @GetMapping("/{memberId}")
    fun selectMemberData(@PathVariable memberId: Long): ResponseEntity<ResultVO<Nothing>>{
        System.out.println("ok")
        return ResultResponse.success(null, ResponseCode.SUCCESS)
    }

}