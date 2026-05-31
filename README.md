# Transaction Gateway

## Run locally

docker-compose down -v
docker-compose up -d --build

## API Base URL

http://localhost:8080

## Endpoints

POST /v1/payments
GET /v1/payments/{transactionId}
GET /v1/payments/user/{userId}
GET /v1/accounts

## Docker services

postgres:5432
redis:6379
zookeeper:2181
kafka:9092
transaction-gateway:8080

## Docker commands

docker-compose ps
docker-compose logs -f transaction-gateway
docker-compose down -v
