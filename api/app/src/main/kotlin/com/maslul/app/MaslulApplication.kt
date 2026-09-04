package com.maslul.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// @EnableJpaRepositories/@EntityScan don't follow scanBasePackages - Boot derives their
// default base package from this class's own package (com.maslul.app) unless told
// otherwise, which would silently miss every entity/repository in the feature modules.
@SpringBootApplication(scanBasePackages = ["com.maslul"])
@EntityScan(basePackages = ["com.maslul"])
@EnableJpaRepositories(basePackages = ["com.maslul"])
class MaslulApplication

fun main(args: Array<String>) {
    runApplication<MaslulApplication>(*args)
}
