package com.xeno.subpilot.subscription

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
class SubscriptionServiceApplication

fun main(args: Array<String>) {
    runApplication<SubscriptionServiceApplication>(*args)
}
