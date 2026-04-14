package com.xeno.subpilot.subscription.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

import java.time.Instant

@Entity
@Table(name = "subscription_user")
class SubscriptionUser(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "registered_at", nullable = false)
    val registeredAt: Instant = Instant.now(),

    @Column(name = "blocked", nullable = false)
    var blocked: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole = UserRole.USER,
)
