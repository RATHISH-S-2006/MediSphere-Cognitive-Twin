# 🏥 MediSphere Cognitive Twin

MediSphere Cognitive Twin is an AI-powered healthcare platform that creates a **Digital Health Twin** for each patient by connecting clinical/FHIR data, laboratory results, and wearable vitals. The platform extends that patient-centric data foundation with explainable risk prediction, real-time monitoring and alerts, and persistent care-plan workflows.

> **Project status:** M1, M2, M3 and M4 are completed and validated as an engineering/demo project.

## Core Flow

```text
Collect Data
    ↓
Create Digital Twin
    ↓
Predict Risk
    ↓
Monitor Patient
    ↓
Generate / Manage Alerts
    ↓
Create Care Plan
    ↓
Track Adherence & Outcomes
```

The platform is organized into four milestones:

- **M1 — FHIR Integration & Digital Twin**
- **M2 — AI Risk Prediction & Federated Learning**
- **M3 — Continuous Monitoring & Alerts**
- **M4 — Care Plans, Adherence & Outcomes**

---

# 🏗️ Architecture

```text
                     Angular 20 Frontend
                           │
                           │ REST / JWT
                           ▼
                  Spring Boot 4 Backend
                           │
          ┌────────────────┼─────────────────┐
          │                │                 │
          ▼                ▼                 ▼
       MongoDB           Kafka          HAPI FHIR R4
          │                │                 │
          │                ▼                 │
          │          Wearable Vitals         │
          │                │                 │
          └────────────┬───┴─────────────────┘
                       │
                       ▼
                 Patient Digital Twin
                       │
             ┌─────────┴─────────┐
             │                   │
             ▼                   ▼
        Risk Prediction     Monitoring
             │                   │
             ▼                   ▼
          SHAP / TFF           Alerts
             │                   │
             └─────────┬─────────┘
                       ▼
                  Care Plans
                       │
                Goals / Actions
                       │
                Adherence / Outcomes
```

### Architectural principles

- Angular is the presentation layer.
- Spring Boot is the system-of-record boundary for application workflows.
- MongoDB persists patient-centric and workflow data.
- Kafka handles real-time wearable events.
- HAPI FHIR provides healthcare interoperability.
- The ML service is isolated from the Angular frontend.
- Security, access control, consent and audit enforcement remain on the backend.

---

# 🧰 Technology Stack

## Backend

- Java 25
- Spring Boot 4
- Spring Security
- Spring Data MongoDB
- Maven

## Frontend

- Angular 20
- TypeScript
- HTML5 / SCSS

## Data & Integration

- MongoDB
- Apache Kafka
- HAPI FHIR R4
- REST APIs

## Machine Learning

- Python 3.11
- FastAPI
- TensorFlow 2.14.0
- TensorFlow Federated 0.87.0
- SHAP 0.46.0
- scikit-learn 1.5.2
- pandas 2.2.2
- NumPy 1.25.2
- JAX / JAXLIB 0.4.14

## Infrastructure

- Docker
- Docker Compose

---

# 1️⃣ M1 — FHIR Integration & Digital Twin

M1 establishes the core patient data platform.

### Implemented

- Patient directory and Patient 360 workspace
- MongoDB-backed Patient and HealthTwin persistence
- Digital Twin completeness calculation
- Historical wearable vitals
- Laboratory-result persistence
- HAPI FHIR R4 integration
- FHIR resource validation and synchronization
- Readable FHIR resource inspection
- Development wearable simulator
- Kafka vitals ingestion
- Idempotency and duplicate-event handling
- Kafka retry and dead-letter handling
- Consent management
- Patient-level authorization
- JWT-based authentication
- RBAC
- Audit logging
- Docker Compose infrastructure

### Example M1 data model

```text
Patient
 ├── HealthTwin
 ├── Vitals
 ├── Lab Results
 ├── FHIR Resources
 ├── Consent
 └── Audit Events
```

### M1 validation

The M1 runtime validation included seeded patients, Digital Twins, FHIR resources, consent records, vitals and lab results. Backend tests, Angular tests, production build, Docker Compose validation and security paths were verified.

