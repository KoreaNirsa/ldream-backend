package kr.co.lovelydream.member.service.impl

import jakarta.transaction.Transactional
import kr.co.lovelydream.member.dto.ReqCreateProfileDTO
import kr.co.lovelydream.member.entity.MemberProfile
import kr.co.lovelydream.member.entity.ProfileDateMood
import kr.co.lovelydream.member.entity.ProfileDays
import kr.co.lovelydream.member.entity.ProfileFood
import kr.co.lovelydream.member.entity.ProfileInterest
import kr.co.lovelydream.member.entity.ProfileTransportation
import kr.co.lovelydream.member.repository.MemberProfileRepository
import kr.co.lovelydream.member.repository.ProfileDateMoodRepository
import kr.co.lovelydream.member.repository.ProfileDaysRepository
import kr.co.lovelydream.member.repository.ProfileFoodRepository
import kr.co.lovelydream.member.repository.ProfileInterestRepository
import kr.co.lovelydream.member.repository.ProfileTransportationRepository
import kr.co.lovelydream.member.service.MemberService
import org.springframework.stereotype.Service

@Service
class MemberServiceImpl(
    private val memberProfileRepository: MemberProfileRepository,
    private val profileInterestRepository: ProfileInterestRepository,
    private val profileFoodRepository: ProfileFoodRepository,
    private val profileDaysRepository: ProfileDaysRepository,
    private val profileTransportationRepository: ProfileTransportationRepository,
    private val profileDateMoodRepository: ProfileDateMoodRepository
) : MemberService {

    @Transactional
    override fun createProfile(reqCreateProfileDTO: ReqCreateProfileDTO) {
        // 1. 기본 프로필 저장
        val profile = MemberProfile(
            memberId = reqCreateProfileDTO.memberId,
            mbti = reqCreateProfileDTO.mbti,
            preferredRegion = reqCreateProfileDTO.preferredLocation,
            preferredTime = reqCreateProfileDTO.preferredTime,
            preferredBudget = reqCreateProfileDTO.budgetRange,
            relationshipStatus = reqCreateProfileDTO.relationshipStatus
        )
        val savedProfile = memberProfileRepository.save(profile)

        // 2. 관심사
        profileInterestRepository.saveAll(
            reqCreateProfileDTO.interests.map {
                ProfileInterest(category = it, memberProfile = savedProfile)
            }
        )

        // 3. 음식 취향
        profileFoodRepository.saveAll(
            reqCreateProfileDTO.foodPreferences.map {
                ProfileFood(foodType = it, memberProfile = savedProfile)
            }
        )

        // 4. 선호 요일
        profileDaysRepository.saveAll(
            reqCreateProfileDTO.preferredDays.map {
                ProfileDays(preferredDays = it, memberProfile = savedProfile)
            }
        )

        // 5. 교통 수단
        profileTransportationRepository.saveAll(
            reqCreateProfileDTO.transportation.map {
                ProfileTransportation(transportation = it, memberProfile = savedProfile)
            }
        )

        // 6. 데이트 분위기
        profileDateMoodRepository.saveAll(
            reqCreateProfileDTO.dateMoods.map {
                ProfileDateMood(dateMood = it, memberProfile = savedProfile)
            }
        )
    }
}