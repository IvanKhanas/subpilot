package com.xeno.subpilot.loyalty

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
class LoyaltyServiceApplication

fun main(args: Array<String>) {
    runApplication<LoyaltyServiceApplication>(*args)
}
