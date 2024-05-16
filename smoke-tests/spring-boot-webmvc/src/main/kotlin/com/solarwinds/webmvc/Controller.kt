/*
 * Copyright SolarWinds Worldwide, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.solarwinds.webmvc

import com.solarwinds.api.ext.SolarwindsAgent
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
        SolarwindsAgent.setTransactionName(name)
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
