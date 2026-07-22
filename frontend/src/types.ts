export type Role =
  | "ADMIN"
  | "MANAGER"
  | "RECEPTIONIST"
  | "PROFESSIONAL"
  | "FINANCE"
  | "STOCK"
  | "HR";

export type ClinicTheme =
  | "CLINICAL_SERENE"
  | "BLUE_TRUST"
  | "VITAL_GREEN"
  | "WELCOMING_LAVENDER"
  | "HUMAN_TERRACOTTA"
  | "TECHNOLOGICAL_GRAPHITE";

export type Session = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: {
    id: string;
    name: string;
    email: string;
    role: Role;
    professionalId?: string;
  };
  clinic: {
    id: string;
    name: string;
    slug: string;
    timezone: string;
    logoUrl?: string;
    themeKey?: ClinicTheme;
  };
};

export type ClinicBranding = {
  clinicName: string;
  clinicSlug: string;
  logoUrl?: string;
  themeKey: ClinicTheme;
};

export type BillingCycle = "MONTHLY" | "YEARLY";
export type BillingPaymentMethod = "CREDIT_CARD" | "PIX";
export type SubscriptionStatus =
  | "TRIAL"
  | "PENDING"
  | "ACTIVE"
  | "PAST_DUE"
  | "SUSPENDED"
  | "CANCELED";
export type SubscriptionAccessMode = "FULL" | "READ_ONLY";

export type SubscriptionPlan = {
  code: string;
  name: string;
  description: string;
  billingCycle: BillingCycle;
  price: number;
  trialDays: number;
  priceGuaranteeMonths?: number;
  availabilityLimit?: number;
  remainingSpots?: number;
  available: boolean;
};

export type BillingProfile = {
  legalName: string;
  cpfCnpj: string;
  email: string;
  phone: string;
  postalCode?: string;
  address?: string;
  addressNumber?: string;
  complement?: string;
  province?: string;
  synchronizedWithAsaas: boolean;
};

export type ClinicSubscription = {
  id: string;
  planCode: string;
  planName: string;
  status: SubscriptionStatus;
  accessMode: SubscriptionAccessMode;
  paymentMethod?: BillingPaymentMethod;
  billingCycle: BillingCycle;
  amount: number;
  trialEndsAt?: string;
  nextDueDate?: string;
  graceEndsAt?: string;
  cancelAtPeriodEnd: boolean;
  canceledAt?: string;
  paymentUrl?: string;
  lastPaymentStatus?: string;
  lastPaymentAt?: string;
};

export type SubscriptionPayment = {
  id: string;
  status: string;
  billingType?: string;
  value: number;
  dueDate?: string;
  paymentDate?: string;
  invoiceUrl?: string;
  bankSlipUrl?: string;
  description?: string;
};

export type BillingOverview = {
  billingConfigured: boolean;
  environment: "sandbox" | "production";
  gracePeriodDays: number;
  plans: SubscriptionPlan[];
  profile?: BillingProfile;
  subscription: ClinicSubscription;
  payments: SubscriptionPayment[];
};

export type BillingProfilePayload = Omit<BillingProfile, "synchronizedWithAsaas">;

export type StartSubscriptionResponse = {
  subscription: ClinicSubscription;
  paymentUrl?: string;
  message: string;
};

export type ClinicUser = {
  id: string;
  name: string;
  email: string;
  role: Role;
  professionalId?: string;
  expectedDailyMinutes: number;
  active: boolean;
};

export type Patient = {
  id: string;
  name: string;
  cpf?: string;
  birthDate?: string;
  email?: string;
  phone: string;
  whatsappOptIn: boolean;
  whatsappOptInAt?: string;
  active: boolean;
};

export type Specialty = {
  id: string;
  name: string;
  color: string;
  active: boolean;
};

export type Professional = {
  id: string;
  name: string;
  council?: string;
  email?: string;
  phone?: string;
  specialtyId: string;
  specialtyName: string;
  specialtyColor: string;
  active: boolean;
};

