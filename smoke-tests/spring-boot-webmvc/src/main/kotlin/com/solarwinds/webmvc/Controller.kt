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
import io.opentelemetry.api.common.AttributeKey.valueKey
import io.opentelemetry.api.common.Value
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
import java.nio.ByteBuffer

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
            .setAttribute(valueKey("complex.list"), Value.of(listOf<Value<String>>(Value.of("one"), Value.of("two"))))
            .setAttribute(
                valueKey("complex.map"),
                Value.of(mapOf<String, Value<String>>("one" to Value.of("one"), "two" to Value.of("two")))
            )
            .setAttribute(
                valueKey("complex.bytearray"),
                Value.of(byteArrayOf(1, 2, 3, 4, 5)),
            )
            .setAttribute(
                valueKey("complex.map.bytearray"), Value.of(
                    mapOf<String, Value<ByteBuffer>>(
                        "one" to Value.of(
                            ByteArray(10) { (it * 3).toByte() }), "two" to Value.of(ByteArray(20) { (it * 2).toByte() })
                    )
                )
            )
            .setAttribute(
                valueKey("complex.map.bytearray.big"), Value.of(
                    mapOf<String, Value<ByteBuffer>>(
                        "one" to Value.of(
                            ByteArray(128) { (it * 3).toByte() }), "two" to Value.of(ByteArray(256) { (it * 2).toByte() })
                    )
                )
            )
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
