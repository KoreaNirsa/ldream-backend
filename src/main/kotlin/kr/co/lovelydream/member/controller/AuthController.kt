package kr.co.lovelydream.member.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.response.ResultResponse
import kr.co.lovelydream.global.vo.ResultVO
import kr.co.lovelydream.member.dto.ReqEmailDTO
import kr.co.lovelydream.member.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.member.dto.ReqSignupMemberDTO
import kr.co.lovelydream.member.dto.ReqSignupTermsDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper
import kr.co.lovelydream.member.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원가입 및 로그인 API")
class AuthController(
    private val authService: AuthService
) {

    @Operation(summary = "회원가입", description = "신규 유저를 등록합니다.")
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody reqSignupWrapper: ReqSignupWrapper
    ): ResponseEntity<ResultVO<Long>> {
        val memberId : Long = authService.signup(reqSignupWrapper)
        return ResultResponse.success(memberId, ResponseCode.SUCCESS)
    }

    @Operation(summary = "이메일 인증", description = "이메일 인증 코드를 전송합니다.")
    @PostMapping("/email")
    fun email(@Valid @RequestBody emailDTO: ReqEmailDTO): ResponseEntity<ResultVO<String>> {
        val code : String = authService.sendEmailCode(emailDTO);
        return ResultResponse.success(code, ResponseCode.SUCCESS)
    }

    @Operation(
        summary = "인증 코드 검증", description = "이메일 인증 코드를 검증합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "성공 - 인증 코드가 일치합니다."
            ),
            ApiResponse(
                responseCode = "400",
                description = "실패 - 요청 형식 오류 (DTO 유효성 검사 실패)",
                content = [Content(schema = Schema(implementation = ResultVO::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "실패 - 인증 코드가 만료되었거나 일치하지 않습니다.",
                content = [Content(schema = Schema(implementation = ResultVO::class))]
            ),
        ]
    )
    @PostMapping("/email/verify")
    fun emailVerify(@Valid @RequestBody emailVerifyDTO: ReqEmailVerifyDTO): ResponseEntity<ResultVO<Nothing>> {
        authService.verifyEmailCode(emailVerifyDTO)
        return ResultResponse.success(null, ResponseCode.SUCCESS)
    }
}