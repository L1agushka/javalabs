plugins {
    java
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}

// Отключаем поиск main-класса, пока серверный модуль не наполнен кодом
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}