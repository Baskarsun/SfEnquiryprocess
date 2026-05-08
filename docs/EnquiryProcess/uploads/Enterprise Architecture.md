**Enterprise Architecture Document **
**(Core Modernization – Leasing Function)**

# Introduction
System: Core Modernization – Leasing Function

## Purpose
This document outlines the solution architecture for the **Leasing Business Line**. It is a "greenfield" design, establishing the definitive, modern technology pattern for the enterprise.
The architecture is built on a containerized, on-premises **Kubernetes** platform. It uses **WSO2/Kong** for North-South API management and **Dapr** as a service mesh sidecar to abstract all East-West (service-to-service) communication and pub/sub mechanics. **Domain-Driven Design (DDD)** principles are used to define microservice boundaries (Bounded Contexts), and **Clean Architecture** is the mandated internal pattern for all **Spring Boot** applications

## Goals
The modernization aims to deliver a unified customer experience, operational efficiency, high automation in credit decisions, enterprise agility, full on-prem control and data-driven insights (Customer 360).
Business Drivers:
Unified Customer Experience (“One-SF”)
Operational Efficiency (reduce onboarding from days to minutes)
Enterprise Agility (launch new verticals in <3 months)
Full On-Premises Control
Data-Driven Insights (Customer 360)

High Level System View Diagram:

## Quality Goals
**Performance****:** p99 latency targets (Kong <50ms; Pricing/CRM <200ms; Dapr invoke <25ms).
**Availability & Resilience****:** 99.99% uptime; RPO=0; RTO<5m.
**Scalability****:** 2,000 concurrent users; 500 originations/hour; horizontal scaling via HPA.
**Security & Compliance****:** AES-256 at rest, TLS1.2+/mTLS, OIDC; immutable audit within 1s.
**Maintainabilit****y:** Clean Architecture enforcement; standardized templates; operators for stateful services.
**Testability & Observability:** CI/CD, GitOps, Prometheus/Grafana/Jaeger; comprehensive test strategy (unit → chaos).

## Top Risks
On-prem Kubernetes complexity
Dapr learning curve
DDD boundary drift
Performance bottlenecks
Storage/network failure
Saga compensation failure
LLM input corruption
Mitigations are documented in the source Risks section (operators, templates, guild governance, proactive load tests, HA configs, human-in-loop for terminal states, ACL validations).

## Measures of Success
Reduce onboarding time from 2–3 days to <10 minutes (to be validated).
Launch new product vertical in <3 months.
Zero data loss for core systems (RPO=0); recovery <5 minutes (RTO<5m).
Day-1 developer onboarding success via Golden Path template.

# Architecture Constraints
## Technical Constraints
On-premises deployment only: All infrastructure and data must reside within the organization’s own data centers, physically located in India.
Kubernetes as orchestrator: All microservices and supporting components are containerized and orchestrated using Kubernetes (K8s), with strict separation of environments (dev, staging, prod, DR).
API Management: North–South traffic is managed via Kong/WSO2 API Gateway, enforcing TLS 1.2+ for all external connections.
Service Mesh: East–West (service-to-service) traffic is abstracted and secured using Dapr sidecars, with mandatory mTLS for all internal communication.
Operators for stateful services: Strimzi (Kafka) and CloudNativePG (PostgreSQL) operators are mandated for managing stateful workloads, ensuring high availability and automated failover.
Clean Architecture: All Spring Boot microservices must implement Clean Architecture (Ports & Adapters), strictly separating business logic from infrastructure.
API-first: All communication must use well-defined, versioned APIs (OpenAPI for synchronous, AsyncAPI for asynchronous).
Event-driven choreography: Asynchronous event bus (Kafka) is the default communication pattern; synchronous calls are permitted only by exception.
## Organizational Constraints
Data residency: All customer and business data must remain within India, complying with RBI and Digital Lending Guidelines.
Compliance and auditability: Immutable audit logs and regulatory controls are required for all business events, enforced by the Regulatory & Compliance Engine.
Security policies: All personal and sensitive data must be encrypted at rest (AES-256 in Vault) and in transit (TLS/mTLS). Authentication is via OIDC/JWT.
Disaster recovery: Warm standby DR site is required, with RPO=0 and RTO<5 minutes for critical systems.
## Standardization Constraints
Open-source preference: All major components (K8s, Kong, Dapr, Kafka, PostgreSQL, Ceph/Rook, Keycloak) are open source, with enterprise support recommended for production.
GitOps: All infrastructure and application deployments must follow GitOps principles, using ArgoCD/GitLab for automated promotion and rollback.
CI/CD: All code and configuration changes must pass through automated pipelines (GitLab CI, SonarQube, Trivy, etc.) before deployment.
Common starter templates: All new microservices must be forked from the standard “Golden Path” repository, enforcing package structure and dependencies.
## Domain and Business Constraints
Unified customer experience: All products and services must integrate into a single onboarding and servicing flow (“One-SF”).
Product governance: Servicing logic must be product-type agnostic, governed by configuration rather than code.
Full sovereignty: No external cloud or SaaS dependencies for core business logic or data storage.

