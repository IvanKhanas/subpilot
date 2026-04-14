plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.ktlint)
}

tasks.test {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.grpc.bom))
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.spring.grpc.bom))
    testImplementation(platform(libs.testcontainers.bom))

    implementation(libs.bundles.spring.boot.base)
    implementation(libs.spring.grpc.starter)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.openai)
    implementation(libs.kotlin.logging)
    implementation(libs.spring.boot.starter.data.redis)

    implementation(project(":proto"))

    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.mockk)
    testImplementation(libs.wiremock)
    testImplementation(libs.datafaker)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit5)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.spring.grpc.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly(libs.junit.platform.launcher)
}
