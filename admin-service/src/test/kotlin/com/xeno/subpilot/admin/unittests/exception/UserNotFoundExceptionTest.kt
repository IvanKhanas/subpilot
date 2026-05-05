/*
 * Copyright 2024 Ivan Khanas
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
package com.xeno.subpilot.admin.unittests.exception

import com.xeno.subpilot.admin.exception.UserNotFoundException
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals

class UserNotFoundExceptionTest {

    @Test
    fun `exception message contains user id`() {
        val exception = UserNotFoundException(AdminTestFixtures.USER_ID)

        assertEquals("User ${AdminTestFixtures.USER_ID} not found", exception.message)
    }
}
