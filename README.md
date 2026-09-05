# MediSphere Cognitive Twin

MediSphere is an M1 healthcare platform that synchronizes synthetic patient, vitals, laboratory, consent, and FHIR R4 data into a digital HealthTwin. The Angular Patient 360 workspace reads the live backend APIs.

## Stack

- Backend: Java 25, Spring Boot 4, Spring Security, Spring Data MongoDB
- Integration: Kafka, HAPI FHIR R4
- Frontend: Angular 20
- Local runtime: Docker Compose

## M1 Features

- Patient directory and Patient 360 view
- Digital HealthTwin completeness and latest-vitals snapshot
- Separate historical vitals and lab-result collections
- FHIR validation, synchronization, readable resource inspection
- Server-side consent enforcement and patient-level authorization
- Kafka vitals ingestion, idempotency, retry, and `.DLT` handling
- Development wearable simulator and synthetic seed data
- HIPAA-oriented audit and security safeguards

M2 prediction, M3 anomaly/alert, and M4 care-plan functionality is intentionally out of scope.

## Run Locally

From the repository root:

```text
docker compose up --build -d
```

Services:

- Angular development server: `cd frontend && npm install && npm start` at `http://localhost:4200`
- Backend: `http://localhost:8082`
- HAPI FHIR: `http://localhost:8081/fhir`
- MongoDB: `localhost:27017`
- Kafka: `localhost:9092`

The development profile seeds synthetic records for three patients. Seed execution is idempotent for patients, twins, FHIR resources, labs, and consent initialization. The wearable simulator is enabled only by the `dev` profile.

Development bearer tokens use the local decoder format:

```text
ADMIN-admin-1
PROVIDER-provider-1
PATIENT-patient-1
```

The Angular app uses `ADMIN-admin-1` by default and attaches it through its HTTP interceptor. Backend authorization remains authoritative.

## API Overview

- `GET /api/health/live`, `/api/health/ready`
- `GET /api/patients`, `/api/patients/{patientId}`
- `GET /api/twins/{patientId}`, `/api/twins/{patientId}/completeness`
- `GET /api/vitals/{patientId}`, `/api/labs/{patientId}`
- `GET /api/fhir/{patientId}`, `POST /api/fhir/validate`
- `GET /api/consents/{patientId}`, `/api/consents/{patientId}/verify`

## M1 Demo

1. Start Docker Compose.
2. Open `http://localhost:4200`.
3. Review the live Dashboard and open Patients.
4. Select a patient to inspect demographics, twin completeness, vitals, labs, FHIR resources, and consent.
5. Verify backend health and persistence through the API or MongoDB.

Backend tests run with `cd backend && ./mvnw test` (Windows: `mvnw.cmd test`). The frontend production build runs with `cd frontend && npm run build`.
