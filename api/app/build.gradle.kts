plugins {
    id("org.springframework.boot")
}

val testcontainersVersion = "2.0.5"
val archunitVersion = "1.3.0"

the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":identity"))
    implementation(project(":assets"))
    implementation(project(":dispatch"))
    implementation(project(":sync"))
    implementation(project(":telemetry"))
    implementation(project(":reports"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
}
