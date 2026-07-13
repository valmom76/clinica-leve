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
