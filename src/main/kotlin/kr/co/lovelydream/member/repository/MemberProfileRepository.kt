package kr.co.lovelydream.member.repository

import kr.co.lovelydream.member.entity.MemberProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberProfileRepository : JpaRepository<MemberProfile, Long>