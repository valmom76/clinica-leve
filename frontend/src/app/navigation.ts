import {
  Box,
  CalendarDays,
  CircleDollarSign,
  Clock3,
  CreditCard,
  FileHeart,
  LayoutDashboard,
  Palette,
  ShieldCheck,
  Stethoscope,
  UsersRound,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { Role } from "../types";

export type ViewKey =
  | "dashboard"
  | "agenda"
  | "patients"
  | "professionals"
  | "clinical"
  | "inventory"
  | "finance"
  | "time"
  | "users"
  | "billing"
  | "settings";

export type NavigationSection = "operation" | "management";

export type NavigationItem = {
  key: ViewKey;
  label: string;
  icon: LucideIcon;
  section: NavigationSection;
  roles?: Role[];
};

export const clinicalRoles: Role[] = [
  "ADMIN",
  "MANAGER",
  "RECEPTIONIST",
  "PROFESSIONAL",
];

export const navItems: NavigationItem[] = [
  { key: "dashboard", label: "Dashboard", icon: LayoutDashboard, section: "operation" },
  { key: "agenda", label: "Agenda", icon: CalendarDays, section: "operation", roles: clinicalRoles },
  { key: "patients", label: "Pacientes", icon: UsersRound, section: "operation", roles: clinicalRoles },
  { key: "professionals", label: "Profissionais", icon: Stethoscope, section: "operation" },
  { key: "clinical", label: "Atendimentos", icon: FileHeart, section: "operation", roles: ["ADMIN", "PROFESSIONAL"] },
  { key: "inventory", label: "Estoque", icon: Box, section: "operation", roles: ["ADMIN", "MANAGER", "STOCK"] },
  { key: "finance", label: "Financeiro", icon: CircleDollarSign, section: "management", roles: ["ADMIN", "MANAGER", "FINANCE"] },
  { key: "time", label: "Ponto", icon: Clock3, section: "management" },
  { key: "users", label: "Equipe e acessos", icon: ShieldCheck, section: "management", roles: ["ADMIN"] },
  { key: "billing", label: "Minha assinatura", icon: CreditCard, section: "management", roles: ["ADMIN"] },
  { key: "settings", label: "Personalização", icon: Palette, section: "management", roles: ["ADMIN"] },
];

export function navigationFor(role: Role) {
  return navItems.filter((item) => !item.roles || item.roles.includes(role));
}

export function canAccessClinicalData(role: Role) {
  return clinicalRoles.includes(role);
}
