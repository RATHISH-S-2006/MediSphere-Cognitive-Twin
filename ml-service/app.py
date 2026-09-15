import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List

import numpy as np
import pandas as pd
import shap
import tensorflow as tf
from fastapi import FastAPI, HTTPException

try:
    import tensorflow_federated as tff
except Exception:  # pragma: no cover - runtime guard
    tff = None
from pydantic import BaseModel
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, confusion_matrix, f1_score, precision_score, recall_score, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("medisphere-ml")

app = FastAPI(title="MediSphere ML Service", version="1.0.0")

DATA_PATH = Path(__file__).resolve().parent / "data" / "synthetic_health_demo.csv"
MODEL_PATH = Path(__file__).resolve().parent / "models"
MODEL_PATH.mkdir(exist_ok=True, parents=True)
DATA_PATH.parent.mkdir(exist_ok=True, parents=True)

APP_READY = False
MODEL_CACHE: Dict[str, dict] = {}


class PredictionRequest(BaseModel):
    patientId: str
    modelType: str = "CARDIOVASCULAR"
    features: Dict[str, float]


class ExplanationItem(BaseModel):
    feature: str
    value: float
    shapValue: float
    impact: str


class PredictionResponse(BaseModel):
    patientId: str
    modelType: str
    riskScore: float
    riskCategory: str
    modelVersion: str
    generatedAt: str
    explanations: List[ExplanationItem]


def runtime_ready() -> bool:
    global APP_READY
    try:
        import tensorflow as _tf  # noqa: F401
        import shap as _shap  # noqa: F401
        import sklearn  # noqa: F401
        import pandas as _pd  # noqa: F401
        import numpy as _np  # noqa: F401
        APP_READY = True
        return True
    except Exception as exc:  # pragma: no cover - runtime guard
        APP_READY = False
        logger.exception("ML runtime failed to initialize")
        raise RuntimeError(f"ML runtime failed to initialize: {exc}") from exc


@app.on_event("startup")
def startup_event() -> None:
    if tff is not None:
        tff.backends.native.set_sync_local_cpp_execution_context(default_num_clients=3)
    runtime_ready()


def load_demo_dataset() -> pd.DataFrame:
    rows = []
    for age in range(30, 75):
        for sex in [0, 1]:
            base = 0.12 + (age / 100.0) * 0.35 + (sex * 0.09)
            sbp = 110 + (age * 0.7) + (sex * 8)
            dbp = 70 + (age * 0.28) + (sex * 5)
            heart_rate = 68 + (age * 0.11) + (sex * 2)
            bmi = 23 + (age * 0.07) / 10
            chol = 150 + (age * 1.6)
            smoking = 1 if age > 50 and sex == 1 and (age % 5 == 0) else 0
            diabetes = 1 if age > 45 and (age % 7 == 0) else 0
            risk = int((base + sbp / 220.0 * 0.22 + smoking * 0.18 + diabetes * 0.17 + chol / 260.0 * 0.12) > 0.54)
            rows.append(
                {
                    "age": age,
                    "sex": sex,
                    "systolicBloodPressure": sbp,
                    "diastolicBloodPressure": dbp,
                    "heartRate": heart_rate,
                    "smokingStatus": smoking,
                    "diabetesStatus": diabetes,
                    "bmi": round(float(bmi), 2),
                    "totalCholesterol": round(float(chol), 2),
                    "label": risk,
                    "source": "synthetic_demo",
                }
            )
    df = pd.DataFrame(rows)
    DATA_PATH.parent.mkdir(exist_ok=True, parents=True)
    df.to_csv(DATA_PATH, index=False)
    return df


def normalize_model_type(model_type: str) -> str:
    normalized = (model_type or "CARDIOVASCULAR").strip().upper()
    if normalized in {"CARDIO", "CARDIOVASCULAR_RISK", "CARDIOVASCULAR"}:
        return "CARDIOVASCULAR"
    if normalized in {"DIABETES", "DIABETES_COMPLICATION"}:
        return "DIABETES"
    return "CARDIOVASCULAR"


