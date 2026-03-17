plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.spring.boot.bom))
    implementation(libs.bundles.spring.boot.base)
    testImplementation(libs.bundles.spring.boot.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