# System Scope and Context
## Business Scope
Leasing Product Lifecycle: Covers the complete end-to-end process for leasing products, including origination, servicing, settlements, and closure.
Enterprise Components: Includes all foundational and reusable components required for leasing, such as customer onboarding, product governance, payments, credit assessment, compliance, reporting, and notifications.
Exclusions: Physical hardware provisioning (servers, switches), core corporate functions (HR, Finance), and physical network setup are out of scope.
## System Context Diagram
The system context diagram illustrates the main actors, external systems, and interfaces.
Actors: Customers, Agents, Product Teams, Regulatory Authorities, Internal Operations.
External Systems: HR System, Finance GL System, Risk Department, Marketing/Product Teams, Document Management System (DMS), Regulatory & Compliance Engine, Notification Service.
Interfaces: Web portal, mobile app, agent desktop, partner APIs, external regulatory feeds, payment gateways.

## Internal Interfaces
Core Capabilities:
Pricing and product configuration
Origination and onboarding
Risk and credit assessment
Customer management
Account management
Reporting and compliance
## External Interfaces
API Gateway: Kong/WSO2 manages all north-south traffic, exposing secure APIs to external consumers.
Service Mesh: Dapr abstracts all east-west communication between microservices.
Regulatory Interfaces: Integration with RBI and other regulatory authorities for compliance, reporting, and audit.
Payment Interfaces: Secure connections to payment gateways and financial systems for settlements and reconciliations.
## Context Boundaries
Bounded Contexts:
L1 Originations (Application AR, PostgreSQL)
L2 Servicing (ServicingContract AR, Event Sourcing)
E3 Settlement (Disbursement AR, Event Sourcing)
S4 Org & HR (Employee/Branch AR, hierarchy SSoT)
Gateways and ACLs:
E1 IAM & Trust Gateway
E4 Data Mesh (CQRS projections)
Anti-Corruption Layers (ACLs) for external policy, AI/voice input, and inbound payments
## Environment Context
To ensure stability and isolation, environments will be deployed on **separate physical Kubernetes clusters**.

| Environment | Cluster | Purpose | Data |
| --- | --- | --- | --- |
| Development | dev-k8s-cluster | Individual developer/team testing. | Ephemeral, unit test data. |
| Staging | staging-k8s-cluster | Full E2E testing, UAT, load testing. | Anonymized, sanitized clone of production data. |
| Production | prod-k8s-cluster | Live customer traffic. | Live production data. |
| DR | dr-k8s-cluster | Cold/Warm standby for disaster recovery. | Replicated from Production. |

# Solution Strategy
## Architectural Approach
Horizontal Shared Services: Core business capabilities (customer, payments, credit) are implemented as horizontal, product-agnostic shared services, maximizing reuse and consistency across product lines.
Domain-Driven Design (DDD): The system is decomposed into logical bounded contexts. Each microservice owns its data and logic, enforcing clear separation of concerns.
API-First: All communication is via well-defined, versioned, and secured APIs (OpenAPI for synchronous, AsyncAPI for asynchronous).
Event-Driven Choreography: Asynchronous event bus (Apache Kafka) is the default communication pattern, enabling loose coupling and resilience. Synchronous calls are the exception, used only when immediate response is required.
Clean Architecture: All Spring Boot microservices strictly implement Clean Architecture (Ports & Adapters), separating business logic from infrastructure and external dependencies.
Service Mesh and Security: Dapr sidecars are injected into every pod, abstracting service discovery, mTLS, pub/sub, and resilience. SPIFFE identities are used for zero-trust service invocation.
On-Premises Kubernetes: All components are containerised and orchestrated using Kubernetes, with strict environment separation and high availability.
GitOps and CI/CD: Infrastructure and application deployments follow GitOps principles, using ArgoCD for automated promotion and rollback. CI/CD pipelines (GitLab CI, SonarQube, Trivy) ensure code quality and security.
## Key Solution Patterns
Transactional Outbox: All services that write to a database and publish an event use the Transactional Outbox pattern, ensuring atomicity and consistency.
CQRS and Event Sourcing: Core financial ledgers use event sourcing and CQRS projections for scalability, auditability, and compliance.
Anti-Corruption Layers (ACLs): Explicit ACLs protect core contexts from unstable external inputs (AI/voice, regulatory schema changes, inbound payments).
Zero Trust Security: OIDC/JWT authentication at the edge, Dapr mTLS internally, and Vault for secrets management. All personal and sensitive data is encrypted at rest and in transit.
Observability: Prometheus, Grafana, and Jaeger are used for monitoring, alerting, and tracing. Proactive load testing and chaos engineering validate performance and resilience.
## Technology Stack

