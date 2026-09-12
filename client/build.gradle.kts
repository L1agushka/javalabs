plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = "21.0.2"
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":core"))

    // Jackson для связи и парсинга DTO
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
}

application {
    mainClass.set("ru.nstu.labs.client.App")
}