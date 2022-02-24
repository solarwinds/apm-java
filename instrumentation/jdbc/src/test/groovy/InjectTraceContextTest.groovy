import com.appoptics.opentelemetry.instrumentation.TraceContextInjector
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

@Unroll
class InjectTraceContextTest extends Specification {
    def "inject trace context from span to sql"(String sql) {
        setup:
        def tracer = GlobalOpenTelemetry.getTracer("test")
        def testScope = tracer.spanBuilder("test").startSpan().makeCurrent()
        Pattern pattern = Pattern.compile("/\\*traceparent:'00-[a-f0-9]{32}-[a-f0-9]{16}-0[0|1]'\\*/ .+")

        expect:
        pattern.matcher(TraceContextInjector.inject(Context.current(), sql)).matches()

        cleanup:
        testScope.close()

        where:
        sql                                       |_
        "select name from students"               |_
        "insert into students values('tom', 1)"   |_
        "/* comment */ select name from students" |_
    }
}
