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
package com.xeno.subpilot.admin.dto

import com.xeno.subpilot.admin.auth.TokenPair

data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
) {
    companion object {
        fun from(tokenPair: TokenPair): AuthTokensResponse =
            AuthTokensResponse(
                accessToken = tokenPair.accessToken,
                refreshToken = tokenPair.refreshToken,
                tokenType = "Bearer",
                expiresIn = tokenPair.expiresIn,
            )
    }
}
