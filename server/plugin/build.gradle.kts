description = "Elvarg Plugins"


val lib = rootProject.project.libs
dependencies {
    implementation(project(":game"))
    implementation(kotlin("script-runtime"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xdisable-default-scripting-plugin"
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
