package kr.co.lovelydream.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import kr.co.lovelydream.auth.constants.JwtConstants.ACCESS_EXPIRATION_MS
import kr.co.lovelydream.auth.dto.ReqEmailDTO
import kr.co.lovelydream.auth.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.auth.dto.ReqLoginDTO
import kr.co.lovelydream.auth.dto.ResAccessTokenDTO
import kr.co.lovelydream.auth.service.AuthService
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.response.ResultResponse
import kr.co.lovelydream.global.util.LoggingUtil
import kr.co.lovelydream.global.vo.ResultVO
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import io.swagger.v3.oas.annotations.parameters.RequestBody as OasRequestBody

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 관련 API")
class AuthController(
    private val authService: AuthService
) {
    private val logger: Logger = LogManager.getLogger(AuthController::class.java)

    @Operation(
        summary = "로그인",
        description = "이메일/비밀번호로 로그인하고 Access/Refresh 토큰을 발급한다. Refresh는 HttpOnly 쿠키로 내려간다.",
        requestBody = OasRequestBody(
            required = true,
            content = [Content(schema = Schema(implementation = ReqLoginDTO::class))]
        ),
        responses = [
            ApiResponse(
                responseCode = "200", description = "성공",
                content = [Content(schema = Schema(implementation = ResAccessTokenDTO::class))]
            ),
            ApiResponse(responseCode = "401", description = "자격 증명 실패"),
            ApiResponse(responseCode = "404", description = "사용자 없음")
        ]
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody reqLoginDTO: ReqLoginDTO,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResAccessTokenDTO> {
        val deviceId = resolveDeviceId(request)
        logger.info("로그인 요청 시작 - 이메일={}, 디바이스ID={}", LoggingUtil.maskEmail(reqLoginDTO.email), deviceId.take(12))

        val token = authService.login(reqLoginDTO, deviceId)

        val refreshCookie = ResponseCookie.from("refreshToken", token.refreshToken)
            .httpOnly(true)
            .secure(false) // 배포 시 true
            .path("/")
            .sameSite("Strict")
            .maxAge(Duration.ofDays(7))
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        logger.info(
            "로그인 성공 - 이메일={}, AccessToken길이={}, RefreshToken 쿠키 설정 완료",
            LoggingUtil.maskEmail(reqLoginDTO.email),
            token.accessToken.length
        )

        return ResponseEntity.ok(
            ResAccessTokenDTO(
                accessToken = token.accessToken,
                expiresIn = ACCESS_EXPIRATION_MS
            )
        )
    }

    @Operation(
        summary = "액세스 토큰 재발급",
        description = "Refresh 쿠키를 이용해 Access/Refresh를 재발급한다(회전).",
        parameters = [
            Parameter(
                name = "refreshToken",
                `in` = ParameterIn.COOKIE,
                required = true,
                description = "HttpOnly 쿠키에 담긴 Refresh JWT"
            )
        ],
        responses = [
            ApiResponse(
                responseCode = "200", description = "성공",
                content = [Content(schema = Schema(implementation = ResAccessTokenDTO::class))]
            ),
            ApiResponse(responseCode = "401", description = "Refresh 무효/만료/재사용"),
            ApiResponse(responseCode = "404", description = "사용자 없음")
        ]
    )
    @PostMapping("/reissue")
    fun reissue(
        @CookieValue("refreshToken") refreshToken: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResAccessTokenDTO> {
        val deviceId = resolveDeviceId(request)
        logger.info("토큰 재발급 요청 시작 - 디바이스ID={}", deviceId.take(12))

        val token = authService.reissue(refreshToken, deviceId)

        val refreshCookie = ResponseCookie.from("refreshToken", token.refreshToken)
            .httpOnly(true)
            .secure(false) // 배포 시 true
            .path("/")
            .sameSite("Strict")
            .maxAge(Duration.ofDays(7))
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        logger.info(
            "토큰 재발급 성공 - 디바이스ID={}, AccessToken길이={}, RefreshToken 회전 완료",
            deviceId.take(12),
            token.accessToken.length
        )

        return ResponseEntity.ok(
            ResAccessTokenDTO(
                accessToken = token.accessToken,
                expiresIn = ACCESS_EXPIRATION_MS
            )
        )
    }

    @Operation(
        summary = "로그아웃",
        description = "Access는 블랙리스트로, Refresh는 삭제 및 재사용 방지로 처리한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
        parameters = [
            Parameter(
                name = HttpHeaders.AUTHORIZATION,
                `in` = ParameterIn.HEADER,
                required = false,
                description = "Bearer 액세스 토큰"
            ),
            Parameter(
                name = "refreshToken",
                `in` = ParameterIn.COOKIE,
                required = false,
                description = "HttpOnly Refresh 쿠키"
            )
        ],
        responses = [
            ApiResponse(responseCode = "200", description = "성공"),
            ApiResponse(responseCode = "401", description = "토큰 무효"),
        ]
    )
    @GetMapping("/logout")
    fun logout(
        @CookieValue("refreshToken", required = false) refreshToken: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authz: String?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val accessToken = extractBearerToken(authz)
        val deviceId = resolveDeviceId(request)
        logger.info(
            "로그아웃 요청 시작 - AccessToken여부={}, RefreshToken여부={}, 디바이스ID={}",
            accessToken != null,
            refreshToken != null,
            deviceId.take(12)
        )

        authService.logout(accessToken, refreshToken, deviceId)
        // refresh 쿠키 삭제
        val expired = ResponseCookie.from("refreshToken", "")
            .httpOnly(true).secure(false).path("/").sameSite("Strict").maxAge(0).build()
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString())
        logger.info("로그아웃 성공 - AccessToken블랙리스트={}, RefreshToken폐기여부={}", accessToken != null, refreshToken != null)

        return ResponseEntity.ok().build()
    }

    @Operation(summary = "이메일 인증", description = "이메일 인증 코드를 전송합니다.")
    @PostMapping("/email")
    fun email(@Valid @RequestBody emailDTO: ReqEmailDTO): ResponseEntity<ResultVO<Nothing>> {
        authService.sendEmailCode(emailDTO);
        return ResultResponse.success(null, ResponseCode.SUCCESS)
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

    // -------- private fun --------
    private fun extractBearerToken(authz: String?): String? =
        authz?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.removePrefix("Bearer ")
            ?.trim()

    private fun resolveDeviceId(request: HttpServletRequest): String {
        // 우선순위: 1) 헤더 2) UA+IP 해시
        request.getHeader("X-Device-Id")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val ua = request.getHeader("User-Agent") ?: "unknown-ua"
        val ip = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr ?: "unknown-ip"
        val source = "$ua|$ip"

        // 간단 해시 (충돌/길이 제한 등 필요 시 교체)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
            java.security.MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
        ).take(24)
    }
}