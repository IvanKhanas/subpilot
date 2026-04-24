package com.xeno.subpilot.payment.service

import com.xeno.subpilot.payment.client.YooKassaClient
import com.xeno.subpilot.payment.dto.PaymentResult
import com.xeno.subpilot.payment.dto.PlanDetails
import com.xeno.subpilot.payment.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.payment.dto.kafka.YooKassaWebhookEvent
import com.xeno.subpilot.payment.entity.OutboxPaymentEvent
import com.xeno.subpilot.payment.entity.Payment
import com.xeno.subpilot.payment.entity.PaymentStatus
import com.xeno.subpilot.payment.repository.OutboxPaymentEventJpaRepository
import com.xeno.subpilot.payment.repository.PaymentJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

import java.time.Clock
import java.time.LocalDateTime

@Service
class YooKassaPaymentService(
    private val paymentJpaRepository: PaymentJpaRepository,
    private val yooKassaClient: YooKassaClient,
    private val outboxPaymentEventJpaRepository: OutboxPaymentEventJpaRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {

    @Transactional
    fun createPayment(
        userId: Long,
        planId: String,
        bonusPointsToApply: Long = 0,
        plan: PlanDetails,
    ): PaymentResult {
        val discount = bonusPointsToApply.coerceAtLeast(0).toBigDecimal().min(plan.price)
        val finalAmount = plan.price - discount
        val now = LocalDateTime.now(clock)
        val payment =
            paymentJpaRepository.save(
                Payment(
                    userId = userId,
                    planId = planId,
                    amount = finalAmount,
                    currency = plan.currency,
                    bonusPointsUsed = discount.toLong(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        val result = yooKassaClient.createPayment(finalAmount, plan.currency, userId, planId)
        payment.yooKassaPaymentId = result.yookassaPaymentId
        return PaymentResult(payment.id!!.toString(), result.confirmationUrl)
    }

    @Transactional
    fun handlePaymentWebhook(event: YooKassaWebhookEvent) {
        val payment = paymentJpaRepository.findByYooKassaPaymentId(event.payment.id) ?: return
        val newStatus =
            when (event.event) {
                "payment.succeeded" -> PaymentStatus.SUCCEEDED
                else -> return
            }
        val now = LocalDateTime.now(clock)
        val updated = paymentJpaRepository.updateStatusIfPending(payment.id!!, newStatus, now)

        if (updated == 0) return

        outboxPaymentEventJpaRepository.save(
            OutboxPaymentEvent(
                eventType = "payment_succeeded",
                payload = buildPayload(payment),
                createdAt = LocalDateTime.now(clock),
            ),
        )
    }

    private fun buildPayload(payment: Payment): String =
        objectMapper.writeValueAsString(
            PaymentSucceededEvent(
                paymentId = payment.id!!,
                userId = payment.userId,
                planId = payment.planId,
                amount = payment.amount,
                bonusPointsUsed = payment.bonusPointsUsed,
            ),
        )
}
