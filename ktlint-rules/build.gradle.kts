plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        suppressWarnings = true
    }
}

dependencies {
    compileOnly("com.pinterest.ktlint:ktlint-rule-engine-core:1.5.0")
    compileOnly("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.5.0")
}
