import type {
  Appointment,
  AppointmentContextReport,
  AppointmentMessage,
  AppointmentMessagingSettings,
  AuthCapabilities,
  BillingOverview,
  BillingPaymentMethod,
  BillingProfile,
  BillingProfilePayload,
  ClinicBranding,
  ClinicalDocument,
  ClinicalEncounter,
  ClinicalPlaceholder,
  ClinicalTemplate,
  ClinicalTemplatePayload,
  CscProvider,
  DocumentSignature,
  ClinicTheme,
  ClinicUser,
  FinancialCategory,
  FinancialEntry,
  FinancialEntryStatus,
  FinancialEntryType,
  FinanceContextReport,
  EmployeeTimeReport,
  InventoryMovementReport,
  InventoryImportDecision,
  InventoryImportPreview,
  InventoryImportResult,
  MaterialCategory,
  ManagementReport,
  Patient,
  Professional,
  Role,
  Session,
  SignatureCredential,
  SignatureVerification,
  Specialty,
  StockMaterial,
  StockMovement,
  StockMovementType,
  StartSubscriptionResponse,
  TimeDaySummary,
  TimeEntry,
  TimeEntryType,
  TimeReportEmployee,
} from "./types";

const API_URL = import.meta.env.VITE_API_URL ?? "/api";
const SESSION_KEY = "clinica-leve.session";
export const SESSION_EXPIRED_EVENT = "clinica-leve:session-expired";

