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
package com.xeno.subpilot.gateway.unittests.config

import com.xeno.subpilot.gateway.config.HttpClientConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse

class HttpClientConfigTest {

    @Test
    fun `restTemplate never reports error regardless of upstream status`() {
        val restTemplate = HttpClientConfig().restTemplate()

        listOf(
            HttpStatus.BAD_REQUEST,
            HttpStatus.NOT_FOUND,
            HttpStatus.INTERNAL_SERVER_ERROR,
        ).forEach { status ->
            val response = mockk<ClientHttpResponse>()
            every { response.statusCode } returns status
            assertFalse(restTemplate.errorHandler.hasError(response))
        }
    }
}