---

# 2️⃣ M2 — AI Risk Prediction & Federated Learning

M2 adds an ML service for two risk domains:

- Cardiovascular risk
- Diabetes complications

### ML architecture

```text
Patient / Twin Data
       ↓
Spring Boot Feature Mapping
       ↓
ML Service
       ↓
Risk Model
       ├── Score
       ├── Category
       ├── Model Version
       └── SHAP Explanation
       ↓
Spring Boot
       ↓
MongoDB Risk Prediction
       ↓
Angular Patient 360
```

### Implemented

- Cardiovascular risk prediction
- Diabetes-complication risk prediction
- SHAP feature explanations
- Model evaluation endpoint
- Prediction persistence
- Prediction history
- Input-feature snapshots
- Strict ML response validation
- Configurable ML connection/read timeouts
- Controlled 503 handling for ML failures
- Spring Boot → ML service integration
- Angular risk display
- Patient access and consent enforcement

### Federated Learning

The project uses **TensorFlow Federated FedAvg** for the federated-learning demonstration.

Validation confirmed:

- Multiple clients
- Multiple training rounds
- Real TFF FedAvg execution
- Non-zero global model-weight updates

### Important limitation

M2 uses **synthetic demonstration data**. Model metrics are engineering/demo metrics and must not be interpreted as clinical performance evidence.

---

# 3️⃣ M3 — Continuous Monitoring & Alerts

M3 connects the existing Kafka vitals pipeline to configurable monitoring rules.

### Flow

```text
Wearable Simulator
      ↓
Kafka: medisphere.vitals
      ↓
VitalsConsumer
      ↓
Monitoring Rules
      ↓
Alert Generation
      ↓
MongoDB alerts
      ↓
Angular / Patient 360
```

### Monitoring rules

The default engineering/demo rules cover:

- Heart rate
- SpO2
- Temperature
- Systolic blood pressure
- Diastolic blood pressure

Each rule contains:

- Alert type
- Vital type
- Severity
- Comparison operator
- Threshold
- Enabled flag

> These are engineering/demo thresholds and are **not medically validated clinical or treatment thresholds**.

### Alert lifecycle

```text
ACTIVE
  ↓
ACKNOWLEDGED
  ↓
RESOLVED
```

Invalid transitions are rejected by the backend.

### Duplicate suppression

Repeated events violating the same rule for the same patient do not create uncontrolled duplicate active alerts.

Different violated rules create separate alerts.

### Implemented

- Kafka vitals monitoring
- MongoDB-backed monitoring configuration
- Alert generation
- Explainable alert messages
- Severity classification
- Duplicate suppression
- Alert lifecycle management
- Protected alert REST APIs
- Patient access enforcement
- Consent enforcement
- Audit integration
- Deterministic NORMAL / ABNORMAL wearable simulator profiles
- Patient 360 active-alert and alert-history views

### Alert APIs

```text
GET  /api/alerts/{patientId}
GET  /api/alerts/{patientId}/active
GET  /api/alerts/{patientId}/history
GET  /api/alerts/by-id/{id}

POST /api/alerts/by-id/{id}/acknowledge
POST /api/alerts/by-id/{id}/resolve
```

### Controlled monitoring demo

The development simulator supports deterministic profiles.

NORMAL:

- Produces valid readings
- Does not generate threshold violations

ABNORMAL:

- Produces deterministic abnormal readings
- Example validation profile:
  - Heart rate: 135
  - SpO2: 89
  - Temperature: 39.1
  - Systolic BP: 155
  - Diastolic BP: 98

This makes M3 demonstrations repeatable without introducing random clinical behavior.

---

# 4️⃣ M4 — Care Plans, Adherence & Outcomes

M4 completes the patient workflow by connecting risk and monitoring signals to persistent care-plan management.

## M4 objective

Create a traceable care-management workflow that can use:

- Patient information
- Digital Twin state
- Recent vitals
- Laboratory results
- M2 risk predictions
- M3 alerts

to generate and manage a deterministic **demo care plan**.