export function loadSession(): Session | null {
  const stored = localStorage.getItem(SESSION_KEY);
  if (!stored) return null;
  try {
    return JSON.parse(stored) as Session;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function saveSession(session: Session | null) {
  if (session) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } else {
    localStorage.removeItem(SESSION_KEY);
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  session?: Session | null,
): Promise<T> {
  const isFormData = options.body instanceof FormData;
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(!isFormData ? { "Content-Type": "application/json" } : {}),
      ...(session ? { Authorization: `Bearer ${session.accessToken}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as {
      message?: string;
    } | null;
    if (response.status === 401 && session) {
      saveSession(null);
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    throw new Error(payload?.message ?? "Não foi possível concluir a operação");
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

async function requestBlob(path: string, session: Session) {
  const response = await fetch(`${API_URL}${path}`, {
    headers: { Authorization: `Bearer ${session.accessToken}` },
  });
  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as { message?: string } | null;
    if (response.status === 401) {
      saveSession(null);
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    throw new Error(payload?.message ?? "Não foi possível baixar o arquivo");
  }
  return response.blob();
}

export const api = {
  publicBranding: (clinicSlug: string) =>
    request<ClinicBranding>(`/public/branding/${encodeURIComponent(clinicSlug.trim().toLowerCase())}`),

  login: (payload: { clinicSlug: string; email: string; password: string }) =>
    request<Session>("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  authCapabilities: () => request<AuthCapabilities>("/auth/capabilities"),

  forgotPassword: (payload: { clinicSlug: string; email: string }) =>
    request<void>("/auth/password/forgot", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  resetPassword: (payload: { token: string; newPassword: string }) =>
    request<void>("/auth/password/reset", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  logoutAll: (session: Session) =>
    request<void>("/auth/logout-all", { method: "POST" }, session),

  patients: (session: Session, search = "") =>
    request<Patient[]>(
      `/patients${search ? `?search=${encodeURIComponent(search)}` : ""}`,
      {},
      session,
    ),

  createPatient: (
    session: Session,
    payload: {
      name: string;
      phone: string;
      email?: string;
      cpf?: string;
      birthDate?: string;
      whatsappOptIn: boolean;
    },
  ) =>
    request<Patient>(
      "/patients",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  updatePatient: (
    session: Session,
    id: string,
    payload: {
      name: string;
      phone: string;
      email?: string;
      cpf?: string;
      birthDate?: string;
      whatsappOptIn: boolean;
    },
  ) => request<Patient>(
    `/patients/${encodeURIComponent(id)}`,
    { method: "PUT", body: JSON.stringify(payload) },
    session,
  ),

  specialties: (session: Session) =>
    request<Specialty[]>("/specialties", {}, session),
  createSpecialty: (session: Session, payload: { name: string; color: string }) =>
    request<Specialty>(
      "/specialties",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  professionals: (session: Session) =>
    request<Professional[]>("/professionals", {}, session),
  managedProfessionals: (session: Session) =>
    request<Professional[]>("/professionals/management", {}, session),
  createProfessional: (
    session: Session,
    payload: {
      name: string;
      specialtyId: string;
      council?: string;
      email?: string;
      phone?: string;
    },
  ) => request<Professional>(
    "/professionals",
    { method: "POST", body: JSON.stringify(payload) },
    session,
  ),
  updateProfessional: (
    session: Session,
    id: string,
    payload: {
      name: string;
      specialtyId: string;
      council?: string;
      email?: string;
      phone?: string;
      active: boolean;
    },
  ) => request<Professional>(
    `/professionals/${encodeURIComponent(id)}`,
    { method: "PUT", body: JSON.stringify(payload) },
    session,
  ),

  appointments: (session: Session, from: string, to: string) =>
    request<Appointment[]>(
      `/appointments?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      {},
      session,
    ),

  createAppointment: (
    session: Session,
    payload: {
      patientId: string;
      professionalId: string;
      specialtyId: string;
      startAt: string;
      endAt: string;
      status: Appointment["status"];
      notes?: string;
    },
  ) =>
    request<Appointment>(
      "/appointments",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  updateAppointment: (
    session: Session,
    id: string,
    payload: {
      patientId: string;
      professionalId: string;
      specialtyId: string;
      startAt: string;
      endAt: string;
      status: Appointment["status"];
      notes?: string;
    },
  ) => request<Appointment>(
    `/appointments/${encodeURIComponent(id)}`,
    { method: "PUT", body: JSON.stringify(payload) },
    session,
  ),

  cancelAppointment: (session: Session, id: string) =>
    request<Appointment>(
      `/appointments/${encodeURIComponent(id)}/cancel`,
      { method: "POST" },
      session,
    ),

  appointmentMessages: (session: Session, appointmentId: string) =>
    request<AppointmentMessage[]>(
      `/appointments/${encodeURIComponent(appointmentId)}/messages`,
      {},
      session,
    ),

  sendAppointmentConfirmation: (session: Session, appointmentId: string) =>
    request<AppointmentMessage>(
      `/appointments/${encodeURIComponent(appointmentId)}/messages/confirmation`,
      { method: "POST" },
      session,
    ),

  appointmentMessagingSettings: (session: Session) =>
    request<AppointmentMessagingSettings>("/appointments/messaging/settings", {}, session),

  saveAppointmentMessagingSettings: (
    session: Session,
    payload: Omit<AppointmentMessagingSettings, "platformConfigured" | "smsPrepared">,
  ) => request<AppointmentMessagingSettings>(
    "/appointments/messaging/settings",
    { method: "PUT", body: JSON.stringify(payload) },
    session,
  ),

  appointmentReport: (
    session: Session,
    filters: {
      from: string;
      to: string;
      professionalId?: string;
      specialtyId?: string;
      status?: Appointment["status"];
    },
  ) => {
    const params = new URLSearchParams({ from: filters.from, to: filters.to });
    if (filters.professionalId) params.set("professionalId", filters.professionalId);
    if (filters.specialtyId) params.set("specialtyId", filters.specialtyId);
    if (filters.status) params.set("status", filters.status);
    return request<AppointmentContextReport>(`/appointments/reports/context?${params}`, {}, session);
  },

  users: (session: Session) =>
    request<ClinicUser[]>("/users", {}, session),

  createUser: (
    session: Session,
    payload: {
      name: string;
      email: string;
      password?: string;
      sendInvitation: boolean;
      role: Role;
      professionalId?: string;
      expectedDailyMinutes: number;
    },
  ) =>
    request<ClinicUser>(
      "/users",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  updateUser: (
    session: Session,
    id: string,
    payload: {
      name: string;
      email: string;
      password?: string;
      role: Role;
      professionalId?: string;
      expectedDailyMinutes: number;
      active: boolean;
    },
  ) =>
    request<ClinicUser>(
      `/users/${id}`,
      { method: "PUT", body: JSON.stringify(payload) },
      session,
    ),

  clinicalEncounters: (session: Session, patientId?: string) =>
    request<ClinicalEncounter[]>(
      `/clinical/encounters${patientId ? `?patientId=${encodeURIComponent(patientId)}` : ""}`,
      {},
      session,
    ),
  startClinicalEncounter: (session: Session, appointmentId: string) =>
    request<ClinicalEncounter>(
      "/clinical/encounters",
      { method: "POST", body: JSON.stringify({ appointmentId }) },
      session,
    ),
  updateClinicalEncounter: (
    session: Session,
    encounter: Pick<ClinicalEncounter, "id" | "lockVersion" | "chiefComplaint" | "subjectiveNotes" | "objectiveNotes" | "assessment" | "carePlan" | "additionalNotes">,
  ) => {
    const { id, ...payload } = encounter;
    return request<ClinicalEncounter>(
      `/clinical/encounters/${id}`,
      { method: "PUT", body: JSON.stringify(payload) },
      session,
    );
  },
  finalizeClinicalEncounter: (session: Session, id: string) =>
    request<ClinicalEncounter>(
      `/clinical/encounters/${id}/finalize`,
      { method: "POST" },
      session,
    ),

  clinicalTemplates: (session: Session) =>
    request<ClinicalTemplate[]>("/clinical/templates", {}, session),
  clinicalPlaceholders: (session: Session) =>
    request<ClinicalPlaceholder[]>("/clinical/templates/placeholders", {}, session),
  createClinicalTemplate: (session: Session, payload: ClinicalTemplatePayload) =>
    request<ClinicalTemplate>(
      "/clinical/templates",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),
  updateClinicalTemplate: (session: Session, id: string, payload: ClinicalTemplatePayload) =>
    request<ClinicalTemplate>(
      `/clinical/templates/${id}`,
      { method: "PUT", body: JSON.stringify(payload) },
      session,
    ),

  clinicalDocuments: (session: Session, encounterId: string) =>
    request<ClinicalDocument[]>(
      `/clinical/documents?encounterId=${encodeURIComponent(encounterId)}`,
      {},
      session,
    ),
  createClinicalDocument: (session: Session, encounterId: string, templateId: string) =>
    request<ClinicalDocument>(
      "/clinical/documents",
      { method: "POST", body: JSON.stringify({ encounterId, templateId }) },
      session,
    ),
  updateClinicalDocument: (
    session: Session,
    id: string,
    payload: Pick<ClinicalDocument, "title" | "content">,
  ) =>
    request<ClinicalDocument>(
      `/clinical/documents/${id}`,
      { method: "PUT", body: JSON.stringify(payload) },
      session,
    ),
  finalizeClinicalDocument: (session: Session, id: string) =>
    request<ClinicalDocument>(
      `/clinical/documents/${id}/finalize`,
      { method: "POST" },
      session,
    ),
  reviseClinicalDocument: (session: Session, id: string) =>
    request<ClinicalDocument>(
      `/clinical/documents/${id}/revisions`,
      { method: "POST" },
      session,
    ),

  signatureCredentials: (session: Session) =>
    request<SignatureCredential[]>("/clinical/signatures/credentials", {}, session),
  cscProviders: (session: Session) =>
    request<CscProvider[]>("/clinical/signatures/credentials/remote-providers", {}, session),
  uploadLocalSignatureCredential: (
    session: Session,
    payload: { file: File; password: string; displayName: string; ownershipConfirmed: boolean },
  ) => {
    const data = new FormData();
    data.append("file", payload.file);
    data.append("password", payload.password);
    data.append("displayName", payload.displayName);
    data.append("ownershipConfirmed", String(payload.ownershipConfirmed));
    return request<SignatureCredential>(
      "/clinical/signatures/credentials/local",
      { method: "POST", body: data },
      session,
    );
  },
  connectRemoteSignatureCredential: (
    session: Session,
    payload: {
      providerKey: string;
      credentialId: string;
      accessToken: string;
      displayName: string;
      ownershipConfirmed: boolean;
    },
  ) => request<SignatureCredential>(
    "/clinical/signatures/credentials/remote",
    { method: "POST", body: JSON.stringify(payload) },
    session,
  ),
  deactivateSignatureCredential: (session: Session, id: string) =>
    request<void>(
      `/clinical/signatures/credentials/${encodeURIComponent(id)}`,
      { method: "DELETE" },
      session,
    ),
  signClinicalDocument: (
    session: Session,
    documentId: string,
    payload: { credentialId: string; secret: string; secondarySecret?: string },
  ) => request<DocumentSignature>(
    `/clinical/signatures/documents/${encodeURIComponent(documentId)}`,
    { method: "POST", body: JSON.stringify(payload) },
    session,
  ),
  signedClinicalDocumentPdf: (session: Session, documentId: string) =>
    requestBlob(`/clinical/signatures/documents/${encodeURIComponent(documentId)}/pdf`, session),
  verifySignature: (code: string) =>
    request<SignatureVerification>(`/public/signatures/verify/${encodeURIComponent(code)}`),

  clinicBranding: (session: Session) =>
    request<ClinicBranding>("/clinic/branding", {}, session),
  uploadClinicLogo: (session: Session, file: File) => {
    const data = new FormData();
    data.append("file", file);
    return request<ClinicBranding>("/clinic/branding/logo", { method: "POST", body: data }, session);
  },
  removeClinicLogo: (session: Session) =>
    request<ClinicBranding>("/clinic/branding/logo", { method: "DELETE" }, session),
  updateClinicTheme: (session: Session, themeKey: ClinicTheme) =>
    request<ClinicBranding>(
      "/clinic/branding/theme",
      { method: "PUT", body: JSON.stringify({ themeKey }) },
      session,
    ),

  billingOverview: (session: Session) =>
    request<BillingOverview>("/billing/overview", {}, session),
  saveBillingProfile: (session: Session, payload: BillingProfilePayload) =>
    request<BillingProfile>(
      "/billing/profile",
      { method: "PUT", body: JSON.stringify(payload) },
      session,
    ),
  startSubscription: (
    session: Session,
    payload: { planCode: string; paymentMethod: BillingPaymentMethod },
  ) =>
    request<StartSubscriptionResponse>(
      "/billing/subscription/start",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),
  refreshSubscription: (session: Session) =>
    request<BillingOverview>(
      "/billing/subscription/refresh",
      { method: "POST" },
      session,
    ),
  cancelSubscription: (session: Session) =>
    request<BillingOverview>(
      "/billing/subscription/cancel",
      { method: "POST" },
      session,
    ),

  materialCategories: (session: Session) =>
    request<MaterialCategory[]>("/inventory/categories", {}, session),

  createMaterialCategory: (session: Session, name: string) =>
    request<MaterialCategory>(
      "/inventory/categories",
      { method: "POST", body: JSON.stringify({ name }) },
      session,
    ),

  materials: (session: Session) =>
    request<StockMaterial[]>("/inventory/materials", {}, session),

  createMaterial: (
    session: Session,
    payload: {
      name: string;
      categoryId: string;
      sku?: string;
      unit: string;
      minimumStock: number;
      lotControlled: boolean;
    },
  ) =>
    request<StockMaterial>(
      "/inventory/materials",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  updateMaterial: (
    session: Session,
    id: string,
    payload: {
      name: string;
      categoryId: string;
      sku?: string;
      unit: string;
      minimumStock: number;
      lotControlled: boolean;
    },
  ) =>
    request<StockMaterial>(
      `/inventory/materials/${id}`,
      { method: "PUT", body: JSON.stringify(payload) },
      session,
    ),

  moveStock: (
    session: Session,
    materialId: string,
    payload: {
      type: StockMovementType;
      quantity: number;
      reason: string;
      lotNumber?: string;
      expirationDate?: string;
    },
  ) =>
    request<StockMaterial>(
      `/inventory/materials/${materialId}/movements`,
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  stockMovements: (session: Session, materialId: string) =>
    request<StockMovement[]>(
      `/inventory/materials/${materialId}/movements`,
      {},
      session,
    ),

  financialCategories: (session: Session) =>
    request<FinancialCategory[]>("/finance/categories", {}, session),
  createFinancialCategory: (session: Session, payload: { name: string; type: FinancialEntryType }) =>
    request<FinancialCategory>("/finance/categories", { method: "POST", body: JSON.stringify(payload) }, session),
  financialEntries: (session: Session) => request<FinancialEntry[]>("/finance/entries", {}, session),
  createFinancialEntry: (session: Session, payload: FinancialEntryPayload) =>
    request<FinancialEntry>("/finance/entries", { method: "POST", body: JSON.stringify(payload) }, session),
  updateFinancialEntry: (session: Session, id: string, payload: FinancialEntryPayload) =>
    request<FinancialEntry>(`/finance/entries/${id}`, { method: "PUT", body: JSON.stringify(payload) }, session),
  settleFinancialEntry: (session: Session, id: string, payload: { paymentDate: string; paymentMethod: string }) =>
    request<FinancialEntry>(`/finance/entries/${id}/settle`, { method: "POST", body: JSON.stringify(payload) }, session),
  reopenFinancialEntry: (session: Session, id: string) =>
    request<FinancialEntry>(`/finance/entries/${id}/reopen`, { method: "POST" }, session),
  cancelFinancialEntry: (session: Session, id: string) =>
    request<FinancialEntry>(`/finance/entries/${id}/cancel`, { method: "POST" }, session),
  financeReport: (
    session: Session,
    filters: {
      from: string;
      to: string;
      type?: FinancialEntryType;
      categoryId?: string;
      status?: FinancialEntryStatus;
    },
  ) => {
    const params = new URLSearchParams({ from: filters.from, to: filters.to });
    if (filters.type) params.set("type", filters.type);
    if (filters.categoryId) params.set("categoryId", filters.categoryId);
    if (filters.status) params.set("status", filters.status);
    return request<FinanceContextReport>(`/finance/reports/context?${params}`, {}, session);
  },

  myTimeDay: (session: Session, date: string) =>
    request<TimeDaySummary>(`/time-clock/me?date=${encodeURIComponent(date)}`, {}, session),
  punchTime: (session: Session, type: TimeEntryType) =>
    request<TimeDaySummary>(
      "/time-clock/me/punch",
      { method: "POST", body: JSON.stringify({ type }) },
      session,
    ),
  teamTimeDay: (session: Session, date: string) =>
    request<TimeDaySummary[]>(`/time-clock/team?date=${encodeURIComponent(date)}`, {}, session),
  createTimeEntry: (
    session: Session,
    payload: { userId: string; type: TimeEntryType; occurredAt: string; notes?: string },
  ) => request<TimeEntry>("/time-clock/entries", { method: "POST", body: JSON.stringify(payload) }, session),
  updateTimeEntry: (
    session: Session,
    id: string,
    payload: { type: TimeEntryType; occurredAt: string; notes?: string },
  ) => request<TimeEntry>(`/time-clock/entries/${id}`, { method: "PUT", body: JSON.stringify(payload) }, session),
  deleteTimeEntry: (session: Session, id: string) =>
    request<void>(`/time-clock/entries/${id}`, { method: "DELETE" }, session),
  employeeTimeReport: (session: Session, userId: string, from: string, to: string) =>
    request<EmployeeTimeReport>(
      `/time-clock/reports/employee?userId=${encodeURIComponent(userId)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      {},
      session,
    ),
  timeReportEmployees: (session: Session) =>
    request<TimeReportEmployee[]>("/time-clock/reports/employees", {}, session),

  inventoryMovementReport: (
    session: Session,
    filters: { from: string; to: string; materialId?: string; type?: StockMovementType },
  ) => {
    const params = new URLSearchParams({ from: filters.from, to: filters.to });
    if (filters.materialId) params.set("materialId", filters.materialId);
    if (filters.type) params.set("type", filters.type);
    return request<InventoryMovementReport>(`/inventory/reports/movements?${params}`, {}, session);
  },

  inventoryImportTemplate: (session: Session) =>
    requestBlob("/inventory/materials/import-template", session),
  previewInventoryImport: (session: Session, file: File) => {
    const data = new FormData();
    data.append("file", file);
    return request<InventoryImportPreview>(
      "/inventory/materials/import-preview",
      { method: "POST", body: data },
      session,
    );
  },
  confirmInventoryImport: (
    session: Session,
    file: File,
    decisions: InventoryImportDecision[],
  ) => {
    const data = new FormData();
    data.append("file", file);
    data.append(
      "decisions",
      new Blob([JSON.stringify({ decisions })], { type: "application/json" }),
    );
    return request<InventoryImportResult>(
      "/inventory/materials/import-confirm",
      { method: "POST", body: data },
      session,
    );
  },

  managementReport: (session: Session, from: string, to: string) =>
    request<ManagementReport>(
      `/reports/management?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      {},
      session,
    ),
};

type FinancialEntryPayload = {
  description: string;
  type: FinancialEntryType;
  categoryId: string;
  amount: number;
  dueDate: string;
  counterparty?: string;
  notes?: string;
};
