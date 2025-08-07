package kr.co.lovelydream.member.service.impl

import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.AuthException
import kr.co.lovelydream.member.dto.ReqEmailDTO
import kr.co.lovelydream.member.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.service.AuthService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AuthServiceImpl(
    private val memberRepository: MemberRepository,
    private val mailSender: JavaMailSender,
    private val redisTemplate: StringRedisTemplate
) : AuthService {
    private val logger: Logger = LogManager.getLogger(MemberServiceImpl::class.java)

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
        val code = (100000..999999).random().toString()
        logger.debug("랜덤 인증 코드 생성: {}", code)
        return code
    }

    private fun sendEmail(to: String, code: String) {
        logger.info("이메일 전송 시작: to={}", to)
        val message = SimpleMailMessage().apply {
            from = "islandtim-project@naver.com"
            setTo(to)
            subject = "[LovelyDrme] 이메일 인증 코드"
            text = "요청하신 인증 코드는 [$code] 입니다.\n5분 내에 입력해주세요."
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