def build_sklearn_model(model_type: str) -> dict:
    model_key = normalize_model_type(model_type)
    if model_key in MODEL_CACHE:
        return MODEL_CACHE[model_key]

    df = load_demo_dataset()
    if model_key == "DIABETES":
        feature_columns = ["age", "sex", "bmi", "systolicBloodPressure", "diastolicBloodPressure", "glucose", "hba1c", "diabetesDuration"]
        rows = []
        for _, row in df.iterrows():
            rows.append(
                {
                    "age": row["age"],
                    "sex": row["sex"],
                    "bmi": row["bmi"],
                    "systolicBloodPressure": row["systolicBloodPressure"],
                    "diastolicBloodPressure": row["diastolicBloodPressure"],
                    "glucose": 92 + row["diabetesStatus"] * 40 + row["age"] * 0.25,
                    "hba1c": 5.4 + row["diabetesStatus"] * 1.7 + row["age"] * 0.02,
                    "diabetesDuration": 0.5 + row["diabetesStatus"] * 4.5 + row["age"] * 0.03,
                    "label": 1 if row["diabetesStatus"] == 1 or row["age"] > 60 and row["sex"] == 1 else 0,
                }
            )
        dataset = pd.DataFrame(rows)
    else:
        feature_columns = ["age", "sex", "systolicBloodPressure", "diastolicBloodPressure", "heartRate", "smokingStatus", "diabetesStatus", "bmi", "totalCholesterol"]
        dataset = df.copy()

    X = dataset[feature_columns].copy()
    y = dataset["label"].astype(int)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.25, random_state=42, stratify=y)

    pipeline = Pipeline(
        [
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
            ("model", LogisticRegression(max_iter=2000, random_state=42)),
        ]
    )
    pipeline.fit(X_train, y_train)

    transformed_train = pipeline.named_steps["scaler"].transform(
        pipeline.named_steps["imputer"].transform(X_train)
    )

    scores = pipeline.predict_proba(X_test)[:, 1]
    predictions = pipeline.predict(X_test)
    metrics = {
        "accuracy": round(float(accuracy_score(y_test, predictions)), 4),
        "precision": round(float(precision_score(y_test, predictions, zero_division=0)), 4),
        "recall": round(float(recall_score(y_test, predictions, zero_division=0)), 4),
        "f1": round(float(f1_score(y_test, predictions, zero_division=0)), 4),
        "roc_auc": round(float(roc_auc_score(y_test, scores)), 4),
        "confusion_matrix": confusion_matrix(y_test, predictions).tolist(),
    }

    bundle = {
        "pipeline": pipeline,
        "columns": feature_columns,
        "modelType": model_key,
        "metrics": metrics,
        "shapBackground": transformed_train,
        "evaluationSamples": int(len(y_test)),
    }
    MODEL_CACHE[model_key] = bundle
    return bundle


def risk_category(score: float) -> str:
    if score >= 0.7:
        return "HIGH"
    if score >= 0.4:
        return "MODERATE"
    return "LOW"


def build_explanations(model_type: str, features: Dict[str, float]) -> List[ExplanationItem]:
    bundle = build_sklearn_model(model_type)
    pipeline = bundle["pipeline"]
    row = pd.DataFrame([features], columns=bundle["columns"])
    transformed_row = pipeline.named_steps["scaler"].transform(
        pipeline.named_steps["imputer"].transform(row)
    )
    explainer = shap.LinearExplainer(pipeline.named_steps["model"], bundle["shapBackground"])
    values = np.asarray(explainer(transformed_row).values)[0]
    top = sorted(
        zip(bundle["columns"], row.iloc[0].to_numpy(), values),
        key=lambda item: abs(float(item[2])),
        reverse=True,
    )[:4]

    explanations: List[ExplanationItem] = []
    for name, value, shap_value in top:
        explanations.append(
            ExplanationItem(
                feature=name,
                value=float(value),
                shapValue=float(shap_value),
                impact="INCREASES_RISK" if shap_value >= 0 else "DECREASES_RISK",
            )
        )
    return explanations


@app.get("/health")
def health() -> dict:
    if not APP_READY:
        try:
            runtime_ready()
        except RuntimeError as exc:  # pragma: no cover - startup guard
            raise HTTPException(status_code=503, detail=str(exc)) from exc
    return {"status": "UP", "service": "medisphere-ml"}


@app.post("/predict")
def predict(req: PredictionRequest) -> dict:
    if not APP_READY:
        runtime_ready()

    model_type = normalize_model_type(req.modelType)
    bundle = build_sklearn_model(model_type)
    pipeline = bundle["pipeline"]
    feature_order = bundle["columns"]

    supplied_features = dict(req.features)
    if model_type == "DIABETES" and "sex" not in supplied_features and "gender" in supplied_features:
        supplied_features["sex"] = supplied_features["gender"]
    record = {key: float(value) for key, value in supplied_features.items() if value is not None}
    for key in feature_order:
        record.setdefault(key, 0.0)

    frame = pd.DataFrame([record], columns=feature_order)
    score = float(pipeline.predict_proba(frame)[0][1])

    response = PredictionResponse(
        patientId=req.patientId,
        modelType=model_type,
        riskScore=score,
        riskCategory=risk_category(score),
        modelVersion=f"{model_type.lower()}-model-v1",
        generatedAt=datetime.now(timezone.utc).isoformat(),
        explanations=build_explanations(model_type, record),
    )
    return response.model_dump() if hasattr(response, "model_dump") else response.dict()


