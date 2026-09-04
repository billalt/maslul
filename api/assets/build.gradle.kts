plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
    `java-library`
}

dependencies {
    implementation(project(":common"))
    // TenantContext - assets controllers pass the current tenant explicitly into the
    // tenant-scoped repository methods as the app-level half of the two-layer tenant
    // enforcement (RLS on the table is the other half; see CLAUDE.md).
    implementation(project(":identity"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // api, not implementation: entity classes expose JTS Geometry/Point in their public
    // constructors, so consumers (app) need it on their compile classpath too.
    api("org.hibernate.orm:hibernate-spatial")
    // CSV import: handles the BOM Excel adds, quoted fields with embedded commas, and
    // inconsistent line endings - a council spreadsheet will have all three.
    implementation("org.apache.commons:commons-csv:1.11.0")
    // GeoJSON import: without this, Jackson falls back to reflective bean access for Kotlin
    // data classes and a missing JSON field lands as null/0 instead of failing - see app's
    // build.gradle.kts for the same reasoning.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
