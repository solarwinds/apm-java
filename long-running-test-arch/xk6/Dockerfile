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
