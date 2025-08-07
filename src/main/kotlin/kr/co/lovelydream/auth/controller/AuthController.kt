package kr.co.lovelydream.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import kr.co.lovelydream.auth.constants.JwtConstants.ACCESS_EXPIRATION_MS
import kr.co.lovelydream.auth.dto.ReqEmailDTO
import kr.co.lovelydream.auth.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.auth.dto.ReqLoginDTO
import kr.co.lovelydream.auth.dto.ReqReissueDTO
import kr.co.lovelydream.auth.dto.ResAccessTokenDTO
import kr.co.lovelydream.auth.service.AuthService
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.response.ResultResponse
import kr.co.lovelydream.global.vo.ResultVO
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 관련 API")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: ReqLoginDTO,
        response: HttpServletResponse
    ): ResponseEntity<ResAccessTokenDTO> {
        val token = authService.login(request)

        val refreshCookie = ResponseCookie.from("refreshToken", token.refreshToken)
            .httpOnly(true)
            .secure(false) // 배포 시 true
            .path("/")
            .sameSite("Strict")
            .maxAge(Duration.ofDays(7))
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())

        return ResponseEntity.ok(
            ResAccessTokenDTO(
                accessToken = token.accessToken,
                expiresIn = ACCESS_EXPIRATION_MS
            )
        )
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestBody request: ReqReissueDTO,
        response: HttpServletResponse
    ): ResponseEntity<ResAccessTokenDTO> {
        val token = authService.reissue(request.refreshToken)

        val refreshCookie = ResponseCookie.from("refreshToken", token.refreshToken)
            .httpOnly(true)
            .secure(false) // 배포 시 true
            .path("/")
            .sameSite("Strict")
            .maxAge(Duration.ofDays(7))
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())

        return ResponseEntity.ok(
            ResAccessTokenDTO(
                accessToken = token.accessToken,
                expiresIn = ACCESS_EXPIRATION_MS
            )
        )
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue("refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        // 1) 서버 측 블랙리스트에 refresh token 등록 (옵션)
        refreshToken?.let { authService.logout(it) }

        // 2) 클라이언트 쿠키 삭제용 Set-Cookie 헤더
        val expiredCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)      // 배포 시 true
            .path("/")
            .sameSite("Strict")
            .maxAge(0)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString())

        return ResponseEntity.ok().build()
    }

    @Operation(summary = "이메일 인증", description = "이메일 인증 코드를 전송합니다.")
    @PostMapping("/email")
    fun email(@Valid @RequestBody emailDTO: ReqEmailDTO): ResponseEntity<ResultVO<String>> {
        val code: String = authService.sendEmailCode(emailDTO);
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