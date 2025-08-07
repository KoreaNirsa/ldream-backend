package kr.co.lovelydream.member.service.impl

import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.AuthException
import kr.co.lovelydream.member.dto.ReqEmailDTO
import kr.co.lovelydream.member.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.service.AuthService
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

    override fun sendEmailCode(emailDTO : ReqEmailDTO) : String {
        if (memberRepository.findByEmail(emailDTO.email) != null) {
            throw AuthException(ResponseCode.AUTH_EMAIL_ALREADY_EXISTS)
        }

        val code = generateCode()
        sendEmail(emailDTO.email, code)

        redisTemplate.opsForValue().set(
            "emailCode:${emailDTO.email}",
            code,
            5, TimeUnit.MINUTES
        )
        return code;
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
        return (100000..999999).random().toString()
    }

    private fun sendEmail(to: String, code: String) {
        val message = SimpleMailMessage().apply {
            setFrom("islandtim-project@naver.com")
            setTo(to)
            subject = "[LovelyDrme] 이메일 인증 코드"
            text = "요청하신 인증 코드는 [$code] 입니다.\n5분 내에 입력해주세요."
        }
        mailSender.send(message)
    }
}