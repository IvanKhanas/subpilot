package com.xeno.subpilot.tgbot.config

import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory

@Configuration
class GrpcClientConfig {

    @Bean
    fun chatServiceStub(channels: GrpcChannelFactory): ChatServiceGrpcKt.ChatServiceCoroutineStub =
        ChatServiceGrpcKt.ChatServiceCoroutineStub(channels.createChannel("chat-service"))
}
