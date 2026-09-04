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
    // Without this, Jackson falls back to reflective bean access for Kotlin classes:
    // missing JSON fields land as null/0 instead of the Kotlin default, silently bypassing
    // non-null typing. Needed for every entity's JSON (de)serialization, not just assets.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    // Signs test-only JWTs so isolation tests can call the real endpoints as a given
    // tenant, the same way TenantContextFilter resolves tenant_id in production.
    testImplementation("io.jsonwebtoken:jjwt-api:0.12.6")
    testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}
