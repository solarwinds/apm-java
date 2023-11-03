import http from "k6/http";
import {check} from "k6";
import names from "./names.js";

export const options = {
    thresholds: {
        http_req_failed: ['rate<0.05'], // http errors should be less than 1%
    },
};

const baseUri = `http://petclinic:9966/petclinic/api`;

const checkXtraceHeader = (headers) => {
    check(headers, {
        'should have X-Trace header': (h) => h['X-Trace'] !== undefined
    })
}

function verify_that_trace_is_persisted() {
    const newOwner = names.randomOwner();
    let retryCount = 10;
    for (; retryCount; retryCount--) {
        const newOwnerResponse = http.post(`${baseUri}/owners`, JSON.stringify(newOwner),
            {headers: {'Content-Type': 'application/json', 'x-trace-options': 'trigger-trace'}});

        check(newOwnerResponse, {"new owner status 201": r => r.status === 201});
        checkXtraceHeader(newOwnerResponse.headers)

        const traceContext = newOwnerResponse.headers['X-Trace']
        const [_, traceId, spanId, flag] = traceContext.split("-")
        if (flag === '00') continue;

        const payload = {
            "operationName": "getTraceDetails",
            "variables": {
                "traceId": traceId.toUpperCase(),
                "spanId": spanId.toUpperCase(),
                "aggregateSpans": true
            },
            "query": "query getTraceDetails($traceId: ID!, $spanId: ID, $aggregateSpans: Boolean, $incomplete: Boolean) {\n  traceDetails(\n    traceId: $traceId\n    spanId: $spanId\n    aggregateSpans: $aggregateSpans\n    incomplete: $incomplete\n  ) {\n    traceId\n    action\n    spanCount\n    time\n    controller\n    duration\n    originSpan {\n      id\n      service\n      status\n      transaction\n      duration\n      method\n      errorCount\n      host\n      startTime\n      action\n      controller\n      serviceEntity\n      containerEntity\n      hostEntity\n      websiteEntity\n      hostEntityName\n      websiteEntityName\n      serviceInstanceEntityName\n      __typename\n    }\n    selectedSpan {\n      id\n      service\n      status\n      transaction\n      duration\n      method\n      errorCount\n      host\n      startTime\n      action\n      controller\n      serviceEntity\n      containerEntity\n      hostEntity\n      websiteEntity\n      hostEntityName\n      websiteEntityName\n      serviceInstanceEntityName\n      __typename\n    }\n    allErrors {\n      hostname\n      message\n      spanLayer\n      time\n      exceptionClassMessageHash\n      spanId\n      __typename\n    }\n    allQueries {\n      ...QueryItem\n      __typename\n    }\n    traceBreakdown {\n      duration\n      errorCount\n      layer\n      percentOfTraceDuration\n      spanCount\n      spanIds\n      __typename\n    }\n    waterfall {\n      ...WaterfallRow\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment QueryItem on QueryItem {\n  averageTime\n  count\n  query\n  queryHash\n  totalTime\n  percentOfTraceDuration\n  spanIds\n  dboQueryId\n  __typename\n}\n\nfragment WaterfallRow on WaterfallRow {\n  parentId\n  items {\n    layer\n    spanId\n    endTime\n    startTime\n    service\n    error {\n      exceptionClassMessageHash\n      message\n      spanId\n      timestamp\n      __typename\n    }\n    async\n    __typename\n  }\n  __typename\n}\n"
        }

        for (; retryCount; retryCount--) {
            const traceDetailResponse = http.post(`${__ENV.SWO_HOST_URL}/common/graphql`, JSON.stringify(payload),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'Cookie': `${__ENV.SWO_COOKIE}`,
                        'X-Csrf-Token': `${__ENV.SWO_XSR_TOKEN}`
                    }
                });

            const responsePayload = JSON.parse(traceDetailResponse.body)
            if (responsePayload['errors']) continue;
            check(traceDetailResponse, {"trace is returned": _ => responsePayload.data.traceDetails.traceId.toLowerCase() === traceId.toLowerCase()});

            return
        }

    }

}
export default function() {
    const specialtiesUrl = `${baseUri}/specialties`;
    const specialtiesResponse = http.get(specialtiesUrl);
    const specialties = JSON.parse(specialtiesResponse.body);

    checkXtraceHeader(specialtiesResponse.headers)

    // Add a new vet to the list
    const newVet = names.randomVet(specialties);
    const response = http.post(`${baseUri}/vets`, JSON.stringify(newVet),
            { headers: { 'Content-Type': 'application/json' } });
    // we don't guard against dupes, so this could fail on occasion
    check(response, { "create vet status 201": (r) => r.status === 201 });
    checkXtraceHeader(response.headers)

    // make sure we can fetch that vet back out
    const vetId = JSON.parse(response.body).id;
    const vetUrl = `${baseUri}/vets/${vetId}`
    const vetResponse = http.get(vetUrl);
    check(vetResponse, { "fetch vet status 200": r => r.status === 200 });
    checkXtraceHeader(vetResponse.headers)

    // add a new owner
    const newOwner = names.randomOwner();
    const newOwnerResponse = http.post(`${baseUri}/owners`, JSON.stringify(newOwner),
          { headers: { 'Content-Type': 'application/json' } });
    check(newOwnerResponse, { "new owner status 201": r => r.status === 201});
    checkXtraceHeader(newOwnerResponse.headers)

    // make sure we can fetch that owner back out
    const ownerId = JSON.parse(newOwnerResponse.body).id;
    const ownerResponse = http.get(`${baseUri}/owners/${ownerId}`);
    check(ownerResponse, { "fetch new owner status 200": r => r.status === 200});
    checkXtraceHeader(ownerResponse.headers)
    const owner = JSON.parse(ownerResponse.body);

    // get the list of all pet types
    const petTypes = JSON.parse(http.get(`${baseUri}/pettypes`).body);
    const owners = JSON.parse(http.get(`${baseUri}/owners`).body);

    // create a 3 new random pets
    const pet1 = names.randomPet(petTypes, owner);
    const pet2 = names.randomPet(petTypes, owner);
    const pet3 = names.randomPet(petTypes, owner);

    const petsUrl = `${baseUri}/pets`;
    const newPetResponses = http.batch([
        ["POST", petsUrl, JSON.stringify(pet1), { headers: { 'Content-Type': 'application/json' } } ],
        ["POST", petsUrl, JSON.stringify(pet2), { headers: { 'Content-Type': 'application/json' } } ],
        ["POST", petsUrl, JSON.stringify(pet3), { headers: { 'Content-Type': 'application/json' } } ],
    ]);
    check(newPetResponses[0], { "pet status 201": r => r.status === 201});
    check(newPetResponses[1], { "pet status 201": r => r.status === 201});
    check(newPetResponses[2], { "pet status 201": r => r.status === 201});

    checkXtraceHeader(newPetResponses[0].headers)
    checkXtraceHeader(newPetResponses[1].headers)
    checkXtraceHeader(newPetResponses[2].headers)

    const responses = http.batch([
        ["GET", `${baseUri}/pets/${JSON.parse(newPetResponses[0].body).id}`],
        ["GET", `${baseUri}/pets/${JSON.parse(newPetResponses[1].body).id}`],
        ["GET", `${baseUri}/pets/${JSON.parse(newPetResponses[2].body).id}`]
    ]);
    check(responses[0], { "pet exists 200": r => r.status === 200});
    check(responses[1], { "pet exists 200": r => r.status === 200});
    check(responses[2], { "pet exists 200": r => r.status === 200});

    checkXtraceHeader(responses[0].headers)
    checkXtraceHeader(responses[1].headers)
    checkXtraceHeader(responses[2].headers)

    // Clean up after ourselves.
    // Delete pets
    const petDeletes = http.batch([
      ["DELETE", `${baseUri}/pets/${JSON.parse(newPetResponses[0].body).id}`],
      ["DELETE", `${baseUri}/pets/${JSON.parse(newPetResponses[1].body).id}`],
      ["DELETE", `${baseUri}/pets/${JSON.parse(newPetResponses[2].body).id}`]
    ]);

    check(petDeletes[0], { "pet deleted 204": r => r.status === 204});
    check(petDeletes[1], { "pet deleted 204": r => r.status === 204});
    check(petDeletes[2], { "pet deleted 204": r => r.status === 204});

    checkXtraceHeader(petDeletes[0].headers)
    checkXtraceHeader(petDeletes[1].headers)
    checkXtraceHeader(petDeletes[2].headers)

    // Delete owner
    const delOwner = http.del(`${baseUri}/owners/${ownerId}`);
    check(delOwner, { "owner deleted 204": r => r.status === 204});
    checkXtraceHeader(delOwner.headers)

    // Delete vet
    const delVet = http.del(`${baseUri}/vets/${vetId}`);
    check(delVet, { "owner deleted 204": r => r.status === 204});
    checkXtraceHeader(delVet.headers)

    verify_that_trace_is_persisted()
};