| Component | Technology | Role (On-Premise) | Rationale |
| --- | --- | --- | --- |
| Orchestrator | Kubernetes (K8s) | On-Prem Foundation | The "on-premise cloud." Manages all container lifecycle, networking, and storage. |
| API Gateway | Kong/WSO2 | North-South Traffic | The single, secure front door to the cluster. Runs on K8s, exposed via MetalLB. Handles AuthN, routing, and WAF. |
| Service Mesh | Dapr (Sidecar) | East-West Traffic | The "glue." Injected into every pod to provide service discovery, mTLS, pub/sub, and resilience via a simple HTTP API. |
| Application | Spring Boot | Business Logic | Implements the business logic of each microservice, following Clean Architecture. |
| Event Bus | Apache Kafka | Async Backbone | Durable, high-throughput event log. Managed by Strimzi operator on K8s. |
| Database | PostgreSQL | Persistence | Relational data store for all services. Managed by CloudNativePG operator on K8s. |
| Load Balancer | MetalLB | K8s Network Add-on | Provides LoadBalancer IPs from our on-prem network for Kong. |
| Storage | Ceph / Rook | K8s Storage Add-on | Provides the StorageClass for persistent volumes (PVs) needed by Kafka and PostgreSQL. |

## Compliance and Regulatory Strategy
Data Residency: All data remains on-premises, within India, complying with RBI and Digital Lending Guidelines.
Auditability: Immutable audit logs for all business events, enforced by the Regulatory & Compliance Engine.
Disaster Recovery: Warm standby DR site with RPO=0 and RTO<5 minutes for critical systems.
## Rollout and Implementation Strategy
Phased, Value-Driven Rollout:
Phase 1: Foundation & Core Services (K8s, Kong, Dapr, Strimzi, CloudNativePG, Prometheus/Grafana/ELK, Keycloak)
Phase 2: Customer Vertical (Customer Onboarding, Regulatory & Compliance, Notification)
Phase 3: Money Vertical (Product & Pricing, Payments & Collections, Document Management)
Phase 4: Leasing MVP (Leasing Core, Credit & Risk Engine, CRM Service)
Phase 5: Lending Fast-Follow (Loan product, reusing all enterprise components)

# Building Block View
## Overview of Building Blocks
The system is structured into several bounded contexts and core components, each implemented as autonomous Spring Boot microservices. These building blocks are grouped by business function and technical role, ensuring clear separation of concerns and maintainability.
### Main Building Blocks
L1 Origination:
Purpose: Manages lease application origination and onboarding.
Core Entity: Application Aggregate Root (AR)
Persistence: PostgreSQL
Invariant: Compliance and funding checks enforced.
L2 Servicing:
Purpose: Handles active agreements, servicing, and ledger operations.
Core Entity: ActiveAgreement AR
Persistence: Event Sourcing (Kafka/Redis)
Invariant: Ledger integrity.
E3 Payment & Accounting:
Purpose: Manages disbursements and settlements.
Core Entity: Disbursement AR
Persistence: Event Sourcing (Kafka/Redis)
Invariant: Immutability lock.
S4 Organization Core:
Purpose: Manages organisational hierarchy and employee data.
Core Entities: Employee AR, Branch AR
Persistence: PostgreSQL
Invariant: Hierarchy as single source of truth (SSoT).
Gateways & Shared Services:
E1 Security & Access Control: Handles authentication, authorisation, and cryptographic operations.
E4 Data Mesh (CQRS Projections): Provides read models and reporting.
Anti-Corruption Layers (ACLs): Isolate core systems from external volatility (AI/voice, regulatory schema, inbound payments).

## Building Block Diagram
Component View: Modular Decomposition

## Internal Structure of Building Blocks
Each bounded context is implemented as a Spring Boot microservice following Clean Architecture principles:
Domain Layer: Contains business entities, aggregates, and value objects.
Application Layer: Implements use cases and business logic, exposing ports for interaction.
Infrastructure Layer: Adapters for REST, Dapr, PostgreSQL, Kafka, and other external systems.

**Figure: Internal Service View (Clean Architecture)**

## Service Grouping Rationale
Services are grouped based on function and stability:
Transactional Group: Core transactional systems with highest autonomy, deployed and scaled independently to enforce data integrity.
Enterprise Group: Execution gateways providing high security, standardized, and horizontally scalable functions.
Portfolio Group: Master data and policy management, acting as lookup oracles.
Interface & Workflow Cores: Isolate core systems from unstable external inputs and manage human intervention points.
## Key Technical Functions
Ledger Operations: Event sourcing for financial transactions and agreements.
Security Policy Enforcement: IAM & Trust Gateway, mTLS, OIDC/JWT.
CQRS Projections: Read models for reporting and compliance.
Input Validation & Task Routing: ACLs and workflow engines (e.g., n8n).

