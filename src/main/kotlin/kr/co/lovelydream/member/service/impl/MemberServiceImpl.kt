package kr.co.lovelydream.member.service.impl

import jakarta.transaction.Transactional
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.MemberException
import kr.co.lovelydream.member.dto.ReqCreateProfileDTO
import kr.co.lovelydream.member.dto.ReqSignupWrapper
import kr.co.lovelydream.member.entity.MemberProfile
import kr.co.lovelydream.member.entity.ProfileDateMood
import kr.co.lovelydream.member.entity.ProfileDays
import kr.co.lovelydream.member.entity.ProfileFood
import kr.co.lovelydream.member.entity.ProfileInterest
import kr.co.lovelydream.member.entity.ProfileTransportation
import kr.co.lovelydream.member.entity.Terms
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
import kr.co.lovelydream.member.service.MemberService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class MemberServiceImpl(
    private val memberRepository: MemberRepository,
    private val termsRepository: TermsRepository,
    private val memberTermsRepository: MemberTermsRepository,
    private val passwordEncoder: PasswordEncoder,
    private val memberProfileRepository: MemberProfileRepository,
    private val profileInterestRepository: ProfileInterestRepository,
    private val profileFoodRepository: ProfileFoodRepository,
    private val profileDaysRepository: ProfileDaysRepository,
    private val profileTransportationRepository: ProfileTransportationRepository,
    private val profileDateMoodRepository: ProfileDateMoodRepository,
) : MemberService {

    private val logger: Logger = LogManager.getLogger(MemberServiceImpl::class.java)

    @Transactional
    override fun signup(
        reqSignupWrapper: ReqSignupWrapper
    ): Long {
        val requestMember = reqSignupWrapper.member
        val requestTerms = reqSignupWrapper.terms

        logger.info("회원가입 요청: email=${requestMember.email}")

        // 중복 이메일 체크
        if (memberRepository.findByEmail(requestMember.email) != null) {
            logger.warn("중복 이메일로 회원가입 시도: ${requestMember.email}")
            throw MemberException(ResponseCode.AUTH_EMAIL_ALREADY_EXISTS)
        }

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(requestMember.password)
        val member = requestMember.toMemberEntity(encodedPassword)
        val savedMember = memberRepository.save(member)

        logger.info("회원 정보 저장 완료: memberId=${savedMember.memberId}")

        // 약관 동의 조회
        val latestTermsMap: Map<TermsType, Terms> = TermsType.entries.associateWith { type ->
            termsRepository.findTopByTypeOrderByVersionDesc(type)
                ?: throw MemberException(ResponseCode.TERMS_NOT_FOUND).also {
                    logger.error("약관 조회 실패: type=$type")
                }
        }

        // 약관 동의 엔티티 생성
        val memberTermsList = requestTerms.toMemberTermsEntity(savedMember, latestTermsMap)

        // 약관 동의 저장
        memberTermsRepository.saveAll(memberTermsList)

        logger.info("약관 동의 저장 완료: size=${memberTermsList.size}")

        return savedMember.memberId!!
    }

    @Transactional
    override fun createProfile(reqCreateProfileDTO: ReqCreateProfileDTO) {
        val memberId = reqCreateProfileDTO.memberId
        logger.info("프로필 생성 요청 시작: memberId={}", memberId)

        // 회원 존재 여부 확인
        if (!memberRepository.existsById(memberId)) {
            logger.warn("존재하지 않는 회원에 대한 프로필 생성 시도: memberId={}", memberId)
            throw MemberException(ResponseCode.MEMBER_NOT_FOUND)
        }

        // 기본 프로필 저장
        val profile = MemberProfile(
            memberId = memberId,
            mbti = reqCreateProfileDTO.mbti,
            preferredRegion = reqCreateProfileDTO.preferredLocation,
            preferredTime = reqCreateProfileDTO.preferredTime,
            preferredBudget = reqCreateProfileDTO.budgetRange,
            relationshipStatus = reqCreateProfileDTO.relationshipStatus
        )
        val savedProfile = memberProfileRepository.save(profile)
        logger.info("기본 프로필 저장 완료: memberProfileId={}", savedProfile.memberProfileId)

        // 관심사 저장
        val interests = reqCreateProfileDTO.interests.map {
            ProfileInterest(category = it, memberProfile = savedProfile)
        }
        profileInterestRepository.saveAll(interests)
        logger.debug("관심사 저장 완료: count={}", interests.size)

        // 음식 취향 저장
        val foods = reqCreateProfileDTO.foodPreferences.map {
            ProfileFood(foodType = it, memberProfile = savedProfile)
        }
        profileFoodRepository.saveAll(foods)
        logger.debug("음식 취향 저장 완료: count={}", foods.size)

        // 선호 요일 저장
        val days = reqCreateProfileDTO.preferredDays.map {
            ProfileDays(preferredDays = it, memberProfile = savedProfile)
        }
        profileDaysRepository.saveAll(days)
        logger.debug("선호 요일 저장 완료: count={}", days.size)

        // 교통 수단 저장
        val transportations = reqCreateProfileDTO.transportation.map {
            ProfileTransportation(transportation = it, memberProfile = savedProfile)
        }
        profileTransportationRepository.saveAll(transportations)
        logger.debug("교통 수단 저장 완료: count={}", transportations.size)

        // 데이트 분위기 저장
        val moods = reqCreateProfileDTO.dateMoods.map {
            ProfileDateMood(dateMood = it, memberProfile = savedProfile)
        }
        profileDateMoodRepository.saveAll(moods)
        logger.debug("데이트 분위기 저장 완료: count={}", moods.size)

        logger.info("프로필 생성 완료: memberProfileId={}", savedProfile.memberProfileId)
    }
}