> M4 care-plan generation uses engineering/demo rules. It is **not clinical treatment advice**.

---

## M4 Data Model

```text
CarePlan
 ├── Goals
 ├── Interventions
 ├── Adherence Records
 ├── Outcomes
 ├── Risk Prediction References
 └── Alert References
```

### CarePlan persistence includes

- ID
- Patient ID
- Title
- Description
- Status
- Priority
- Created / updated timestamps
- Generated timestamp
- Source
- Generation reasons
- Linked risk prediction IDs
- Linked alert IDs
- Goals
- Interventions
- Adherence summary
- Outcome information
- Version

---

## Care-plan generation

The `CarePlanGenerationService` uses existing patient context instead of generating random plans.

Generation considers available:

- Patient data
- Digital Twin information
- Recent vitals
- Laboratory data
- Risk predictions
- Alerts

The generated plan records why it was created and links back to the M2/M3 records that contributed to the workflow.

Duplicate active care plans are prevented.

---

## Care-plan lifecycle

Supported lifecycle:

```text
DRAFT
  ↓
ACTIVE
  ↓
PAUSED
  ↓
ACTIVE
  ↓
COMPLETED
```

Alternative terminal path:

```text
ACTIVE
  ↓
CANCELLED
```

Invalid lifecycle transitions are rejected by the backend.

Care-plan actions persist actor and timestamp information through the audit workflow.

---

## Goals

Goals support:

- Target
- Progress
- Status

The frontend presents goal progress from persisted data rather than inventing values.

---

## Interventions

Supported intervention types include:

- MONITORING
- FOLLOW_UP
- LIFESTYLE
- MEDICATION_REVIEW
- LAB_CHECK
- CLINICIAN_REVIEW
- EDUCATION

The project does not generate medication names or dosages.

Medication-related actions remain clinician-review oriented.

---

## Adherence tracking

Intervention adherence supports:

- PENDING
- COMPLETED
- MISSED
- SKIPPED

The system records actual intervention actions.

Adherence percentage is calculated from recorded completion/missed events:

```text
Adherence % =
completed / (completed + missed) × 100
```

No fabricated adherence values are generated.

Example validation:

```text
Completed = 1
Missed = 1

Adherence = 1 / (1 + 1) × 100
          = 50%
```

Adherence updates are rejected after a care plan reaches a terminal state such as COMPLETED or CANCELLED.

Unknown adherence actions are rejected rather than silently interpreted.

---

## Outcomes

Outcomes record:

- Measured value
- Unit
- Recorded timestamp
- Status

Outcome status is derived from configured goal comparisons for the demonstration workflow.

The system does not fabricate patient outcomes.

---

## M4 REST workflow

Care-plan APIs are protected by the existing:

- Authentication
- RBAC
- PatientAccessChecker
- Consent enforcement
- Audit logging

The Angular frontend uses typed care-plan models and services to display:

- Care plans
- Goals
- Interventions
- Adherence
- Outcomes
- Lifecycle actions

---

# 🔐 Security Architecture

Security is enforced at the backend boundary.

### Authentication

JWT-based authentication is used for protected APIs.

### RBAC

The application supports role-aware access paths including:

- Patient
- Provider
- Clinician
- Admin

### Patient isolation

`PatientAccessChecker` prevents patients from accessing another patient's protected data.

### Consent

Sensitive workflows enforce active consent where required.

Revoked or expired consent results in access denial for protected operations.

### Audit

Important operations are persisted as audit events with actor and timestamp information.

Examples include:

- CARE_PLAN_CREATED
- CARE_PLAN_ACTIVATED
- CARE_PLAN_COMPLETED
- INTERVENTION_COMPLETED
- INTERVENTION_MISSED
- OUTCOME_RECORDED
- Alert creation
- Alert acknowledgement
- Alert resolution

---

# 🖥️ Frontend

The Angular application provides a clinical workspace containing:

- Dashboard
- Patients
- Patient 360
- Risk and prediction views
- Monitoring and alert views
- Care-plan views
- FHIR/clinical data views
- Consent/access views
- Audit views

