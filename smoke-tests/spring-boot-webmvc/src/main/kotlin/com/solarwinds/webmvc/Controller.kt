/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
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
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.annotations.WithSpan
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
    private val otelSdk: OpenTelemetry = GlobalOpenTelemetry.get()
    private val tracer = otelSdk.getTracer("sdk.tracing")

    @GetMapping("greet/{name}")
    fun greet(@PathVariable name: String): String{
        val startSpan = tracer.spanBuilder("greet-span")
            .setAttribute("sw.test.source", "SDK.trace.test")
            .startSpan()
        startSpan.makeCurrent().use {
        SolarwindsAgent.setTransactionName(name)
            startSpan.end()
        return  "Hello $name\n===================================\n\n"
        }
    }

    @WithSpan
    @GetMapping("distributed")
    fun distributed(): String {
        val startSpan = tracer.spanBuilder("greet-span")
            .setAttribute("sw.test.source", "SDK.trace.test")
            .startSpan()

        startSpan.makeCurrent().use {
        log.info("Got request at distributed")
            startSpan.end()
        }
        return HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://petclinic:9966/petclinic/api/pettypes")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            ).body().also { log.info("Responded at distributed") }
    }
}
