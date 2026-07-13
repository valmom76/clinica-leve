import type { FinancialEntryStatus, FinancialEntryType } from "../../types";

export const typeLabel: Record<FinancialEntryType, string> = { INCOME: "Receita", EXPENSE: "Despesa" };
export const statusLabel: Record<FinancialEntryStatus, string> = {
  OPEN: "Em aberto", PAID: "Baixado", CANCELLED: "Cancelado", OVERDUE: "Atrasado",
};
export function currency(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}
export function localDate(value?: string) {
  return value ? new Date(`${value}T12:00:00`).toLocaleDateString("pt-BR") : "—";
}
