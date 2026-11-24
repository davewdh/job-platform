# Event-Driven Job Processing Platform

A local operations dashboard for submitting background jobs, watching retry attempts, and recovering failures. Spring Boot manages the job lifecycle, PostgreSQL stores state and attempt history, Kafka delivers work and dead-letter events, and React receives live updates over STOMP.

## Run the full stack

With Docker and Docker Compose running, start from the repository root.

```sh
docker compose -f docker/docker-compose.yml up --build
```

Open the dashboard at [localhost:3000](http://localhost:3000). If port 3000 is already in use, prefix the Compose command with `JOB_FRONTEND_PORT=3004` and open that port instead. The API is available at [localhost:8080/jobs](http://localhost:8080/jobs). The containers expose ports on loopback and use development database credentials. Stop the stack with `docker compose -f docker/docker-compose.yml down`. PostgreSQL data persists in the Compose volume.

## Try the demo

Click **Run all examples** to submit these scenarios together.

| Scenario | Expected result |
| --- | --- |
| Succeed immediately | One successful attempt |
| Fail once, then succeed | One failed attempt, followed by success after a one-second backoff |
| Exhaust retries | Four failed attempts, with one-, two-, and four-second backoffs, then a Kafka dead-letter event |

Select a job to inspect its attempt timeline. Filter for failed jobs and open the dead-lettered example. Choose **Succeed immediately** as its next scenario and click **Requeue job**. The second run adds a successful attempt while keeping the first run’s history.

PostgreSQL stores the next eligible attempt and the last dispatch time. A scheduler publishes due work to Kafka and waits for acknowledgement. The worker checks the job ID, run, and attempt under a row lock, so a duplicate delivery or a stale message from an earlier run cannot add another completed attempt. The dashboard refreshes when the connection returns and polls while disconnected.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/jobs?name=Report&scenario=FAIL_ONCE` | Submit a job |
| GET | `/jobs` | List the 200 most recent jobs |
| GET | `/jobs/{id}` | Get a job and its attempts |
| POST | `/jobs/generate-test` | Submit the three examples |
| POST | `/jobs/{id}/reprocess-dlq?scenario=SUCCESS` | Requeue a dead-lettered job |

## Development and checks

Use Java 17, Maven, and Node.js 24 if developing outside the application containers. To run the API and frontend locally, start only the infrastructure with `docker compose -f docker/docker-compose.yml up -d postgres kafka`. Run `mvn spring-boot:run` in `backend`, then run `npm ci` and `REACT_APP_API_URL=http://localhost:8080 npm start` in `frontend`.

- State and backoff tests: `cd backend && mvn -Dtest=JobTest test`.
- Dashboard build: `cd frontend && npm ci && npm run build`.
