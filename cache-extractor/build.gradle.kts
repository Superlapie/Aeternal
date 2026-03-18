plugins {
    java
    application
}

group = "com.cacheextractor"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Use a simpler approach without OpenRS dependency
    // We'll create a basic cache reader that works with the local cache structure
    
    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Command line argument parsing
    implementation("info.picocli:picocli:4.7.4")
    annotationProcessor("info.picocli:picocli-codegen:4.7.4")
    
    // Progress reporting
    implementation("me.tongfei:progressbar:0.9.5")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.8")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

application {
    mainClass.set("com.cacheextractor.CacheExtractor")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
}

// Create a fat JAR with all dependencies
tasks.register<Jar>("fatJar") {
    manifest {
        attributes["Main-Class"] = "com.cacheextractor.CacheExtractor"
    }
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
    with(tasks.jar.get())
}