The Patient 360 workspace connects the complete workflow:

```text
Patient
  ↓
Digital Twin
  ├── Vitals
  ├── Labs
  ├── FHIR
  └── Consent
        ↓
      M2 Risk
        ↓
      M3 Alerts
        ↓
      M4 Care Plan
        ├── Goals
        ├── Interventions
        ├── Adherence
        └── Outcomes
```

Angular does not call the ML service directly. The Spring Boot backend remains the application integration boundary.

---

# 📁 Project Structure

Typical high-level structure:

```text
MediSphere-Cognitive-Twin/
│
├── backend/
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/
│
├── frontend/
│   ├── src/app/
│   └── src/
│
├── ml-service/
│   ├── app.py
│   ├── requirements.txt
│   ├── Dockerfile
│   └── tests/
│
├── docker-compose.yml
└── README.md
```

---

# 🚀 Running Locally

From the repository root:

```bash
docker compose ps
```

Expected services:

```text
MongoDB
Kafka
HAPI FHIR
Backend
ML Service
```

Start the stack:

```bash
docker compose up -d
```

Check status:

```bash
docker compose ps
```

---

# 🔎 Backend Verification

Liveness:

```bash
curl.exe http://localhost:8082/api/health/live
```

Readiness:

```bash
curl.exe http://localhost:8082/api/health/ready
```

Backend:

```text
http://localhost:8082
```

---

# 🧬 FHIR Verification

HAPI FHIR runs on:

```text
http://localhost:8081
```

Verify metadata:

```bash
curl.exe http://localhost:8081/fhir/metadata
```

---

# 🤖 ML Service Verification

ML service:

```text
http://localhost:8001
```

Health:

```text
http://localhost:8001/health
```

Important endpoints:

```text
POST /predict
GET  /model-evaluation
GET  /federated-demo
```

---

# 🖥️ Start Angular

Open another terminal:

```bash
cd frontend
npm install
npm start
```

Then open:

```text
http://localhost:4200
```

Angular communicates with:

```text
http://localhost:8082
```

It does **not** directly call port 8001.

---

# 🧪 Testing

## Backend

From `backend/`:

```bash
./mvnw test
```

Windows:

```powershell
.\\mvnw.cmd test
```

The final M4 validation reached **45 passing backend tests**.

## ML Service

```bash
docker compose exec ml-service pytest
```

The ML test suite included federated-learning validation.

## Angular

From `frontend/`:

```bash
npm test
```

Production build:

```bash
npm run build
```

---

# 🐳 Useful Docker Commands

```bash
# Status
docker compose ps

# Start
docker compose up -d

# Stop
docker compose down

# All logs
docker compose logs

# Backend logs
docker compose logs backend

# ML logs
docker compose logs ml-service

# Kafka logs
docker compose logs kafka

# Follow backend logs
docker compose logs -f backend

# Rebuild backend
docker compose build --no-cache backend

# Start rebuilt backend
docker compose up -d backend
```

---

# 🎬 Recommended Demonstration Flow

For a complete M1–M4 presentation/demo:

### 1. Dashboard

Show:

- Patient count
- Digital Twin count
- Consent status
- FHIR resources
- Recent vitals
- Data completeness

### 2. Patient 360

Open a seeded patient such as:

```text
patient-1
```

Show:

- Patient identity
- Digital Twin
- Vitals
- Labs
- FHIR resources
- Consent

### 3. M2 Risk Prediction

Run:

- Cardiovascular prediction
- Diabetes prediction

Show:

- Risk score
- Category
- Model version
- Timestamp
- SHAP explanations
- Prediction history

### 4. M3 Monitoring

Use the deterministic wearable simulator.

Demonstrate:

```text
Wearable
   ↓
Kafka
   ↓
VitalsConsumer
   ↓
Monitoring Rule
   ↓
Alert
```

Then:

```text
ACTIVE
  ↓
ACKNOWLEDGED
  ↓
RESOLVED
```

### 5. M4 Care Plan

Generate a care plan and demonstrate:

- Generation reason
- Risk references
- Alert references
- Goals
- Interventions
- Lifecycle
- Adherence
- Outcome

