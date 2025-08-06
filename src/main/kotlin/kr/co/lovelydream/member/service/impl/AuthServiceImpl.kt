package kr.co.lovelydream.member.service.impl

import jakarta.transaction.Transactional
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.AuthException
import kr.co.lovelydream.member.dto.ReqEmailDTO
import kr.co.lovelydream.member.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.member.service.AuthService
import kr.co.lovelydream.member.dto.ReqSignupMemberDTO
import kr.co.lovelydream.member.dto.ReqSignupTermsDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper
import kr.co.lovelydream.member.entity.Terms
import kr.co.lovelydream.member.entity.TermsType
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.repository.MemberTermsRepository
import kr.co.lovelydream.member.repository.TermsRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AuthServiceImpl(
    private val memberRepository: MemberRepository,
    private val termsRepository: TermsRepository,
    private val memberTermsRepository: MemberTermsRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailSender: JavaMailSender,
    private val redisTemplate: StringRedisTemplate
) : AuthService {

    @Transactional
    override fun signup(
        reqSignupWrapper : ReqSignupWrapper
    ) : Long {
        val requestMember = reqSignupWrapper.member
        val requestTerms = reqSignupWrapper.terms

        // 회원 정보 저장
        if (memberRepository.findByEmail(requestMember.email) != null) {
            throw AuthException(ResponseCode.AUTH_EMAIL_ALREADY_EXISTS)
        }
        val encodedPassword = passwordEncoder.encode(requestMember.password)
        val member = requestMember.toMemberEntity(encodedPassword)
        val savedMember = memberRepository.save(member)

        // 약관 동의 조회
        val latestTermsMap: Map<TermsType, Terms> = TermsType.entries.associateWith { type ->
            termsRepository.findTopByTypeOrderByVersionDesc(type)
                ?: throw AuthException(ResponseCode.TERMS_NOT_FOUND)
        }

        val memberTermsList = requestTerms.toMemberTermsEntity(savedMember, latestTermsMap)

        // 약관 동의 저장
        memberTermsRepository.saveAll(memberTermsList)

        return savedMember.memberId!!
    }

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