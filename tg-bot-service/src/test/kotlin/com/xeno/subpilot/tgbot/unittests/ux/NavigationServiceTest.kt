package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.properties.NavigationProperties
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate

import java.time.Duration

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class NavigationServiceTest {

    @MockK(relaxed = true)
    private lateinit var redis: StringRedisTemplate

    @MockK(relaxed = true)
    private lateinit var listOperations: ListOperations<String, String>

    @MockK
    private lateinit var navigationProperties: NavigationProperties

    private lateinit var service: NavigationService

    @BeforeEach
    fun setUp() {
        every { redis.opsForList() } returns listOperations
        every { navigationProperties.stackTtl } returns Duration.ofMinutes(20)
        service = NavigationService(redis, navigationProperties)
    }

    fun `push stores screen name in Redis`() {
        every { listOperations.rightPush(any(), any<String>()) } returns 1L
        justRun { redis.expire(any(), any<Duration>()) }

        service.push(42L, BotScreen.MAIN_MENU)

        verify { redis.expire("nav:stack:42", Duration.ofMinutes(30)) }
    }

    @Test
    fun `pop returns screen matching Redis value`() {
        every { listOperations.rightPop("nav:stack:42") } returns "PROVIDER_MENU"

        val result = service.pop(42L)

        assertEquals(BotScreen.PROVIDER_MENU, result)
    }

    @Test
    fun `pop returns null when Redis returns null`() {
        every { listOperations.rightPop("nav:stack:42") } returns null

        val result = service.pop(42L)

        assertNull(result)
    }

    @Test
    fun `pop returns null for unknown screen name`() {
        every { listOperations.rightPop("nav:stack:42") } returns "UNKNOWN_SCREEN"

        val result = service.pop(42L)

        assertNull(result)
    }

    @Test
    fun `clear deletes Redis key`() {
        every { redis.delete("nav:stack:42") } returns true

        service.clear(42L)

        verify { redis.delete("nav:stack:42") }
    }
}
