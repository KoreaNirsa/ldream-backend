package kr.co.lovelydream.member.service.impl

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.MemberException
import kr.co.lovelydream.member.dto.ReqCreateProfileDTO
import kr.co.lovelydream.member.entity.MemberProfile
import kr.co.lovelydream.member.entity.ProfileDateMood
import kr.co.lovelydream.member.entity.ProfileDays
import kr.co.lovelydream.member.entity.ProfileFood
import kr.co.lovelydream.member.entity.ProfileInterest
import kr.co.lovelydream.member.entity.ProfileTransportation
import kr.co.lovelydream.member.repository.MemberProfileRepository
import kr.co.lovelydream.member.repository.MemberRepository
import kr.co.lovelydream.member.repository.MemberTermsRepository
import kr.co.lovelydream.member.repository.ProfileDateMoodRepository
import kr.co.lovelydream.member.repository.ProfileDaysRepository
import kr.co.lovelydream.member.repository.ProfileFoodRepository
import kr.co.lovelydream.member.repository.ProfileInterestRepository
import kr.co.lovelydream.member.repository.ProfileTransportationRepository
import kr.co.lovelydream.member.repository.TermsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockKExtension::class)
@DisplayName("프로필 생성 - MemberServiceImpl")
class MemberSignupServiceImplTest {
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

    private fun sampleRequest(memberId: Long = 100L) = ReqCreateProfileDTO(
        memberId = memberId,
        mbti = "INFP",
        preferredLocation = "서울",
        preferredTime = "주말 저녁",
        budgetRange = "3만~5만원",
        relationshipStatus = "연애중",
        preferredDays = listOf("토", "일"),
        dateMoods = listOf("인스타 감성", "차분한"),
        interests = listOf("카페", "사진", "독서"),
        foodPreferences = listOf("한식", "양식"),
        transportation = listOf("도보", "대중교통")
    )

    @Test
    @DisplayName("회원이 존재하면 프로필과 하위 항목들이 저장된다")
    fun createProfile_success() {
        // given
        val req = sampleRequest()

        every { memberRepository.existsById(req.memberId) } returns true

        val savedProfile = MemberProfile(
            memberProfileId = 1L,
            memberId = req.memberId,
            mbti = req.mbti,
            preferredRegion = req.preferredLocation,
            preferredTime = req.preferredTime,
            preferredBudget = req.budgetRange,
            relationshipStatus = req.relationshipStatus
        )
        every { memberProfileRepository.save(any()) } returns savedProfile

        // saveAll은 그대로 첫 인자 반환
        every { profileInterestRepository.saveAll(any<Iterable<ProfileInterest>>()) } answers { firstArg() }
        every { profileFoodRepository.saveAll(any<Iterable<ProfileFood>>()) } answers { firstArg() }
        every { profileDaysRepository.saveAll(any<Iterable<ProfileDays>>()) } answers { firstArg() }
        every { profileTransportationRepository.saveAll(any<Iterable<ProfileTransportation>>()) } answers { firstArg() }
        every { profileDateMoodRepository.saveAll(any<Iterable<ProfileDateMood>>()) } answers { firstArg() }

        // when
        memberService.createProfile(req) // ✅ 변수명 통일

        // then
        verify(exactly = 1) { memberRepository.existsById(req.memberId) }
        verify(exactly = 1) { memberProfileRepository.save(any()) }

        // 각 saveAll 호출 횟수
        verify(exactly = 1) { profileInterestRepository.saveAll(any<Iterable<ProfileInterest>>()) }
        verify(exactly = 1) { profileFoodRepository.saveAll(any<Iterable<ProfileFood>>()) }
        verify(exactly = 1) { profileDaysRepository.saveAll(any<Iterable<ProfileDays>>()) }
        verify(exactly = 1) { profileTransportationRepository.saveAll(any<Iterable<ProfileTransportation>>()) }
        verify(exactly = 1) { profileDateMoodRepository.saveAll(any<Iterable<ProfileDateMood>>()) }

        // withArg로 사이즈 + 매핑 검증
        verify {
            profileInterestRepository.saveAll(withArg { list: Iterable<ProfileInterest> ->
                kotlin.test.assertEquals(req.interests.size, list.count())
                kotlin.test.assertEquals(savedProfile.memberProfileId, list.first().memberProfile.memberProfileId)
            })
            profileFoodRepository.saveAll(withArg { list: Iterable<ProfileFood> ->
                kotlin.test.assertEquals(req.foodPreferences.size, list.count())
                kotlin.test.assertEquals(savedProfile.memberProfileId, list.first().memberProfile.memberProfileId)
            })
            profileDaysRepository.saveAll(withArg { list: Iterable<ProfileDays> ->
                kotlin.test.assertEquals(req.preferredDays.size, list.count())
                kotlin.test.assertEquals(savedProfile.memberProfileId, list.first().memberProfile.memberProfileId)
            })
            profileTransportationRepository.saveAll(withArg { list: Iterable<ProfileTransportation> ->
                kotlin.test.assertEquals(req.transportation.size, list.count())
                kotlin.test.assertEquals(savedProfile.memberProfileId, list.first().memberProfile.memberProfileId)
            })
            profileDateMoodRepository.saveAll(withArg { list: Iterable<ProfileDateMood> ->
                kotlin.test.assertEquals(req.dateMoods.size, list.count())
                kotlin.test.assertEquals(savedProfile.memberProfileId, list.first().memberProfile.memberProfileId)
            })
        }
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 발생하고 어떤 것도 저장되지 않는다")
    fun createProfile_memberNotFound() {
        // given
        val req = sampleRequest(memberId = 9999L)
        every { memberRepository.existsById(req.memberId) } returns false

        // when
        val ex = assertThrows(MemberException::class.java) {
            memberService.createProfile(req) // ✅ 변수명 통일
        }

        // then
        assertEquals(ResponseCode.MEMBER_NOT_FOUND, ex.code)
        verify(exactly = 0) { memberProfileRepository.save(any()) }
        verify(exactly = 0) { profileInterestRepository.saveAll(any<Iterable<ProfileInterest>>()) }
        verify(exactly = 0) { profileFoodRepository.saveAll(any<Iterable<ProfileFood>>()) }
        verify(exactly = 0) { profileDaysRepository.saveAll(any<Iterable<ProfileDays>>()) }
        verify(exactly = 0) { profileTransportationRepository.saveAll(any<Iterable<ProfileTransportation>>()) }
        verify(exactly = 0) { profileDateMoodRepository.saveAll(any<Iterable<ProfileDateMood>>()) }
    }
}