package kr.co.lovelydream.auth.projection

import kr.co.lovelydream.member.enums.MemberStatus

interface MemberLoginProjection {
    val memberId: Long
    val password: String
    val status: MemberStatus
}