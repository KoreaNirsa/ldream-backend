package kr.co.lovelydream.auth.service.impl

import kr.co.lovelydream.auth.constants.JwtConstants.ACCESS_BLACKLIST_PREFIX
import kr.co.lovelydream.auth.constants.JwtConstants.REFRESH_PREFIX
import kr.co.lovelydream.auth.service.TokenStoreService
import kr.co.lovelydream.global.util.LoggingUtil
import kr.co.lovelydream.global.util.LoggingUtil.maskJti
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class TokenStoreServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>
) : TokenStoreService {
    private val logger: Logger = LogManager.getLogger(TokenStoreServiceImpl::class.java)

    /**
     * 리프레시 토큰(JTI/해시)을 Redis에 저장한다.
     * - Key: refresh:{userId}:{deviceId}
     * - TTL: refresh 만료까지 남은 시간
     */
    override fun saveRefreshToken(userId: String, deviceId: String, refreshJtiOrHash: String, expiredAt: Date) {
        val millisLeft = expiredAt.time - System.currentTimeMillis()
        if (millisLeft <= 0) {
            logger.warn(
                "리프레시 토큰 저장 건너뜀 - 만료 시간이 이미 지남 | userId={}, deviceId={}, ttlMs={}",
                userId, LoggingUtil.maskDevice(deviceId), millisLeft
            )
            return
        }

        val ttl = Duration.ofMillis(millisLeft)
        val key = keyRefresh(userId, deviceId)

        try {
            redisTemplate.opsForValue().set(key, refreshJtiOrHash, ttl)
            logger.info(
                "리프레시 토큰 저장 완료 | key={}, jti={}, ttlSec={}",
                key, maskJti(refreshJtiOrHash), ttl.seconds
            )
        } catch (ex: RedisConnectionFailureException) {
            logger.error("리프레시 토큰 저장 실패(연결 실패) | key={}, 원인={}", key, ex.message)
            throw ex
        } catch (ex: DataAccessException) {
            logger.error("리프레시 토큰 저장 실패(데이터 접근) | key={}, 원인={}", key, ex.message)
            throw ex
        }
    }

    /**
     * 저장된 리프레시 토큰(JTI/해시)을 조회한다.
     * - Key: refresh:{userId}:{deviceId}
     * @return 저장된 값 또는 null
     */
    override fun getRefreshToken(userId: String, deviceId: String): String? {
        val key = keyRefresh(userId, deviceId)
        return try {
            val value = redisTemplate.opsForValue().get(key)
            logger.debug("리프레시 토큰 조회 | key={}, hit={}", key, value != null)
            value
        } catch (ex: RedisConnectionFailureException) {
            logger.error("리프레시 토큰 조회 실패(연결 실패) | key={}, 원인={}", key, ex.message)
            throw ex
        } catch (ex: DataAccessException) {
            logger.error("리프레시 토큰 조회 실패(데이터 접근) | key={}, 원인={}", key, ex.message)
            throw ex
        }
    }

    /**
     * 저장된 리프레시 토큰 레코드를 삭제한다.
     * - Key: refresh:{userId}:{deviceId}
     */
    override fun deleteRefreshToken(userId: String, deviceId: String) {
        val key = keyRefresh(userId, deviceId)
        try {
            val deleted = redisTemplate.delete(key)
            logger.info("리프레시 토큰 삭제 | key={}, deleted={}", key, deleted)
        } catch (ex: RedisConnectionFailureException) {
            logger.error("리프레시 토큰 삭제 실패(연결 실패) | key={}, 원인={}", key, ex.message)
            throw ex
        } catch (ex: DataAccessException) {
            logger.error("리프레시 토큰 삭제 실패(데이터 접근) | key={}, 원인={}", key, ex.message)
            throw ex
        }
    }

    /**
     * 액세스 토큰의 JTI를 블랙리스트에 등록한다.
     * - Key: access:blacklist:{accessJti}
     * - TTL: 액세스 토큰 남은 수명(초)
     */
    override fun blacklistAccessJti(accessJti: String, ttlSeconds: Long) {
        if (ttlSeconds <= 0) {
            logger.warn("Access 블랙리스트 건너뜀 - TTL <= 0 | jti={}, ttlSec={}", maskJti(accessJti), ttlSeconds)
            return
        }
        val key = "${ACCESS_BLACKLIST_PREFIX}${accessJti}"
        try {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds))
            logger.info("Access 블랙리스트 등록 | key={}, ttlSec={}", key, ttlSeconds)
        } catch (ex: RedisConnectionFailureException) {
            logger.error("Access 블랙리스트 등록 실패(연결 실패) | key={}, 원인={}", key, ex.message)
            throw ex
        } catch (ex: DataAccessException) {
            logger.error("Access 블랙리스트 등록 실패(데이터 접근) | key={}, 원인={}", key, ex.message)
            throw ex
        }
    }

    /**
     * 액세스 토큰의 JTI가 블랙리스트에 존재하는지 확인한다.
     * - Key: access:blacklist:{accessJti}
     */
    override fun isAccessBlacklisted(accessJti: String): Boolean {
        val key = "${ACCESS_BLACKLIST_PREFIX}${accessJti}"
        return try {
            val exists = redisTemplate.hasKey(key) == true
            logger.debug("Access 블랙리스트 조회 | key={}, exists={}", key, exists)
            exists
        } catch (ex: RedisConnectionFailureException) {
            logger.error("Access 블랙리스트 조회 실패(연결 실패) | key={}, 원인={}", key, ex.message)
            throw ex
        } catch (ex: DataAccessException) {
            logger.error("Access 블랙리스트 조회 실패(데이터 접근) | key={}, 원인={}", key, ex.message)
            throw ex
        }
    }

    /**
     * 리프레시 토큰의 JTI를 '재사용됨' 상태로 표시한다(토큰 회전 보호).
     * - Key: refresh:used:{refreshJti}
     * - TTL: 리프레시 남은 수명(초)
     */
    override fun markRefreshUsed(refreshJti: String, ttlSeconds: Long) {
        if (ttlSeconds <= 0) {
            logger.warn("Refresh 재사용 마킹 건너뜀 - TTL <= 0 | jti={}, ttlSec={}", maskJti(refreshJti), ttlSeconds)
            return
        }
        val key = "refresh:used:${refreshJti}"
        try {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds))
            logger.info("Refresh 재사용 마킹 완료 | key={}, ttlSec={}", key, ttlSeconds)
        } catch (ex: RedisConnectionFailureException) {
            logger.error("Refresh 재사용 마킹 실패(연결 실패) | key={}, 원인={}", key, ex.message)
            throw ex
        } catch (ex: DataAccessException) {
            logger.error("Refresh 재사용 마킹 실패(데이터 접근) | key={}, 원인={}", key, ex.message)
            throw ex
        }
    }

    /**
     * 리프레시 토큰의 JTI가 '재사용됨' 상태인지 확인한다.
     * - Key: refresh:used:{refreshJti}
     */
    override fun isRefreshUsed(refreshJti: String): Boolean {
        val key = "refresh:used:${refreshJti}"
        return try {
            val exists = redisTemplate.hasKey(key) == true
            logger.debug("Refresh 재사용 여부 조회 | key={}, exists={}", key, exists)
            exists
        } catch (ex: RedisConnectionFailureException) {
            logger.error("Refresh 재사용 여부 조회 실패(연결 실패) | key={}, 원인={}", key, ex.message)
            throw ex
        } catch (ex: DataAccessException) {
            logger.error("Refresh 재사용 여부 조회 실패(데이터 접근) | key={}, 원인={}", key, ex.message)
            throw ex
        }
    }

    // -------- private fun --------

    private fun keyRefresh(userId: String, deviceId: String) =
        "${REFRESH_PREFIX}${userId}:${deviceId}"
}