# Runtime View
## Typical Scenarios and Flows
This section describes how the system behaves during key business processes, focusing on interactions between building blocks, services, and external actors.
### Example 1: Lease Origination Flow
Customer initiates a lease application via the web portal or mobile app.
L1 Originations service receives the application, validates input, and persists the Application AR in PostgreSQL.
Credit & Risk Engine is invoked (via Dapr) to assess creditworthiness.
Regulatory & Compliance Engine subscribes to relevant Kafka topics to ensure all events are logged immutably.
Notification Service sends updates to the customer (e.g., application received, KYC required).
Payments Service is triggered for disbursement upon approval.
### Example 2: Payment and Settlement Flow
Customer makes a payment via the portal or external gateway.
Inbound Payment ACL validates and transforms the payment data.
L2 Servicing updates the ServicingContract AR using event sourcing (Kafka/Redis).
E3 Payment & Accounting ensures disbursement AR is updated and cryptographically verified.
Regulatory & Compliance Engine logs all state-changing events for audit.
### Example 3: Regulatory Submission
Compliance Engine aggregates business events from Kafka.
Generates regulatory reports and submits them to RBI via secure APIs.
Audit Log ensures all submissions are immutable and traceable.
## Concurrency and Coordination
Transactional (Synchronous): Aggregate Roots (ARs) within a single microservice enforce serializable consistency using pessimistic locking or event sequencing.
Compensatory (Asynchronous Saga): n8n-based orchestration coordinates state changes across multiple services, managing eventual consistency and defining compensation tasks for rollback failures.

**Diagram: Cross-Context Process Flow (High-Level Choreography)**
## Runtime Diagrams
Process View: Concurrency and Coordination

This diagram shows a typical lease origination flow, including synchronous and asynchronous interactions.
## Error Handling and Resilience
Saga Compensation: For multi-service transactions, compensation tasks are defined to handle failures and ensure eventual consistency.
Chaos Engineering: LitmusChaos is used to validate high availability and resilience claims in staging, simulating failures and measuring recovery times.
Observability: Prometheus, Grafana, and Jaeger provide real-time monitoring, alerting, and tracing of runtime behaviour.

# Deployment View
## Deployment Architecture Overview
On-Premises Kubernetes Clusters:
All microservices and supporting components are deployed as containers orchestrated by Kubernetes (K8s). Separate clusters are maintained for development, staging, production, and disaster recovery (DR) environments.
Pod Structure:
Each microservice runs as a Kubernetes Pod, which includes:
The Spring Boot application container
A Dapr sidecar container for service mesh, mTLS, pub/sub, and secrets management
Stateful Services:
Kafka: Managed by Strimzi operator for high availability and automated failover
PostgreSQL: Managed by CloudNativePG operator for HA, backups, and streaming replication
Storage: Ceph/Rook provides persistent volumes for Kafka and PostgreSQL

## Deployment Diagram
Kubernetes/Dapr Pattern:

This diagram illustrates the deployment of microservices as Kubernetes Pods, each with a Dapr sidecar for service mesh and security.
## Environment Management

| Environment | Cluster | Purpose | Data |
| --- | --- | --- | --- |
| Development | dev-k8s-cluster | Individual developer/team testing. | Ephemeral, unit test data. |
| Staging | staging-k8s-cluster | Full E2E testing, UAT, load testing. | Anonymized, sanitized clone of production data. |
| Production | prod-k8s-cluster | Live customer traffic. | Live production data. |
| DR | dr-k8s-cluster | Cold/Warm standby for disaster recovery. | Replicated from Production. |

## Deployment Infrastructure Details
Orchestrator: Kubernetes v1.28+ deployed on-premises (e.g., via kubeadm)
Networking: Calico for CNI/network policies, MetalLB for load balancer IPs
Storage: Rook/Ceph for persistent volumes
Stateful Operators:
Strimzi for Kafka clusters
CloudNativePG for PostgreSQL clusters
Application Deployment:
K8s Deployment and Service for each microservice
Dapr operator injects daprd sidecar via annotations
API Gateway Deployment:
Kong in DB-less mode, configuration stored in K8s CRDs via Kong Ingress Controller
kong-proxy service of type LoadBalancer, picking up IP from MetalLB

## Promotion & GitOps Strategy
GitOps Model:
Application repo holds Spring Boot source code
Config repo holds K8s YAML for all environments
CI pipeline builds, tests, and pushes Docker images to on-prem Harbor registry
CD pipeline (ArgoCD) detects config changes and applies rolling updates to clusters

## Physical Deployment Details
Server & Rack Layout:
Primary Rack: 1x K8s control plane node, 4x worker nodes (stateless), 2x storage nodes (stateful)
Secondary Rack: 2x K8s control plane nodes (for quorum), 4x worker nodes, 2x storage nodes
Ensures loss of an entire rack does not bring down control plane or storage cluster
Server Specifications:
Control Plane: 3 nodes, 16 cores, 64GB RAM, 2x 500GB SSD (RAID 1), 2x 10GbE
Worker (Stateless): 8 nodes, 32 cores, 128GB RAM, 2x 500GB SSD (RAID 1), 2x 10GbE
Storage (Stateful): 4 nodes, 24 cores, 96GB RAM, 10x 2TB NVMe SSD, 2x 25GbE

