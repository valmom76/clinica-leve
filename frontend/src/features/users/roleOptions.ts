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

export const roleAccess: Record<Role, string> = {
  ADMIN: "Acesso completo, usuários, permissões e personalização da clínica.",
  MANAGER: "Dashboard gerencial, estoque, financeiro e gestão do ponto, sem conteúdo de prontuário.",
  RECEPTIONIST: "Agenda, pacientes e profissionais, sem financeiro ou administração.",
  PROFESSIONAL: "Agenda e dados clínicos necessários aos atendimentos.",
  FINANCE: "Lançamentos, baixas e relatórios financeiros.",
  STOCK: "Materiais, entradas, saídas e relatórios de estoque.",
  HR: "Próprio ponto e gestão das marcações e relatórios da equipe.",
};
