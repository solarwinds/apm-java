import com.appoptics.opentelemetry.instrumentation.TraceContextInjector
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class InjectTraceContextTest extends Specification {
    def "inject trace context from span to sql"() {
        setup:
        def tracer = GlobalOpenTelemetry.getTracer("test")

        when:
        def testScope = tracer.spanBuilder("test").startSpan().makeCurrent()
        String sql = "select name from students";
        String injectedSql = TraceContextInjector.inject(Context.current(), sql)

        then:
        injectedSql.startsWith("/*traceparent:")

        cleanup:
        testScope.close()
    }
}
