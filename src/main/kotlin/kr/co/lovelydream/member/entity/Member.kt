package kr.co.lovelydream.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "member")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val memberId: Long? = null,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false, length = 50)
    val nickname: String,

    @Column(nullable = false)
    val birthDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    val gender: Gender,

    @Column(length = 255)
    var password: String? = null,

    @Column(nullable = false)
    var mileage: Int = 0,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE,

    var deletedAt: LocalDateTime? = null
)

enum class Gender { M, F }
enum class MemberStatus { ACTIVE, SUSPENDED, WITHDRAWN }