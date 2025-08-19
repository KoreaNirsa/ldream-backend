package kr.co.lovelydream.member.repository

import kr.co.lovelydream.member.entity.MemberRelation
import kr.co.lovelydream.member.enums.RelationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemberRelationRepository : JpaRepository<MemberRelation, Long> {

    /**
     * memberId가 from 또는 to로 포함된 관계 중,
     * 상태가 ACTIVE인 것의 가장 최신(생성일 내림차순) 1건을 조회
     */
    @Query("""
    SELECT r
    FROM MemberRelation r
    WHERE r.status = :status
      AND (r.fromMember.memberId = :memberId OR r.toMember.memberId = :memberId)
    ORDER BY r.createdAt DESC
""")
    fun findLatestActiveRelation(
        @Param("status") status: RelationStatus,
        @Param("memberId") memberId: Long
    ): MemberRelation?
}