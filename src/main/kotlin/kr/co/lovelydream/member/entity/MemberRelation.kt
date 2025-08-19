package kr.co.lovelydream.member.entity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import kr.co.lovelydream.member.enums.RelationStatus
import kr.co.lovelydream.member.enums.RelationType
import java.time.LocalDateTime

@Entity
@Table(name = "member_relation")
class MemberRelation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_id")
    val relationId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_member_id", nullable = false)
    val fromMember: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_member_id", nullable = false)
    val toMember: Member,

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    val relationType: RelationType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: RelationStatus = RelationStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
) {
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}