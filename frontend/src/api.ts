import type {
  Appointment,
  ClinicUser,
  FinancialCategory,
  FinancialEntry,
  FinancialEntryType,
  MaterialCategory,
  ManagementReport,
  Patient,
  Professional,
  Role,
  Session,
  Specialty,
  StockMaterial,
  StockMovement,
  StockMovementType,
  TimeDaySummary,
  TimeEntry,
  TimeEntryType,
} from "./types";

const API_URL = import.meta.env.VITE_API_URL ?? "/api";
const SESSION_KEY = "clinica-leve.session";

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
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(session ? { Authorization: `Bearer ${session.accessToken}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as {
      message?: string;
    } | null;
    throw new Error(payload?.message ?? "Não foi possível concluir a operação");
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const api = {
  login: (payload: { clinicSlug: string; email: string; password: string }) =>
    request<Session>("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

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
    },
  ) =>
    request<Patient>(
      "/patients",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  specialties: (session: Session) =>
    request<Specialty[]>("/specialties", {}, session),

  professionals: (session: Session) =>
    request<Professional[]>("/professionals", {}, session),

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
      status: "CONFIRMED";
      notes?: string;
    },
  ) =>
    request<Appointment>(
      "/appointments",
      { method: "POST", body: JSON.stringify(payload) },
      session,
    ),

  users: (session: Session) =>
    request<ClinicUser[]>("/users", {}, session),

  createUser: (
    session: Session,
    payload: { name: string; email: string; password: string; role: Role; expectedDailyMinutes: number },
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
      expectedDailyMinutes: number;
      active: boolean;
    },
  ) =>
    request<ClinicUser>(
      `/users/${id}`,
      { method: "PUT", body: JSON.stringify(payload) },
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
