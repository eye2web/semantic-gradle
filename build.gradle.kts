plugins {
    `kotlin-dsl`
}

group = "nl.eye2web"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
}

gradlePlugin {
    plugins {
        register("semanticGradlePlugin") {
            id = "nl.eye2web.semantic.gradle"
            implementationClass = "nl.eye2web.semantic.gradle.SemanticGradlePlugin"
        }

        register("SemanticGradleBuildConfigurationPlugin") {
            id = "nl.eye2web.semantic.gradle.build.config"
            implementationClass =
                "nl.eye2web.semantic.gradle.build.configuration.SemanticGradleBuildConfigurationPlugin"
        }


    }


}
