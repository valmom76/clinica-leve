export type Role =
  | "ADMIN"
  | "MANAGER"
  | "RECEPTIONIST"
  | "PROFESSIONAL"
  | "FINANCE"
  | "STOCK"
  | "HR";

export type Session = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: {
    id: string;
    name: string;
    email: string;
    role: Role;
  };
  clinic: {
    id: string;
    name: string;
    slug: string;
    timezone: string;
  };
};

export type ClinicUser = {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
};

export type Patient = {
  id: string;
  name: string;
  cpf?: string;
  birthDate?: string;
  email?: string;
  phone: string;
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
