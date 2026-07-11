import type {
  Appointment,
  ClinicUser,
  MaterialCategory,
  Patient,
  Professional,
  Role,
  Session,
  Specialty,
  StockMaterial,
  StockMovement,
  StockMovementType,
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
    payload: { name: string; email: string; password: string; role: Role },
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
};
