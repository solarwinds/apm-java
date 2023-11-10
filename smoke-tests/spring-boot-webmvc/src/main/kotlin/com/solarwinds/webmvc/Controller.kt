package com.solarwinds.webmvc

import com.appoptics.api.ext.SolarWindsAgent
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@RestController
@RequestMapping
class Controller {
    private val log = KotlinLogging.logger {}

    @GetMapping("greet/{name}")
    fun greet(@PathVariable name: String): String{
        SolarWindsAgent.setTransactionName(name)
        return  "Hello $name\n===================================\n\n"
    }

    @GetMapping("distributed")
    fun distributed(): String {
        log.info("Got request at distributed")
        return HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://petclinic:9966/petclinic/api/pettypes")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            ).body().also { log.info("Responded at distributed") }
    }
}