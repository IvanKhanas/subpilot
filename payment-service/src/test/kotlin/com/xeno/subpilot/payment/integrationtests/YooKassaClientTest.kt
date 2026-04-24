package com.xeno.subpilot.payment.integrationtests

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.ninjasquad.springmockk.MockkBean
import com.xeno.subpilot.payment.client.YooKassaClient
import com.xeno.subpilot.payment.exception.YooKassaException
import com.xeno.subpilot.payment.testcontainers.TestContainersConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import java.math.BigDecimal
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class YooKassaClientTest {

    @MockkBean(relaxed = true)
    @Suppress("unused")
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var yooKassaClient: YooKassaClient

    companion object {
        private val postgres = TestContainersConfiguration.postgres

        @RegisterExtension
        @JvmField
        val wireMock: WireMockExtension =
            WireMockExtension
                .newInstance()
                .options(wireMockConfig().dynamicPort())
                .build()

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.kafka.bootstrap-servers") { "localhost:9999" }
            registry.add("yookassa.api-base-url") { "http://localhost:${wireMock.port}" }
        }

        const val USER_ID = 1L
        const val PLAN_ID = "openai-basic"
        val AMOUNT = BigDecimal("199.00")
        const val CURRENCY = "RUB"
        val YOOKASSA_PAYMENT_ID: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        const val CONFIRMATION_URL = "https://yookassa.ru/checkout/payments/test-id"
        const val PAYMENTS_PATH = "/payments"

        fun successfulPaymentResponse(): String =
            """
            {
              "id": "$YOOKASSA_PAYMENT_ID",
              "status": "pending",
              "confirmation": {
                "confirmation_url": "$CONFIRMATION_URL"
              }
            }
            """.trimIndent()
    }

    @Test
    fun `createPayment sends POST to YooKassa payments endpoint`() {
        wireMock.stubFor(
            post(urlEqualTo(PAYMENTS_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(
                            200,
                        ).withHeader(
                            "Content-Type",
                            "application/json",
                        ).withBody(successfulPaymentResponse()),
                ),
        )

        yooKassaClient.createPayment(AMOUNT, CURRENCY, USER_ID, PLAN_ID)

        wireMock.verify(postRequestedFor(urlEqualTo(PAYMENTS_PATH)))
    }

    @Test
    fun `createPayment sends correct amount and currency`() {
        wireMock.stubFor(
            post(urlEqualTo(PAYMENTS_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(
                            200,
                        ).withHeader(
                            "Content-Type",
                            "application/json",
                        ).withBody(successfulPaymentResponse()),
                ),
        )

        yooKassaClient.createPayment(AMOUNT, CURRENCY, USER_ID, PLAN_ID)

        wireMock.verify(
            postRequestedFor(urlEqualTo(PAYMENTS_PATH))
                .withRequestBody(matchingJsonPath("$.amount.value", equalTo("199.00")))
                .withRequestBody(matchingJsonPath("$.amount.currency", equalTo(CURRENCY))),
        )
    }

    @Test
    fun `createPayment returns yookassaPaymentId and confirmationUrl`() {
        wireMock.stubFor(
            post(urlEqualTo(PAYMENTS_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(
                            200,
                        ).withHeader(
                            "Content-Type",
                            "application/json",
                        ).withBody(successfulPaymentResponse()),
                ),
        )

        val result = yooKassaClient.createPayment(AMOUNT, CURRENCY, USER_ID, PLAN_ID)

        assert(result.yookassaPaymentId == YOOKASSA_PAYMENT_ID) {
            "must return yookassaPaymentId from response"
        }
        assert(
            result.confirmationUrl == CONFIRMATION_URL,
        ) { "must return confirmationUrl from response" }
    }

    @Test
    fun `createPayment throws YooKassaException on HTTP error`() {
        wireMock.stubFor(
            post(urlEqualTo(PAYMENTS_PATH))
                .willReturn(aResponse().withStatus(500)),
        )

        assertThrows<YooKassaException> {
            yooKassaClient.createPayment(AMOUNT, CURRENCY, USER_ID, PLAN_ID)
        }
    }

    @Test
    fun `createPayment includes Idempotence-Key header`() {
        wireMock.stubFor(
            post(urlEqualTo(PAYMENTS_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(
                            200,
                        ).withHeader(
                            "Content-Type",
                            "application/json",
                        ).withBody(successfulPaymentResponse()),
                ),
        )

        yooKassaClient.createPayment(AMOUNT, CURRENCY, USER_ID, PLAN_ID)

        wireMock.verify(
            postRequestedFor(urlEqualTo(PAYMENTS_PATH))
                .withHeader(
                    "Idempotence-Key",
                    com.github.tomakehurst.wiremock.client.WireMock
                        .matching(".+"),
                ),
        )
    }
}
