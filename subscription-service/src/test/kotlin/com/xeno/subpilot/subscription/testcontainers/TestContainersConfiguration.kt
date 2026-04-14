package com.xeno.subpilot.subscription.testcontainers

import org.testcontainers.containers.PostgreSQLContainer

object TestContainersConfiguration {

    val postgres: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }
}
