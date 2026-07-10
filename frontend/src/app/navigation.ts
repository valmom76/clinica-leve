import {
  BarChart3,
  Box,
  CalendarDays,
  CircleDollarSign,
  Clock3,
  LayoutDashboard,
  Stethoscope,
  UsersRound,
} from "lucide-react";

export type ViewKey =
  | "dashboard"
  | "agenda"
  | "patients"
  | "professionals"
  | "inventory"
  | "finance"
  | "time"
  | "reports";

export const navItems = [
  { key: "dashboard" as const, label: "Visão geral", icon: LayoutDashboard },
  { key: "agenda" as const, label: "Agenda", icon: CalendarDays },
  { key: "patients" as const, label: "Pacientes", icon: UsersRound },
  { key: "professionals" as const, label: "Profissionais", icon: Stethoscope },
  { key: "inventory" as const, label: "Estoque", icon: Box },
  { key: "finance" as const, label: "Financeiro", icon: CircleDollarSign },
  { key: "time" as const, label: "Ponto", icon: Clock3 },
  { key: "reports" as const, label: "Relatórios", icon: BarChart3 },
];
