package com.xeno.subpilot.tgbot.integrationtests

import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.ninjasquad.springmockk.MockkBean
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.Update
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.runtime.TelegramLongPollingService
import com.xeno.subpilot.tgbot.runtime.TelegramMessageHandler
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import io.mockk.coEvery
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import kotlin.random.Random

import kotlinx.coroutines.runBlocking

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TelegramBotIntegrationTest {

    @MockkBean(relaxed = true)
    private lateinit var longPollingService: TelegramLongPollingService

    @MockkBean
    private lateinit var chatClient: ChatClient

    @MockkBean(relaxed = true)
    private lateinit var navigationService: NavigationService

    companion object {

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
            registry.add("telegram.bot.base-url") { "http://localhost:${wireMock.port}" }
            registry.add("telegram.bot.token") { BOT_TOKEN }
        }

        private val faker = Faker()
        private const val BOT_TOKEN = "test-token"

        private fun sendMessagePath() = "/bot$BOT_TOKEN/sendMessage"

        private fun answerCallbackPath() = "/bot$BOT_TOKEN/answerCallbackQuery"
    }

    @Autowired
    private lateinit var messageHandler: TelegramMessageHandler

    @BeforeEach
    fun setUp() {
        wireMock.resetAll()
        wireMock.stubFor(
            post(
                anyUrl(),
            ).willReturn(okJson("""{"ok":true,"result":{"message_id":1,"chat":{"id":1}}}""")),
        )
        coEvery { chatClient.processMessage(any(), any(), any()) } returns
            ProcessMessageResponse.newBuilder().setText("AI response").build()
    }

    @Test
    fun `start command with firstName sends personalized greeting`() {
        val firstName = faker.name().firstName()

        runBlocking {
            messageHandler.onUpdate(
                messageUpdate(randomId(), "/start", firstName = firstName),
            )
        }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(matchingJsonPath("$.text", containing(firstName))),
        )
    }

    @Test
    fun `start command without firstName falls back to friend`() {
        runBlocking {
            messageHandler.onUpdate(
                messageUpdate(randomId(), "/start", firstName = null),
            )
        }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(matchingJsonPath("$.text", containing("friend"))),
        )
    }

    @Test
    fun `start command routes reply to correct chat`() {
        val chatId = randomId()

        runBlocking { messageHandler.onUpdate(messageUpdate(chatId, "/start")) }

        wireMock.verify(
            2,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(matchingJsonPath("$[?(@.chat_id == $chatId)]")),
        )
    }

    @Test
    fun `start command attaches main menu reply keyboard`() {
        runBlocking { messageHandler.onUpdate(messageUpdate(randomId(), "/start")) }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(
                    matchingJsonPath(
                        "$.reply_markup.keyboard[0][0].text",
                        equalTo(BotButtons.BTN_START_CHAT),
                    ),
                ),
        )
    }

    @Test
    fun `help command sends help text`() {
        runBlocking { messageHandler.onUpdate(messageUpdate(randomId(), "/help")) }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(matchingJsonPath("$.text", containing("Available commands"))),
        )
    }

    @Test
    fun `start chat text button sends greeting message`() {
        runBlocking {
            messageHandler.onUpdate(
                messageUpdate(randomId(), BotButtons.BTN_START_CHAT),
            )
        }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath())),
        )
    }

    @Test
    fun `help text button sends help text`() {
        runBlocking { messageHandler.onUpdate(messageUpdate(randomId(), BotButtons.BTN_HELP)) }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(matchingJsonPath("$.text", containing("Available commands"))),
        )
    }

    @Test
    fun `start_chat callback acknowledges without sending a message`() {
        val callbackId = faker.internet().uuid()

        runBlocking {
            messageHandler.onUpdate(
                callbackUpdate(randomId(), callbackId, "start_chat"),
            )
        }

        wireMock.verify(0, postRequestedFor(urlPathEqualTo(sendMessagePath())))
        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(answerCallbackPath()))
                .withRequestBody(matchingJsonPath("$.callback_query_id", equalTo(callbackId))),
        )
    }

    @Test
    fun `help callback acknowledges without sending a message`() {
        val callbackId = faker.internet().uuid()

        runBlocking { messageHandler.onUpdate(callbackUpdate(randomId(), callbackId, "help")) }

        wireMock.verify(0, postRequestedFor(urlPathEqualTo(sendMessagePath())))
        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(answerCallbackPath()))
                .withRequestBody(matchingJsonPath("$.callback_query_id", equalTo(callbackId))),
        )
    }

    @Test
    fun `unknown text message is forwarded to chat service`() {
        runBlocking { messageHandler.onUpdate(messageUpdate(randomId(), faker.lorem().sentence())) }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(matchingJsonPath("$.text", equalTo("AI response"))),
        )
    }

    @Test
    fun `unknown command sends unknown command response`() {
        runBlocking { messageHandler.onUpdate(messageUpdate(randomId(), "/unknowncommand")) }

        wireMock.verify(
            1,
            postRequestedFor(urlPathEqualTo(sendMessagePath()))
                .withRequestBody(matchingJsonPath("$.text", containing("Unknown command"))),
        )
    }

    @Test
    fun `unknown callback acknowledges without sending a message`() {
        val callbackId = faker.internet().uuid()

        runBlocking {
            messageHandler.onUpdate(
                callbackUpdate(randomId(), callbackId, "unknown_data"),
            )
        }

        wireMock.verify(0, postRequestedFor(urlPathEqualTo(sendMessagePath())))
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(answerCallbackPath())))
    }

    private fun messageUpdate(
        chatId: Long,
        text: String,
        firstName: String? = faker.name().firstName(),
    ) = Update(
        updateId = randomId(),
        message =
            Message(
                messageId = randomId(),
                from = User(id = randomId(), firstName = firstName),
                chat = Chat(id = chatId),
                text = text,
            ),
    )

    private fun callbackUpdate(
        chatId: Long,
        callbackId: String,
        data: String,
    ) = Update(
        updateId = randomId(),
        callbackQuery =
            CallbackQuery(
                id = callbackId,
                from = User(id = randomId()),
                message =
                    Message(
                        messageId = randomId(),
                        chat = Chat(id = chatId),
                    ),
                data = data,
            ),
    )

    private fun randomId() = Random.nextLong(1, 1_000_000_000)
}
