package kr.co.lovelydream.auth.service.impl

import kr.co.lovelydream.auth.constants.EmailConstants.CODE_RANGE
import kr.co.lovelydream.auth.constants.EmailConstants.EMAIL_BODY_TEMPLATE
import kr.co.lovelydream.auth.constants.EmailConstants.EMAIL_SUBJECT
import kr.co.lovelydream.auth.constants.EmailConstants.SENDER_EMAIL
import kr.co.lovelydream.auth.dto.ReqEmailDTO
import kr.co.lovelydream.auth.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.auth.dto.ReqLoginDTO
import kr.co.lovelydream.auth.dto.TokenDTO
import kr.co.lovelydream.auth.jwt.JwtTokenProvider
import kr.co.lovelydream.auth.service.AuthService
import kr.co.lovelydream.auth.service.RedisTokenService
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.AuthException
import kr.co.lovelydream.global.exception.MemberException
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.service.impl.MemberServiceImpl
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AuthServiceImpl(
    private val memberRepository: MemberRepository,
    private val mailSender: JavaMailSender,
    private val redisTemplate: StringRedisTemplate,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtTokenProvider,
    private val redisTokenService: RedisTokenService
) : AuthService {
    private val logger: Logger = LogManager.getLogger(MemberServiceImpl::class.java)

    override fun login(request: ReqLoginDTO): TokenDTO {
        val member = memberRepository.findByEmail(request.email)
            ?: throw MemberException(ResponseCode.MEMBER_NOT_FOUND)

        if (!passwordEncoder.matches(request.password, member.password)) {
            throw MemberException(ResponseCode.AUTH_INVALID_CREDENTIAL)
        }

        val accessToken  = jwtProvider.generateAccessToken(member.email)
        val refreshToken = jwtProvider.generateRefreshToken(member.email)
        val expiration   = jwtProvider.getExpiration(refreshToken)

        redisTokenService.saveRefreshToken(member.email, refreshToken, expiration)

        return TokenDTO(accessToken, refreshToken)
    }

    override fun reissue(refreshToken: String): TokenDTO {
        if (!jwtProvider.isValid(refreshToken)) {
            throw MemberException(ResponseCode.AUTH_UNAUTHORIZED)
        }

        val email = jwtProvider.getEmail(refreshToken)
        val saved = redisTokenService.getRefreshToken(email)
        if (saved == null || saved != refreshToken) {
            throw MemberException(ResponseCode.AUTH_UNAUTHORIZED)
        }

        val newAccess = jwtProvider.generateAccessToken(email)
        val newRefresh = jwtProvider.generateRefreshToken(email)

        redisTokenService.saveRefreshToken(email, newRefresh, jwtProvider.getExpiration(newRefresh))

        return TokenDTO(newAccess, newRefresh)
    }

    override fun logout(token: String) {
        if (!jwtProvider.isValid(token)) return

        val email = jwtProvider.getEmail(token)
        redisTokenService.deleteRefreshToken(email)

        val expiry = jwtProvider.getExpiration(token)
        redisTokenService.blacklistAccessToken(token, expiry)
    }

    override fun sendEmailCode(emailDTO: ReqEmailDTO): String {
        val email = emailDTO.email
        logger.info("이메일 인증 요청: email={}", email)

        if (memberRepository.findByEmail(email) != null) {
            logger.warn("중복 이메일로 인증 요청: {}", email)
            throw AuthException(ResponseCode.AUTH_EMAIL_ALREADY_EXISTS)
        }

        val code = generateCode()
        logger.debug("생성된 인증 코드: {}", code)

        sendEmail(email, code)
        logger.info("인증 코드 이메일 발송 완료: email={}", email)

        redisTemplate.opsForValue().set(
            "emailCode:$email",
            code,
            5, TimeUnit.MINUTES
        )
        logger.debug("Redis에 인증 코드 저장: key=emailCode:$email")

        return code
    }

    override fun verifyEmailCode(emailVerifyDTO: ReqEmailVerifyDTO) {
        val key = "emailCode:${emailVerifyDTO.email}"
        val savedCode = redisTemplate.opsForValue().get(key)

        if (savedCode == null) {
            throw AuthException(ResponseCode.AUTH_CODE_EXPIRED)
        }

        if (savedCode != emailVerifyDTO.code) {
            throw AuthException(ResponseCode.AUTH_CODE_MISMATCH)
        }
        redisTemplate.delete(key)
    }

    private fun generateCode(): String {
        val code = CODE_RANGE.random().toString()
        logger.debug("랜덤 인증 코드 생성: {}", code)
        return code
    }

    private fun sendEmail(to: String, code: String) {
        logger.info("이메일 전송 시작: to={}", to)
        val message = SimpleMailMessage().apply {
            from = SENDER_EMAIL
            setTo(to)
            subject = EMAIL_SUBJECT
            text = EMAIL_BODY_TEMPLATE.format(code)
        }

        try {
            mailSender.send(message)
            logger.info("이메일 전송 성공: to={}", to)
        } catch (ex: Exception) {
            logger.error("이메일 전송 실패: to={}, error={}", to, ex.message)
            throw AuthException(ResponseCode.AUTH_EMAIL_SEND_FAILED)
        }
    }
}