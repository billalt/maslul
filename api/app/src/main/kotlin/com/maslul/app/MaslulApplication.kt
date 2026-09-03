package com.maslul.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.maslul"])
class MaslulApplication

fun main(args: Array<String>) {
    runApplication<MaslulApplication>(*args)
}
