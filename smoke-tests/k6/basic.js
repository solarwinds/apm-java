import http from "k6/http";
import {check} from "k6";
import names from "./names.js";

const baseUri = `http://petclinic:9966/petclinic/api`;
const webMvcUri = `http://webmvc:8080`;

function verify_that_trace_is_persisted() {
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 10;
    for (; retryCount; retryCount--) {
        const petTypesResponse = http.get(`${baseUri}/pettypes`);
        check(petTypesResponse.headers, {
            'should have X-Trace header': (h) => h['X-Trace'] !== undefined
        })

        const traceContext = petTypesResponse.headers['X-Trace']
        const [_, traceId, spanId, flag] = traceContext.split("-")
        if (flag === '00') continue;

        const traceDetailPayload = {
            "operationName": "getTraceDetails",
            "variables": {
                "traceId": traceId.toUpperCase(),
                "spanId": spanId.toUpperCase(),
                "aggregateSpans": true
            },
            "query": "query getTraceDetails($traceId: ID!, $spanId: ID, $aggregateSpans: Boolean, $incomplete: Boolean) {\n  traceDetails(\n    traceId: $traceId\n    spanId: $spanId\n    aggregateSpans: $aggregateSpans\n    incomplete: $incomplete\n  ) {\n    traceId\n    action\n    spanCount\n    time\n    controller\n    duration\n    originSpan {\n      id\n      service\n      status\n      transaction\n      duration\n      method\n      errorCount\n      host\n      startTime\n      action\n      controller\n      serviceEntity\n      containerEntity\n      hostEntity\n      websiteEntity\n      hostEntityName\n      websiteEntityName\n      serviceInstanceEntityName\n      __typename\n    }\n    selectedSpan {\n      id\n      service\n      status\n      transaction\n      duration\n      method\n      errorCount\n      host\n      startTime\n      action\n      controller\n      serviceEntity\n      containerEntity\n      hostEntity\n      websiteEntity\n      hostEntityName\n      websiteEntityName\n      serviceInstanceEntityName\n      __typename\n    }\n    allErrors {\n      hostname\n      message\n      spanLayer\n      time\n      exceptionClassMessageHash\n      spanId\n      __typename\n    }\n    allQueries {\n      ...QueryItem\n      __typename\n    }\n    traceBreakdown {\n      duration\n      errorCount\n      layer\n      percentOfTraceDuration\n      spanCount\n      spanIds\n      __typename\n    }\n    waterfall {\n      ...WaterfallRow\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment QueryItem on QueryItem {\n  averageTime\n  count\n  query\n  queryHash\n  totalTime\n  percentOfTraceDuration\n  spanIds\n  dboQueryId\n  __typename\n}\n\nfragment WaterfallRow on WaterfallRow {\n  parentId\n  items {\n    layer\n    spanId\n    endTime\n    startTime\n    service\n    error {\n      exceptionClassMessageHash\n      message\n      spanId\n      timestamp\n      __typename\n    }\n    async\n    __typename\n  }\n  __typename\n}\n"
        }

        for (; retryCount; retryCount--) {
            let traceDetailResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(traceDetailPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            traceDetailResponse = JSON.parse(traceDetailResponse.body)
            if (traceDetailResponse['errors']) continue
            check(traceDetailResponse, {
                "trace is returned": tdr => tdr.data.traceDetails.traceId.toLowerCase() === traceId.toLowerCase()
            });

            // check that db query is captured(JDBC check)
            const {data: {traceDetails: {allQueries}}} = traceDetailResponse;
            check(allQueries, {"JDBC is not broken": aq => aq.length > 0})
            return
        }

    }

}

function verify_that_span_data_is_persisted() {
    const newOwner = names.randomOwner();
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 10;
    for (; retryCount; retryCount--) {
        const newOwnerResponse = http.post(`${baseUri}/owners`, JSON.stringify(newOwner),
            {
                headers: {
                    'Content-Type': 'application/json',
                    "x-trace-options": "trigger-trace;custom-info=chubi;sw-keys=lo:se,check-id:123"
                }
            });

        const traceContext = newOwnerResponse.headers['X-Trace']
        const [_, traceId, __, flag] = traceContext.split("-")
        if (flag === '00') continue;

        const spanRawDataPayload = {
            "operationName": "getSubSpanRawData",
            "variables": {
                "traceId": traceId.toUpperCase()
            },
            "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
        }

        for (; retryCount; retryCount--) {
            let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            spanDataResponse = JSON.parse(spanDataResponse.body)
            if (spanDataResponse['errors']) continue

            const {data: {traceArchive: {traceSpans: {edges}}}} = spanDataResponse
            for (let i = 0; i < edges.length; i++) {
                const edge = edges[i]
                const {node: {events}} = edge
                for (let j = 0; j < events.length; j++) {
                    const event = events[j]
                    const {properties} = event
                    let found = false

                    for (let k = 0; k < properties.length; k++) {
                        const property = properties[k]
                        check(property, {"mvc handler name is added": prop => prop.key === "HandlerName"})
                        check(property, {"code profiling": prop => prop.key === "NewFrames"})

                        check(property, {"trigger trace": prop => prop.key === "TriggeredTrace"})
                        if (property.key === "PDKeys") {
                            check(property, {"xtrace-options is added to root span": prop => prop.value === "{check-id=123, lo=se}"})
                            found = true
                        }

                        if (property.key === "custom-info") {
                            check(property, {"xtrace-options is added to root span": prop => prop.value === "chubi"})
                            found = true
                        }
                    }
                    if (found) return; // assumes that all checked property must exist at the same node(root span).
                }
            }
        }

    }

}

function verify_that_span_data_is_persisted_0() {
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 10;
    for (; retryCount; retryCount--) {
        const transactionName = "int-test"
        const response = http.get(`${webMvcUri}/greet/${transactionName}`, {
            headers: {
                'Content-Type': 'application/json',
                "x-trace-options": "trigger-trace;custom-info=chubi;sw-keys=lo:se,check-id:123"
            }
        });

        const traceContext = response.headers['X-Trace']
        const [_, traceId, __, flag] = traceContext.split("-")
        if (flag === '00') continue;

        const spanRawDataPayload = {
            "operationName": "getSubSpanRawData",
            "variables": {
                "traceId": traceId.toUpperCase()
            },
            "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
        }

        for (; retryCount; retryCount--) {
            let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            spanDataResponse = JSON.parse(spanDataResponse.body)
            if (spanDataResponse['errors']) continue

            const {data: {traceArchive: {traceSpans: {edges}}}} = spanDataResponse
            for (let i = 0; i < edges.length; i++) {
                const edge = edges[i]
                const {node: {events}} = edge

                for (let j = 0; j < events.length; j++) {
                    const event = events[j]
                    const {properties} = event

                    for (let k = 0; k < properties.length; k++) {
                        const property = properties[k]

                        check(property, {"trigger trace": prop => prop.key === "TriggeredTrace"})
                        check(property, {"code profiling": prop => prop.key === "NewFrames"})
                        if (property.key === "TransactionName") {
                            check(property, {"custom transaction name": prop => prop.value === transactionName})
                            return;
                        }
                    }
                }
            }
        }
    }
}

function verify_distributed_trace() {
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 10;
    for (; retryCount; retryCount--) {
        const response = http.get(`${webMvcUri}/distributed`, {
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const traceContext = response.headers['X-Trace']
        const [_, traceId, __, flag] = traceContext.split("-")
        if (flag === '00') continue;

        const spanRawDataPayload = {
            "operationName": "getSubSpanRawData",
            "variables": {
                "traceId": traceId.toUpperCase()
            },
            "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
        }

        for (; retryCount; retryCount--) {
            let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            spanDataResponse = JSON.parse(spanDataResponse.body)
            if (spanDataResponse['errors']) continue

            const {data: {traceArchive: {traceSpans: {edges}}}} = spanDataResponse
            console.log("Edges: ", edges)

            for (let i = 0; i < edges.length; i++) {
                const edge = edges[i]
                const {node: {events}} = edge

                for (let j = 0; j < events.length; j++) {
                    const event = events[j]
                    const {properties} = event

                    for (let k = 0; k < properties.length; k++) {
                        const property = properties[k]
                        if (property.key === "service.name") {
                            check(property, {"check that remote service, java-apm-smoke-test, is path of the trace": prop => prop.value === "java-apm-smoke-test"})
                            if (property.value === "java-apm-smoke-test") return;
                        }
                    }
                }
            }
        }
    }
}

function verify_that_specialty_path_is_not_sampled() {
    const specialtiesUrl = `${baseUri}/specialties`;
    const specialtiesResponse = http.get(specialtiesUrl);
    const traceContext = specialtiesResponse.headers['X-Trace']

    const [_, __, ___, flag] = traceContext.split("-")
    check(flag, {"verify that transaction is filtered": f => f === "00"})
}

function verify_that_metrics_are_reported(metric, checkFn) {
  let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 10;
  for (let i = 0; i < retryCount; i++) {
    const newOwner = names.randomOwner();
    http.post(`${baseUri}/owners`, JSON.stringify(newOwner),
        {
          headers: {
            'Content-Type': 'application/json',
          }
        }
    );
  }

  const metricQueryPayload = {
    "operationName": "getMetricMeasurementsByMetricNamesWithComparison",
    "variables": {
      "includeComparisonMeasurements": false,
      "metricNames": [
        metric
      ],
      "metricInput": {
        "aggregation": {
          "method": "AVG",
          "bucketSizeInS": 60,
          "missingDataPointsHandling": "NULL_FILL",
          "fillIfResultEmpty": false
        },
        "filter": null,
        "query": "service.name:\"lambda-e2e\"",
        "groupBy": [
          "service.name"
        ],
        "timeRange": {
          "startTime": "30 minutes ago",
          "endTime": "now"
        }
      },
      "comparisonMetricInput": {
        "aggregation": {
          "method": "AVG",
          "bucketSizeInS": 60,
          "missingDataPointsHandling": "NULL_FILL",
          "fillIfResultEmpty": false
        },
        "filter": null,
        "query": "service.name:\"lambda-e2e\"",
        "groupBy": [
          "service.name"
        ],
        "timeRange": {}
      }
    },
    "query": "query getMetricMeasurementsByMetricNamesWithComparison($metricNames: [String!]!, $metricInput: MetricQueryInput!, $comparisonMetricInput: MetricQueryInput!, $includeComparisonMeasurements: Boolean = false) {\n  metrics {\n    byNames(names: $metricNames) {\n      name\n      units\n      measurements(metricInput: $metricInput) {\n        ...MetricMeasurements\n        __typename\n      }\n      comparisonMeasurements: measurements(metricInput: $comparisonMetricInput) @include(if: $includeComparisonMeasurements) {\n        ...MetricMeasurements\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment MetricMeasurements on Measurements {\n  series {\n    bucketSizeInSeconds\n    tags {\n      key\n      value\n      __typename\n    }\n    measurements {\n      time\n      value\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n"
  }

  for (let i = 0; i < retryCount; i++) {
    console.log("Metric request: ")
    let metricDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(metricQueryPayload),
        {
          headers: {
            'Content-Type': 'application/json',
            'Cookie': `${__ENV.SWO_COOKIE}`,
            'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
          }
        }
    );

    metricDataResponse = JSON.parse(metricDataResponse.body)
    if (metricDataResponse['errors']) continue

    const {data: {metrics: {byNames: metrics}}} = metricDataResponse
    for (let i = 0; i < metrics.length; i++) {
      const _metric = metrics[i]
      const {measurements: {series}} = _metric

      for (let j = 0; j < series.length; j++) {
        const {measurements} = series[j]

        for (let k = 0; k < measurements.length; k++) {
          const measurement = measurements[k];
          if (measurement.value > 0) {
            checkFn(measurement)
            return
          }
        }
      }
    }
  }
}

function verify_transaction_name() {
  let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 10;
  for (; retryCount; retryCount--) {
    const newOwner = names.randomOwner();
    const response = http.post(`${baseUri}/owners`, JSON.stringify(newOwner),
        {
          headers: {
            'Content-Type': 'application/json',
          }
        }
    );

    const traceContext = response.headers['X-Trace']
    const [_, traceId, __, flag] = traceContext.split("-")
    if (flag === '00') continue;

    const spanRawDataPayload = {
      "operationName": "getSubSpanRawData",
      "variables": {
        "traceId": traceId.toUpperCase()
      },
      "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
    }

    for (; retryCount; retryCount--) {
      let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
          {
            headers: {
              'Content-Type': 'application/json',
              'Cookie': `${__ENV.SWO_COOKIE}`,
              'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
            }
          });

      spanDataResponse = JSON.parse(spanDataResponse.body)
      if (spanDataResponse['errors']) continue

      const {data: {traceArchive: {traceSpans: {edges}}}} = spanDataResponse
      for (let i = 0; i < edges.length; i++) {
        const edge = edges[i]
        const {node: {events}} = edge

        for (let j = 0; j < events.length; j++) {
          const event = events[j]
          const {properties} = event

          for (let k = 0; k < properties.length; k++) {
            const property = properties[k]
            if (property.key === "TransactionName") {
              check(property, {"transaction-name": prop => prop.value === "lambda-test-txn"})
              return;
            }
          }
        }
      }
    }
  }
}

function silence(fn) {
  try {
    fn()
  } catch (e) {
  }
}

export default function () {
  if (`${__ENV.LAMBDA}` === "true") {
    const request_count = (measurement) => check(measurement, {"request_count": mrs => mrs.value > 0})
    const tracecount = (measurement) => check(measurement, {"tracecount": mrs => mrs.value > 0})
    const samplecount = (measurement) => check(measurement, {"samplecount": mrs => mrs.value > 0})
    const sample_rate = (measurement) => check(measurement, {"sample_rate": mrs => mrs.value > 0})
    const sample_source = (measurement) => check(measurement, {"sample_source": mrs => mrs.value > 0})
    const response_time = (measurement) => check(measurement, {"response_time": mrs => mrs.value > 0})

    silence(function () {
      verify_that_metrics_are_reported("trace.service.request_count", request_count)
    })

    silence(function () {
      verify_that_metrics_are_reported("trace.service.tracecount", tracecount)
    })

    silence(function () {
      verify_that_metrics_are_reported("trace.service.samplecount", samplecount)
    })

    silence(function () {
      verify_that_metrics_are_reported("trace.service.sample_rate", sample_rate)
    })

    silence(function () {
      verify_that_metrics_are_reported("trace.service.sample_source", sample_source)
    })

    silence(function () {
      verify_that_metrics_are_reported("trace.service.response_time", response_time)
    })

    silence(verify_transaction_name)

  } else {
    silence(verify_that_specialty_path_is_not_sampled)
    silence(verify_that_span_data_is_persisted_0)
    silence(verify_that_span_data_is_persisted)

    silence(verify_that_trace_is_persisted)
    silence(verify_distributed_trace)
  }
};
