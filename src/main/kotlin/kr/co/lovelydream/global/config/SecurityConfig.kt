package kr.co.lovelydream.global.config

import kr.co.lovelydream.auth.filter.JwtAuthenticationFilter
import kr.co.lovelydream.auth.service.JwtService
import kr.co.lovelydream.auth.service.TokenStoreService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig (
    private val jwtService: JwtService,
    private val tokenStoreService: TokenStoreService
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/auth/**",
                        "/api/member/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                    ).permitAll()
                    // 유료 회원 추가 적용 예정
                    // .requestMatchers("/api/premium/**").hasAnyRole("PRO", "PREMIUM")
                    .requestMatchers("/api/auth/logout").authenticated()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtService, tokenStoreService),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:5173")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}