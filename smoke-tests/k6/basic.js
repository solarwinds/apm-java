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

import http from "k6/http";
import {check} from "k6";
import names from "./names.js";

const baseUri = `http://petclinic:9966/petclinic/api`;
const webMvcUri = `http://webmvc:8080`;
export const options = {
  duration: "15m",
  minIterationDuration: "3m",
  vus: 10,
  iterations: 200,
};

function verify_that_trace_is_persisted() {
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
    for (; retryCount > 0; retryCount--) {
        const petTypesResponse = http.get(`${baseUri}/pettypes`);
        check(petTypesResponse.headers, {
            'should have X-Trace header': (h) => h['X-Trace'] !== undefined
        })

        const traceContext = petTypesResponse.headers['X-Trace']
        const [_, traceId, spanId, flag] = traceContext.split("-")
        console.log("Trace context -> ", traceContext)
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

        for (; retryCount > 0; retryCount--) {
            let traceDetailResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(traceDetailPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            traceDetailResponse = JSON.parse(traceDetailResponse.body)
            if (traceDetailResponse['errors']) {
              console.log("Error -> Trace detail response:", JSON.stringify(traceDetailResponse))
              continue
            }

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
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
    for (; retryCount > 0; retryCount--) {
        const newOwnerResponse = http.post(`${baseUri}/owners`, JSON.stringify(newOwner),
            {
                headers: {
                    'Content-Type': 'application/json',
                    "x-trace-options": "trigger-trace;custom-info=chubi;sw-keys=lo:se,check-id:123"
                }
            });

        const traceContext = newOwnerResponse.headers['X-Trace']
        const [_, traceId, __, flag] = traceContext.split("-")
        console.log("Trace context -> ", traceContext)
        if (flag === '00') continue;

        const spanRawDataPayload = {
            "operationName": "getSubSpanRawData",
            "variables": {
                "traceId": traceId.toUpperCase()
            },
            "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
        }

        for (; retryCount > 0; retryCount--) {
            let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            spanDataResponse = JSON.parse(spanDataResponse.body)
            if (spanDataResponse['errors']) {
              console.log("Error -> Persisted trace response:", JSON.stringify(spanDataResponse))
              continue
            }

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
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
    for (; retryCount > 0; retryCount--) {
        const transactionName = "int-test"
        const response = http.get(`${webMvcUri}/greet/${transactionName}`, {
            headers: {
                'Content-Type': 'application/json',
                "x-trace-options": "trigger-trace;custom-info=chubi;sw-keys=lo:se,check-id:123"
            }
        });

        const traceContext = response.headers['X-Trace']
        const [_, traceId, __, flag] = traceContext.split("-")
        console.log("Trace context -> ", traceContext)
        if (flag === '00') continue;

        const spanRawDataPayload = {
            "operationName": "getSubSpanRawData",
            "variables": {
                "traceId": traceId.toUpperCase()
            },
            "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
        }

        for (; retryCount > 0; retryCount--) {
            let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            spanDataResponse = JSON.parse(spanDataResponse.body)
            if (spanDataResponse['errors']) {
              console.log("Error -> Persisted trace response:", JSON.stringify(spanDataResponse))
              continue
            }

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
                        if (property.key === "sw.transaction") {
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
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
    for (; retryCount > 0; retryCount--) {
        const response = http.get(`${webMvcUri}/distributed`, {
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const traceContext = response.headers['X-Trace']
        const [_, traceId, __, flag] = traceContext.split("-")
        console.log("Trace context -> ", traceContext)
        if (flag === '00') continue;

        const spanRawDataPayload = {
            "operationName": "getSubSpanRawData",
            "variables": {
                "traceId": traceId.toUpperCase()
            },
            "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
        }

        for (; retryCount > 0; retryCount--) {
            let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            spanDataResponse = JSON.parse(spanDataResponse.body)
            if (spanDataResponse['errors']) {
              console.log("Error -> Distributed trace response:", JSON.stringify(spanDataResponse))
              continue
            }

            const {data: {traceArchive: {traceSpans: {edges}}}} = spanDataResponse
            console.log("Edges: ", edges)


            let contextCheck = false
            let sdkCheck = false
            for (let i = 0; i < edges.length; i++) {
                const edge = edges[i]
                const {node: {events}} = edge

                for (let j = 0; j < events.length; j++) {
                    const event = events[j]
                    const {properties} = event

                    for (let k = 0; k < properties.length; k++) {
                        const property = properties[k]
                        check(property, {
                            "check that remote service, java-apm-smoke-test, is path of the trace": prop => {
                                contextCheck = prop.value === "java-apm-smoke-test"
                                return contextCheck
                            }
                        })
                        check(property, {
                            "sdk-trace": prop => {
                                sdkCheck = prop.value === "SDK.trace.test"
                                return sdkCheck
                            }
                        })
                    }

                    if (contextCheck && sdkCheck) {
                        return
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

function verify_that_metrics_are_reported(metric, checkFn, service="lambda-e2e") {
  let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
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
        "query": `service.name:\"${service}\"`,
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
        "query": `service.name:\"${service}\"`,
        "groupBy": [
          "service.name"
        ],
        "timeRange": {}
      }
    },
    "query": "query getMetricMeasurementsByMetricNamesWithComparison($metricNames: [String!]!, $metricInput: MetricQueryInput!, $comparisonMetricInput: MetricQueryInput!, $includeComparisonMeasurements: Boolean = false) {\n  metrics {\n    byNames(names: $metricNames) {\n      name\n      units\n      measurements(metricInput: $metricInput) {\n        ...MetricMeasurements\n        __typename\n      }\n      comparisonMeasurements: measurements(metricInput: $comparisonMetricInput) @include(if: $includeComparisonMeasurements) {\n        ...MetricMeasurements\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment MetricMeasurements on Measurements {\n  series {\n    bucketSizeInSeconds\n    tags {\n      key\n      value\n      __typename\n    }\n    measurements {\n      time\n      value\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n"
  }

  for (let i = 0; i < retryCount; i++) {
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
    if (metricDataResponse['errors']) {
      console.log("Error -> Metric response:", JSON.stringify(metricDataResponse))
      continue
    }

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

function check_transaction_name(property) {
    if (property.key === "sw.transaction") {
        check(property, {"transaction-name": prop => prop.value === "lambda-test-txn"})
        return true;
    }
    return false
}


function check_code_stack_trace(property) {
    if (property.key === "code.stacktrace") {
        check(property, {"code.stacktrace": _ => true})
        return true;
    }
    return false
}

function check_property(fn) {
    let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
    for (; retryCount > 0; retryCount--) {
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
        console.log("Trace context -> ", traceContext)
        if (flag === '00') continue;

        const spanRawDataPayload = {
            "operationName": "getSubSpanRawData",
            "variables": {
                "traceId": traceId.toUpperCase()
            },
            "query": "query getSubSpanRawData($traceId: ID!, $spanFilter: TraceArchiveSpanFilter, $incomplete: Boolean) {\n  traceArchive(\n    traceId: $traceId\n    spanFilter: $spanFilter\n    incomplete: $incomplete\n  ) {\n    traceId\n    traceSpans {\n      edges {\n        node {\n          events {\n            eventId\n            properties {\n              key\n              value\n              __typename\n            }\n            __typename\n          }\n          spanId\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n"
        }

        for (; retryCount > 0; retryCount--) {
            let spanDataResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(spanRawDataPayload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            spanDataResponse = JSON.parse(spanDataResponse.body)
            if (spanDataResponse['errors']) {
                console.log("Error -> Transaction name response:", JSON.stringify(spanDataResponse))
                continue
            }

            const {data: {traceArchive: {traceSpans: {edges}}}} = spanDataResponse
            for (let i = 0; i < edges.length; i++) {
                const edge = edges[i]
                const {node: {events}} = edge

                for (let j = 0; j < events.length; j++) {
                    const event = events[j]
                    const {properties} = event

                    for (let k = 0; k < properties.length; k++) {
                        const property = properties[k]
                        if (fn(property)) {
                            return;
                        }
                    }
                }
            }
        }
    }
}

function getEntityId() {
  let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
  const entityQueryPayload = {
    "operationName": "getServiceEntitiesQuery",
    "variables": {
      "includeKubernetesClusterUid": false,
      "filter": {
        "types": [
          "Service"
        ]
      },
      "timeFilter": {
        "startTime": "1 hour ago",
        "endTime": "now"
      },
      "sortBy": {
        "sorts": [
          {
            "propertyName": "name",
            "direction": "DESC"
          }
        ]
      },
      "pagination": {
        "first": 50
      },
      "bucketSizeInS": 60
    },
    "query": "query getServiceEntitiesQuery($filter: EntityFilterInput, $timeFilter: TimeRangeInput!, $sortBy: EntitySortInput, $pagination: PagingInput, $bucketSizeInS: Int!, $includeKubernetesClusterUid: Boolean = false) {\n  entities {\n    search(\n      filter: $filter\n      sortBy: $sortBy\n      paging: $pagination\n      timeRange: $timeFilter\n    ) {\n      totalEntitiesCount\n      pageInfo {\n        endCursor\n        hasNextPage\n        startCursor\n        hasPreviousPage\n        __typename\n      }\n      groups {\n        entities {\n          ... on Service {\n            ...ServiceEntity\n            __typename\n          }\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment ServiceEntity on Service {\n  id\n  name: displayName\n  lastSeenTime\n  language\n  kubernetesPodInstances @include(if: $includeKubernetesClusterUid) {\n    clusterUid\n    __typename\n  }\n  healthScore {\n    scoreV2\n    categoryV2\n    __typename\n  }\n  traceServiceErrorRatio {\n    ...MetricSeriesMeasurementsForServiceEntity\n    __typename\n  }\n  traceServiceErrorRatioValue\n  traceServiceRequestRate {\n    ...MetricSeriesMeasurementsForServiceEntity\n    __typename\n  }\n  traceServiceRequestRateValue\n  responseTime {\n    ...MetricSeriesMeasurementsForServiceEntity\n    __typename\n  }\n  responseTimeValue\n  sumRequests\n  __typename\n}\n\nfragment MetricSeriesMeasurementsForServiceEntity on Metric {\n  measurements(\n    metricInput: {aggregation: {method: AVG, bucketSizeInS: $bucketSizeInS, missingDataPointsHandling: NULL_FILL}, timeRange: $timeFilter}\n  ) {\n    series {\n      measurements {\n        time\n        value\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n"
  }

  for (; retryCount > 0; retryCount--) {
    let entityResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(entityQueryPayload),
        {
          headers: {
            'Content-Type': 'application/json',
            'Cookie': `${__ENV.SWO_COOKIE}`,
            'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
          }
        });

    entityResponse = JSON.parse(entityResponse.body)
    if (entityResponse['errors']) {
      console.log("Error -> Entity id response:", JSON.stringify(entityResponse))
      continue
    }

    const {data: {entities: {search:{groups}}}} = entityResponse
    for (let i = 0; i < groups.length; i++) {
      const {entities} = groups[i]
      for (let j = 0; j < entities.length; j++) {
        const {name, id} = entities[j]

        if(name === `${__ENV.SERVICE_NAME}`){
          return id
        }
      }
    }
  }
}

function verify_logs_export() {
  let retryCount = Number.parseInt(`${__ENV.SWO_RETRY_COUNT}`) || 1000;
  const logQueryPayload = {
    "operationName": "getLogEvents",
    "variables": {
      "input": {
        "direction": "BACKWARD",
        "searchLimit": 500,
        "entityIds": [
          getEntityId()
        ],
        "query": ""
      }
    },
    "query": "query getLogEvents($input: LogEventsInput!) {\n  logEvents(input: $input) {\n    events {\n      id\n      facility\n      program\n      message\n      receivedAt\n      severity\n      sourceName\n      isJson\n      positions {\n        length\n        starts\n        __typename\n      }\n      __typename\n    }\n    cursor {\n      maxId\n      maxTimestamp\n      minId\n      minTimestamp\n      __typename\n    }\n    __typename\n  }\n}\n"
  }

  for (; retryCount > 0; retryCount--) {
    let logResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(logQueryPayload),
        {
          headers: {
            'Content-Type': 'application/json',
            'Cookie': `${__ENV.SWO_COOKIE}`,
            'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
          }
        });

    logResponse = JSON.parse(logResponse.body)
    if (logResponse['errors']) {
      console.log("Error -> Log response:", JSON.stringify(logResponse))
      continue
    }

    const {data: {logEvents: {events}}} = logResponse
    check(events, {"logs": events => events.length > 0})
  }
}

function silence(fn) {
  try {
    fn()
  } catch (e) {
    console.log("Error -> Exception running function: ", fn)
  }
}

export default function () {
  silence(verify_that_span_data_is_persisted_0)

  const request_count = (measurement) => check(measurement, {"request_count": mrs => mrs.value > 0})
  const tracecount = (measurement) => check(measurement, {"tracecount": mrs => mrs.value > 0})
  const samplecount = (measurement) => check(measurement, {"samplecount": mrs => mrs.value > 0})
  const response_time = (measurement) => check(measurement, {"response_time": mrs => mrs.value > 0})

  if (`${__ENV.LAMBDA}` === "true") {

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
      verify_that_metrics_are_reported("trace.service.response_time", response_time)
    })

    silence(function () {
        check_property(check_transaction_name)
    })

    silence(function () {
      check_property(check_code_stack_trace)
    })

  } else {
      const service = "java-apm-smoke-test"
      silence(function () {
          verify_that_metrics_are_reported("trace.service.request_count", request_count, service)
      })

      silence(function () {
          verify_that_metrics_are_reported("trace.service.tracecount", tracecount, service)
      })

      silence(function () {
          verify_that_metrics_are_reported("trace.service.samplecount", samplecount, service)
      })

      silence(function () {
          verify_that_metrics_are_reported("trace.service.response_time", response_time, service)
      })

      silence(function () {
          verify_that_metrics_are_reported("jvm.memory.used",
              (measurement) => check(measurement, {"otel-metrics": mrs => mrs.value > 0}),
              service
          )
      })

    silence(verify_logs_export)
    silence(verify_that_specialty_path_is_not_sampled)
    silence(function () {
        check_property(check_code_stack_trace)
    })
    silence(verify_that_span_data_is_persisted)
    silence(verify_that_trace_is_persisted)
    silence(verify_distributed_trace)
  }
};
