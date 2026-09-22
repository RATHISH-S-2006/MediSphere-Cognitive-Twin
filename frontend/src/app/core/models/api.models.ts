export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PatientSummary {
  id: string;
  fhirPatientId: string;
  mrn: string | null;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: string;
  active: boolean;
  sourceSystem: string;
}

export interface PatientDetail extends PatientSummary {
  email: string | null;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  providerIds: string[];
  schemaVersion: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface LatestVitals {
  heartRate: number | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  spo2: number | null;
  recordedAt: string | null;
  deviceId: string | null;
  source: string | null;
}

export interface HealthTwin {
  twinId: string;
  patientId: string;
  modelVersion: string;
  patientFirstName: string;
  patientLastName: string;
  patientDateOfBirth: string;
  patientGender: string;
  latestVitals: LatestVitals | null;
  recentLabResultIds: string[];
  fhirResourceIds: string[];
  activeConsentId: string | null;
  consentStatus: string | null;
  completenessPercentage: number;
  missingDataPoints: string[];
  lastFhirSyncAt: string | null;
  lastVitalsUpdateAt: string | null;
  schemaVersion: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface Vitals {
  id: string;
  patientId: string;
  eventId: string;
  heartRate: number | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  spo2: number | null;
  temperature: number | null;
  respiratoryRate: number | null;
  dataQualityValid: boolean;
  dataQualityNote: string | null;
  deviceId: string | null;
  source: string | null;
  recordedAt: string;
  receivedAt: string | null;
  schemaVersion: string;
  createdAt: string | null;
}

export interface LabResult {
  id: string;
  patientId: string;
  fhirObservationId: string | null;
  testName: string;
  testCode: string;
  value: string | null;
  unit: string | null;
  referenceRange: string | null;
  interpretation: string | null;
  category: string | null;
  sourceSystem: string | null;
  collectedAt: string | null;
  reportedAt: string | null;
  schemaVersion: string;
  createdAt: string | null;
}

export interface Consent {
  consentId: string;
  patientId: string;
  status: string;
  purpose: string;
  scope: string;
  grantedBy: string;
  grantedAt: string | null;
  expiresAt: string | null;
  revokedAt: string | null;
  revokedBy: string | null;
  revokeReason: string | null;
  notes: string | null;
  schemaVersion: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface ConsentVerification {
  patientId: string;
  active: boolean;
  consentId: string | null;
  reason: string | null;
  evaluatedAt: string;
}

export interface FhirValidationResponse {
  valid: boolean;
  resourceType: string | null;
  resourceId: string | null;
  errors: string[];
}

export interface FhirResource {
  id: string;
  fhirResourceId: string;
  resourceType: string;
  patientId: string;
  fhirPatientRef: string | null;
  sourceSystem: string | null;
  version: string | null;
  validationPassed: boolean;
  lastUpdated: string | null;
}

export interface RiskExplanation {
  feature: string;
  label: string;
  value: number;
  shapValue: number;
  impact: string;
}

export interface RiskPrediction {
  id: string;
  patientId: string;
  modelType: string;
  riskScore: number;
  riskCategory: string;
  modelVersion: string;
  generatedAt: string;
  explanations: RiskExplanation[];
  inputFeatures?: Record<string, number>;
}

export type AlertStatus = 'ACTIVE' | 'ACKNOWLEDGED' | 'RESOLVED';
export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Alert {
  id: string;
  patientId: string;
  alertType: string;
  vitalType: string;
  observedValue: number;
  threshold: number;
  operator: string;
  severity: AlertSeverity;
  message: string;
  source: string | null;
  createdAt: string;
  status: AlertStatus;
  acknowledgedBy: string | null;
  acknowledgedAt: string | null;
  resolvedBy: string | null;
  resolvedAt: string | null;
}

export type CarePlanStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
export type CarePlanPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type InterventionStatus = 'PENDING' | 'COMPLETED' | 'MISSED' | 'SKIPPED';

export interface CarePlanGoal {
  id: string;
  description: string;
  targetType: string;
  targetValue: number | null;
  unit: string | null;
  startDate: string | null;
  targetDate: string | null;
  status: string;
  progress: number;
  notes: string | null;
}

export interface CarePlanIntervention {
  id: string;
  type: string;
  title: string;
  description: string;
  frequency: string;
  scheduledTime: string | null;
  startDate: string | null;
  endDate: string | null;
  status: InterventionStatus;
  priority: CarePlanPriority;
  instructions: string;
  completionCount: number;
  missedCount: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AdherenceSummary { completed: number; missed: number; pending: number; adherencePercentage: number; }
export interface OutcomeSummary { total: number; achieved: number; }
export interface CarePlan {
  id: string;
  patientId: string;
  title: string;
  description: string;
  status: CarePlanStatus;
  priority: CarePlanPriority;
  createdAt: string | null;
  updatedAt: string | null;
  generatedAt: string | null;
  source: string;
  generationReasons: string[];
  basedOnRiskPredictionIds: string[];
  basedOnAlertIds: string[];
  goals: CarePlanGoal[];
  interventions: CarePlanIntervention[];
  adherenceSummary: AdherenceSummary;
  outcomeSummary: OutcomeSummary;
  version: number;
}
export interface CarePlanOutcome {
  id: string;
  patientId: string;
  carePlanId: string;
  goalId: string;
  measuredValue: number;
  unit: string | null;
  recordedAt: string;
  notes: string | null;
  source: string;
  status: string;
}
