import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.sonarqube") version "7.2.3.7755"
    id("org.springframework.boot") version "4.0.3" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }

        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }
    }

    plugins.withId("java") {
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

sonar {
    properties {
        property("sonar.sourceEncoding", "UTF-8")
    }
}
