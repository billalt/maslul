plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
}

dependencies {
    implementation(project(":common"))
    // TenantContext - sync controllers pass the current tenant explicitly into ingestion,
    // the app-level half of the two-layer tenant enforcement (RLS is the other half).
    implementation(project(":identity"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // JsonNode for arbitrary event payloads, and Kotlin data class support when parsing the
    // gzip-decoded breadcrumb body - see assets' build.gradle.kts for the same reasoning.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
