package kr.co.lovelydream.member.repository

import kr.co.lovelydream.member.entity.ProfileDays
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileDaysRepository : JpaRepository<ProfileDays, Long>