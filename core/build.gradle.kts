plugins {
    `java-library`
}

dependencies {
    // Jackson для сериализации JSON с поддержкой java.time.LocalDate
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("runBenchmark") {
    group = "benchmark"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.nstu.labs.core.benchmark.BenchmarkRunner")
}
