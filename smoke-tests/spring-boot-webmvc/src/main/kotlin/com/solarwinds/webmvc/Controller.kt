package com.solarwinds.webmvc

import com.appoptics.api.ext.SolarWindsAgent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class Controller {
    @GetMapping("greet/{name}")
    fun greet(@PathVariable name: String): String{
        SolarWindsAgent.setTransactionName(name)
        return  "Hello $name\n===================================\n\n"
    }
}