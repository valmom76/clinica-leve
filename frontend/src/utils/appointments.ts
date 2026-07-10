import type { Appointment } from "../types";

export const statusLabel: Record<Appointment["status"], string> = {
  SCHEDULED: "Agendado",
  CONFIRMED: "Confirmado",
  WAITING: "Aguardando",
  IN_PROGRESS: "Em atendimento",
  COMPLETED: "Concluído",
  CANCELLED: "Cancelado",
  NO_SHOW: "Faltou",
};
