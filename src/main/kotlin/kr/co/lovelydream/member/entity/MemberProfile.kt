package kr.co.lovelydream.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "member_profile")
data class MemberProfile(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val memberProfileId: Long = 0,

    val memberId: Long,
    val mbti: String,
    val preferredRegion: String,
    val preferredTime: String,
    val preferredBudget: String,
    val relationshipStatus: String,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)