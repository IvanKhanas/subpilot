plugins {
    java
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
    testImplementation(platform(libs.spring.boot.bom))
    implementation(libs.bundles.spring.boot.base)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.logging)
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.mockk)
    testImplementation(libs.wiremock)
    testImplementation(libs.datafaker)
    testRuntimeOnly(libs.junit.platform.launcher)
}
