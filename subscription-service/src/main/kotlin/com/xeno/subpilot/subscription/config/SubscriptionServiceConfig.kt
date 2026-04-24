package com.xeno.subpilot.subscription.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.time.Clock

@Configuration
class SubscriptionServiceConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
