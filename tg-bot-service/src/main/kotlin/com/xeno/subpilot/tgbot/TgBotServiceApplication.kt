package com.xeno.subpilot.tgbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TgBotServiceApplication

fun main(args: Array<String>) {
    runApplication<TgBotServiceApplication>(*args)
}
