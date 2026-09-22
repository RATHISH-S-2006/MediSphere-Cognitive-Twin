# MediSphere Cognitive Twin

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

## M4 Care plans, interventions, adherence, and outcomes

M4 extends Patient 360 with a persisted care-plan layer. `POST /api/care-plans/{patientId}/generate` gathers the latest stored risk predictions, active or acknowledged alerts, and available vital context. It creates deterministic demonstration goals and interventions, records generation reasons, and stores links to the contributing risk and alert IDs. An existing active plan is returned instead of creating an identical duplicate.

Care-plan status transitions are explicitly validated: `DRAFT -> ACTIVE`, `ACTIVE -> PAUSED|COMPLETED|CANCELLED`, and `PAUSED -> ACTIVE`. Intervention completion or missed status is recorded once per intervention and calendar date, and the plan summary calculates completed, missed, pending, and `completed / (completed + missed) * 100` adherence. Outcomes are persisted against a plan and goal and are shown as measured engineering/demo observations.

M4 endpoints include:

- `GET /api/care-plans/{patientId}` and `/active`
- `POST /api/care-plans/{patientId}/generate`
- `GET /api/care-plans/by-id/{id}`
- `POST /api/care-plans/by-id/{id}/activate|pause|complete|cancel`
- `POST /api/care-plans/by-id/{id}/interventions/{interventionId}/complete|miss`
- `GET /api/care-plans/by-id/{id}/adherence`
- `POST` and `GET /api/care-plans/by-id/{id}/outcomes`

Patient-level access and active consent checks reuse the existing Spring Security, `PatientAccessChecker`, and `ConsentService` architecture. Care-plan creation, lifecycle changes, intervention actions, and outcomes are recorded through `AuditService`. Angular uses the typed `CarePlanService` and renders the M4 section inside the existing Patient 360 page.

The care-plan rules are engineering/demo logic and are not medical advice or validated clinical guidelines. The system never invents medication names, doses, or treatment instructions; medication review requires clinician review. Outcomes are not clinically meaningful unless supported by real measurements. A dedicated internal-to-FHIR CarePlan mapping is still a future interoperability enhancement; M4 currently persists the internal model without replacing the existing M1 FHIR resources.

## Run Locally

From the repository root:

```bash
docker compose up -d --build
```

Services:

- Angular development server: `cd frontend && npm install && npm start` at `http://localhost:4200`
- Backend: `http://localhost:8082`
- HAPI FHIR: `http://localhost:8081/fhir`
- MongoDB: `localhost:27017`
- Kafka: `localhost:9092`
- ML service: `http://localhost:8001/health`

## ML service runtime

The ML service runs in a dedicated Linux container using Python 3.11. This keeps the ML dependencies isolated from the Java and Angular development environments.

Dockerfile summary:

- Python: `3.11-slim`
- TensorFlow: `2.14.0`
- TensorFlow Federated: `0.87.0`
- SHAP: `0.46.0`
- scikit-learn: `1.5.2`
- pandas: `2.2.2`
- NumPy: `1.25.2`
- JAX/JAXLIB: `0.4.14`

The dependency contract is pinned in `ml-service/requirements.txt`.

## ML endpoints

- `GET /health` -> runtime health
- `POST /predict` -> model-backed inference with model type and feature payload
- `GET /model-evaluation` -> evaluation metrics on the synthetic demonstration dataset
- `GET /federated-demo` -> TensorFlow Federated FedAvg rounds and global model update metrics

## Risk API examples

```bash
curl -X POST http://localhost:8001/predict \
  -H "Content-Type: application/json" \
  -d '{"patientId":"patient-1","modelType":"CARDIOVASCULAR","features":{"age":52,"sex":1,"systolicBloodPressure":138,"diastolicBloodPressure":88,"heartRate":76,"smokingStatus":1,"diabetesStatus":0,"bmi":28.4,"totalCholesterol":210}'
```

```bash
curl -X POST http://localhost:8001/predict \
  -H "Content-Type: application/json" \
  -d '{"patientId":"patient-2","modelType":"DIABETES","features":{"age":58,"sex":0,"bmi":31.6,"systolicBloodPressure":142,"diastolicBloodPressure":90,"glucose":132,"hba1c":7.1,"diabetesDuration":4}'
```

## Federated training

The ML service uses TensorFlow Federated's unweighted FedAvg algorithm. The demo creates three synthetic hospital clients, keeps each client's raw data local to its client dataset, trains a shared Keras model locally, aggregates client updates through TFF, and completes three federated rounds.

## Verify backend tests

```bash
cd backend
./mvnw test
```

Windows:

```powershell
cd backend
mvnw.cmd test
```

## Verify ML tests

```bash
cd ml-service
python -m pytest -q
```

## Verify frontend build

```bash
cd frontend
npm install
npm run build
```

## Verify Docker health

```bash
docker compose config
docker compose build --no-cache ml-service
docker compose up -d
docker compose ps
curl http://localhost:8001/health
```

## Security and authorization

The backend risk endpoints enforce patient access checks and active consent rules through the existing Spring Security configuration. Angular communicates with the Spring Boot API; it does not call the ML service directly.

Care-plan endpoints use the same authorization and consent policy. Patients can view authorized plans and record their own intervention adherence; providers, clinicians, and administrators can generate plans and manage plan lifecycle or outcomes according to the existing role policy.

## Data and clinical-use note

M2 model training and evaluation use synthetic demonstration data. Evaluation metrics are engineering/demo metrics only and must not be interpreted as clinical performance evidence.

M3 thresholds and M4 care-plan generation rules are configurable engineering/demo behavior. This project does not establish HIPAA or clinical compliance. Real-world deployment requires clinician governance, validated models and guidelines, production privacy controls, and independent safety review.
