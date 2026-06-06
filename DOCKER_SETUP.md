# Docker Setup Guide - SwiftPay Transaction Gateway

This guide explains how to containerize and run the entire SwiftPay microservices ecosystem using Docker and Docker Compose.

## Prerequisites

- **Docker Desktop** installed (includes Docker and Docker Compose)
  - Download from: https://www.docker.com/products/docker-desktop
  - Windows: Ensure WSL 2 is enabled
- **Git** (optional, for cloning if needed)

## Project Structure

The containerization setup includes:

```
transaction-gateway/
├── Dockerfile                 # Multi-stage build for Spring Boot app
├── docker-compose.yml         # Orchestrates all services
├── pom.xml                    # Maven configuration with all dependencies
├── src/
│   └── main/resources/
│       └── application.properties  # Spring Boot configuration
└── DOCKER_SETUP.md           # This file
```

## What Gets Deployed?

The `docker-compose.yml` orchestrates 5 services:

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| **postgres** | postgres:16-alpine | 5432 | Database persistence |
| **redis** | redis:7-alpine | 6379 | Idempotency cache |
| **zookeeper** | confluentinc/cp-zookeeper:7.5.0 | 2181 | Kafka coordination |
| **kafka** | confluentinc/cp-kafka:7.5.0 | 9092 | Event streaming |
| **transaction-gateway** | (built from Dockerfile) | 8080 | Spring Boot app |

## Quick Start

### 1. Navigate to Project Root

```bash
cd e:\transaction-gateway
```

### 2. Build and Start All Services

```bash
docker-compose up -d
```

This will:
- Build the Spring Boot application using the multi-stage Dockerfile
- Download required images (PostgreSQL, Redis, Zookeeper, Kafka)
- Create a custom Docker network (`swiftpay-network`)
- Create persistent volumes for databases
- Start all 5 services

### 3. Verify Services Are Running

```bash
docker-compose ps
```

Expected output:
```
NAME                                 STATUS              PORTS
swiftpay-postgres                    Up (healthy)        5432/tcp
swiftpay-redis                       Up (healthy)        6379/tcp
swiftpay-zookeeper                   Up (healthy)        2181/tcp
swiftpay-kafka                       Up (healthy)        9092/tcp
swiftpay-transaction-gateway         Up (healthy)        0.0.0.0:8080->8080/tcp
```

### 4. Test the API

Wait ~30-40 seconds for the app to fully start (health check has 30s startup period).

**Create a Payment:**
```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "user1",
    "receiverId": "user2",
    "amount": 100.00,
    "currency": "USD"
  }'
```

**Get Payment Status:**
```bash
curl http://localhost:8080/v1/payments/{transactionId}
```

**List All Accounts:**
```bash
curl http://localhost:8080/v1/accounts
```

### 5. Check Logs

View application logs:
```bash
docker-compose logs -f transaction-gateway
```

View specific service logs:
```bash
docker-compose logs -f postgres      # PostgreSQL logs
docker-compose logs -f kafka         # Kafka logs
docker-compose logs -f redis         # Redis logs
```

### 6. Stop Services

```bash
docker-compose down
```

To also remove volumes and data:
```bash
docker-compose down -v
```

## Dockerfile Explanation

The Dockerfile uses a **multi-stage build** pattern:

### Stage 1: Builder
```dockerfile
FROM eclipse-temurin:21-jdk AS builder
# Compiles the Spring Boot application using Maven
# Runs: ./mvnw.cmd package -DskipTests
# Output: target/*.jar
```

### Stage 2: Runtime
```dockerfile
FROM eclipse-temurin:21-jre
# Only includes JRE (not JDK) for smaller image size
# Copies JAR from builder stage
# Result: ~200MB runtime image (vs ~700MB with full JDK)
```

**Benefits:**
- Faster builds after first build (dependency cache)
- Smaller runtime images (50-60% size reduction)
- Security: No build tools in production image

## docker-compose.yml Key Features

### Service Dependencies & Health Checks
```yaml
depends_on:
  postgres:
    condition: service_healthy  # Wait for postgres health check
  kafka:
    condition: service_healthy  # Wait for kafka health check
```

This ensures services start in the correct order and are ready before dependent services start.

