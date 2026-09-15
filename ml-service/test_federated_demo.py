from fastapi.testclient import TestClient

from app import app


CARDIO_FEATURES = {
    "age": 52,
    "sex": 1,
    "systolicBloodPressure": 138,
    "diastolicBloodPressure": 88,
    "heartRate": 76,
    "smokingStatus": 1,
    "diabetesStatus": 0,
    "bmi": 28.4,
    "totalCholesterol": 210,
}

DIABETES_FEATURES = {
    "age": 58,
    "sex": 0,
    "bmi": 31.6,
    "systolicBloodPressure": 142,
    "diastolicBloodPressure": 90,
    "glucose": 132,
    "hba1c": 7.1,
    "diabetesDuration": 4,
}


def test_health_and_predictions_are_model_backed_and_deterministic():
    client = TestClient(app)

    assert client.get("/health").status_code == 200
    for model_type, features in (("CARDIOVASCULAR", CARDIO_FEATURES), ("DIABETES", DIABETES_FEATURES)):
        request = {"patientId": "test-patient", "modelType": model_type, "features": features}
        first = client.post("/predict", json=request)
        second = client.post("/predict", json=request)

        assert first.status_code == 200, first.text
        assert second.status_code == 200, second.text
        first_payload = first.json()
        second_payload = second.json()
        assert 0.0 <= first_payload["riskScore"] <= 1.0
        assert first_payload["riskCategory"] == (
            "HIGH" if first_payload["riskScore"] >= 0.7 else
            "MODERATE" if first_payload["riskScore"] >= 0.4 else "LOW"
        )
        assert first_payload["riskScore"] == second_payload["riskScore"]
        assert first_payload["explanations"] == second_payload["explanations"]
        assert first_payload["explanations"]
        assert any(item["shapValue"] != 0 for item in first_payload["explanations"])


def test_model_evaluation_reports_real_metrics_for_both_models():
    response = TestClient(app).get("/model-evaluation")

    assert response.status_code == 200, response.text
    payload = response.json()
    assert set(payload) == {"cardiovascular", "diabetes"}
    for evaluation in payload.values():
        assert evaluation["dataset"] == "synthetic_health_demo"
        assert evaluation["evaluationSamples"] > 0
        for metric in ("accuracy", "precision", "recall", "f1", "roc_auc"):
            assert 0.0 <= evaluation[metric] <= 1.0


def test_federated_demo_uses_tensorflow_federated():
    client = TestClient(app)
    response = client.get("/federated-demo")

    assert response.status_code == 200, response.text
    payload = response.json()
    assert payload["engine"] == "tensorflow-federated"
    assert payload["globalModel"] == "tff-fedavg"
    assert len(payload["history"]) >= 1
    assert all("loss" in item and "accuracy" in item for item in payload["history"])
