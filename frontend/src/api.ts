import type {
  Appointment,
  Patient,
  Professional,
  Session,
  Specialty,
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
};
