package kr.co.lovelydream.member.repository

import kr.co.lovelydream.auth.projection.MemberLoginProjection
import kr.co.lovelydream.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    // 이메일 중복 검사
    fun findByEmail(email: String): MemberLoginProjection?

    @Query("SELECT m.memberId FROM Member m WHERE m.email = :email")
    fun findIdByEmail(@Param("email") email: String): Long?
}