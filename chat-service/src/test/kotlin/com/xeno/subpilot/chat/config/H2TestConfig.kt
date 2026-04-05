package com.xeno.subpilot.chat.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

import javax.sql.DataSource

@TestConfiguration
class H2TestConfig {

    @Bean
    @Primary
    fun h2DataSource(): DataSource =
        DataSourceBuilder
            .create()
            .driverClassName("org.h2.Driver")
            .url(
                "jdbc:h2:mem:chat-service-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            ).username("sa")
            .password("")
            .build()
}