export type AppointmentStatus =
  | "SCHEDULED"
  | "CONFIRMED"
  | "RESCHEDULE_REQUESTED"
  | "WAITING"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW";

export type Appointment = {
  id: string;
  patientId: string;
  patientName: string;
  professionalId: string;
  professionalName: string;
  specialtyId: string;
  specialtyName: string;
  color: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  notes?: string;
  confirmationRequestedAt?: string;
  confirmedAt?: string;
  rescheduleRequestedAt?: string;
};

export type AppointmentMessageStatus =
  | "PENDING"
  | "PROCESSING"
  | "SENT"
  | "DELIVERED"
  | "READ"
  | "RESPONDED"
  | "FAILED"
  | "CANCELLED";

export type AppointmentMessage = {
  id: string;
  channel: "WHATSAPP" | "SMS";
  purpose: "CONFIRMATION" | "REMINDER";
  direction: "OUTBOUND" | "INBOUND";
  status: AppointmentMessageStatus;
  recipient: string;
  templateName?: string;
  scheduledAt: string;
  attemptCount: number;
  maxAttempts: number;
  providerMessageId?: string;
  responseAction?: "CONFIRMED" | "RESCHEDULE_REQUESTED";
  errorMessage?: string;
  sentAt?: string;
  deliveredAt?: string;
  readAt?: string;
  respondedAt?: string;
  createdAt: string;
};

export type AppointmentMessagingSettings = {
  platformConfigured: boolean;
  whatsappEnabled: boolean;
  confirmationTemplateName: string;
  reminderTemplateName: string;
  languageCode: string;
  confirmationPreview: string;
  reminderPreview: string;
  firstReminderHours: number;
  secondReminderHours?: number;
  maxAttempts: number;
  retryMinutes: number;
  smsPrepared: boolean;
};

export type AppointmentContextReport = {
  from: string;
  to: string;
  total: number;
  completed: number;
  cancelled: number;
  noShows: number;
  attendanceRate: number;
  appointments: Appointment[];
};

export type EncounterStatus = "DRAFT" | "FINALIZED";

export type ClinicalEncounter = {
  id: string;
  appointmentId: string;
  patientId: string;
  patientName: string;
  professionalId: string;
  professionalName: string;
  professionalCouncil?: string;
  specialtyId: string;
  status: EncounterStatus;
  chiefComplaint?: string;
  subjectiveNotes?: string;
  objectiveNotes?: string;
  assessment?: string;
  carePlan?: string;
  additionalNotes?: string;
  finalizedByUserId?: string;
  finalizedAt?: string;
  lockVersion: number;
  createdAt: string;
  updatedAt: string;
};

export type ClinicalDocumentType =
  | "CLINICAL_REPORT"
  | "MEDICAL_CERTIFICATE"
  | "EXAM_REQUEST"
  | "ATTENDANCE_DECLARATION"
  | "PRESCRIPTION"
  | "FREE_DOCUMENT";

export type ClinicalTemplate = {
  id: string;
  type: ClinicalDocumentType;
  name: string;
  titleTemplate: string;
  bodyTemplate: string;
  favorite: boolean;
  active: boolean;
  versionNumber: number;
  createdAt: string;
  updatedAt: string;
};

export type ClinicalTemplatePayload = Omit<
  ClinicalTemplate,
  "id" | "versionNumber" | "createdAt" | "updatedAt"
>;

export type ClinicalDocument = {
  id: string;
  encounterId: string;
  patientId: string;
  patientName: string;
  professionalId: string;
  professionalName: string;
  professionalCouncil?: string;
  templateId?: string;
  type: ClinicalDocumentType;
  status: "DRAFT" | "FINALIZED" | "SIGNED";
  title: string;
  content: string;
  templateVersion?: number;
  revisionNumber: number;
  parentDocumentId?: string;
  finalizedAt?: string;
  documentHash?: string;
  signedAt?: string;
  signedPdfHash?: string;
  verificationCode?: string;
  signatureMode?: SignatureMode;
  createdAt: string;
  updatedAt: string;
};

