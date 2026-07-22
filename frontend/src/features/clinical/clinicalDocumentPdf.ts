import { jsPDF } from "jspdf";
import type { ClinicalDocument, Session } from "../../types";
import { clinicTheme, hexToRgb } from "../../utils/clinicThemes";
import { loadLogo, type LogoAsset } from "../../utils/reportPdf";

type PdfMode = "download" | "print";

export async function outputClinicalDocumentPdf(
  session: Session,
  clinicalDocument: ClinicalDocument,
  mode: PdfMode,
) {
  const printWindow = mode === "print" ? window.open("", "_blank") : null;
  const pdf = new jsPDF({ unit: "mm", format: "a4" });
  const logo = await loadLogo(session.clinic.logoUrl);
  drawDocument(pdf, session, clinicalDocument, logo);
  if (mode === "download") {
    pdf.save(`${safeFilename(clinicalDocument.title)}-r${clinicalDocument.revisionNumber}.pdf`);
    return;
  }

  const url = URL.createObjectURL(pdf.output("blob"));
  if (printWindow) {
    printWindow.location.href = url;
  }
  window.setTimeout(() => {
    printWindow?.print();
    URL.revokeObjectURL(url);
  }, 800);
}

function drawDocument(
  pdf: jsPDF,
  session: Session,
  clinicalDocument: ClinicalDocument,
  logo: LogoAsset | null,
) {
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const theme = clinicTheme(session.clinic.themeKey);
  const primary = hexToRgb(theme.colors.primary);
  const deep = hexToRgb(theme.colors.deep);
  const margin = 18;

  if (logo) {
    const scale = Math.min(28 / logo.width, 16 / logo.height);
    pdf.addImage(logo.dataUrl, margin, 12, logo.width * scale, logo.height * scale);
  } else {
    pdf.setFillColor(...primary);
    pdf.roundedRect(margin, 12, 16, 16, 3, 3, "F");
    pdf.setTextColor(255, 255, 255);
    pdf.setFont("helvetica", "bold");
    pdf.setFontSize(10);
    pdf.text("CL", margin + 3.5, 22);
  }

  pdf.setTextColor(...deep);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(12);
  pdf.text(session.clinic.name, 52, 18);
  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(8);
  pdf.setTextColor(93, 111, 107);
  pdf.text("Documento clínico", 52, 24);
  pdf.setDrawColor(218, 225, 219);
  pdf.line(margin, 34, pageWidth - margin, 34);

  pdf.setTextColor(...deep);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(15);
  const titleLines = pdf.splitTextToSize(clinicalDocument.title, pageWidth - margin * 2);
  pdf.text(titleLines, margin, 46);
  let cursor = 46 + titleLines.length * 6 + 4;

  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(8.5);
  pdf.setTextColor(80, 101, 96);
  pdf.text(`Paciente: ${clinicalDocument.patientName}`, margin, cursor);
  cursor += 5;
  pdf.text(
    `Profissional: ${clinicalDocument.professionalName}${clinicalDocument.professionalCouncil ? ` · ${clinicalDocument.professionalCouncil}` : ""}`,
    margin,
    cursor,
  );
  cursor += 9;

  pdf.setTextColor(39, 55, 52);
  pdf.setFontSize(10.5);
  const bodyLines = pdf.splitTextToSize(clinicalDocument.content, pageWidth - margin * 2);
  for (const line of bodyLines) {
    if (cursor > pageHeight - 27) {
      pdf.addPage();
      cursor = 20;
    }
    pdf.text(line, margin, cursor);
    cursor += 5.2;
  }

  const totalPages = pdf.getNumberOfPages();
  for (let page = 1; page <= totalPages; page += 1) {
    pdf.setPage(page);
    pdf.setDrawColor(224, 229, 225);
    pdf.line(margin, pageHeight - 18, pageWidth - margin, pageHeight - 18);
    pdf.setFont("helvetica", "normal");
    pdf.setFontSize(7);
    pdf.setTextColor(104, 119, 116);
    const state = clinicalDocument.status === "FINALIZED"
      ? `Finalizado no sistema em ${formatDateTime(clinicalDocument.finalizedAt)}`
      : "Rascunho · não finalizado e não assinado digitalmente";
    pdf.text(state, margin, pageHeight - 12);
    pdf.text(`Página ${page} de ${totalPages}`, pageWidth - margin, pageHeight - 12, { align: "right" });
    if (clinicalDocument.documentHash) {
      pdf.setFontSize(6.3);
      pdf.text(`Integridade SHA-256: ${clinicalDocument.documentHash}`, margin, pageHeight - 8);
    }
  }
}

function safeFilename(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .toLowerCase() || "documento-clinico";
}

function formatDateTime(value?: string) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
