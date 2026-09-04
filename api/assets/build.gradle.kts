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
}
