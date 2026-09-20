# 🏥 MediSphere Cognitive Twin

> **An AI-powered healthcare platform that creates a Digital Health Twin for each patient, predicts health risks, continuously monitors vital signs, generates explainable alerts, and provides a foundation for personalized care planning.**

## 📌 What is MediSphere Cognitive Twin?

MediSphere Cognitive Twin is a healthcare technology platform designed to maintain a continuously updated **Digital Health Twin** of a patient.

It brings together:

- 🏥 Hospital/EHR data
- 🧪 Laboratory results
- ⌚ Wearable vital signs
- 🔗 FHIR R4 resources
- 🧠 AI-based risk predictions
- 🚨 Real-time monitoring and alerts
- 🔐 Consent, authorization, and audit information

### Core flow

```text
Patient Data
     ↓
Digital Health Twin
     ↓
AI Risk Prediction
     ↓
Continuous Monitoring
     ↓
Clinical Alerts
     ↓
Personalized Care Plan
     ↓
Intervention & Outcome Tracking
```

The current implementation covers **Milestones 1–3**. Milestone 4 is planned.

---

# 🎯 Project Objectives

1. **Create a unified Digital Twin** that represents the patient's current health state.
2. **Integrate healthcare data** using FHIR R4 and MongoDB.
3. **Predict health risks** for cardiovascular and diabetes-related conditions.
4. **Explain AI predictions** using SHAP.
5. **Demonstrate federated learning** using real TensorFlow Federated execution.
6. **Continuously monitor vitals** through Apache Kafka.
7. **Generate and manage alerts** when configurable monitoring rules are violated.
8. **Build toward personalized care plans**, adherence, and outcome tracking.

---

# 🧩 Implemented Features

## ✅ Milestone 1 — FHIR Integration & Digital Twin

### FHIR R4 Integration

Implemented:

- HAPI FHIR R4 integration
- FHIR metadata access
- FHIR resource validation
- FHIR synchronization
- Patient-specific FHIR resource retrieval
- FHIR DTO mapping
- Patient 360 FHIR display

### Digital Health Twin

Each patient has a Health Twin stored in MongoDB containing information such as:

- Demographics
- Latest vitals
- Laboratory data
- FHIR resources
- Data completeness
- Consent state

The twin is synchronized as new data arrives.

### Patient 360

The Angular dashboard provides:

- Patient demographics
- Health Twin
- Latest vitals
- Labs
- FHIR resources
- Completeness
- Consent
- AI risk predictions
- SHAP explanations
- Risk history
- Alerts

### Security

Supported roles:

- `PATIENT`
- `PROVIDER`
- `CLINICIAN`
- `ADMIN`

Patient-level access checking prevents one patient from accessing another patient's protected data.

Consent is enforced on protected operations.

Example:

```text
Patient 1 → Patient 1 data     → 200 OK
Patient 1 → Patient 2 data     → 403 Forbidden
Revoked consent               → 403 Forbidden
```

### Audit Logging

Important operations are recorded for traceability, including access, consent, prediction, monitoring, and alert lifecycle operations.

---

# 🤖 Milestone 2 — AI Risk Prediction

Milestone 2 adds the AI layer on top of the Digital Twin.

## ❤️ Cardiovascular Risk

```text
Patient Data
     ↓
Spring Boot Feature Extraction
     ↓
FastAPI ML Service
     ↓
Cardiovascular Model
     ↓
Risk Score + Category + SHAP
     ↓
MongoDB
     ↓
Patient 360
```

## 🩺 Diabetes Complication Risk

A separate prediction flow is implemented for diabetes-related complication risk.

```text
Patient Data
     ↓
Feature Mapping
     ↓
FastAPI ML Service
     ↓
Diabetes Model
     ↓
Risk Score + Category + SHAP
     ↓
MongoDB
     ↓
Patient 360
```

## 🧠 SHAP Explainability

Predictions include SHAP-based feature contributions.

For example:

```text
Feature A     +36.11
Feature B      -4.82
Feature C     +53.49
Feature D      -8.22
```

Conceptually:

- Positive contribution → pushes the model output upward.
- Negative contribution → pushes the model output downward.

The frontend displays these explanations alongside the risk result.

