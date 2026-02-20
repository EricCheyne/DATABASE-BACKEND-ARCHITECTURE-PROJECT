# Scalable Task Processing System

A production-ready, high-throughput asynchronous task processing system built with **Java 21**, **Spring Boot 3**, **Kafka**, **PostgreSQL**, and **Redis**.

This system is designed to handle high-volume task submission and execution with strict reliability, horizontal scalability, and comprehensive observability.

## 🏗 Architecture Overview

The system follows a distributed architecture consisting of two primary microservices:

1.  **Task Service (API)**: A stateless RESTful service that accepts task submissions, validates payloads, persists task state to PostgreSQL, and publishes events to Kafka.
2.  **Task Worker (Consumer)**: A scalable background worker that consumes tasks from Kafka, executes business logic, manages retries, and updates task status.

### Tech Stack
-   **Backend**: Spring Boot 3.2, Java 21 (Virtual Threads ready)
-   **Messaging**: Apache Kafka (Distributed event streaming)
-   **Database**: PostgreSQL (Partitioned, Read/Write separation)
-   **Caching/Locking**: Redis (Distributed locking, Rate limiting)
-   **Migration**: Flyway (Versioned schema evolution)
-   **Observability**: Prometheus, Micrometer, SLF4J + MDC (Correlation IDs), JSON Logging
-   **Deployment**: Docker & Docker Compose

---

## 🚀 Key Design Decisions

### 1. High-Throughput Database Strategy
-   **Table Partitioning**: The `tasks` table is range-partitioned by `created_at` (monthly). This enables efficient data retention management (dropping old partitions instead of expensive `DELETE` operations) and maintains small, performant local indexes.
-   **Read/Write Separation**: Implemented a `TransactionRoutingDataSource` that automatically routes `@Transactional(readOnly = true)` queries to a database replica, offloading the primary node.
-   **Partial Indexing**: Created a partial index on the `status` column specifically for active tasks (`PENDING`, `PROCESSING`, `RETRYING`). This keeps the index size minimal and optimized for the worker's most frequent lookups.
-   **JSONB Support**: Utilizes PostgreSQL's `JSONB` for flexible task payloads with a GIN index for high-performance searching within the JSON data.

### 2. Distributed Reliability & Idempotency
-   **Distributed Locking**: To prevent race conditions during Kafka rebalances or accidental double-processing, the Worker uses **Redis-based locking** (via Redisson) to ensure only one worker processes a specific task at a time.
-   **At-Least-Once Delivery**: Configured Kafka with `acks=all` and idempotent producers to ensure zero data loss during submission.
-   **Retry Strategy**: Implemented a sophisticated retry mechanism using `@RetryableTopic` with exponential backoff.
-   **Dead Letter Topic (DLT)**: Tasks that fail after 4 attempts are automatically moved to a `tasks-dlt` topic for manual inspection and recovery.

### 3. Distributed Rate Limiting
-   **Redis + Bucket4j**: Implemented a distributed rate limiter on the `POST /api/tasks` endpoint. It uses the Token Bucket algorithm backed by Redis to enforce consistent limits across multiple API instances.

---

## 📈 Scaling Approach

-   **Stateless API Scaling**: The Task Service stores no local state. It can be scaled horizontally to $N$ replicas behind a standard L7 load balancer (Nginx/ALB).
-   **Worker Autoscaling**: Task Workers are part of a Kafka Consumer Group. Scaling is driven by **Kafka Consumer Lag**—as the backlog grows, more worker pods can be added up to the number of partitions in the Kafka topic.
-   **Kafka Partitioning**: The system is designed for "over-partitioning" (e.g., 32+ partitions) to allow for significant horizontal growth without requiring topic re-creation. Key-based routing (using `taskId`) ensures per-task ordering.

---

## 🔍 Observability & Monitoring

-   **Distributed Tracing**: Implemented **Correlation IDs** that propagate from the initial HTTP request, through Kafka headers, into the background worker's logs via SLF4J MDC.
-   **Structured Logging**: Configured Logback to output JSON logs (optional), making it ready for ELK/Splunk ingestion.
-   **Metrics**: Custom Micrometer metrics track:
    -   `task_processing_time`: Latency distribution of task execution.
    -   `task_retry_count`: Frequency of retries per task type.
    -   `task_failures_total`: Aggregated failure counts tagged by error type.
-   **Prometheus**: All services expose an `/actuator/prometheus` endpoint for scraping.

---

## 🛠 How to Run Locally

### Prerequisites
-   Docker and Docker Compose

### Startup
1.  Clone the repository.
2.  Run the entire stack:
    ```bash
    docker-compose up -d
    ```
3.  The API will be available at `http://localhost:8080`.
4.  Prometheus dashboard: `http://localhost:9090`.

### Testing the System
**Create a Task:**
```bash
curl -X POST http://localhost:8080/api/tasks \
     -H "Content-Type: application/json" \
     -H "X-Correlation-Id: test-123" \
     -d '{"payload": "{\"action\": \"process_data\"}"}'
```

---

## 🔮 Future Improvements
-   **Outbox Pattern**: Implement the Transactional Outbox pattern to ensure atomic DB writes and Kafka publishes.
-   **Circuit Breakers**: Add Resilience4j to the API's Kafka producer to handle transient broker failures gracefully.
-   **K8s Operator**: Develop a custom Kubernetes operator to autoscale workers based on real-time Kafka lag metrics.

---

## 📄 Resume Bullet Points (Senior Backend Engineer)
-   Designed and implemented a high-throughput asynchronous task processing system handling $N$ tasks/sec using **Spring Boot**, **Kafka**, and **PostgreSQL**.
-   Optimized database performance by implementing **PostgreSQL range partitioning** and **partial indexing**, reducing query latency by $X$% for active task lookups.
-   Engineered a distributed reliability layer using **Redis-based idempotent processing** and **exponential backoff retries** with Dead Letter Topics (DLT).
-   Implemented a distributed **rate-limiting** solution using **Bucket4j and Redis** to protect downstream resources from traffic spikes.
-   Enhanced system observability by integrating **Prometheus** metrics and implementing cross-service **correlation ID propagation** for distributed tracing.