### 6. Security

Demonstrate:

- Unauthenticated request → 401
- Cross-patient access → 403
- Authorized provider/clinician access
- Revoked consent → 403

---

# 🧩 Major Engineering Challenges & Solutions

## TensorFlow Federated compatibility

The initial ML dependency combination caused compatibility issues between TensorFlow Federated and JAX.

### Solution

A tested dependency contract was established:

- Python 3.11
- TensorFlow 2.14.0
- TensorFlow Federated 0.87.0
- JAX/JAXLIB 0.4.14
- SHAP 0.46.0

The implementation was updated to the TFF 0.87 API and validated with real FedAvg execution.

---

## Stale Docker images

New M3/M4 backend routes were initially unavailable because the running container still contained an older backend image.

### Solution

Rebuilt and recreated the affected Docker service and verified the actual runtime endpoints.

---

## Alert timestamp defect

Runtime verification discovered that newly created alerts could have a null `createdAt`.

### Solution

Alert creation explicitly records `Instant.now()`, and regression coverage was added.

---

## M4 state-validation defects

Runtime/review identified two correctness issues:

- Unknown adherence actions were being accepted.
- Adherence updates were possible after terminal care-plan states.

### Solution

The controller now rejects unknown adherence actions, and the service rejects adherence updates after COMPLETED/CANCELLED states.

---

## Frontend integration

The initial frontend was centered around Patient 360. The UI was later expanded into a consistent clinical workspace with operational module navigation and standalone views while retaining Patient 360 as the detailed patient-level workflow.

---

# 📊 Validation Summary

| Area | Validation |
|---|---|
| M1 Backend | ✅ |
| M1 FHIR | ✅ |
| M1 Kafka | ✅ |
| M1 Security | ✅ |
| M2 Risk Prediction | ✅ |
| M2 SHAP | ✅ |
| M2 TensorFlow Federated | ✅ |
| M2 Persistence | ✅ |
| M3 Kafka Monitoring | ✅ |
| M3 Alert Generation | ✅ |
| M3 Duplicate Suppression | ✅ |
| M3 Alert Lifecycle | ✅ |
| M4 Care Plan Generation | ✅ |
| M4 Lifecycle Validation | ✅ |
| M4 Adherence | ✅ |
| M4 Outcomes | ✅ |
| M4 Risk/Alert Linking | ✅ |
| M4 Security/Consent | ✅ |
| M4 Audit | ✅ |
| Angular Build | ✅ |
| Docker Compose | ✅ |
| Git diff validation | ✅ |

---

# ⚠️ Limitations

This project is an engineering/demo implementation and is **not a clinically validated medical system**.

Important limitations:

1. M2 models use synthetic demonstration data.
2. Model evaluation results must not be interpreted as clinical performance.
3. M3 monitoring thresholds are engineering/demo thresholds and are not medically validated.
4. M4 care-plan generation uses deterministic demonstration rules and is not clinical treatment advice.
5. FHIR CarePlan resource mapping is not implemented.
6. Patient outcome entry is currently demonstrated through the implemented backend workflow rather than a dedicated clinical outcome-entry workflow.
7. The project should undergo clinical, security, privacy, regulatory and production validation before real patient use.
8. The existing Angular Patient 360 stylesheet budget warning is a build warning and has not been hidden by artificially increasing the budget.

---

# 🏁 Milestone Status

```text
M1  FHIR + Digital Twin
    ✅ COMPLETED

M2  AI Risk + SHAP + Federated Learning
    ✅ COMPLETED

M3  Monitoring + Alerts
    ✅ COMPLETED

M4  Care Plans + Adherence + Outcomes
    ✅ COMPLETED
```

The final platform connects the complete engineering workflow:

```text
FHIR / Clinical Data
        ↓
Digital Health Twin
        ↓
Risk Intelligence
        ↓
Real-Time Monitoring
        ↓
Alerts
        ↓
Care Plans
        ↓
Adherence
        ↓
Outcomes
```

**MediSphere Cognitive Twin — M1 through M4 completed.**
