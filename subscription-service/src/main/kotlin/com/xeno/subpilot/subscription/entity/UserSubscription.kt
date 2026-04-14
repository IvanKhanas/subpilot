package com.xeno.subpilot.subscription.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_subscription")
class UserSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "payment_id", nullable = false, unique = true)
    val paymentId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "plan_id", nullable = false)
    val planId: String,

    @Column(name = "provider", nullable = false)
    val provider: String,

    @Column(name = "earned_requests", nullable = false)
    val earnedRequests: Int,

    @Column(name = "activated_at", nullable = false)
    val activatedAt: Instant,
)
