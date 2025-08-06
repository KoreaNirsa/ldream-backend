package kr.co.lovelydream.member.repository

import kr.co.lovelydream.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    // 이메일 중복 검사
    fun findByEmail(email: String): Member?

}