package kr.co.lovelydream.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.co.lovelydream.member.enums.Gender
import kr.co.lovelydream.member.enums.MemberStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "member")
class Member(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var memberId: Long? = null,

    @Column(nullable = false, length = 255)
    var email: String = "",

    @Column(nullable = false, length = 50)
    var name: String = "",

    @Column(nullable = false, length = 50)
    var nickname: String = "",

    @Column(nullable = false)
    var birthDate: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    var gender: Gender = Gender.M,

    @Column(length = 255)
    var password: String? = null,

    @Column(nullable = false)
    var mileage: Int = 0,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE,

    var deletedAt: LocalDateTime? = null
) {
    constructor() : this(
        null, "", "", "", LocalDate.now(), Gender.M,
        null, 0, LocalDateTime.now(), null, MemberStatus.ACTIVE, null
    )
}