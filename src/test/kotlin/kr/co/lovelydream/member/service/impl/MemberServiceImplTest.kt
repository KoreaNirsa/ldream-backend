package kr.co.lovelydream.member.service.impl

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.MemberException
import kr.co.lovelydream.member.dto.ReqSignupMemberDTO
import kr.co.lovelydream.member.dto.ReqSignupTermsDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper
import kr.co.lovelydream.member.entity.Member
import kr.co.lovelydream.member.entity.MemberTerms
import kr.co.lovelydream.member.entity.Terms
import kr.co.lovelydream.member.enums.Gender
import kr.co.lovelydream.member.enums.TermsType
import kr.co.lovelydream.member.repository.MemberProfileRepository
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.repository.MemberTermsRepository
import kr.co.lovelydream.member.repository.ProfileDateMoodRepository
import kr.co.lovelydream.member.repository.ProfileDaysRepository
import kr.co.lovelydream.member.repository.ProfileFoodRepository
import kr.co.lovelydream.member.repository.ProfileInterestRepository
import kr.co.lovelydream.member.repository.ProfileTransportationRepository
import kr.co.lovelydream.member.repository.TermsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class MemberServiceImplTest {
    @MockK lateinit var memberRepository: MemberRepository
    @MockK lateinit var memberTermsRepository: MemberTermsRepository
    @MockK lateinit var termsRepository: TermsRepository
    @MockK lateinit var memberProfileRepository: MemberProfileRepository
    @MockK lateinit var profileInterestRepository: ProfileInterestRepository
    @MockK lateinit var profileFoodRepository: ProfileFoodRepository
    @MockK lateinit var profileDaysRepository: ProfileDaysRepository
    @MockK lateinit var profileTransportationRepository: ProfileTransportationRepository
    @MockK lateinit var profileDateMoodRepository: ProfileDateMoodRepository
    @MockK lateinit var passwordEncoder: PasswordEncoder
    @MockK lateinit var mailSender: JavaMailSender
    @MockK lateinit var redisTemplate: StringRedisTemplate
    @InjectMockKs lateinit var memberService: MemberServiceImpl

    private val sampleEmail = "user@example.com"

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    // 이메일이 이미 존재할 때 MemberException이 발생하는지 검증하는 테스트
    @Test
    fun signup_whenEmailExists_throwsMemberException() {
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

        // When & Then: MemberException 발생 및 메시지 검증
        val ex = assertThrows<MemberException> {
            memberService.signup(wrapper)
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
        val resultId = memberService.signup(wrapper)

        // Then: 반환된 ID와 저장 로직 검증
        assertEquals(1L, resultId)
        verify { memberRepository.save(match { it.email == sampleEmail && it.password == "encodedPwd" }) }
        verify { termsRepository.findTopByTypeOrderByVersionDesc(any<TermsType>()) }
        verify { memberTermsRepository.saveAll(any<List<MemberTerms>>()) }  // saveAll 호출 검증
    }

    // 최신 약관을 찾을 수 없을 때 MemberException(Terms Not Found) 발생 검증 테스트
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

        // PRIVACY는 null 반환 및 나머지는 정상 반환
        TermsType.entries.forEach { type ->
            val shouldReturnNull = (type == TermsType.PRIVACY)
            every { termsRepository.findTopByTypeOrderByVersionDesc(type) } returns (
                    if (shouldReturnNull) null else Terms(
                        termsId = 1L,
                        type = type,
                        version = "v1",
                        content = "Sample content",
                        isRequired = true,
                        createdAt = LocalDateTime.now()
                    )
                    )
        }

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
        val ex = assertThrows<MemberException> { memberService.signup(wrapper) }
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
        memberService.signup(wrapper)

        // Then: 요청한 모든 TermsType 개수만큼 saveAll 호출 검증
        val expectedCount = 5 // agreeAge, agreeTerms, agreePrivacy, agreeLocation, agreePaymentPolicy를 동의했으므로 5개
        val slotList = slot<List<MemberTerms>>()
        verify { memberTermsRepository.saveAll(capture(slotList)) }
        assertEquals(expectedCount, slotList.captured.size)
    }

}
