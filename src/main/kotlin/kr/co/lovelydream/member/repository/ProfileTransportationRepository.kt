package kr.co.lovelydream.member.repository

import kr.co.lovelydream.member.entity.ProfileTransportation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileTransportationRepository : JpaRepository<ProfileTransportation, Long>