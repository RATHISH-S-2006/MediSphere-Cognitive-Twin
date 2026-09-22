# 🏥 MediSphere Cognitive Twin

MediSphere is a healthcare digital twin platform that ingests synthetic patient, lab, consent, FHIR, and wearable data, then exposes a digital patient profile plus an AI risk-prediction layer.

## Stack

- Backend: Java 25, Spring Boot 4, Spring Security, Spring Data MongoDB
- Integration: Kafka, HAPI FHIR R4
- Frontend: Angular 20
- Local runtime: Docker Compose
- ML runtime: Python 3.11 in Docker, TensorFlow 2.14.0, TensorFlow Federated 0.87.0, SHAP 0.46.0, scikit-learn 1.5.2, pandas 2.2.2, NumPy 1.25.2, JAX/JAXLIB 0.4.14

## M1 Features

- Patient directory and Patient 360 view
- Digital HealthTwin completeness and latest-vitals snapshot
- Separate historical vitals and lab-result collections
- FHIR validation, synchronization, readable resource inspection
- Server-side consent enforcement and patient-level authorization
- Kafka vitals ingestion, idempotency, retry, and `.DLT` handling
- Development wearable simulator and synthetic seed data
- HIPAA-oriented audit and security safeguards

## M2 Features

- ML service in Docker with Python 3.11 runtime
- Cardiovascular and diabetes risk models trained from synthetic demonstration data
- SHAP feature explanations
- Federated learning with TensorFlow Federated FedAvg
- Spring Boot endpoint integration using `ML_SERVICE_URL`
- MongoDB persistence for risk predictions, feature snapshots, and history
- Angular Patient 360 risk prediction and history UI

## M3 Continuous monitoring and alerts

M3 reuses the existing `medisphere.vitals` Kafka topic and `VitalsConsumer`. After the existing patient, consent, validation, vitals persistence, and HealthTwin update steps succeed, the consumer evaluates the same `VitalEvent` against the configurable monitoring rules in `backend/src/main/resources/application.yml`. Violations are stored in MongoDB's `alerts` collection and exposed in Patient 360 through the alert API.

The default rules cover heart rate, SpO2, temperature, systolic blood pressure, and diastolic blood pressure. These are engineering/demo thresholds, not medically validated clinical or treatment thresholds. Each rule defines its alert type, vital type, severity, comparison operator, threshold, and enabled flag.

An existing `ACTIVE` alert with the same patient, alert type, and vital type is retained when repeated events violate that rule. Different rules create separate alerts. Lifecycle transitions are `ACTIVE -> ACKNOWLEDGED -> RESOLVED`; an active alert may also be resolved directly. Invalid transitions return a conflict response.

Alert endpoints are protected by the existing JWT roles, `PatientAccessChecker`, and active-consent enforcement:

- `GET /api/alerts/{patientId}` - patient alert records
- `GET /api/alerts/{patientId}/active` - active and acknowledged alerts
- `GET /api/alerts/{patientId}/history` - resolved alert history
- `GET /api/alerts/by-id/{id}` - one alert
- `POST /api/alerts/by-id/{id}/acknowledge` - acknowledge an active alert
- `POST /api/alerts/by-id/{id}/resolve` - resolve an active or acknowledged alert

Patient 360 polls active and resolved alert endpoints every 10 seconds and stops polling when the component is destroyed. Alert creation, acknowledgement, and resolution use the existing audit collection.

### Controlled monitoring demo

Keep the development simulator in its default `NORMAL` profile to demonstrate that valid readings create no alert. To emit deterministic abnormal readings through the existing Kafka producer and consumer, start the backend with `MEDISPHERE_WEARABLE_SIMULATOR_PROFILE=ABNORMAL` and the development profile enabled. Repeated abnormal readings create one active alert per violated rule; acknowledge and resolve it in Patient 360, then repeat the profile to demonstrate a new alert after the previous one is resolved.

M3 monitoring does not change the M2 risk models, feature extraction, SHAP explanations, or federated-learning service.

## Run Locally

From the repository root:

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

## Security and authorization

The backend risk endpoints enforce patient access checks and active consent rules through the existing Spring Security configuration. Angular communicates with the Spring Boot API; it does not call the ML service directly.

## Data and clinical-use note

M2 model training and evaluation use synthetic demonstration data. Evaluation metrics are engineering/demo metrics only and must not be interpreted as clinical performance evidence.