# Cross-cutting Concepts
## Security
Zero Trust Model:
OIDC/JWT authentication at the API gateway (Kong/WSO2).
Dapr mTLS for all internal (east-west) service-to-service traffic.
SPIFFE identities for service invocation.
HashiCorp Vault for secrets management, integrated via Dapr Secret Store.
AES-256 encryption for all personal and sensitive data at rest.
TLS 1.2+ for all external (north-south) traffic.

Threat Modelling:
STRIDE analysis performed on key flows (e.g., lease origination, payment, regulatory submission).
Mitigations include strict access controls, immutable audit logs, rate limiting, and service-to-service authentication.

| Threat | Description | Mitigation |
| --- | --- | --- |
| Spoofing | A malicious user spoofs a request as another customer. | Kong + IAM (Keycloak): Kong validates the OIDC JWT on every request at the edge. The Leasing Core reads the ecid from the token, not from the request body. |
| Tampering | An attacker on the network intercepts and modifies the credit application (e.g., changes income) between the Leasing Core and the Credit Service. | Dapr mTLS: All East-West (service-to-service) traffic is automatically encrypted and authenticated. Tampering is not possible. |
| Repudiation | A customer claims they never signed the lease agreement. | DMS Svc + RCE Svc: The DMS Svc uses a certified E-Sign provider. The document.signed event, with the e-Sign provider's transaction ID, is captured in the ImmutableAuditLog by the RCE Svc. |
| Information Disclosure | The CRM Svc (for agents) is compromised, and an attacker dumps all customer PII. | Customer Svc (Data Segregation): The CRM Svc does not have PII. It only stores the ecid. To display PII, it must make a real-time Dapr invoke call to the Customer Svc, which has strict, auditable access controls. PII is not replicated. |
| Denial of Service (DoS) | An attacker floods the POST /quote API, which is computationally expensive. | Kong: The API Gateway will enforce strict rate-limiting (NFR-P1) on this public-facing endpoint. |
| Elevation of Privilege | An agent (User) in the CRM tries to access the Credit Svc POST /evaluate API (Service) to approve a loan. | Dapr Service-to-Service Auth: Dapr access policies will be configured. The credit-svc will only accept invoke calls from the leasing-core app-id. All calls from the crm-svc app-id will be rejected at the sidecar level. |

## Data Management
Event Sourcing:
Core financial ledgers use event sourcing for persistence and auditability.
CQRS projections provide scalable read models for reporting and compliance.
Immutable Audit:
Regulatory & Compliance Engine subscribes to all Kafka topics and writes every business event to an append-only log.
All state-changing events are captured within one second of publication.
Data Residency:
All data remains on-premises, within India, to comply with RBI and Digital Lending Guidelines.
## Observability
Monitoring and Alerting:
Prometheus for metrics collection.
Grafana for dashboards and visualisation.
Jaeger for distributed tracing.
Proactive load testing (K6, JMeter) and chaos engineering (LitmusChaos) validate performance and resilience.
## Scalability and Performance
Horizontal Scaling:
All Spring Boot services scale horizontally via Kubernetes HPA (based on CPU/memory) with no downtime.
Kafka bus supports 10,000 messages/sec sustained throughput.
Platform components (K8s, Dapr, Kong) and services target 99.99% uptime.
Performance Targets:
Kong Gateway: <50ms p99 latency overhead.
Pricing/CRM APIs: <200ms p99 latency.
Dapr invoke calls: <25ms p99 latency.
## Compliance and Regulatory
Auditability:
All business events are logged immutably for regulatory compliance.
Consent management and KYC workflows are standardized and auditable.
Disaster recovery (DR) plan ensures RPO=0 and RTO<5 minutes for critical systems.
## Usability and Developer Experience
Golden Path Starter Repository:
Standardized templates for microservices, enforcing Clean Architecture and best practices.
Day-1 developer onboarding: ability to run a service locally and make a successful Dapr invoke call within the first day.
Test Strategy:
Comprehensive testing at all levels: unit, component, contract, end-to-end, performance, and chaos.

