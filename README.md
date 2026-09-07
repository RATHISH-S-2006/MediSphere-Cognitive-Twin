# MediSphere Cognitive Twin

MediSphere is a healthcare digital twin platform that ingests synthetic patient, lab, consent, FHIR, and wearable data, then exposes a digital patient profile plus an AI risk-prediction layer.

## Stack

- Backend: Java 25, Spring Boot 4, Spring Security, Spring Data MongoDB
- Integration: Kafka, HAPI FHIR R4
- Frontend: Angular 20
- Local runtime: Docker Compose
- ML runtime: Python 3.9 in Docker, TensorFlow CPU 2.9.1, TensorFlow Federated 0.33.0, SHAP 0.46.0, scikit-learn 1.5.2, pandas 2.2.2, NumPy 1.23.5

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

- ML service in Docker with Python 3.9 runtime
- Cardiovascular and diabetes risk models trained from synthetic data
- SHAP feature explanations
- Federated learning simulation using TensorFlow Federated FedAvg
- Spring Boot endpoint integration using `ML_SERVICE_URL`

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

The ML service runs in a dedicated Linux container using Python 3.9. This avoids host-level Python incompatibilities and keeps the Java + Angular stack unchanged.

Dockerfile summary:

- Python: `3.9-slim`
- TensorFlow CPU: `2.9.1`
- TensorFlow Federated: `0.33.0`
- SHAP: `0.46.0`
- scikit-learn: `1.5.2`
- pandas: `2.2.2`
- NumPy: `1.23.5`

## ML endpoints

- `GET /health` -> `{"status":"UP","service":"medisphere-ml"}`
- `POST /predict` -> actual inference with modelType and feature payload
- `GET /model-evaluation` -> model metrics
- `GET /federated-demo` -> actual federated simulation rounds and global model output

## Risk API examples

```bash
curl -X POST http://localhost:8001/predict \
  -H "Content-Type: application/json" \
  -d '{"patientId":"patient-1","modelType":"CARDIOVASCULAR","features":{"age":52,"sex":1,"systolicBloodPressure":138,"diastolicBloodPressure":88,"heartRate":76,"smokingStatus":1,"diabetesStatus":0,"bmi":28.4,"totalCholesterol":210}}'
```

```bash
curl -X POST http://localhost:8001/predict \
  -H "Content-Type: application/json" \
  -d '{"patientId":"patient-2","modelType":"DIABETES","features":{"age":58,"sex":0,"bmi":31.6,"systolicBloodPressure":142,"diastolicBloodPressure":90,"glucose":132,"hba1c":7.1,"diabetesDuration":4}}'
```

## Federated training

The ML service includes a real federated simulation using TensorFlow Federated FedAvg. It creates multiple synthetic hospital clients, partitions the data, trains locally, aggregates client updates, and completes several federated rounds.

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

The backend risk endpoints continue to enforce patient access checks and active consent rules through the existing Spring Security configuration.
