package com.xeno.subpilot.payment.testcontainers

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object TestContainersConfiguration {

    val postgres: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }

    val kafka: KafkaContainer =
        KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .apply { start() }
}
