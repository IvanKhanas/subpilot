package com.xeno.subpilot.subscription.testcontainers

import com.xeno.subpilot.subscription.dto.DenialReason
import com.xeno.subpilot.subscription.entity.UserFreeQuota
import com.xeno.subpilot.subscription.entity.UserRequestBalance
import com.xeno.subpilot.subscription.repository.SubscriptionUserJpaRepository
import com.xeno.subpilot.subscription.repository.UserFreeQuotaJpaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceJpaRepository
import com.xeno.subpilot.subscription.service.AccessService
import net.datafaker.Faker
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class AccessServiceContainerTest {

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @Autowired
    lateinit var accessService: AccessService

    @Autowired
    lateinit var userJpaRepository: SubscriptionUserJpaRepository

    @Autowired
    lateinit var freeQuotaJpaRepository: UserFreeQuotaJpaRepository

    @Autowired
    lateinit var balanceJpaRepository: UserRequestBalanceJpaRepository

    private fun randomUserId() = faker.number().numberBetween(100_000L, Long.MAX_VALUE)

    private fun givenUser(userId: Long) {
        userJpaRepository.insertIfAbsent(userId)
    }

    private fun givenFreeQuota(
        userId: Long,
        provider: String,
        remaining: Int,
    ) {
        freeQuotaJpaRepository.save(
            UserFreeQuota(userId, provider, remaining, LocalDateTime.now().plusDays(7)),
        )
    }

    private fun givenPaidBalance(
        userId: Long,
        provider: String,
        amount: Int,
    ) {
        balanceJpaRepository.save(UserRequestBalance(userId, provider, amount))
    }

    @Test
    fun `checkAndConsume denies blocked user`() {
        val userId = randomUserId()
        givenUser(userId)
        givenFreeQuota(userId, "openai", 5)
        userJpaRepository.findById(userId).get().also { it.blocked = true }

        val result = accessService.checkAndConsume(userId, "gpt-4o-mini")

        assertFalse(result.allowed)
        assertEquals(DenialReason.BLOCKED, result.denialReason)
    }

    @Test
    fun `checkAndConsume allows and deducts from free quota`() {
        val userId = randomUserId()
        givenUser(userId)
        givenFreeQuota(userId, "openai", 5)

        val result = accessService.checkAndConsume(userId, "gpt-4o-mini")

        assertTrue(result.allowed)
        assertEquals(1, result.freeConsumed)
        assertEquals(0, result.paidConsumed)
        assertEquals(
            4,
            freeQuotaJpaRepository
                .findByUserIdAndProviderForUpdate(
                    userId,
                    "openai",
                )!!
                .requestsRemaining,
        )
    }

    @Test
    fun `checkAndConsume denies when free quota exhausted and no paid balance`() {
        val userId = randomUserId()
        givenUser(userId)
        givenFreeQuota(userId, "openai", 0)

        val result = accessService.checkAndConsume(userId, "gpt-4o-mini")

        assertFalse(result.allowed)
        assertEquals(DenialReason.NO_SUBSCRIPTION, result.denialReason)
    }

    @Test
    fun `checkAndConsume deducts from paid balance when free quota is zero`() {
        val userId = randomUserId()
        givenUser(userId)
        givenFreeQuota(userId, "openai", 0)
        givenPaidBalance(userId, "openai", 10)

        val result = accessService.checkAndConsume(userId, "gpt-4o-mini")

        assertTrue(result.allowed)
        assertEquals(0, result.freeConsumed)
        assertEquals(1, result.paidConsumed)
        assertEquals(
            9,
            balanceJpaRepository.findByUserIdAndProvider(userId, "openai")!!.requestsRemaining,
        )
    }

    @Test
    fun `checkAndConsume does not deduct balance below zero`() {
        val userId = randomUserId()
        givenUser(userId)
        givenFreeQuota(userId, "openai", 0)
        givenPaidBalance(userId, "openai", 0)

        accessService.checkAndConsume(userId, "gpt-4o-mini")

        assertEquals(
            0,
            balanceJpaRepository.findByUserIdAndProvider(userId, "openai")!!.requestsRemaining,
        )
    }

    @Test
    fun `checkAndConsume returns resetAt when free quota just exhausted`() {
        val userId = randomUserId()
        givenUser(userId)
        givenFreeQuota(userId, "openai", 1)

        val result = accessService.checkAndConsume(userId, "gpt-4o-mini")

        assertTrue(result.allowed)
        assertNotNull(result.resetAt)
    }

    @ParameterizedTest(name = "freeConsumed={0} paidConsumed={1} refunded back to correct buckets")
    @CsvSource(
        "3, 0",
        "0, 3",
        "1, 2",
    )
    fun `refund restores each amount to its original bucket`(
        freeConsumed: Int,
        paidConsumed: Int,
    ) {
        val userId = randomUserId()
        givenUser(userId)
        givenFreeQuota(userId, "openai", 0)
        givenPaidBalance(userId, "openai", paidConsumed)

        accessService.refund(
            userId,
            "gpt-4o",
            freeConsumed = freeConsumed,
            paidConsumed = paidConsumed,
        )

        val freeAfter =
            freeQuotaJpaRepository
                .findByUserIdAndProviderForUpdate(
                    userId,
                    "openai",
                )!!
                .requestsRemaining
        val paidAfter =
            balanceJpaRepository
                .findByUserIdAndProvider(
                    userId,
                    "openai",
                )!!
                .requestsRemaining

        assertEquals(freeConsumed, freeAfter)
        assertEquals(paidConsumed * 2, paidAfter)
    }

    @Test
    fun `insertIfAbsent is idempotent for the same user`() {
        val userId = randomUserId()

        val first = userJpaRepository.insertIfAbsent(userId)
        val second = userJpaRepository.insertIfAbsent(userId)

        assertEquals(1, first)
        assertEquals(0, second)
    }
}