export type ClinicalPlaceholder = { key: string; description: string };

export type SignatureMode = "LOCAL_PKCS12" | "REMOTE_CSC";
export type DocumentSignatureStatus = "PROCESSING" | "SIGNED" | "FAILED";

export type SignatureCredential = {
  id: string;
  mode: SignatureMode;
  providerKey?: string;
  providerName?: string;
  displayName: string;
  subjectName: string;
  issuerName: string;
  serialNumber: string;
  fingerprintSha256: string;
  validFrom: string;
  validUntil: string;
  remoteSecretKind?: string;
  active: boolean;
  ownershipConfirmed: boolean;
  lastUsedAt?: string;
};

export type CscProvider = { key: string; name: string };

export type DocumentSignature = {
  id: string;
  documentId: string;
  credentialId: string;
  mode: SignatureMode;
  providerKey?: string;
  status: DocumentSignatureStatus;
  signerSubject: string;
  certificateSerial: string;
  certificateFingerprint: string;
  signedAt?: string;
  signedPdfHash?: string;
  verificationCode?: string;
  failureMessage?: string;
};

export type SignatureVerification = {
  found: boolean;
  signed: boolean;
  integrityValid: boolean;
  cryptographicSignatureValid: boolean;
  clinicName?: string;
  documentType?: ClinicalDocumentType;
  mode?: SignatureMode;
  providerName?: string;
  signerSubject?: string;
  certificateSerial?: string;
  certificateFingerprint?: string;
  signedAt?: string;
  signedPdfHash?: string;
  notice: string;
};

export type MaterialCategory = {
  id: string;
  name: string;
};

export type StockMaterial = {
  id: string;
  name: string;
  categoryId: string;
  categoryName: string;
  sku?: string;
  unit: string;
  minimumStock: number;
  currentStock: number;
  lotControlled: boolean;
  lowStock: boolean;
  nearestExpiration?: string;
  active: boolean;
};

export type StockMovementType = "ENTRY" | "EXIT";

export type StockMovement = {
  id: string;
  type: StockMovementType;
  quantity: number;
  balanceAfter: number;
  reason: string;
  lotNumber?: string;
  expirationDate?: string;
  occurredAt: string;
};

export type FinancialEntryType = "INCOME" | "EXPENSE";
export type FinancialEntryStatus = "OPEN" | "PAID" | "CANCELLED" | "OVERDUE";

export type FinancialCategory = { id: string; name: string; type: FinancialEntryType };

export type FinancialEntry = {
  id: string;
  description: string;
  type: FinancialEntryType;
  categoryId: string;
  categoryName: string;
  amount: number;
  dueDate: string;
  paymentDate?: string;
  status: FinancialEntryStatus;
  counterparty?: string;
  paymentMethod?: string;
  notes?: string;
};

export type FinanceContextReport = {
  from: string;
  to: string;
  entryCount: number;
  received: number;
  paid: number;
  net: number;
  receivable: number;
  payable: number;
  entries: FinancialEntry[];
};

export type TimeEntryType = "CLOCK_IN" | "BREAK_START" | "BREAK_END" | "CLOCK_OUT";
export type TimeEntrySource = "SELF_SERVICE" | "MANUAL";
export type TimeDayStatus = "NOT_STARTED" | "WORKING" | "ON_BREAK" | "CLOSED";

export type TimeEntry = {
  id: string;
  userId: string;
  type: TimeEntryType;
  occurredAt: string;
  source: TimeEntrySource;
  notes?: string;
  edited: boolean;
};

