package com.xeno.subpilot.loyalty.config

import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory

@Configuration
class SubscriptionGrpcClientConfig {

    @Bean
    fun subscriptionServiceStub(
        channels: GrpcChannelFactory,
    ): SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub =
        SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub(
            channels.createChannel("subscription-service"),
        )
}
