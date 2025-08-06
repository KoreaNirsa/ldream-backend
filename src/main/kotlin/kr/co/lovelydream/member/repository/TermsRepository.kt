package kr.co.lovelydream.member.repository

import kr.co.lovelydream.member.entity.Terms
import kr.co.lovelydream.member.enums.TermsType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TermsRepository : JpaRepository<Terms, Long> {
    fun findTopByTypeOrderByVersionDesc(type: TermsType): Terms?
}