package kr.co.lovelydream.member.service.impl

import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kr.co.lovelydream.auth.dto.ReqEmailDTO
import kr.co.lovelydream.auth.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.auth.dto.ReqLoginDTO
import kr.co.lovelydream.auth.dto.TokenDTO
import kr.co.lovelydream.auth.service.JwtService
import kr.co.lovelydream.auth.service.TokenStoreService
import kr.co.lovelydream.auth.service.impl.AuthServiceImpl
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.AuthException
import kr.co.lovelydream.member.entity.Member
import kr.co.lovelydream.member.enums.Gender
import kr.co.lovelydream.member.enums.MemberStatus
import kr.co.lovelydream.member.repository.MemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
@DisplayName("이메일 인증 - AuthServiceImpl")
class AuthServiceImplTest {
    @MockK lateinit var memberRepository: MemberRepository
    @MockK lateinit var mailSender: JavaMailSender
    @MockK lateinit var redisTemplate: StringRedisTemplate
    @InjectMockKs lateinit var authService: AuthServiceImpl
    @MockK lateinit var valueOps: ValueOperations<String, String>
    @MockK(relaxed = true) lateinit var passwordEncoder: PasswordEncoder
    @MockK(relaxed = true) lateinit var jwtService: JwtService
    @MockK(relaxed = true) lateinit var tokenStoreService: TokenStoreService

