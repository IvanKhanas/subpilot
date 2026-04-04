import com.google.protobuf.gradle.id

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val protobufVersion: String? = catalog.findVersion("protobuf").get().requiredVersion
val grpcVersion: String? = catalog.findVersion("grpc").get().requiredVersion
val grpcKotlinVersion: String? = catalog.findVersion("grpc-kotlin").get().requiredVersion

dependencies {
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.kotlin)
    implementation(libs.kotlinx.coroutines.core)
    compileOnly(libs.javax.annotation.api)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc") { }
                id("grpckt") { }
            }
            task.builtins {
                id("kotlin") { }
            }
        }
    }
}
