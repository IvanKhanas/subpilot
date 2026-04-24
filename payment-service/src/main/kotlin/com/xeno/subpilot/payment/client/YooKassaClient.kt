package com.xeno.subpilot.payment.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.xeno.subpilot.payment.dto.YooKassaResult
import com.xeno.subpilot.payment.exception.YooKassaException
import com.xeno.subpilot.payment.properties.YooKassaProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

import java.math.BigDecimal
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class YooKassaClient(
    private val yooKassaRestClient: RestClient,
    properties: YooKassaProperties,
) {
    private val returnUrl = properties.returnUrl

    fun createPayment(
        amount: BigDecimal,
        currency: String,
        userId: Long,
        planId: String,
    ): YooKassaResult {
        try {
            val response =
                yooKassaRestClient
                    .post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotence-Key", UUID.randomUUID().toString())
                    .body(
                        CreatePaymentRequest(
                            amount = AmountDto(amount.toPlainString(), currency),
                            description = "Subscription plan: $planId",
                            confirmation = ConfirmationDto("redirect", returnUrl),
                            capture = true,
                        ),
                    ).retrieve()
                    .body(CreatePaymentResponse::class.java)!!
            return YooKassaResult(
                yookassaPaymentId = response.id,
                confirmationUrl = response.confirmation.confirmationUrl,
            )
        } catch (ex: Exception) {
            logger.atError {
                message = "yookassa_create_payment_failed"
                cause = ex
                payload = mapOf("user_id" to userId, "plan_id" to planId)
            }
            throw YooKassaException("Failed to create YooKassa payment", ex)
        }
    }
}

private data class CreatePaymentRequest(
    val amount: AmountDto,
    val description: String,
    val confirmation: ConfirmationDto,
    val capture: Boolean,
)

private data class AmountDto(
    val value: String,
    val currency: String,
)

private data class ConfirmationDto(
    val type: String,

    @JsonProperty("return_url") val returnUrl: String,
)

private data class CreatePaymentResponse(
    val id: UUID,
    val confirmation: ConfirmationResponseDto,
)

private data class ConfirmationResponseDto(
    @JsonProperty("confirmation_url") val confirmationUrl: String,
)
