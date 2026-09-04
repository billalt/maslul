plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
    `java-library`
}

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // api, not implementation: entity classes expose JTS Geometry/Point in their public
    // constructors, so consumers (app) need it on their compile classpath too.
    api("org.hibernate.orm:hibernate-spatial")
}
