/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val gatewayProperties: GatewayProperties,
) {

    @Bean
    fun gatewaySigningKey(): SecretKey =
        SecretKeySpec(
            gatewayProperties.auth.jwt.secret
                .toByteArray(Charsets.UTF_8),
            "HmacSHA256",
        )

    @Bean
    fun accessJwtDecoder(gatewaySigningKey: SecretKey): JwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(gatewaySigningKey).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(gatewayProperties.auth.jwt.issuer),
                AudienceValidator(gatewayProperties.auth.jwt.audience),
                TokenTypeValidator("access"),
            ),
        )
        return decoder
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        accessJwtDecoder: JwtDecoder,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health", "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/admin/**")
                    .hasAnyAuthority("SCOPE_admin.read", "SCOPE_admin.write")
                    .requestMatchers("/api/v1/admin/**")
                    .hasAuthority("SCOPE_admin.write")
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll()
            }.oauth2ResourceServer {
                it.jwt { jwt -> jwt.decoder(accessJwtDecoder) }
            }

        return http.build()
    }
}

class AudienceValidator(
    private val requiredAudience: String,
) : OAuth2TokenValidator<Jwt> {

    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (token.audience.contains(requiredAudience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error(
                    "invalid_token",
                    "Required audience '$requiredAudience' is missing",
                    null,
                ),
            )
        }
}

class TokenTypeValidator(
    private val expectedType: String,
) : OAuth2TokenValidator<Jwt> {

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val tokenType = token.getClaimAsString("token_type")
        return if (tokenType == expectedType) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error("invalid_token", "Expected token_type '$expectedType'", null),
            )
        }
    }
}
