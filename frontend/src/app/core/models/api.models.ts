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
