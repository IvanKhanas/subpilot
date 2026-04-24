package com.xeno.subpilot.loyalty.testcontainers

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName

object TestContainersConfiguration {

    val postgres: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }

    val kafka: ConfluentKafkaContainer =
        ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .apply { start() }
}
