# Event-Driven Job Processing Platform

## Overview
This project is an **Event-Driven Job Processing Platform** designed to simulate a scalable, enterprise-level backend system.  
It demonstrates the use of **Kafka message queues, Spring Boot backend, and PostgreSQL database** to handle asynchronous job processing in a reliable and maintainable way.

Core features include:
- Submit jobs via REST API
- Event-driven job processing using Kafka and Worker services
- Job status tracking (PENDING, RUNNING, SUCCEEDED, FAILED)
- Support for retry mechanism and error handling
- Modular, extensible backend structure suitable for enterprise projects

---

Jobs are retried automatically via Kafka with exponential backoff.
Failed jobs are sent to a DLQ and can be manually reprocessed through a REST API.