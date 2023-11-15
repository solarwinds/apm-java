package com.solarwinds.webmvc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class WebMVCAppApplication

fun main(args: Array<String>) {
	runApplication<WebMVCAppApplication>(*args)
}