# Architecture Decisions
This section documents the most important, cross-cutting architectural decisions made for the system, along with their rationale and implications.
## Key Architecture Decision Records (ADRs)
ADR-001: Horizontal (Shared) vs. Vertical (Siloed) Component Strategy
Decision: Adopt a horizontal, shared-platform strategy for core business capabilities.
Rationale: The siloed approach was rejected due to high data duplication, compliance overhead, and fractured customer experience. Shared services maximize reuse and consistency.
ADR-002: Asynchronous (Choreography) vs. Synchronous (Orchestration) Communication
Decision: Event-driven choreography via Kafka is the default for inter-service notifications. Synchronous orchestration (via Dapr Invoke) is used only when immediate response is required.
Rationale: Synchronous-only models create temporal coupling and cascading failures. Choreography is more resilient.
ADR-003: Centralised Customer Master
Decision: All customer PII and identity data is owned only by the Customer Onboarding & KYC service. Other services reference customers via the opaque EnterpriseCustomerID (ECID).
Rationale: Allowing services to cache PII is a compliance and data-staleness risk.
ADR-004: Transactional Outbox Pattern
Decision: All services that write to a database and publish an event must use the Transactional Outbox pattern.
Rationale: Dual-write offers no guarantees; Outbox ensures atomic commit and systemic consistency.
ADR-005: Choice of Dapr for Service Mesh
Decision: Use Dapr as the service mesh and abstraction layer.
Rationale: Dapr provides a developer-centric abstraction (building blocks API) that fits Clean Architecture and simplifies pub/sub, state, and secrets.
ADR-006: On-Premises Stateful Services via Operators
Decision: Mandate use of Kubernetes Operators (Strimzi for Kafka, CloudNativePG for Postgres) for all stateful services.
Rationale: Operators encode expert “day 2” knowledge (backups, failover, upgrades) into software, reducing operational burden.
ADR-007: Clean Architecture as Mandated Internal Pattern
Decision: All Spring Boot microservices must implement Clean Architecture (Ports & Adapters).
Rationale: Enforces separation of business rules from infrastructure details, preventing “fat services”.
## Decision Process
Decisions are made collaboratively by the Enterprise Architecture Guild, with input from product, compliance, risk, and operations teams.
Each ADR is documented, reviewed, and approved before implementation.
Changes to major decisions require stakeholder review and formal amendment of the ADRs.
## Implications
Consistency: Shared services and Clean Architecture ensure maintainability and scalability.
Compliance: Centralized PII management and immutable audit logs support regulatory requirements.
Resilience: Event-driven choreography and operator-managed stateful services improve fault tolerance.
Developer Experience: Dapr and standardized templates accelerate onboarding and reduce learning curve.

# Quality Requirements
## Overview
This section defines the most important quality attributes for the system, including measurable non-functional requirements (NFRs) and quality goals that drive architectural decisions.
## Performance
External API Latency:
Kong Gateway must add less than 50ms p99 latency overhead.
Pricing and CRM APIs (POST /quote, GET /cases) must respond in less than 200ms p99 latency.
All Dapr service invocation calls must complete in less than 25ms p99 latency (network plus sidecar).
Asynchronous Throughput:
Kafka bus must support 10,000 messages per second sustained throughput.
## Availability & Resilience
Platform Uptime:
All platform components (K8s, Dapr, Kong) and services must achieve 99.99% uptime.
Data Loss (Core):
Customer, Payments, Leasing (PostgreSQL): RPO = 0 (synchronous streaming replication).
Recovery Time (Core):
Customer, Payments, Leasing (PostgreSQL): RTO < 5 minutes (automated failover via CloudNativePG).
Data Loss (Events):
Kafka bus: RPO = 0 (replication factor of 3).
## Scalability
Concurrent Users:
Support 2,000 concurrent active users on web and mobile portals.
Application Throughput:
Support 500 new lease originations per hour.
Horizontal Scaling:
All Spring Boot services must scale horizontally (via K8s HPA) based on CPU/memory, with no downtime.
## Security
Data at Rest:
All personally identifiable information (PII) at rest must be encrypted (AES-256) in HashiCorp Vault.
Data in Transit:
All internal (east-west) traffic must be encrypted via Dapr mTLS. All external (north-south) traffic must be TLS 1.2 or higher.
Authentication:
All API endpoints (Kong & Dapr) must be authenticated via OIDC (JWT).
## Auditability
Immutable Log:
All state-changing business events must be captured by the Regulatory & Compliance Engine.
Log Ingestion:
Events must appear in the immutable audit log within one second of being published to Kafka.
## Usability
Developer Onboarding:
A new developer must be able to run a service locally and make a successful Dapr invoke call within their first day.
## Testability & Observability
Comprehensive Test Strategy:
Unit, component, contract, end-to-end, performance, and chaos tests are mandated.
Observability:
Prometheus, Grafana, and Jaeger must provide real-time monitoring, alerting, and tracing.
## Compliance
Regulatory Mapping:
All RBI and Digital Lending Guidelines must be mapped to architectural controls and enforced by the platform.

## Quality Attribute Scenario Table