@app.get("/model-evaluation")
def model_evaluation() -> dict:
    if not APP_READY:
        runtime_ready()
    evaluations = {}
    for model_type in ("CARDIOVASCULAR", "DIABETES"):
        bundle = build_sklearn_model(model_type)
        evaluations[model_type.lower()] = {
            **bundle["metrics"],
            "dataset": "synthetic_health_demo",
            "evaluationSamples": bundle["evaluationSamples"],
            "modelVersion": f"{model_type.lower()}-model-v1",
        }
    return evaluations


def run_federated_demo() -> dict:
    if not APP_READY:
        runtime_ready()

    if tff is None:
        raise RuntimeError("TensorFlow Federated is unavailable in this runtime")

    tff.backends.native.set_sync_local_cpp_execution_context(default_num_clients=3)

    df = load_demo_dataset().copy()
    feature_cols = ["age", "sex", "systolicBloodPressure", "diastolicBloodPressure", "heartRate", "smokingStatus", "diabetesStatus", "bmi", "totalCholesterol"]
    X = df[feature_cols].astype(np.float32).to_numpy()
    y = df["label"].astype(np.float32).to_numpy()

    client_count = 3
    positive_indices = np.where(y == 1)[0]
    negative_indices = np.where(y == 0)[0]
    client_datasets = []

    feature_mean = X.mean(axis=0)
    feature_scale = X.std(axis=0)
    X = (X - feature_mean) / np.maximum(feature_scale, 1e-6)

    for idx in range(client_count):
        pos_start = idx * len(positive_indices) // client_count
        pos_end = (idx + 1) * len(positive_indices) // client_count if idx < client_count - 1 else len(positive_indices)
        neg_start = idx * len(negative_indices) // client_count
        neg_end = (idx + 1) * len(negative_indices) // client_count if idx < client_count - 1 else len(negative_indices)
        client_indices = np.concatenate([positive_indices[pos_start:pos_end], negative_indices[neg_start:neg_end]])

        client_features = X[client_indices]
        client_labels = y[client_indices]
        client_dataset = (
            tf.data.Dataset.from_tensor_slices((client_features, client_labels))
            .batch(16)
            .shuffle(50, reshuffle_each_iteration=True)
        )
        client_datasets.append(client_dataset)

    def build_keras_model() -> tf.keras.Model:
        model = tf.keras.Sequential(
            [
                tf.keras.layers.Input(shape=(len(feature_cols),)),
                tf.keras.layers.Dense(16, activation="relu"),
                tf.keras.layers.Dense(8, activation="relu"),
                tf.keras.layers.Dense(1, activation="sigmoid"),
            ],
            name="federated_health_model",
        )
        return model

    def model_fn():
        keras_model = build_keras_model()
        return tff.learning.models.from_keras_model(
            keras_model,
            input_spec=client_datasets[0].element_spec,
            loss=tf.keras.losses.BinaryCrossentropy(),
            metrics=[tf.keras.metrics.BinaryAccuracy(name="accuracy")],
        )

    fed_avg = tff.learning.algorithms.build_unweighted_fed_avg(
        model_fn=model_fn,
        client_optimizer_fn=tff.learning.optimizers.build_adam(learning_rate=0.01),
        server_optimizer_fn=tff.learning.optimizers.build_adam(learning_rate=0.01),
    )

    state = fed_avg.initialize()
    initial_weights = tf.nest.map_structure(
        lambda value: np.array(value, copy=True), fed_avg.get_model_weights(state)
    )
    history = []

    for round_no in range(3):
        state, metrics = fed_avg.next(state, client_datasets)
        train_metrics = metrics["client_work"]["train"]
        train_loss = float(train_metrics["loss"])
        train_accuracy = float(train_metrics["accuracy"])
        updated_weights = tf.nest.map_structure(
            lambda value: np.array(value, copy=True), fed_avg.get_model_weights(state)
        )
        weight_delta_norm = float(
            np.sqrt(
                sum(
                    np.sum(np.square(updated - previous))
                    for updated, previous in zip(
                        tf.nest.flatten(updated_weights),
                        tf.nest.flatten(initial_weights),
                    )
                )
            )
        )

        logger.info("TensorFlow Federated round %s complete; loss=%s accuracy=%s", round_no + 1, train_loss, train_accuracy)
        history.append(
            {
                "round": round_no + 1,
                "loss": round(train_loss, 4),
                "accuracy": round(train_accuracy, 4),
                "globalWeightDeltaNorm": round(weight_delta_norm, 6),
                "globalStateChanged": weight_delta_norm > 0.0,
            }
        )
        initial_weights = updated_weights

    return {
        "clients": [f"hospital-{idx + 1}" for idx in range(client_count)],
        "clientCount": client_count,
        "rounds": len(history),
        "globalModel": "tff-fedavg",
        "aggregation": {
            "method": "TFF unweighted FedAvg",
            "clientUpdatesAggregatedPerRound": client_count,
        },
        "rawDataLocal": True,
        "status": "completed",
        "engine": "tensorflow-federated",
        "history": history,
    }


@app.get("/federated-demo")
async def federated_demo() -> dict:
    return run_federated_demo()
