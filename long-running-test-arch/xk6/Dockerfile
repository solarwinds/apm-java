#
# © SolarWinds Worldwide, LLC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM --platform=amd64 golang:1.22.1 AS build
WORKDIR /app
COPY go.mod go.mod
COPY k6_otel.go k6_otel.go

RUN go get .
RUN go install go.k6.io/xk6/cmd/xk6@latest
RUN xk6 build --with xk6-otel-output=.

FROM  --platform=arm64 debian:12-slim
WORKDIR /app
COPY --from=build /app/k6 .

ENV PATH="${PATH}:/app"
ENTRYPOINT [ "k6" ]
