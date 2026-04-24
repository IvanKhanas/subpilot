/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.payment.config

import com.xeno.subpilot.payment.properties.YooKassaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

import java.net.http.HttpClient
import java.time.Clock

@Configuration
class PaymentServiceConfig {

    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()

    @Bean
    fun yooKassaRestClient(properties: YooKassaProperties): RestClient =
        RestClient
            .builder()
            .requestFactory(
                JdkClientHttpRequestFactory(
                    HttpClient
                        .newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build(),
                ),
            ).baseUrl(properties.apiBaseUrl)
            .defaultHeaders { it.setBasicAuth(properties.shopId, properties.secretKey) }
            .build()
}
