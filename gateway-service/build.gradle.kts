plugins {
    java
    jacoco
    id("subpilot.jacoco-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.spring.boot.bom))

    implementation(libs.bundles.spring.boot.base)
    implementation(libs.bundles.observability)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.kotlin.logging)

    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.mockk)
    testImplementation(libs.springmockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}
