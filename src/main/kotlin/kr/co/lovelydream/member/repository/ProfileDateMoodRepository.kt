package kr.co.lovelydream.member.repository

import kr.co.lovelydream.member.entity.ProfileDateMood
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileDateMoodRepository : JpaRepository<ProfileDateMood, Long>