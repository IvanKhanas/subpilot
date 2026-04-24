package com.xeno.subpilot.loyalty.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "loyalty_transaction")
class LoyaltyTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: LoyaltyTransactionType,

    @Column(name = "payment_id")
    val paymentId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,
)
