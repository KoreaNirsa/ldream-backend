package kr.co.lovelydream.auth.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lovelydream.auth.jwt.JwtTokenProvider
import kr.co.lovelydream.auth.service.RedisTokenService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

class JwtAuthenticationFilter(
    private val jwtProvider: JwtTokenProvider,
    private val redisTokenService: RedisTokenService
) : OncePerRequestFilter() {
    private val pathMatcher = AntPathMatcher()

    // 인증 스킵할 경로 (필요에 따라 추가/수정)
    private val whiteList = listOf(
        "/api/auth/email",
        "/api/auth/email/verify",
        "/api/auth/signup",
        "/api/member/profile",
        "/api/auth/login",
        "/api/auth/reissue",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/error"
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return whiteList.any { pattern -> pathMatcher.match(pattern, path) }
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // CORS preflight는 패스
        if ("OPTIONS".equals(request.method, ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = resolveAccessToken(request)

        if (token.isNullOrBlank()) {
            // 토큰이 없으면 그냥 다음 필터로
            filterChain.doFilter(request, response)
            return
        }
        
        // typ 확인
        if (jwtProvider.getTyp(token) != "access") {
            writeUnauthorized(response, "ACCESS_TOKEN_TYP_MISMATCH"); return
        }
        
        // 유효성 확인(서명/만료)
        if (!jwtProvider.isValid(token)) {
            writeUnauthorized(response, "INVALID_OR_EXPIRED_ACCESS_TOKEN")
            return
        }

        // 블랙리스트(JTI) 확인
        val jti = runCatching { jwtProvider.getJti(token) }.getOrNull()
        if (jti.isNullOrBlank()) {
            writeUnauthorized(response, "ACCESS_TOKEN_JTI_MISSING")
            return
        }
        if (redisTokenService.isAccessBlacklisted(jti)) {
            writeUnauthorized(response, "ACCESS_TOKEN_BLACKLISTED")
            return
        }

        // 인증 컨텍스트 설정
        val email = runCatching { jwtProvider.getEmail(token) }.getOrNull()
        if (email.isNullOrBlank()) {
            writeUnauthorized(response, "ACCESS_TOKEN_SUBJECT_MISSING")
            return
        }

        val authentication = UsernamePasswordAuthenticationToken(email, null, emptyList())
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }

    private fun resolveAccessToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        val prefix = "Bearer "
        return if (header.startsWith(prefix, ignoreCase = true)) {
            header.substring(prefix.length).trim()
        } else {
            null
        }
    }

    private fun writeUnauthorized(response: HttpServletResponse, code: String) {
        if (response.isCommitted) return
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = "application/json;charset=UTF-8"
        response.writer.use { out ->
            out.write("""{"success":false,"code":"$code","message":"Unauthorized"}""")
        }
    }
}