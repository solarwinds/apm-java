package com.solarwinds.webmvc

import mu.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class CustomFilter: Filter {
    private val log = KotlinLogging.logger {}
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse?, filterChain: FilterChain) {
        val httpServletRequest = servletRequest as HttpServletRequest
        val httpServletResponse = servletResponse as HttpServletResponse
        log.info("request headers: {}", httpServletRequest.headerNames.toList())
        log.info("response headers: {}", httpServletResponse.headerNames.toList())
        filterChain.doFilter(servletRequest, servletResponse)
    }
}