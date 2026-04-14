package com.xeno.subpilot.tgbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.util.concurrent.Executors

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

@Configuration
class CoroutineConfig {

    @Bean(destroyMethod = "close")
    fun ioDispatcher(): ExecutorCoroutineDispatcher =
        Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
}
