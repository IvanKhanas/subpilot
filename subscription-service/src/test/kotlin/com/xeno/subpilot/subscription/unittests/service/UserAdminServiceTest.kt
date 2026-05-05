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
package com.xeno.subpilot.subscription.unittests.service

import com.xeno.subpilot.subscription.entity.SubscriptionUser
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.service.UserAdminService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class UserAdminServiceTest {

    @MockK
    lateinit var subscriptionUserRepository: SubscriptionUserRepository

    private lateinit var service: UserAdminService

    companion object {
        const val USER_ID = 42L
        const val UNKNOWN_USER_ID = 404L
    }

    @BeforeEach
    fun setUp() {
        service = UserAdminService(subscriptionUserRepository)
    }

    @Test
    fun `getUserInfo delegates to repository`() {
        val user = SubscriptionUser(userId = USER_ID)
        every { subscriptionUserRepository.findById(USER_ID) } returns user

        val result = service.getUserInfo(USER_ID)

        assertEquals(user, result)
    }

    @ParameterizedTest(name = "blocked={0}")
    @CsvSource(
        "true",
        "false",
    )
    fun `setBlocked delegates to repository when user exists`(blocked: Boolean) {
        every { subscriptionUserRepository.findById(USER_ID) } returns SubscriptionUser(USER_ID)
        justRun { subscriptionUserRepository.setBlocked(USER_ID, blocked) }

        if (blocked) {
            service.blockUser(USER_ID)
        } else {
            service.unblockUser(USER_ID)
        }

        verify(exactly = 1) { subscriptionUserRepository.setBlocked(USER_ID, blocked) }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "block user, true",
        "unblock user, false",
    )
    fun `setBlocked throws when user does not exist`(
        caseName: String,
        blocked: Boolean,
    ) {
        assertTrue(caseName.isNotBlank())
        every { subscriptionUserRepository.findById(UNKNOWN_USER_ID) } returns null

        assertThrows<IllegalArgumentException> {
            if (blocked) {
                service.blockUser(UNKNOWN_USER_ID)
            } else {
                service.unblockUser(UNKNOWN_USER_ID)
            }
        }

        verify(exactly = 0) { subscriptionUserRepository.setBlocked(any(), any()) }
    }
}
