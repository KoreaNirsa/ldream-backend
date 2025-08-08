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
import kr.co.lovelydream.global.util.LoggingUtil.maskEmail
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.service.impl.MemberServiceImpl
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
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

    override fun login(reqLoginDTO: ReqLoginDTO, deviceId: String): TokenDTO {
        logger.info("로그인 처리 시작 - 이메일={}, 디바이스ID={}", maskEmail(reqLoginDTO.email), deviceId.take(12))

        val member = memberRepository.findByEmail(reqLoginDTO.email)
            ?: throw MemberException(ResponseCode.MEMBER_NOT_FOUND)

        if (!passwordEncoder.matches(reqLoginDTO.password, member.password)) {
            logger.warn("로그인 실패 - 비밀번호 불일치, 이메일={}", maskEmail(reqLoginDTO.email))
            throw MemberException(ResponseCode.AUTH_INVALID_CREDENTIAL)
        }

        val accessToken = jwtProvider.generateAccessToken(member.email)
        val refreshToken = jwtProvider.generateRefreshToken(member.email)

        val expiration = jwtProvider.getExpiration(refreshToken)
        val refreshJti = jwtProvider.getJti(refreshToken)

        redisTokenService.saveRefreshToken(member.memberId!!, deviceId, refreshJti, expiration)
        logger.info("로그인 완료 - 이메일={}, AccessToken길이={}, RefreshJTI={}", maskEmail(reqLoginDTO.email), accessToken.length, refreshJti.take(8))

        return TokenDTO(accessToken, refreshToken)
    }

    override fun reissue(refreshToken: String, deviceId : String): TokenDTO {
        logger.info("토큰 재발급 처리 시작 - 디바이스ID={}", deviceId.take(12))

        if (!jwtProvider.isValid(refreshToken)) {
            logger.warn("토큰 재발급 실패 - RefreshToken 유효하지 않음")

            throw AuthException(ResponseCode.AUTH_INVALID_REFRESH_TOKEN)
        }

        val email = jwtProvider.getEmail(refreshToken)
        val member = memberRepository.findByEmail(email) ?: throw MemberException(ResponseCode.MEMBER_NOT_FOUND)

        val refreshJti = jwtProvider.getJti(refreshToken)
        val savedJti = redisTokenService.getRefreshToken(member.memberId!!, deviceId)
        if (savedJti == null || savedJti != refreshJti) {
            logger.warn("토큰 재발급 실패 - Redis 저장값 불일치")
            throw AuthException(ResponseCode.AUTH_INVALID_REFRESH_TOKEN)
        }
        // 재사용 감지(optional)
        if (redisTokenService.isRefreshUsed(refreshJti)) {
            logger.warn("토큰 재발급 실패 - 이미 사용된 RefreshToken")
            throw AuthException(ResponseCode.AUTH_REUSED_REFRESH_TOKEN)
        }
        // 회전
        redisTokenService.deleteRefreshToken(member.memberId!!, deviceId)
        redisTokenService.markRefreshUsed(refreshJti, jwtProvider.getRemainingTtlSeconds(refreshToken))

        val newAccess = jwtProvider.generateAccessToken(email)
        val newRefresh = jwtProvider.generateRefreshToken(email)
        val newExp = jwtProvider.getExpiration(newRefresh)
        val newJti = jwtProvider.getJti(newRefresh)
        redisTokenService.saveRefreshToken(member.memberId!!, deviceId, newJti, newExp)
        logger.info("토큰 재발급 완료 - 디바이스ID={}, 새로운RefreshJTI={}", deviceId.take(12), newJti.take(8))

        return TokenDTO(newAccess, newRefresh)
    }

    override fun logout(accessToken: String?, refreshToken: String?, deviceId : String?) {
        logger.info("로그아웃 처리 시작 - AccessToken여부={}, RefreshToken여부={}, 디바이스ID={}", accessToken != null, refreshToken != null, deviceId?.take(12))

        // Access 블랙리스트 (유효하고 typ=access)
        accessToken
            ?.takeIf { jwtProvider.isValid(it) && jwtProvider.getTyp(it) == "access" }
            ?.let {
                val jti = jwtProvider.getJti(it)
                val ttl = jwtProvider.getRemainingTtlSeconds(it)
                if (ttl > 0) redisTokenService.blacklistAccessJti(jti, ttl)
                logger.info("AccessToken 블랙리스트 등록 - JTI={}", jti.take(8))

            }

        //  Refresh 폐기 + 재사용 마킹 (유효하고 typ=refresh)
        refreshToken
            ?.takeIf { jwtProvider.isValid(it) && jwtProvider.getTyp(it) == "refresh" }
            ?.let { rt ->
                val email = jwtProvider.getEmail(rt)
                val memberId = memberRepository.findByEmail(email)?.memberId ?: return@let

                val did = deviceId ?: error("deviceId must be provided by controller/filter")
                redisTokenService.deleteRefreshToken(memberId, did)
                redisTokenService.markRefreshUsed(
                    jwtProvider.getJti(rt),
                    jwtProvider.getRemainingTtlSeconds(rt)
                )
                logger.info("RefreshToken 폐기 및 재사용 방지 처리 완료 - JTI={}", jwtProvider.getJti(rt).take(8))
            }
        logger.info("로그아웃 처리 완료")
    }

    override fun blacklistAccessIfValid(at: String) {
        if (jwtProvider.isValid(at)) {
            redisTokenService.blacklistAccessJti(jwtProvider.getJti(at), jwtProvider.getRemainingTtlSeconds(at))
        }
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

    // ---------- private fun ----------
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
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