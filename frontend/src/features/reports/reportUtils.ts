import type { ManagementReport } from "../../types";
import { downloadCsv, localDate } from "../../utils/reporting";

export { clinicToday, firstDayOfMonth, shiftDays } from "../../utils/reporting";

export function currency(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

export function hours(minutes: number) {
  const absolute = Math.abs(minutes);
  const value = `${Math.floor(absolute / 60)}h ${String(absolute % 60).padStart(2, "0")}min`;
  if (minutes === 0) return value;
  return `${minutes > 0 ? "+" : "−"}${value}`;
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
  downloadCsv(`relatorio-clinica-leve-${report.period.from}-${report.period.to}.csv`, rows);
}
