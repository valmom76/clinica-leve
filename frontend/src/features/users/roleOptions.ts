import type { Role } from "../../types";

export const roleLabel: Record<Role, string> = {
  ADMIN: "Administrador",
  MANAGER: "Gestor",
  RECEPTIONIST: "Recepção",
  PROFESSIONAL: "Profissional de saúde",
  FINANCE: "Financeiro",
  STOCK: "Estoque",
  HR: "Recursos humanos",
};

export const roleOptions = (Object.entries(roleLabel) as [Role, string][]).map(
  ([value, label]) => ({ value, label }),
);