| Attribute | Stimulus | Source | Environment | Artifact | Response | Response Measure |
| --- | --- | --- | --- | --- | --- | --- |
| Performance | Customer submits lease application | Customer | Production | Origination Service | Process request and return quote | p99 latency <200ms |
| Availability | Primary Postgres node fails | Infrastructure | Production | PostgreSQL Cluster | Failover to replica node | RTO <5 minutes |
| Scalability | Load increases to 500 originations/hour | System Load | Staging | Leasing Core | Scale horizontally via HPA | No downtime; maintain SLA |
| Security | API request from external client | External System | Production | API Gateway (Kong) | Authenticate and authorise via OIDC/JWT | 100% requests validated |
| Auditability | Business event published to Kafka | Microservice | Production | Compliance Engine | Log event immutably | Event logged within 1 second |
| Usability | New developer joins team | Developer | Development | Golden Path Template | Run service locally and invoke Dapr call | Success within Day 1 |
| Testability | CI pipeline executes component tests | CI/CD System | Staging | Microservice | Run automated tests with Testcontainers | >80% coverage; all tests pass |
| Observability | Service latency spikes | Monitoring System | Production | Prometheus/Grafana | Trigger alert and visualise metrics | Alert within 30s; dashboard updated |
| Compliance | Generate RBI report | Compliance Engine | Production | Reporting Service | Produce and submit report | Meets RBI schema; submission logged |

# Risks and Technical Debt
## Key Risks
On-premises Kubernetes Complexity:
The operational overhead and learning curve for managing on-premises Kubernetes clusters is high, especially for stateful workloads and HA configurations.
Mitigation: Phased rollout, use of operators (Strimzi, CloudNativePG), dedicated SRE team, and comprehensive documentation.
Dapr Learning Curve:
Developers and operations staff may require time to become proficient with Dapr’s abstractions and integration patterns.
Mitigation: Standardised Clean Architecture templates, “Golden Path” starter repository, and internal training.
DDD Boundary Drift:
Over time, bounded contexts may become blurred, leading to coupling and loss of clarity in domain models.
Mitigation: Architecture Guild reviews, living context map, and regular refactoring.
Performance Bottlenecks:
Unexpected latency or throughput issues may arise due to misconfiguration, resource contention, or scaling limits.
Mitigation: Observability-first approach, proactive load testing (K6, JMeter), and continuous monitoring with Prometheus/Grafana.
Storage/Network Failure:
Hardware or network failures could impact data availability or integrity.
Mitigation: High availability configurations for operators, redundant server and rack layouts, and automated failover.
Saga Compensation Failure:
Asynchronous workflows (sagas) may fail to compensate correctly in complex multi-service transactions.
Mitigation: Mandatory human intervention tasks for terminal saga failure states, robust error handling, and monitoring.
LLM Input Corruption:
AI/voice input interfaces may introduce invalid or malicious data into core systems.
Mitigation: Mandatory anti-corruption layers (ACLs) with strict schema and business rule validation.
## Technical Debt
Legacy Integration:
Some legacy systems may require temporary adapters or manual processes, increasing maintenance overhead.
Mitigation: Plan for phased decommissioning and replacement with modern interfaces.
Manual Configuration:
Initial data seeding and configuration may involve manual steps, which can be error-prone.
Mitigation: Automate as much as possible using scripts, templates, and GitOps practices.
Documentation Gaps:
Rapid development and rollout may lead to incomplete documentation, impacting onboarding and maintainability.
Mitigation: Enforce documentation as part of CI/CD pipelines and developer onboarding.

| Debt Item | Description | Impact | Mitigation |
| --- | --- | --- | --- |
| Legacy Integration | Adapters for old systems increase maintenance overhead | High | Plan phased decommissioning |
| Manual Configuration | Initial data seeding and config require manual steps | Medium | Automate via scripts and GitOps |
| Documentation Gaps | Rapid rollout may lead to incomplete docs | Medium | Enforce docs in CI/CD pipelines |
| Limited Test Coverage | Early phases may lack full test automation | Medium | Expand test strategy (unit→chaos) |
| Observability Lag | Metrics and tracing may be incomplete initially | Low | Prioritise Prometheus/Grafana/Jaeger setup |

## Risk Table

| Risk ID | Risk | Likelihood | Impact | Mitigation Strategy |
| --- | --- | --- | --- | --- |
| R-01 | On-prem K8s Complexity | High | High | Phased rollout, operators, SRE team |
| R-02 | Dapr Learning Curve | High | Medium | Clean Architecture templates, training, ps-commonkit |
| R-03 | DDD Boundary Drift | Medium | High | Architecture Guild, context map |
| R-04 | Performance Bottlenecks | Medium | High | Observability, load testing, monitoring |
| R-05 | Storage/Network Failure | Low | Critical | HA configurations, redundancy |
| R-06 | Kafka Event Ordering | Low | High | Partitioning by Agreement ID |
| R-07 | Saga Compensation Failure | Low | High | Human intervention, error handling |
| R-08 | LLM Input Corruption | High | Medium | ACL validation, schema enforcement |

## Management and Monitoring
Risks and technical debt are tracked and reviewed regularly by the Enterprise Architecture Guild and SRE teams.
Mitigation strategies are incorporated into project plans, CI/CD pipelines, and operational runbooks.
Continuous improvement is encouraged through retrospectives and stakeholder feedback.

# Glossary
This section defines the formal Ubiquitous Language (a core Domain-Driven Design concept) to be used by all teams—business, product, and engineering—to eliminate ambiguity.
## Key Terms and Definitions

