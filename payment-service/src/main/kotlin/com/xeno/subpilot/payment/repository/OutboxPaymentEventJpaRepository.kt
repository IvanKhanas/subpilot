package com.xeno.subpilot.payment.repository

import com.xeno.subpilot.payment.entity.OutboxPaymentEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import java.time.LocalDateTime

interface OutboxPaymentEventJpaRepository : JpaRepository<OutboxPaymentEvent, Long> {

    @Query(
        value =
            "SELECT * FROM outbox_payment_event WHERE published_at IS NULL ORDER BY created_at LIMIT :limit",
        nativeQuery = true,
    )
    fun findUnpublished(
        @Param("limit") limit: Int,
    ): List<OutboxPaymentEvent>

    @Modifying
    @Query(
        "UPDATE OutboxPaymentEvent event" +
            " SET event.publishedAt = :now WHERE event.id IN :ids",
    )
    fun markPublished(
        @Param("ids") ids: List<Long>,
        @Param("now") now: LocalDateTime,
    )
}