### Environment Variables
The app receives database/Kafka configs via environment variables:
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/swiftpay
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  SPRING_DATA_REDIS_HOST: redis
```

**Important:** Container-to-container communication uses internal hostnames:
- `postgres:5432` (NOT localhost:5432)
- `kafka:29092` (internal), `localhost:9092` (external)
- `redis:6379` (NOT localhost:6379)

### Volumes & Persistence
```yaml
volumes:
  postgres_data:/var/lib/postgresql/data     # Database files persist
  redis_data:/data                           # Redis snapshots persist
  kafka_data:/var/lib/kafka/data             # Kafka topics persist
```

When you run `docker-compose down -v`, these are deleted. Without `-v`, they're retained.

### Custom Docker Network
```yaml
networks:
  swiftpay-network:
    driver: bridge
```

All services connect to this internal network automatically, enabling secure communication.

## Troubleshooting

### Issue: Port Already in Use

If port 8080, 5432, 9092, etc. are already in use:

Edit `docker-compose.yml` and change the port mapping:
```yaml
ports:
  - "9080:8080"  # Change external port from 8080 to 9080
```

Then access the app at `http://localhost:9080`

### Issue: Application Won't Start

Check logs:
```bash
docker-compose logs transaction-gateway
```

Common causes:
- PostgreSQL not ready: Wait 10-15 seconds and check `docker-compose ps`
- Kafka not ready: Wait 20-30 seconds
- Port conflicts: Verify ports aren't already in use

### Issue: "Connection refused" to Database

The app tries to connect to `postgres:5432`. Make sure:
1. PostgreSQL container is running: `docker-compose ps`
2. Application is using service hostname `postgres`, not `localhost`
3. Check `application.properties`: `spring.datasource.url=jdbc:postgresql://postgres:5432/swiftpay`

### Issue: Kafka Topics Not Auto-Creating

Ensure `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"` in docker-compose.yml (it's already there).

### Rebuild After Code Changes

If you modify Java code:
```bash
docker-compose down
docker-compose up -d --build
```

The `--build` flag rebuilds the image from the Dockerfile.

## Accessing Services Directly

### PostgreSQL
```bash
docker exec -it swiftpay-postgres psql -U postgres -d swiftpay

# Inside psql:
\dt                    # List tables
SELECT * FROM transactions;
SELECT * FROM accounts;
```

### Redis
```bash
docker exec -it swiftpay-redis redis-cli

# Inside redis-cli:
KEYS payment:*        # List all idempotency keys
GET payment:xxx       # Get specific key
```

### Kafka
```bash
docker exec -it swiftpay-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic payment-initiated \
  --from-beginning
```

## Production Considerations

### Security
- Change PostgreSQL password: `POSTGRES_PASSWORD: 2208` → secure value
- Use `.env` file for secrets (don't commit to git)
- Enable SSL/TLS for Kafka connections
- Use private Docker registries

### Performance
- Increase JVM heap: Add `JAVA_OPTS: "-Xmx512m"` to app environment
- Configure Kafka partitions based on throughput needs
- Set PostgreSQL `shared_buffers` based on available RAM
- Use read replicas for high-traffic scenarios

### Monitoring
- Add Prometheus for metrics: `spring-boot-starter-actuator` (already included)
- Add ELK Stack (Elasticsearch, Logstash, Kibana) for logging
- Configure Docker logging drivers: `json-file`, `awslogs`, etc.

### Scaling
```bash
# Scale transaction-gateway to 3 replicas
docker-compose up -d --scale transaction-gateway=3
```

(Requires load balancer like Nginx in front)

## Next Steps

1. **Local Testing:** Run `docker-compose up -d` and test all endpoints
2. **CI/CD Integration:** Push Dockerfile and docker-compose.yml to version control
3. **Container Registry:** Push built images to Docker Hub or private registry
4. **Kubernetes:** Use these manifests as basis for Kubernetes Deployments
5. **Environment Configs:** Create separate compose files for dev/staging/production

## Useful Commands

```bash
# View real-time logs
docker-compose logs -f

# Execute command in container
docker exec swiftpay-transaction-gateway ls -la

# Restart a service
docker-compose restart transaction-gateway

# Rebuild a specific service
docker-compose build transaction-gateway

# Remove everything including volumes
docker-compose down -v

# Check network
docker network inspect swiftpay_swiftpay-network
```

## References

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Kafka Docker Images](https://hub.docker.com/r/confluentinc/cp-kafka)

---

**Questions?** Check the logs with `docker-compose logs -f` for detailed error messages.