export type TimeDaySummary = {
  userId: string;
  userName: string;
  date: string;
  status: TimeDayStatus;
  workedMinutes: number;
  expectedMinutes: number;
  balanceMinutes: number;
  entries: TimeEntry[];
};

export type ReportTrendPoint = {
  key: string;
  label: string;
  periodStart: string;
  appointments: number;
  completed: number;
  received: number;
  paid: number;
};

export type SpecialtyPerformance = {
  specialtyName: string;
  color: string;
  total: number;
  completed: number;
  noShows: number;
  attendanceRate: number;
};

export type EmployeeHours = {
  userId: string;
  userName: string;
  daysWithRecords: number;
  workedMinutes: number;
  expectedMinutes: number;
  balanceMinutes: number;
};

export type ManagementReport = {
  period: {
    from: string;
    to: string;
    days: number;
    granularity: "DAILY" | "MONTHLY";
  };
  appointments: {
    total: number;
    completed: number;
    cancelled: number;
    noShows: number;
    attendanceRate: number;
  };
  finance: {
    received: number;
    paid: number;
    net: number;
    receivable: number;
    payable: number;
  };
  inventory: {
    snapshotDate: string;
    activeMaterials: number;
    lowStock: number;
    expiredBatches: number;
    expiringIn30Days: number;
  };
  time: {
    employeesWithRecords: number;
    daysWithRecords: number;
    workedMinutes: number;
    expectedMinutes: number;
    balanceMinutes: number;
  };
  trend: ReportTrendPoint[];
  specialties: SpecialtyPerformance[];
  employeeHours: EmployeeHours[];
};

export type EmployeeTimeReport = {
  userId: string;
  userName: string;
  from: string;
  to: string;
  daysWithRecords: number;
  closedDays: number;
  workedMinutes: number;
  expectedMinutes: number;
  balanceMinutes: number;
  days: TimeDaySummary[];
};

export type TimeReportEmployee = {
  userId: string;
  userName: string;
  active: boolean;
};

export type InventoryMovementReportRow = {
  id: string;
  materialId: string;
  materialName: string;
  unit: string;
  type: StockMovementType;
  quantity: number;
  balanceAfter: number;
  reason: string;
  lotNumber?: string;
  createdByUserName: string;
  occurredAt: string;
};

export type InventoryMovementReport = {
  from: string;
  to: string;
  materialId?: string;
  materialName?: string;
  unit?: string;
  movementCount: number;
  entryCount: number;
  exitCount: number;
  distinctMaterials: number;
  totalEntryQuantity?: number;
  totalExitQuantity?: number;
  movements: InventoryMovementReportRow[];
};

export type InventoryImportSuggestedAction =
  | "CREATE"
  | "UPDATE"
  | "REVIEW"
  | "UNCHANGED"
  | "ERROR";

export type InventoryImportAction = "CREATE" | "UPDATE" | "SKIP";

export type InventorySimilarMaterial = {
  materialId: string;
  materialName: string;
  sku?: string;
  similarityPercent: number;
};

export type InventoryImportPreviewRow = {
  rowNumber: number;
  sourceId?: string;
  name?: string;
  categoryName?: string;
  sku?: string;
  unit?: string;
  minimumStock?: number;
  lotControlled?: boolean;
  currentStock?: number;
  suggestedAction: InventoryImportSuggestedAction;
  targetMaterialId?: string;
  targetMaterialName?: string;
  matchReason?: string;
  similarMaterials: InventorySimilarMaterial[];
  warnings: string[];
  errors: string[];
};

export type InventoryImportPreview = {
  totalRows: number;
  createCount: number;
  updateCount: number;
  reviewCount: number;
  unchangedCount: number;
  errorCount: number;
  rows: InventoryImportPreviewRow[];
};

export type InventoryImportDecision = {
  rowNumber: number;
  action: InventoryImportAction;
  targetMaterialId?: string;
};

export type InventoryImportResult = {
  created: number;
  updated: number;
  skipped: number;
};
