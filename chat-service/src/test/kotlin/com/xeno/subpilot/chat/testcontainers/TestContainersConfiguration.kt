package com.xeno.subpilot.chat.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

object TestContainersConfiguration {

    val redis: GenericContainer<*> =
        GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)
            .apply { start() }

    val postgres: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }
}
