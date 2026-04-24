package com.xeno.subpilot.tgbot.config

import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.loyalty.v1.LoyaltyServiceGrpcKt
import com.xeno.subpilot.proto.payment.v1.PaymentServiceGrpcKt
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory

@Configuration
class GrpcClientConfig {

    @Bean
    fun chatServiceStub(channels: GrpcChannelFactory): ChatServiceGrpcKt.ChatServiceCoroutineStub =
        ChatServiceGrpcKt.ChatServiceCoroutineStub(channels.createChannel("chat-service"))

    @Bean
    fun subscriptionServiceStub(
        channels: GrpcChannelFactory,
    ): SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub =
        SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub(
            channels.createChannel("subscription-service"),
        )

    @Bean
    fun paymentServiceStub(
        channels: GrpcChannelFactory,
    ): PaymentServiceGrpcKt.PaymentServiceCoroutineStub =
        PaymentServiceGrpcKt.PaymentServiceCoroutineStub(channels.createChannel("payment-service"))

    @Bean
    fun loyaltyServiceStub(
        channels: GrpcChannelFactory,
    ): LoyaltyServiceGrpcKt.LoyaltyServiceCoroutineStub =
        LoyaltyServiceGrpcKt.LoyaltyServiceCoroutineStub(channels.createChannel("loyalty-service"))
}