    private val deviceId = "test-device-123"
    private val sampleEmail = "user@example.com"

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { redisTemplate.opsForValue() } returns valueOps
    }

    private fun dummyMember(
        id: Long = 1L,
        email: String = "user@example.com",
        encodedPw: String = "\$2a\$10\$encoded",
        status: MemberStatus = MemberStatus.ACTIVE
    ): Member {
        return Member(
            memberId = id,
            email = email,
            name = "테스트사용자",
            nickname = "tester",
            birthDate = LocalDate.of(1990, 1, 1),
            gender = Gender.M,
            password = encodedPw,
            mileage = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = null,
            status = status,
            deletedAt = null
        )
    }

    // 이메일 인증 코드 전송 시 메일 발송과 Redis TTL 저장을 검증하는 테스트
    @Test
    @DisplayName("성공: 이메일 코드 전송 → 메일 발송 & Redis TTL 저장")
    fun sendEmailCode_sendsEmailAndStoresTTL() {
        // Given: Redis 저장과 메일 전송 stub
        val dto = ReqEmailDTO(sampleEmail)
        val codeSlot = slot<String>()
        every { valueOps.set(any(), capture(codeSlot), 5, TimeUnit.MINUTES) } just Runs

        val msgSlot = slot<SimpleMailMessage>()
        every { mailSender.send(capture(msgSlot)) } just Runs
        every { memberRepository.findByEmail(sampleEmail) } returns null

        // When: 이메일 코드 전송 호출
        authService.sendEmailCode(dto)

        // Then: 수신자 및 본문, TTL 저장 검증
        val savedCode = codeSlot.captured

        var called = false
        runCatching {
            verify {
                valueOps.set("emailCode:$sampleEmail", savedCode, 5, TimeUnit.MINUTES)
            }
        }.onSuccess { called = true }
        runCatching {
            verify {
                valueOps.set("emailCode:$sampleEmail", savedCode, Duration.ofMinutes(5))
            }
        }.onSuccess { called = true }
        assertTrue(called, "Redis set with TTL(5m) was not called")

        verify { mailSender.send(any<SimpleMailMessage>()) }
    }

    // 인증 코드가 없을 때 만료 예외(AuthException)가 발생하는지 검증하는 테스트
    @Test
    @DisplayName("실패: 코드 없음 → AUTH_CODE_EXPIRED")
    fun verifyEmailCode_whenNoCode_throwsExpiredException() {
        // Given: Redis에 키 없음
        val dto = ReqEmailVerifyDTO(sampleEmail, "123456")
        every { valueOps.get("emailCode:$sampleEmail") } returns null

        // When & Then: 만료 예외 검증
        val ex = assertThrows<AuthException> { authService.verifyEmailCode(dto) }
        assertEquals(ResponseCode.AUTH_CODE_EXPIRED, ex.code)
    }

    // 인증 코드 불일치 시 불일치 예외(AuthException)가 발생하는지 검증하는 테스트
    @Test
    @DisplayName("실패: 코드 불일치 → AUTH_CODE_MISMATCH")
    fun verifyEmailCode_whenMismatch_throwsMismatchException() {
        // Given: Redis에 다른 코드 저장
        val dto = ReqEmailVerifyDTO(sampleEmail, "123456")
        every { redisTemplate.opsForValue().get("emailCode:$sampleEmail") } returns "654321"

        // When & Then: 불일치 예외 검증
        val ex = assertThrows<AuthException> { authService.verifyEmailCode(dto) }
        assertEquals(ResponseCode.AUTH_CODE_MISMATCH, ex.code)
    }

    // 인증 코드 일치 시 Redis 키가 삭제되는지 검증하는 테스트
    @Test
    @DisplayName("성공: 코드 일치 → Redis 키 삭제")
    fun verifyEmailCode_whenMatch_deletesKey() {
        // Given: Redis에 일치 코드 저장
        val dto = ReqEmailVerifyDTO(sampleEmail, "123456")
        every { redisTemplate.opsForValue().get("emailCode:$sampleEmail") } returns "123456"
        every { redisTemplate.delete("emailCode:$sampleEmail") } returns true

        // When: 코드 검증 호출
        authService.verifyEmailCode(dto)

        // Then: 키 삭제 호출 검증
        verify { redisTemplate.delete("emailCode:$sampleEmail") }
    }

    @Test
    @DisplayName("로그인 성공 - 올바른 자격증명 시 토큰 생성 및 저장")
    fun loginSuccess_GeneratesTokens_And_PersistsRefresh() {
        // given
        val email = "user@example.com"
        val rawPassword = "P@ssw0rd!"
        val member = dummyMember(id = 99L, email = email, encodedPw = "ENCODED")

        every { memberRepository.findByEmail(email) } returns member
        every { passwordEncoder.matches(rawPassword, member.password!!) } returns true

        // JwtService stubs (파라미터 타입/값 차이를 흡수)
        val access = "access.jwt.token"
        val refresh = "refresh.jwt.token"
        val refreshJti = "refresh-jti-1234"
        val accessJti = "access-jti-5678"
        val refreshExp = Date(System.currentTimeMillis() + 7 * 24 * 3600_000L)

        every { jwtService.generateAccessToken(any()) } returns access
        every { jwtService.generateRefreshToken(any()) } returns refresh
        every { jwtService.getJti(refresh) } returns refreshJti
        every { jwtService.getJti(access) } returns accessJti
        every { jwtService.getExpiration(refresh) } returns refreshExp

        every { tokenStoreService.saveRefreshToken(any(), deviceId, refreshJti, refreshExp) } just Runs

        // when
        val result: TokenDTO = authService.login(
            ReqLoginDTO(email = email, password = rawPassword),
            deviceId
        )

        // then
        assertThat(result.accessToken).isEqualTo(access)
        assertThat(result.refreshToken).isEqualTo(refresh)

        verifyOrder {
            memberRepository.findByEmail(email)
            passwordEncoder.matches(rawPassword, member.password!!)
            jwtService.generateAccessToken(email)
            jwtService.generateRefreshToken(email)
            jwtService.getJti(refresh)
            jwtService.getExpiration(refresh)
            tokenStoreService.saveRefreshToken(member.memberId!!.toString(), deviceId, refreshJti, refreshExp)
        }
        confirmVerified(memberRepository, passwordEncoder, jwtService, tokenStoreService)
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 MEMBER_NOT_FOUND 예외")
    fun loginFail_MemberNotFound_ThrowsException() {
        val email = "none@example.com"
        val rawPassword = "pw"
        every { memberRepository.findByEmail(email) } returns null

        assertThatThrownBy {
            authService.login(
                ReqLoginDTO(email = email, password = rawPassword),
                deviceId
            )
        }
            .isInstanceOf(AuthException::class.java)
            .extracting("code")
            .isEqualTo(ResponseCode.MEMBER_NOT_FOUND)

        verify(exactly = 1) { memberRepository.findByEmail(email) }
        verify { passwordEncoder wasNot Called }
        verify { jwtService wasNot Called }
        verify { tokenStoreService wasNot Called }
    }

    @Test
    @DisplayName("비밀번호 불일치면 AUTH_INVALID_CREDENTIAL 예외")
    fun loginFail_InvalidPassword_ThrowsException() {
        val email = "user@example.com"
        val rawPassword = "wrong"
        val member = dummyMember(email = email, encodedPw = "ENCODED")
        every { memberRepository.findByEmail(email) } returns member
        every { passwordEncoder.matches(rawPassword, member.password!!) } returns false

        assertThatThrownBy {
            authService.login(
                ReqLoginDTO(email = email, password = rawPassword),
                deviceId
            )
        }
            .isInstanceOf(AuthException::class.java)
            .extracting("code")
            .isEqualTo(ResponseCode.AUTH_INVALID_CREDENTIAL)

        verifyOrder {
            memberRepository.findByEmail(email)
            passwordEncoder.matches(rawPassword, member.password!!)
        }
        verify { jwtService wasNot Called }
        verify { tokenStoreService wasNot Called }
    }

    @Test
    @DisplayName("비활성/탈퇴 계정이면 AUTH_UNAUTHORIZED 예외")
    fun loginFail_InactiveOrDeletedMember_ThrowsException() {
        val email = "blocked@example.com"
        val member = dummyMember(email = email, status = MemberStatus.DELETED)
        every { memberRepository.findByEmail(email) } returns member
        every { passwordEncoder.matches(any(), any()) } returns true

        assertThatThrownBy {
            authService.login(ReqLoginDTO(email, "any"), deviceId)
        }
            .isInstanceOf(AuthException::class.java)
            .extracting("code")
            .isEqualTo(ResponseCode.AUTH_UNAUTHORIZED)

        verify { jwtService wasNot Called }
        verify { tokenStoreService wasNot Called }
    }
}