## 🌐 ML Service

The ML service uses:

- Python
- FastAPI
- TensorFlow
- TensorFlow Federated
- SHAP
- scikit-learn
- pandas
- NumPy

Angular does **not** directly call the ML service.

```text
Angular
   ↓
Spring Boot
   ↓
ML Service
   ↓
Spring Boot
   ↓
MongoDB
   ↓
Angular
```

This keeps authorization, validation, persistence, and ML orchestration in the backend.

## 🌍 Federated Learning

The project contains a real TensorFlow Federated implementation using:

- TensorFlow Federated
- FedAvg
- 3 simulated clients
- 3 training rounds
- Global model aggregation

The implementation verifies non-zero global model weight changes across training rounds.

This is a federated-learning engineering demonstration using simulated clients, not real hospital deployments.

## 📈 Model Evaluation

The ML service exposes model evaluation functionality.

The current demonstration dataset is explicitly identified as:

```text
synthetic_health_demo
```

Therefore its metrics are for engineering/demo validation and **must not be interpreted as clinical validation or evidence of clinical effectiveness**.

---

# 🚨 Milestone 3 — Continuous Monitoring & Alerts

Milestone 3 introduces continuous vital-sign monitoring.

```text
Wearable Simulator
       ↓
     Kafka
       ↓
VitalsConsumer
       ↓
MonitoringService
       ↓
Monitoring Rules
       ↓
AlertService
       ↓
MongoDB
       ↓
REST API
       ↓
Patient 360
```

## 📡 Kafka Vital Streaming

The existing Kafka topic is:

```text
medisphere.vitals
```

Supported vital information includes:

- Heart rate
- SpO₂
- Temperature
- Blood pressure

Vitals are consumed, persisted, and used to update monitoring state.

## ⚙️ Configurable Monitoring Rules

Rules support:

- Vital type
- Comparison operator
- Threshold
- Severity
- Rule identification
- Enable/disable configuration

Example:

```text
Heart Rate > 120
        ↓
ABNORMAL
        ↓
HIGH severity alert
```

The current thresholds are **engineering/demo thresholds**, not clinically validated treatment thresholds.

## 🚨 Alert Generation

When a vital violates a configured rule:

1. Kafka delivers the event.
2. `VitalsConsumer` processes it.
3. Monitoring rules are evaluated.
4. `AlertService` creates the alert.
5. MongoDB persists it.
6. REST APIs expose it.
7. Patient 360 displays it.

Alert records contain information such as:

- Patient ID
- Vital type
- Observed value
- Threshold
- Severity
- Explanation
- Status
- Created timestamp
- Acknowledgement information
- Resolution information

## ♻️ Duplicate Suppression

Repeated abnormal values do not create unlimited duplicate alerts for the same active condition.

```text
HR = 135
   ↓
Alert created

HR = 136
   ↓
Same active rule
   ↓
No duplicate alert
```

## 🔄 Alert Lifecycle

```text
ACTIVE
  ↓
ACKNOWLEDGED
  ↓
RESOLVED
```

The backend validates lifecycle transitions.

## ⌚ Deterministic Wearable Simulator

The simulator provides deterministic profiles for testing.

### NORMAL

Example:

```text
Heart Rate ≈ 75
SpO₂ ≈ 98
Normal temperature
Normal blood pressure
```

Expected:

```text
Kafka event
    ↓
MongoDB vital
    ↓
No alert
```

### ABNORMAL

Example:

```text
Heart Rate = 135
SpO₂ = 89
Temperature = 39.1
Blood Pressure = 155/98
```

Expected:

```text
Kafka event
    ↓
Monitoring rules
    ↓
Multiple violations
    ↓
Alerts persisted
```

The deterministic profiles make demonstrations reproducible.

---

# 🖥️ Frontend

The frontend uses:

- Angular
- TypeScript
- SCSS
- HTTP services
- Route guards
- Authentication interceptor

Patient 360 contains:

```text
Patient Overview
├── Demographics
├── Digital Twin
├── Vitals
├── Labs
├── FHIR Resources
├── Consent
├── AI Risk
│   ├── Cardiovascular
│   ├── Diabetes
│   ├── SHAP Explanation
│   └── Risk History
└── Alerts
    ├── Active Alerts
    ├── Acknowledge
    └── Resolve
```

