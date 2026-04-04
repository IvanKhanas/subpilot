plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
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

    implementation(libs.bundles.spring.boot.base)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.grpc.starter)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.openai)
    implementation(libs.kotlin.logging)
    implementation(libs.liquibase.core)
    implementation(libs.spring.boot.starter.data.redis)

    implementation(project(":proto"))
    runtimeOnly(project(":migrations"))

    runtimeOnly(libs.postgresql)

    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.mockk)
    testImplementation(libs.spring.grpc.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
