package kr.co.lovelydream.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "profile_date_mood")
data class ProfileDateMood(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_date_mood_id", nullable = false)
    val profileDateMoodId: Long = 0,

    @Column(name = "date_mood", nullable = false)
    val dateMood: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_profile_id", nullable = false)
    val memberProfile : MemberProfile
)