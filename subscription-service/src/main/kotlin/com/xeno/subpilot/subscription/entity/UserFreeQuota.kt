package com.xeno.subpilot.subscription.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table

import java.io.Serializable
import java.time.LocalDateTime

data class UserFreeQuotaId(
    val userId: Long = 0,
    val provider: String = "",
) : Serializable

@Entity
@IdClass(UserFreeQuotaId::class)
@Table(name = "user_free_quota")
class UserFreeQuota(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Id
    @Column(name = "provider", nullable = false)
    val provider: String,

    @Column(name = "requests_remaining", nullable = false)
    var requestsRemaining: Int,

    @Column(name = "next_reset_at", nullable = false)
    var nextResetAt: LocalDateTime,
)
