import type { ManagementReport } from "../../types";

export function currency(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

export function hours(minutes: number) {
  const absolute = Math.abs(minutes);
  const value = `${Math.floor(absolute / 60)}h ${String(absolute % 60).padStart(2, "0")}min`;
  if (minutes === 0) return value;
  return `${minutes > 0 ? "+" : "−"}${value}`;
}

export function clinicToday(timeZone: string) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const get = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

export function shiftDays(value: string, amount: number) {
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(year, month - 1, day, 12);
  date.setDate(date.getDate() + amount);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function firstDayOfMonth(value: string) {
  return `${value.slice(0, 7)}-01`;
}

export function downloadReportCsv(report: ManagementReport, clinicName: string) {
  const rows: Array<Array<string | number>> = [
    ["Relatório gerencial", clinicName],
    ["Período", `${localDate(report.period.from)} a ${localDate(report.period.to)}`],
    [],
    ["ATENDIMENTOS"],
    ["Total", report.appointments.total],
    ["Concluídos", report.appointments.completed],
    ["Cancelados", report.appointments.cancelled],
    ["Faltas", report.appointments.noShows],
    ["Comparecimento (%)", report.appointments.attendanceRate],
    [],
    ["FINANCEIRO"],
    ["Recebido", report.finance.received.toFixed(2)],
    ["Pago", report.finance.paid.toFixed(2)],
    ["Saldo realizado", report.finance.net.toFixed(2)],
    ["A receber no período", report.finance.receivable.toFixed(2)],
    ["A pagar no período", report.finance.payable.toFixed(2)],
    [],
    ["ESTOQUE ATUAL"],
    ["Materiais ativos", report.inventory.activeMaterials],
    ["Estoque baixo", report.inventory.lowStock],
    ["Lotes vencidos", report.inventory.expiredBatches],
    ["Lotes a vencer em 30 dias", report.inventory.expiringIn30Days],
    [],
    ["ESPECIALIDADES", "Atendimentos", "Concluídos", "Faltas", "Comparecimento (%)"],
    ...report.specialties.map((item) => [item.specialtyName, item.total, item.completed, item.noShows, item.attendanceRate]),
    [],
    ["EQUIPE", "Dias registrados", "Horas trabalhadas", "Horas previstas", "Saldo (minutos)"],
    ...report.employeeHours.map((item) => [item.userName, item.daysWithRecords, item.workedMinutes, item.expectedMinutes, item.balanceMinutes]),
  ];
  const csv = `\uFEFF${rows.map((row) => row.map(csvCell).join(";")).join("\r\n")}`;
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `relatorio-clinica-leve-${report.period.from}-${report.period.to}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function localDate(value: string) {
  return value.split("-").reverse().join("/");
}

function csvCell(value: string | number) {
  const text = String(value).replaceAll('"', '""');
  return `"${text}"`;
}
