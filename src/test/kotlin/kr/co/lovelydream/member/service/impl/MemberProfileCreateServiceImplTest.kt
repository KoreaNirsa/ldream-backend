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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
@DisplayName("프로필 생성 - MemberServiceImpl")
class MemberProfileCreateServiceImplTest {

    // 기존 회원가입 테스트와 동일한 필드 구성을 유지(의존성 주입 호환용)
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

    @InjectMockKs
    lateinit var memberService: MemberServiceImpl

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    private fun sampleReq(memberId: Long = 100L) = ReqCreateProfileDTO(
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
    @DisplayName("성공: 회원 존재 → 프로필 및 하위 항목 저장")
    fun createProfile_success_savesProfileAndAllChildren() {
        // given
        val req = sampleReq()
        every { memberRepository.existsById(req.memberId) } returns true

        val savedProfile = MemberProfile(
            memberProfileId = 10L,
            memberId = req.memberId,
            mbti = req.mbti,
            preferredRegion = req.preferredLocation,
            preferredTime = req.preferredTime,
            preferredBudget = req.budgetRange,
            relationshipStatus = req.relationshipStatus
        )
        every { memberProfileRepository.save(any<MemberProfile>()) } returns savedProfile

        // 각 saveAll 인자 캡쳐
        val interestsSlot = slot<List<ProfileInterest>>()
        val foodsSlot = slot<List<ProfileFood>>()
        val daysSlot = slot<List<ProfileDays>>()
        val transSlot = slot<List<ProfileTransportation>>()
        val moodsSlot = slot<List<ProfileDateMood>>()

        every { profileInterestRepository.saveAll(capture(interestsSlot)) } answers { firstArg() }
        every { profileFoodRepository.saveAll(capture(foodsSlot)) } answers { firstArg() }
        every { profileDaysRepository.saveAll(capture(daysSlot)) } answers { firstArg() }
        every { profileTransportationRepository.saveAll(capture(transSlot)) } answers { firstArg() }
        every { profileDateMoodRepository.saveAll(capture(moodsSlot)) } answers { firstArg() }

        // when
        memberService.createProfile(req)

        // then: 존재 확인 + 프로필 저장 1회
        verify(exactly = 1) { memberRepository.existsById(req.memberId) }
        verify(exactly = 1) { memberProfileRepository.save(any()) }

        // 하위 saveAll 1회씩
        verify(exactly = 1) { profileInterestRepository.saveAll(any<List<ProfileInterest>>()) }
        verify(exactly = 1) { profileFoodRepository.saveAll(any<List<ProfileFood>>()) }
        verify(exactly = 1) { profileDaysRepository.saveAll(any<List<ProfileDays>>()) }
        verify(exactly = 1) { profileTransportationRepository.saveAll(any<List<ProfileTransportation>>()) }
        verify(exactly = 1) { profileDateMoodRepository.saveAll(any<List<ProfileDateMood>>()) }

        // 개수 검증
        assertEquals(req.interests.size, interestsSlot.captured.size)
        assertEquals(req.foodPreferences.size, foodsSlot.captured.size)
        assertEquals(req.preferredDays.size, daysSlot.captured.size)
        assertEquals(req.transportation.size, transSlot.captured.size)
        assertEquals(req.dateMoods.size, moodsSlot.captured.size)

        // 매핑 검증(샘플로 첫 원소만)
        assertEquals(savedProfile.memberProfileId, interestsSlot.captured.first().memberProfile.memberProfileId)
        assertEquals(savedProfile.memberProfileId, foodsSlot.captured.first().memberProfile.memberProfileId)
        assertEquals(savedProfile.memberProfileId, daysSlot.captured.first().memberProfile.memberProfileId)
        assertEquals(savedProfile.memberProfileId, transSlot.captured.first().memberProfile.memberProfileId)
        assertEquals(savedProfile.memberProfileId, moodsSlot.captured.first().memberProfile.memberProfileId)
    }

    @Test
    @DisplayName("실패: 회원 미존재 → MEMBER_NOT_FOUND, 저장 호출 없음")
    fun createProfile_whenMemberNotFound_throwsMemberException_andNoSaves() {
        // given
        val req = sampleReq(memberId = 9999L)
        every { memberRepository.existsById(req.memberId) } returns false

        // when
        val ex = assertThrows<MemberException> { memberService.createProfile(req) }

        // then
        assertEquals(ResponseCode.MEMBER_NOT_FOUND, ex.code)
        verify(exactly = 1) { memberRepository.existsById(req.memberId) }

        // 어떤 저장도 일어나지 않아야 함
        verify(exactly = 0) { memberProfileRepository.save(any()) }
        verify(exactly = 0) { profileInterestRepository.saveAll(any<List<ProfileInterest>>()) }
        verify(exactly = 0) { profileFoodRepository.saveAll(any<List<ProfileFood>>()) }
        verify(exactly = 0) { profileDaysRepository.saveAll(any<List<ProfileDays>>()) }
        verify(exactly = 0) { profileTransportationRepository.saveAll(any<List<ProfileTransportation>>()) }
        verify(exactly = 0) { profileDateMoodRepository.saveAll(any<List<ProfileDateMood>>()) }
    }

    @Test
    fun createProfile_withEmptyLists_callsSaveAllWithEmptyCollections() {
        // given: 리스트 항목이 비어있는 요청
        val req = ReqCreateProfileDTO(
            memberId = 101L,
            mbti = "ENTJ",
            preferredLocation = "부산",
            preferredTime = "평일 밤",
            budgetRange = "5만~7만원",
            relationshipStatus = "솔로",
            preferredDays = emptyList(),
            dateMoods = emptyList(),
            interests = emptyList(),
            foodPreferences = emptyList(),
            transportation = emptyList()
        )
        every { memberRepository.existsById(req.memberId) } returns true

        val savedProfile = MemberProfile(
            memberProfileId = 11L,
            memberId = req.memberId,
            mbti = req.mbti,
            preferredRegion = req.preferredLocation,
            preferredTime = req.preferredTime,
            preferredBudget = req.budgetRange,
            relationshipStatus = req.relationshipStatus
        )
        every { memberProfileRepository.save(any<MemberProfile>()) } returns savedProfile

        val interestsSlot = slot<List<ProfileInterest>>()
        val foodsSlot = slot<List<ProfileFood>>()
        val daysSlot = slot<List<ProfileDays>>()
        val transSlot = slot<List<ProfileTransportation>>()
        val moodsSlot = slot<List<ProfileDateMood>>()

        every { profileInterestRepository.saveAll(capture(interestsSlot)) } answers { firstArg() }
        every { profileFoodRepository.saveAll(capture(foodsSlot)) } answers { firstArg() }
        every { profileDaysRepository.saveAll(capture(daysSlot)) } answers { firstArg() }
        every { profileTransportationRepository.saveAll(capture(transSlot)) } answers { firstArg() }
        every { profileDateMoodRepository.saveAll(capture(moodsSlot)) } answers { firstArg() }

        // when
        memberService.createProfile(req)

        // then: saveAll은 호출되되, 비어있는 리스트가 전달됨
        assertEquals(0, interestsSlot.captured.size)
        assertEquals(0, foodsSlot.captured.size)
        assertEquals(0, daysSlot.captured.size)
        assertEquals(0, transSlot.captured.size)
        assertEquals(0, moodsSlot.captured.size)
    }
}