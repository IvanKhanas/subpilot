package com.xeno.subpilot.loyalty.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.time.Clock

@Configuration
class LoyaltyServiceConfig {

    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
