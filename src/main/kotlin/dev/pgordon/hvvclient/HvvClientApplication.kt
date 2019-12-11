package dev.pgordon.hvvclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HvvClientApplication

fun main(args: Array<String>) {
    runApplication<HvvClientApplication>(*args)
}
