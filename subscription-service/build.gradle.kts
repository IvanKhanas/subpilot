plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.grpc.bom))
    testImplementation(platform(libs.spring.boot.bom))

    implementation(libs.bundles.spring.boot.base)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.grpc.starter)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    implementation(libs.liquibase.core)
    implementation(libs.spring.kafka)

    implementation(project(":proto"))
    runtimeOnly(project(":migrations"))

    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.datafaker)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit5)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}
