package com.xeno.subpilot.payment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

import java.time.LocalDateTime

@Entity
@Table(name = "outbox_payment_event")
class OutboxPaymentEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    val payload: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "published_at")
    val publishedAt: LocalDateTime? = null,
)
