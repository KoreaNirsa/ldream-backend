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
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
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
        val member = memberRepository.findByEmail(reqLoginDTO.email)
            ?: throw MemberException(ResponseCode.MEMBER_NOT_FOUND)

        if (!passwordEncoder.matches(reqLoginDTO.password, member.password)) {
            throw MemberException(ResponseCode.AUTH_INVALID_CREDENTIAL)
        }

        val accessToken = jwtProvider.generateAccessToken(member.email)
        val refreshToken = jwtProvider.generateRefreshToken(member.email)

        val expiration = jwtProvider.getExpiration(refreshToken)
        val refreshJti = jwtProvider.getJti(refreshToken)

        redisTokenService.saveRefreshToken(member.memberId!!, deviceId, refreshJti, expiration)

        return TokenDTO(accessToken, refreshToken)
    }

    override fun reissue(refreshToken: String, deviceId : String): TokenDTO {
        if (!jwtProvider.isValid(refreshToken)) throw AuthException(ResponseCode.AUTH_INVALID_REFRESH_TOKEN)

        val email = jwtProvider.getEmail(refreshToken)
        val member = memberRepository.findByEmail(email) ?: throw MemberException(ResponseCode.MEMBER_NOT_FOUND)

        val refreshJti = jwtProvider.getJti(refreshToken)
        val savedJti = redisTokenService.getRefreshToken(member.memberId!!, deviceId)
        if (savedJti == null || savedJti != refreshJti) {
            throw AuthException(ResponseCode.AUTH_INVALID_REFRESH_TOKEN)
        }
        // 재사용 감지(optional)
        if (redisTokenService.isRefreshUsed(refreshJti)) {
            // 보호조치: 해당 유저의 모든 세션 제거 등
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

        return TokenDTO(newAccess, newRefresh)
    }

    override fun logout(accessToken: String?, refreshToken: String?, deviceId : String?) {
        // (1) Access 블랙리스트 (유효하고 typ=access)
        accessToken
            ?.takeIf { jwtProvider.isValid(it) && jwtProvider.getTyp(it) == "access" }
            ?.let {
                val jti = jwtProvider.getJti(it)
                val ttl = jwtProvider.getRemainingTtlSeconds(it)
                if (ttl > 0) redisTokenService.blacklistAccessJti(jti, ttl)
            }

        // (2) Refresh 폐기 + 재사용 마킹 (유효하고 typ=refresh)
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
            }
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

    private fun resolveDeviceId(): String {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: return "unknown-device"

        val request = requestAttributes.request
        val headerDeviceId = request.getHeader("X-Device-Id")

        // 클라이언트가 직접 보낸 deviceId 사용
        if (!headerDeviceId.isNullOrBlank()) {
            return headerDeviceId.trim()
        }

        // 없으면 User-Agent + IP로 해시 생성
        val userAgent = request.getHeader("User-Agent") ?: "unknown-ua"
        val ip = request.remoteAddr ?: "unknown-ip"

        val raw = "$userAgent|$ip"
        return sha256(raw).take(16)
    }

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