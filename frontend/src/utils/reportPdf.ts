import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import type { Session } from "../types";
import { clinicTheme, hexToRgb } from "./clinicThemes";

type PdfReportOptions = {
  session: Session;
  filename: string;
  title: string;
  subtitle: string;
  filters?: Array<[string, string]>;
  summary?: Array<[string, string]>;
  columns: string[];
  rows: Array<Array<string | number>>;
  landscape?: boolean;
};

export type LogoAsset = { dataUrl: string; width: number; height: number };
type ReportPalette = ReturnType<typeof reportPalette>;

export async function downloadReportPdf(options: PdfReportOptions) {
  const document = new jsPDF({
    orientation: options.landscape ? "landscape" : "portrait",
    unit: "mm",
    format: "a4",
  });
  const logo = await loadLogo(options.session.clinic.logoUrl);
  const palette = reportPalette(options.session);
  drawHeader(document, options, logo, palette);

  let cursor = 35;
  if (options.filters?.length) {
    document.setFontSize(8.5);
    document.setTextColor(83, 105, 101);
    const filterText = options.filters.map(([label, value]) => `${label}: ${value}`).join("  |  ");
    const lines = document.splitTextToSize(filterText, document.internal.pageSize.getWidth() - 28);
    document.text(lines, 14, cursor);
    cursor += lines.length * 4 + 3;
  }

  if (options.summary?.length) {
    cursor = drawSummary(document, options.summary, cursor, palette);
  }

  autoTable(document, {
    startY: cursor,
    head: [options.columns],
    body: options.rows,
    margin: { top: 34, right: 12, bottom: 16, left: 12 },
    styles: { font: "helvetica", fontSize: 7.5, cellPadding: 2.2, textColor: [45, 64, 61] },
    headStyles: { fillColor: palette.medium, textColor: 255, fontStyle: "bold" },
    alternateRowStyles: { fillColor: palette.soft },
    didDrawPage: (data) => {
      if (data.pageNumber > 1) drawHeader(document, options, logo, palette);
    },
  });

  const pages = document.getNumberOfPages();
  for (let page = 1; page <= pages; page += 1) {
    document.setPage(page);
    drawFooter(document, options.session, page, pages, palette);
  }
  document.save(options.filename);
}

function drawHeader(document: jsPDF, options: PdfReportOptions, logo: LogoAsset | null, palette: ReportPalette) {
  const width = document.internal.pageSize.getWidth();
  if (logo) {
    const maxWidth = 25;
    const maxHeight = 15;
    const scale = Math.min(maxWidth / logo.width, maxHeight / logo.height);
    document.addImage(logo.dataUrl, 14, 9, logo.width * scale, logo.height * scale);
  } else {
    document.setFillColor(...palette.primary);
    document.roundedRect(14, 9, 15, 15, 3, 3, "F");
    document.setTextColor(255, 255, 255);
    document.setFontSize(10);
    document.setFont("helvetica", "bold");
    document.text("CL", 17.2, 18.5);
  }
  document.setTextColor(...palette.deep);
  document.setFont("helvetica", "bold");
  document.setFontSize(13);
  document.text(options.title, 43, 14);
  document.setFont("helvetica", "normal");
  document.setFontSize(8.5);
  document.setTextColor(95, 117, 112);
  document.text(`${options.session.clinic.name} · ${options.subtitle}`, 43, 20);
  document.setDrawColor(218, 225, 219);
  document.line(14, 28, width - 14, 28);
}

function drawSummary(document: jsPDF, summary: Array<[string, string]>, startY: number, palette: ReportPalette) {
  const pageWidth = document.internal.pageSize.getWidth();
  const availableWidth = pageWidth - 28;
  const columns = Math.min(4, summary.length);
  const gap = 3;
  const boxWidth = (availableWidth - gap * (columns - 1)) / columns;
  const rows = Math.ceil(summary.length / columns);

  summary.forEach(([label, value], index) => {
    const column = index % columns;
    const row = Math.floor(index / columns);
    const x = 14 + column * (boxWidth + gap);
    const y = startY + row * 17;
    document.setFillColor(...palette.soft);
    document.setDrawColor(...palette.softBorder);
    document.roundedRect(x, y, boxWidth, 14, 2, 2, "FD");
    document.setTextColor(91, 112, 107);
    document.setFont("helvetica", "normal");
    document.setFontSize(7);
    document.text(label, x + 3, y + 5);
    document.setTextColor(...palette.primary);
    document.setFont("helvetica", "bold");
    document.setFontSize(10);
    document.text(document.splitTextToSize(value, boxWidth - 6)[0], x + 3, y + 10.5);
  });
  return startY + rows * 17 + 2;
}

function drawFooter(document: jsPDF, session: Session, page: number, pages: number, palette: ReportPalette) {
  const width = document.internal.pageSize.getWidth();
  const height = document.internal.pageSize.getHeight();
  document.setDrawColor(224, 229, 225);
  document.line(14, height - 11, width - 14, height - 11);
  document.setFont("helvetica", "normal");
  document.setFontSize(7);
  document.setTextColor(...palette.footer);
  document.text(`Clínica Leve · ${session.clinic.name}`, 14, height - 6.5);
  document.text(`Página ${page} de ${pages}`, width - 14, height - 6.5, { align: "right" });
}

function reportPalette(session: Session) {
  const { colors } = clinicTheme(session.clinic.themeKey);
  return {
    primary: hexToRgb(colors.primary),
    deep: hexToRgb(colors.deep),
    medium: hexToRgb(colors.medium),
    soft: hexToRgb(colors.soft),
    softBorder: mixWithWhite(hexToRgb(colors.accent), 0.7),
    footer: mixWithWhite(hexToRgb(colors.primary), 0.42),
  };
}

function mixWithWhite(color: [number, number, number], whiteAmount: number): [number, number, number] {
  return color.map((channel) => Math.round(channel + (255 - channel) * whiteAmount)) as [number, number, number];
}

export async function loadLogo(url?: string): Promise<LogoAsset | null> {
  if (!url) return null;
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const dataUrl = await blobToDataUrl(await response.blob());
    const dimensions = await imageDimensions(dataUrl);
    return { dataUrl, ...dimensions };
  } catch {
    return null;
  }
}

function blobToDataUrl(blob: Blob) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(blob);
  });
}

function imageDimensions(source: string) {
  return new Promise<{ width: number; height: number }>((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve({ width: image.naturalWidth, height: image.naturalHeight });
    image.onerror = reject;
    image.src = source;
  });
}
