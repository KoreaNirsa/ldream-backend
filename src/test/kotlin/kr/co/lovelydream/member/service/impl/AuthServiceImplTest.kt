package kr.co.lovelydream.member.service.impl

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.AuthException
import kr.co.lovelydream.member.dto.ReqEmailDTO
import kr.co.lovelydream.member.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.member.dto.ReqSignupMemberDTO
import kr.co.lovelydream.member.dto.ReqSignupTermsDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper
import kr.co.lovelydream.member.entity.Member
import kr.co.lovelydream.member.entity.MemberTerms
import kr.co.lovelydream.member.entity.Terms
import kr.co.lovelydream.member.enums.Gender
import kr.co.lovelydream.member.enums.TermsType
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.repository.MemberTermsRepository
import kr.co.lovelydream.member.repository.TermsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class AuthServiceImplTest {
    @MockK
    lateinit var memberRepository: MemberRepository

    @MockK
    lateinit var termsRepository: TermsRepository

    @MockK
    lateinit var memberTermsRepository: MemberTermsRepository

    @MockK
    lateinit var passwordEncoder: PasswordEncoder

    @MockK
    lateinit var mailSender: JavaMailSender

    @MockK
    lateinit var redisTemplate: StringRedisTemplate

    @InjectMockKs
    lateinit var authService: AuthServiceImpl

    private val sampleEmail = "user@example.com"

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    // 이메일이 이미 존재할 때 AuthException이 발생하는지 검증하는 테스트
    @Test
    fun signup_whenEmailExists_throwsAuthException() {
        // Given: 이메일이 이미 존재하도록 설정
        every { memberRepository.findByEmail(sampleEmail) } returns Member(
            memberId = 1L,
            email = sampleEmail,
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now(),
            gender = Gender.M,
            password = "encoded"
        )
        val memberDto = ReqSignupMemberDTO(
            email = sampleEmail,
            emailVerified = "Y",
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now().toString(),
            gender = "M",
            password = "Password123!"
        )
        val termsDto = ReqSignupTermsDTO(
            agreeAge = true,
            agreeTerms = true,
            agreePrivacy = true,
            agreeLocation = true,
            agreePaymentPolicy = true,
            agreeMarketing = false,
            agreePersonalized = false
        )
        val wrapper = ReqSignupWrapper(memberDto, termsDto)    // DTO 래퍼 생성

        // When & Then: AuthException 발생 및 메시지 검증
        val ex = assertThrows<AuthException> {
            authService.signup(wrapper)
        }
        assertEquals("이미 존재하는 이메일입니다.", ex.message) // 예외 메시지 검증
        verify(exactly = 1) { memberRepository.findByEmail(sampleEmail) } // 조회 메서드 호출 검증
    }

    // 정상 입력 시 회원과 약관이 저장되고 올바른 ID가 반환되는지 검증하는 테스트
    @Test
    fun signup_withValidInput_savesMemberAndTerms() {
        // Given: 이메일 미존재, 암호화 결과, 저장된 회원 설정
        every { memberRepository.findByEmail(sampleEmail) } returns null
        every { passwordEncoder.encode("Password123!") } returns "encodedPwd"
        val savedMember = Member(
            memberId = 1L,
            email = sampleEmail,
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now(),
            gender = Gender.M,
            password = "encodedPwd"
        )
        every { memberRepository.save(any<Member>()) } returns savedMember
        // TermsRepository stub: 모든 TermsType에 대해 동작하도록 설정
        every { termsRepository.findTopByTypeOrderByVersionDesc(any<TermsType>()) } returns Terms(
            termsId = 1L,
            type = TermsType.SERVICE,
            version = "v1",
            content = "Sample content",
            isRequired = true,
            createdAt = LocalDateTime.now()
        )
        // MemberTermsRepository stub: saveAll 호출 처리
        every { memberTermsRepository.saveAll(any<List<MemberTerms>>()) } answers { firstArg<List<MemberTerms>>() }

        val memberDto = ReqSignupMemberDTO(
            email = sampleEmail,
            emailVerified = "Y",
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now().toString(),
            gender = "M",
            password = "Password123!"
        )
        val termsDto = ReqSignupTermsDTO(
            agreeAge = true,
            agreeTerms = true,
            agreePrivacy = true,
            agreeLocation = true,
            agreePaymentPolicy = true,
            agreeMarketing = false,
            agreePersonalized = false
        )
        val wrapper = ReqSignupWrapper(memberDto, termsDto)

        // When: 회원가입 호출
        val resultId = authService.signup(wrapper)

        // Then: 반환된 ID와 저장 로직 검증
        assertEquals(1L, resultId)
        verify { memberRepository.save(match { it.email == sampleEmail && it.password == "encodedPwd" }) }
        verify { termsRepository.findTopByTypeOrderByVersionDesc(any<TermsType>()) }
        verify { memberTermsRepository.saveAll(any<List<MemberTerms>>()) }  // saveAll 호출 검증
    }

    // 최신 약관을 찾을 수 없을 때 AuthException(Terms Not Found) 발생 검증 테스트
    @Test
    fun signup_whenLatestTermsNotFound_throwsTermsNotFoundException() {
        // Given: 이메일 미존재, 패스워드 인코딩, 회원 저장 설정
        every { memberRepository.findByEmail(sampleEmail) } returns null
        every { passwordEncoder.encode(any()) } returns "encodedPwd"
        every { memberRepository.save(any<Member>()) } returns Member(
            memberId = 1L,
            email = sampleEmail,
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now(),
            gender = Gender.M,
            password = "encodedPwd"
        )
        // TermsRepository stub: SERVICE는 정상, PRIVACY는 null로 설정
        every { termsRepository.findTopByTypeOrderByVersionDesc(TermsType.SERVICE) } returns Terms(
            termsId = 1L,
            type = TermsType.SERVICE,
            version = "v1",
            content = "Sample content",
            isRequired = true,
            createdAt = LocalDateTime.now()
        )
        every { termsRepository.findTopByTypeOrderByVersionDesc(TermsType.PRIVACY) } returns null

        val memberDto = ReqSignupMemberDTO(
            email = sampleEmail,
            emailVerified = "Y",
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now().toString(),
            gender = "M",
            password = "Password123!"
        )
        val termsDto = ReqSignupTermsDTO(
            agreeAge = true, agreeTerms = true, agreePrivacy = true, agreeLocation = true, agreePaymentPolicy = true
        )
        val wrapper = ReqSignupWrapper(memberDto, termsDto)

        // When & Then: TERMS_NOT_FOUND 예외 검증
        val ex = assertThrows<AuthException> { authService.signup(wrapper) }
        assertEquals(ResponseCode.TERMS_NOT_FOUND, ex.code)
    }

    // 정상 약관 동의 시 saveAll 호출 및 리스트 크기 검증 테스트
    @Test
    fun signup_withValidTerms_savesAllMemberTerms() {
        // Given: 이메일 미존재, 패스워드 인코딩, 회원 저장 설정
        every { memberRepository.findByEmail(sampleEmail) } returns null
        every { passwordEncoder.encode(any()) } returns "encodedPwd"
        every { memberRepository.save(any<Member>()) } returns Member(
            memberId = 1L,
            email = sampleEmail,
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now(),
            gender = Gender.M,
            password = "encodedPwd"
        )
        // TermsRepository stub: 모든 타입에 대해 실제 Terms 반환
        every { termsRepository.findTopByTypeOrderByVersionDesc(any<TermsType>()) } answers {
            val type = firstArg<TermsType>()
            Terms(
                termsId = type.ordinal.toLong(),
                type = type,
                version = "v${type.ordinal}",
                content = "Content for $type",
                isRequired = true,
                createdAt = LocalDateTime.now()
            )
        }
        // MemberTermsRepository stub
        every { memberTermsRepository.saveAll(any<List<MemberTerms>>()) } answers { firstArg() }

        val memberDto = ReqSignupMemberDTO(
            email = sampleEmail,
            emailVerified = "Y",
            name = "홍길동",
            nickname = "길동이",
            birthDate = LocalDate.now().toString(),
            gender = "M",
            password = "Password123!"
        )
        val termsDto = ReqSignupTermsDTO(
            agreeAge = true, agreeTerms = true, agreePrivacy = true, agreeLocation = true, agreePaymentPolicy = true
        )
        val wrapper = ReqSignupWrapper(memberDto, termsDto)

        // When: 회원가입 호출
        authService.signup(wrapper)

        // Then: 요청한 모든 TermsType 개수만큼 saveAll 호출 검증
        val expectedCount = 4 // agreeTerms, agreePrivacy, agreeLocation, agreePaymentPolicy를 동의했으므로 4개
        val slotList = slot<List<MemberTerms>>()
        verify { memberTermsRepository.saveAll(capture(slotList)) }
        assertEquals(expectedCount, slotList.captured.size)
    }

    // 이메일 인증 코드 전송 시 메일 발송과 Redis TTL 저장을 검증하는 테스트
    @Test
    fun sendEmailCode_sendsEmailAndStoresTTL() {
        // Given: Redis 저장과 메일 전송 stub
        val dto = ReqEmailDTO(sampleEmail)
        every { redisTemplate.opsForValue().set(any(), any(), 5, TimeUnit.MINUTES) } just Runs
        val msgSlot = slot<SimpleMailMessage>()
        every { mailSender.send(capture(msgSlot)) } just Runs

        // When: 이메일 코드 전송 호출
        val code = authService.sendEmailCode(dto)
        val message = msgSlot.captured

        // Then: 수신자 및 본문, TTL 저장 검증
        assertTrue(message.to?.contains(sampleEmail) == true)
        assertTrue(message.text?.contains(code) == true)
        verify { redisTemplate.opsForValue().set("emailCode:$sampleEmail", code, 5, TimeUnit.MINUTES) }
        verify { mailSender.send(any<SimpleMailMessage>()) }
    }

    // 인증 코드가 없을 때 만료 예외(AuthException)가 발생하는지 검증하는 테스트
    @Test
    fun verifyEmailCode_whenNoCode_throwsExpiredException() {
        // Given: Redis에 키 없음
        val dto = ReqEmailVerifyDTO(sampleEmail, "123456")
        every { redisTemplate.opsForValue().get("emailCode:$sampleEmail") } returns null

        // When & Then: 만료 예외 검증
        val ex = assertThrows<AuthException> { authService.verifyEmailCode(dto) }
        assertEquals(ResponseCode.AUTH_CODE_EXPIRED, ex.code)
    }

    // 인증 코드 불일치 시 불일치 예외(AuthException)가 발생하는지 검증하는 테스트
    @Test
    fun verifyEmailCode_whenMismatch_throwsMismatchException() {
        // Given: Redis에 다른 코드 저장
        val dto = ReqEmailVerifyDTO(sampleEmail, "123456")
        every { redisTemplate.opsForValue().get("emailCode:$sampleEmail") } returns "654321"

        // When & Then: 불일치 예외 검증
        val ex = assertThrows<AuthException> { authService.verifyEmailCode(dto) }
        assertEquals(ResponseCode.AUTH_CODE_MISMATCH, ex.code)
    }

    // 인증 코드 일치 시 Redis 키가 삭제되는지 검증하는 테스트
    @Test
    fun verifyEmailCode_whenMatch_deletesKey() {
        // Given: Redis에 일치 코드 저장
        val dto = ReqEmailVerifyDTO(sampleEmail, "123456")
        every { redisTemplate.opsForValue().get("emailCode:$sampleEmail") } returns "123456"
        every { redisTemplate.delete("emailCode:$sampleEmail") } returns true

        // When: 코드 검증 호출
        authService.verifyEmailCode(dto)

        // Then: 키 삭제 호출 검증
        verify { redisTemplate.delete("emailCode:$sampleEmail") }
    }
}
