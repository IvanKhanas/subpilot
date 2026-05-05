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
package com.xeno.subpilot.admin.auth

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import java.sql.ResultSet

@Repository
class AdminAccountRepository(
    private val jdbcTemplate: JdbcTemplate,
) {

    fun findByUsername(username: String): AdminAccount? =
        jdbcTemplate
            .query(
                """
                SELECT id, username, password_hash, scopes, enabled, created_at
                FROM admin_account
                WHERE username = ?
                """.trimIndent(),
                this::mapRow,
                username,
            ).firstOrNull()

    fun insert(
        username: String,
        passwordHash: String,
        scopes: String,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO admin_account (username, password_hash, scopes, enabled)
            VALUES (?, ?, ?, true)
            """.trimIndent(),
            username,
            passwordHash,
            scopes,
        )
    }

    private fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): AdminAccount =
        AdminAccount(
            id = rs.getLong("id"),
            username = rs.getString("username"),
            passwordHash = rs.getString("password_hash"),
            scopes = rs.getString("scopes"),
            enabled = rs.getBoolean("enabled"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}
