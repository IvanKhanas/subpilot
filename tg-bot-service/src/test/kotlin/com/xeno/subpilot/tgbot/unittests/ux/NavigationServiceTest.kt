/*
 * Copyright 2026 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.properties.NavigationProperties
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate

import java.time.Duration
import java.util.stream.Stream

import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class NavigationServiceTest {

    @MockK(relaxed = true)
    private lateinit var redis: StringRedisTemplate

    @MockK(relaxed = true)
    private lateinit var listOperations: ListOperations<String, String>

    @MockK
    private lateinit var navigationProperties: NavigationProperties

    private val faker = Faker()

    private lateinit var service: NavigationService
    private var chatId: Long = 0L
    private lateinit var stackKey: String

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        stackKey = "nav:stack:$chatId"
        every { redis.opsForList() } returns listOperations
        every { navigationProperties.stackTtl } returns Duration.ofMinutes(20)
        service = NavigationService(redis, navigationProperties)
    }

    @Test
    fun `push stores screen name in Redis`() {
        every { listOperations.rightPush(any(), any<String>()) } returns 1L
        justRun { redis.expire(any(), any<Duration>()) }

        service.push(chatId, BotScreen.MAIN_MENU)

        verify { redis.expire(stackKey, Duration.ofMinutes(30)) }
    }

    @Test
    fun `pop returns screen matching Redis value`() {
        every { listOperations.rightPop(stackKey) } returns "PROVIDER_MENU"

        val result = service.pop(chatId)

        assertEquals(BotScreen.PROVIDER_MENU, result)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyPopCases")
    fun `pop returns null for invalid Redis value`(
        caseName: String,
        redisValue: String?,
    ) {
        assertTrue(caseName.isNotBlank())
        every { listOperations.rightPop(stackKey) } returns redisValue

        val result = service.pop(chatId)

        assertNull(result)
    }

    @Test
    fun `clear deletes Redis key`() {
        every { redis.delete(stackKey) } returns true

        service.clear(chatId)

        verify { redis.delete(stackKey) }
    }

    companion object {
        @JvmStatic
        fun emptyPopCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of("redis returns null", null),
                Arguments.of("redis returns unknown screen", "UNKNOWN_SCREEN"),
            )
    }
}
