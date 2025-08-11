package kr.co.lovelydream.member.service.impl

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kr.co.lovelydream.auth.dto.ReqEmailDTO
import kr.co.lovelydream.auth.dto.ReqEmailVerifyDTO
import kr.co.lovelydream.auth.service.JwtService
import kr.co.lovelydream.auth.service.TokenStoreService
import kr.co.lovelydream.auth.service.impl.AuthServiceImpl
import kr.co.lovelydream.global.enums.ResponseCode
import kr.co.lovelydream.global.exception.AuthException
import kr.co.lovelydream.member.repository.MemberRepository
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

    private val sampleEmail = "user@example.com"

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { redisTemplate.opsForValue() } returns valueOps
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
        val message = msgSlot.captured
        val savedCode = codeSlot.captured

        assertTrue(message.to?.contains(sampleEmail) == true)
        assertTrue(message.text?.contains(savedCode) == true)
        verify { redisTemplate.opsForValue().set("emailCode:$sampleEmail", savedCode, 5, TimeUnit.MINUTES) }
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
}