---

# 🏗️ Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Angular / TypeScript / SCSS |
| Backend | Java / Spring Boot 4 |
| Java | OpenJDK Temurin 25 |
| Build | Maven 3.9.16 |
| Database | MongoDB |
| Messaging | Apache Kafka |
| Healthcare interoperability | FHIR R4 |
| FHIR server | HAPI FHIR |
| AI API | Python / FastAPI |
| ML | TensorFlow |
| Federated Learning | TensorFlow Federated |
| Explainability | SHAP |
| ML utilities | scikit-learn / pandas / NumPy |
| Authentication | JWT |
| Containerization | Docker / Docker Compose |
| Version control | Git / GitHub |

---

# 🗂️ Project Structure

```text
MediSphere-Cognitive-Twin/
│
├── backend/
│   ├── src/main/java/com/medisphere/
│   │   ├── audit/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── fhir/
│   │   ├── kafka/
│   │   ├── monitoring/
│   │   ├── repository/
│   │   ├── security/
│   │   ├── service/
│   │   ├── wearable/
│   │   └── ...
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── frontend/
│   ├── src/app/
│   │   ├── core/
│   │   └── features/
│   │       └── patient-360/
│   └── package.json
│
├── ml-service/
│   ├── app.py
│   ├── requirements.txt
│   ├── Dockerfile
│   └── test_federated_demo.py
│
├── docker-compose.yml
├── README.md
└── .gitignore
```

---

# 🔌 Service Ports

| Service | Host Port | Purpose |
|---|---:|---|
| Angular | `4200` | Web application |
| Spring Boot | `8082` | Main API |
| ML Service | `8001` | AI API |
| HAPI FHIR | `8081` | FHIR server |
| MongoDB | `27017` | Database |
| Kafka | `9092` | Vital streaming |

---

# 🚀 Running the Project

## 1. Prerequisites

Install:

- Git
- Docker Desktop
- Node.js + npm
- Java 25
- Maven is optional because Maven Wrapper is included

Verify:

```bash
java --version
javac --version
docker --version
docker compose version
node --version
npm --version
```

---

## 2. Clone

```bash
git clone https://github.com/RATHISH-S-2006/MediSphere-Cognitive-Twin.git
cd MediSphere-Cognitive-Twin
```

---

## 3. Start the backend stack

From the project root:

```bash
docker compose up -d
```

Check:

```bash
docker compose ps
```

The expected services are:

```text
MongoDB
Kafka
HAPI FHIR
Backend
ML Service
```

---

## 4. Verify Backend

Liveness:

```bash
curl.exe http://localhost:8082/api/health/live
```

Readiness:

```bash
curl.exe http://localhost:8082/api/health/ready
```

Expected responses indicate that the service is alive and ready.

---

## 5. Verify FHIR

```bash
curl.exe http://localhost:8081/fhir/metadata
```

A successful FHIR metadata response confirms HAPI FHIR is running.

---

## 6. Verify ML Service

Open:

```text
http://localhost:8001/health
```

Important ML endpoints include:

```text
/predict
/model-evaluation
/federated-demo
```

---

## 7. Start Angular

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

It does not directly call port `8001`.

---

# 🧪 Testing

## Backend

From `backend/`:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

## ML Service

```bash
docker compose exec ml-service pytest
```

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

# Stop everything
docker compose down

# Rebuild a service
docker compose build --no-cache backend

