import type { TimeDayStatus, TimeEntryType } from "../../types";

export const timeEntryLabel: Record<TimeEntryType, string> = {
  CLOCK_IN: "Entrada",
  BREAK_START: "Início do intervalo",
  BREAK_END: "Fim do intervalo",
  CLOCK_OUT: "Saída",
};

export const timeStatusLabel: Record<TimeDayStatus, string> = {
  NOT_STARTED: "Não iniciado",
  WORKING: "Em expediente",
  ON_BREAK: "Em intervalo",
  CLOSED: "Concluído",
};

export function formatMinutes(minutes: number) {
  const safe = Math.max(0, Math.abs(minutes));
  const hours = Math.floor(safe / 60);
  const rest = safe % 60;
  return `${hours}h ${String(rest).padStart(2, "0")}min`;
}

export function formatBalance(minutes: number) {
  if (minutes === 0) return "0h 00min";
  return `${minutes > 0 ? "+" : "−"}${formatMinutes(minutes)}`;
}

export function timeFromLocalDateTime(value: string) {
  return value.slice(11, 16);
}

export function clinicDate(timeZone: string, date = new Date()) {
  return dateParts(timeZone, date).date;
}

export function clinicDateTime(timeZone: string, date = new Date()) {
  const parts = dateParts(timeZone, date);
  return `${parts.date}T${parts.time}`;
}

export function longDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Intl.DateTimeFormat("pt-BR", {
    weekday: "long",
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(new Date(year, month - 1, day));
}

function dateParts(timeZone: string, date: Date) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date);
  const get = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? "00";
  return {
    date: `${get("year")}-${get("month")}-${get("day")}`,
    time: `${get("hour")}:${get("minute")}`,
  };
}
