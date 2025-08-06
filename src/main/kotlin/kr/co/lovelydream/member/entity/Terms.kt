package kr.co.lovelydream.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import kr.co.lovelydream.member.enums.TermsType
import java.time.LocalDateTime

@Entity
@Table(name = "terms")
class Terms(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val termsId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: TermsType,

    @Column(nullable = false, length = 10)
    val version: String,

    @Lob
    @Column(nullable = false)
    val content: String,

    @Column(nullable = false)
    val isRequired: Boolean,

    val createdAt: LocalDateTime? = null
) {
    protected constructor() : this(
        termsId = null,
        type = TermsType.SERVICE,
        version = "",
        content = "",
        isRequired = false,
        createdAt = null
    )
}