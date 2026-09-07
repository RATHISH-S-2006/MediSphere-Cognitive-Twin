from fastapi.testclient import TestClient

from app import app


def test_federated_demo_uses_tensorflow_federated():
    client = TestClient(app)
    response = client.get("/federated-demo")

    assert response.status_code == 200, response.text
    payload = response.json()
    assert payload["engine"] == "tensorflow-federated"
    assert payload["globalModel"] == "tff-fedavg"
    assert len(payload["history"]) >= 1
    assert all("loss" in item and "accuracy" in item for item in payload["history"])