| Term | Bounded Context | Definition |
| --- | --- | --- |
| Application | Leasing Core | A LeaseApplication. The digital form a customer fills out before approval. Temporary object. |
| Audit Event | Compliance | The final, immutable record of a business event, stored in the ImmutableAuditLog. |
| Case | Servicing | A single, trackable unit of work for a service agent (e.g., a "Payment Dispute" case). |
| Contract | Leasing Core | A LeaseContract. The legal, active agreement with a customer after disbursement. Long-lived. |
| Customer | Customer | The Customer Aggregate. The single, master record of a verified entity, identified by ECID. |
| Decision | Credit & Risk | The RiskEvaluation object. The formal, auditable result of a credit assessment. |
| Document | Documents (DMS) | The Document Aggregate. The digital file (e.g., PDF of a LeaseContract) and its metadata. |
| ECID | Customer | Enterprise Customer ID. The single, immutable, enterprise-wide identifier for a Customer. |
| Mandate | Payments | The Mandate Aggregate. The customer's authorization (e.g., e-NACH) for SF to collect funds. |
| Product | Products | The Product Aggregate. The definition and rules for a financial offering (e.g., "3-Year Lease"). |
| Quote | Products | The Quote Value Object. The result of a pricing calculation (EMI, fees, etc.). Ephemeral. |
| Transaction | Payments | The Transaction Aggregate. An attempt to move money (e.g., collection or disbursement). |

## Acronyms and Abbreviations

| Acronym | Definition | Context/Role |
| --- | --- | --- |
| AR | Aggregate Root | Transactional boundary enforcing a critical invariant. |
| Dapr | Distributed Application Runtime | Layer 4 framework acting as I/O adapter gateway for pub/sub, etc. |
| ES | Event Sourcing | Mandatory persistence pattern for core financial ledgers. |
| PQC | Post-Quantum Cryptography | Future-proofing; abstracted via IAM & Trust Gateway. |
| VO | Value Object | Immutable domain object (e.g., PricingSnapshot VO). |

## Additional Domain Concepts
Bounded Context: A logical boundary within which a particular domain model applies and is consistent.
Aggregate: A cluster of domain objects treated as a single unit for data changes.
Saga: A sequence of local transactions managed for distributed consistency.
CQRS: Command Query Responsibility Segregation—separates read and write models for scalability and auditability.
ACL: Anti-Corruption Layer—protects internal models from external volatility.

# Appendix A: Non-Functional Requirements (NFRs)
The architecture is designed to meet the following specific, measurable requirements.

| Category | NFR ID | Requirement | Metric |
| --- | --- | --- | --- |
| Performance | NFR-P1 | High-throughput external APIs | Kong Gateway: < 50ms p99 latency overhead. |
|  | NFR-P2 | Real-time user experience | POST /quote (Pricing) & GET /cases (CRM): < 200ms p99 latency. |
|  | NFR-P3 | Fast internal communication | All Dapr invoke calls: < 25ms p99 latency (network + sidecar). |
|  | NFR-P4 | Asynchronous throughput | Kafka Bus: Must support 10,000 messages/sec sustained. |
| Availability | NFR-A1 | High Availability (HA) | All platform components (K8s, Dapr, Kong) & services: 99.99% uptime. |
|  | NFR-A2 | Data Loss (Core) | Customer, Payments, Leasing (PostgreSQL): RPO = 0 (synchronous streaming replication). |
|  | NFR-A3 | Recovery Time (Core) | Customer, Payments, Leasing (PostgreSQL): RTO < 5 minutes (automated failover via CloudNativePG). |
|  | NFR-A4 | Data Loss (Events) | Kafka Bus: RPO = 0 (replication factor of 3). |
| Scalability | NFR-S1 | Concurrent Users | Support 2,000 concurrent active users on web/mobile portals. |
|  | NFR-S2 | Application Throughput | Support 500 new lease originations per hour. |
|  | NFR-S3 | Horizontal Scaling | All Spring Boot services must scale horizontally (via K8s HPA) based on CPU/memory with no downtime. |
| Security | NFR-S1 | Data-at-Rest | All PII data at rest must be encrypted (AES-256) in the HashiCorp Vault. |
|  | NFR-S2 | Data-in-Transit | All internal (East-West) traffic must be encrypted via Dapr mTLS. All external (North-South) traffic must be TLS 1.2+. |
|  | NFR-S3 | Authentication | All API endpoints (Kong & Dapr) must be authenticated via OIDC (JWT). |
| Auditability | NFR-A1 | Immutable Log | All state-changing business events must be captured by the Regulatory & Compliance Engine. |
|  | NFR-A2 | Log Ingestion | Events must appear in the immutable audit log within 1 second of being published to Kafka. |
| Usability | NFR-U1 | Developer Onboarding | A new developer must be able to run a service locally and make a successful Dapr invoke call within their first day. |
