package com.xeno.subpilot.tgbot.testcontainers

import org.testcontainers.containers.GenericContainer

object TestContainersConfiguration {

    val redis: GenericContainer<*> =
        GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)
            .apply { start() }
}
