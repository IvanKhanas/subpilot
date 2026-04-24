package com.xeno.subpilot.payment.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaConfig {

    @Bean
    fun kafkaProducerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
    ): ProducerFactory<String, String> =
        DefaultKafkaProducerFactory(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ),
        )

    @Bean
    fun kafkaTemplate(
        kafkaProducerFactory: ProducerFactory<String, String>,
    ): KafkaTemplate<String, String> = KafkaTemplate(kafkaProducerFactory)
}
