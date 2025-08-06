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
import java.time.LocalDateTime

@Entity
@Table(name = "member_terms")
class MemberTerms(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val memberTermsId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id", nullable = false)
    val terms: Terms,

    @Column(nullable = false)
    val agreedAt: LocalDateTime = LocalDateTime.now()
)