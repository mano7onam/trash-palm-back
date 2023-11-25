package com.cyprus.trash

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TrashApplication

fun main(args: Array<String>) {
	runApplication<TrashApplication>(*args)
}
