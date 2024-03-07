package k6_otel

import (
	"context"
	"fmt"
	"io"
	"os"

	"go.k6.io/k6/metrics"
	"go.k6.io/k6/output"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"go.opentelemetry.io/otel/sdk/metric"
	"go.opentelemetry.io/otel/sdk/resource"
)

func newResource() (*resource.Resource, error) {
	return resource.Default(), nil
}

func newMeterProvider(res *resource.Resource) (*metric.MeterProvider, error) {
	metricExporter, err := otlpmetricgrpc.New(context.Background())
	if err != nil {
		return nil, err
	}

	meterProvider := metric.NewMeterProvider(
		metric.WithResource(res),
		metric.WithReader(metric.NewPeriodicReader(metricExporter)),
	)
	return meterProvider, nil
}

// OtelOutput writes k6 metric samples via otlp.
type OtelOutput struct {
	meterProvider *metric.MeterProvider
	out           io.Writer
	ctx           context.Context
}

// init is called by the Go runtime at application startup.
func init() {
	output.RegisterExtension("otelout", New)
}

// New returns a new instance of OtelOutput.
func New(params output.Params) (output.Output, error) {
	return &OtelOutput{out: params.StdOut, ctx: context.Background()}, nil
}

// Description returns a short human-readable description of the output.
func (*OtelOutput) Description() string {
	return "otelout"
}

// Start initializes any state needed for the output, establishes network
// connections, etc.
func (otelout *OtelOutput) Start() error {
	fmt.Fprintf(otelout.out, "OTEL_EXPORTER_OTLP_HEADERS=%s\nOTEL_EXPORTER_OTLP_ENDPOINT=%s\nOTEL_SERVICE_NAME=%s\n",
		os.Getenv("OTEL_EXPORTER_OTLP_HEADERS"),
		os.Getenv("OTEL_EXPORTER_OTLP_ENDPOINT"),
		os.Getenv("OTEL_SERVICE_NAME"))

	res, err_res := newResource()
	if err_res != nil {
		fmt.Fprintf(otelout.out, "Observed error during resource creation: %s\n", err_res.Error())
		return err_res
	}

	meterProvider, err := newMeterProvider(res)
	if err != nil {
		fmt.Fprintf(otelout.out, "Observed error during meter provider creation: %s\n", err.Error())
		return err
	}

	otelout.meterProvider = meterProvider
	return err
}

// AddMetricSamples receives metric samples from the k6 Engine as they're emitted.
func (otelout *OtelOutput) AddMetricSamples(samples []metrics.SampleContainer) {
	for _, sample := range samples {
		samples := sample.GetSamples()
		otelout.recordMetric(samples)
	}
}

func (otelout *OtelOutput) recordMetric(samples []metrics.Sample) {
	name := os.Getenv("METER_NAME")
	if name == "" {
		name = "k6.metrics"
	}

	meter := otelout.meterProvider.Meter(name)
	for _, sample := range samples {
		if sample.Metric.Name == "http_req_duration" {
			instrument, err := meter.Float64Histogram("http_req_duration")
			if err == nil {
				instrument.Record(otelout.ctx, sample.Value)
			} else {
				fmt.Fprintf(otelout.out, "Observed error during histogram creation: %s\n", err.Error())
			}
		}

		if sample.Metric.Name == "http_reqs" {
			instrument, err := meter.Float64Counter("http_reqs")
			if err == nil {
				instrument.Add(otelout.ctx, sample.Value)
			} else {
				fmt.Fprintf(otelout.out, "Observed error during counter creation: %s\n", err.Error())
			}
		}
	}
}

// Stop finalizes any tasks in progress, closes network connections, etc.
func (otelout *OtelOutput) Stop() error {
	return otelout.meterProvider.Shutdown(otelout.ctx)
}