# Start a rebuilt service
docker compose up -d backend
```

---

# 🔍 Recommended Demonstration Flow

### 1. Open Patient 360

```text
http://localhost:4200
```

Open a seeded patient such as:

```text
patient-1
```

### 2. Show the Digital Twin

Show:

- Patient information
- Health Twin
- Vitals
- Labs
- FHIR resources
- Consent

### 3. Show AI Risk Prediction

Show:

- Cardiovascular risk
- Diabetes risk
- Risk score
- Category
- Model version
- SHAP explanation
- Risk history

### 4. Demonstrate Normal Monitoring

Run the NORMAL wearable profile.

Expected:

```text
Vital → Kafka → MongoDB
```

with no alert.

### 5. Demonstrate Abnormal Monitoring

Run the ABNORMAL profile.

Expected:

```text
Vital → Kafka → Monitoring → Alert → MongoDB → Patient 360
```

### 6. Demonstrate Alert Lifecycle

```text
ACTIVE → ACKNOWLEDGED → RESOLVED
```

### 7. Demonstrate Security

Show that:

```text
patient-1 → patient-2
```

returns:

```text
403 Forbidden
```

Also demonstrate consent enforcement by revoking and restoring consent where appropriate.

---

# 🔐 Security Architecture

The Spring Boot backend is the main security boundary.

```text
                    ┌───────────────┐
                    │    Angular    │
                    └───────┬───────┘
                            │
                       JWT / HTTP
                            │
                            ▼
                  ┌───────────────────┐
                  │   Spring Boot     │
                  │ Security Boundary │
                  └─────────┬─────────┘
                            │
          ┌─────────────────┼──────────────────┐
          │                 │                  │
          ▼                 ▼                  ▼
      MongoDB          ML Service          Kafka
```

Backend responsibilities include:

- Authentication
- Role authorization
- Patient access checks
- Consent checks
- Request validation
- ML response validation
- Persistence
- Audit logging

---

# 🗄️ MongoDB Persistence

Application state includes:

- Patients
- Health Twins
- Vitals
- Lab results
- FHIR resources
- Consents
- Audit events
- Risk predictions
- Monitoring rules
- Alerts

Risk records preserve:

- Patient ID
- Model type
- Score
- Category
- Model version
- Timestamp
- SHAP values
- Input feature snapshot
- Prediction source

Alert records preserve:

- Patient
- Rule
- Vital type
- Observed value
- Threshold
- Severity
- Explanation
- Status
- Created timestamp
- Acknowledgement details
- Resolution details

---

# 🧪 Validation Summary

The project has been validated at multiple layers.

### M1

- Backend tests
- Angular tests
- Production build
- Docker Compose validation
- FHIR connectivity
- MongoDB persistence
- Kafka connectivity
- Authorization and consent checks

### M2

- Backend tests
- ML tests
- Angular tests
- Production build
- Real TensorFlow Federated execution
- Spring Boot → ML integration
- Risk persistence
- SHAP response validation
- Authorization/consent validation
- End-to-end prediction flow

### M3

- Monitoring service tests
- Alert service tests
- Kafka consumer tests
- Angular tests
- Production build
- Kafka → MongoDB validation
- Normal profile → no alert
- Abnormal profile → alert generation
- Duplicate suppression
- Alert lifecycle
- Timestamp persistence
- Authorization validation
- M1/M2 regression checks

---

# 📊 Milestone Status

| Milestone | Area | Status |
|---|---|---|
| M1 | FHIR Integration & Digital Twin | ✅ Complete |
| M2 | AI Risk Prediction & Federated Learning | ✅ Complete |
| M3 | Continuous Monitoring & Alerts | ✅ Complete |
| M4 | Care Plan & Treatment | 🔄 Planned |

---

# 🛣️ Milestone 4 — Planned

The final milestone extends the system from detection to intervention:

```text
Risk + Alerts
     ↓
Personalized Care Plan
     ↓
Recommended Interventions
     ↓
Adherence Tracking
     ↓
Outcome Tracking
```

Planned capabilities:

- Personalized care plans
- Guideline-driven recommendations
- Intervention tracking
- Patient adherence
- Care-plan status
- Outcome recording
- Care-plan history

Clinical recommendations should use appropriately sourced and verified clinical guidance rather than invented medical rules.

---

# ⚠️ Current Limitations

MediSphere is currently an engineering/research prototype and is **not a production clinical system**.

Important limitations:

- ML evaluation uses synthetic demonstration data.
- Federated learning uses simulated clients rather than real hospitals.
- Monitoring thresholds are engineering/demo values and are not clinically validated.
- Development JWT configuration is intended for local development.
- Milestone 4 care-plan/intervention functionality is not yet implemented.
- Production deployment would require additional security hardening, secrets management, observability, compliance controls, infrastructure, and clinical validation.

---

# 🧠 Architecture Principles

### 1. Backend is the system-of-record boundary

Frontend clients do not directly access internal services.