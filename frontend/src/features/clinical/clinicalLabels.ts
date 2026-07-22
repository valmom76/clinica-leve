import type { ClinicalDocumentType } from "../../types";

export const documentTypeLabel: Record<ClinicalDocumentType, string> = {
  CLINICAL_REPORT: "Relatório clínico",
  MEDICAL_CERTIFICATE: "Atestado",
  EXAM_REQUEST: "Solicitação de exames",
  ATTENDANCE_DECLARATION: "Declaração de comparecimento",
  PRESCRIPTION: "Receita simples",
  FREE_DOCUMENT: "Documento livre",
};

export const documentTypeOptions = (Object.entries(documentTypeLabel) as Array<
  [ClinicalDocumentType, string]
>).map(([value, label]) => ({ value, label }));
