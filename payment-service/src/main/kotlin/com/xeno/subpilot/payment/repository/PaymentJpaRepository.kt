package com.xeno.subpilot.payment.repository

import com.xeno.subpilot.payment.entity.Payment
import com.xeno.subpilot.payment.entity.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

import java.time.LocalDateTime
import java.util.UUID

interface PaymentJpaRepository : JpaRepository<Payment, UUID> {

    @Modifying
    @Query(
        "UPDATE Payment p SET p.status = :status," +
            " p.updatedAt = :now WHERE p.id = :id AND p.status = :pending",
    )
    fun updateStatusIfPending(
        id: UUID,
        status: PaymentStatus,
        now: LocalDateTime,
        pending: PaymentStatus = PaymentStatus.PENDING,
    ): Int

    @Query("SELECT p FROM Payment p WHERE p.yooKassaPaymentId = :yooKassaPaymentId")
    fun findByYooKassaPaymentId(yooKassaPaymentId: UUID): Payment?
}
