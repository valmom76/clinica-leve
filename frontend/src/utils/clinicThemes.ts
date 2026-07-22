import type { ClinicTheme } from "../types";

export type ClinicThemeOption = {
  key: ClinicTheme;
  name: string;
  description: string;
  colors: {
    primary: string;
    deep: string;
    medium: string;
    accent: string;
    soft: string;
    chartSecondary: string;
  };
};

export const DEFAULT_CLINIC_THEME: ClinicTheme = "CLINICAL_SERENE";

export const CLINIC_THEMES: ClinicThemeOption[] = [
  {
    key: "CLINICAL_SERENE",
    name: "Clínico Sereno",
    description: "Equilíbrio entre serenidade, acolhimento e confiança.",
    colors: { primary: "#0b4a51", deep: "#153c43", medium: "#3c716e", accent: "#5e9f89", soft: "#eaf1ec", chartSecondary: "#5f8394" },
  },
  {
    key: "BLUE_TRUST",
    name: "Azul Confiança",
    description: "Institucional, preciso e adequado a clínicas médicas.",
    colors: { primary: "#1d4e6d", deep: "#173c56", medium: "#2f6c8f", accent: "#5d96b3", soft: "#e7f1f6", chartSecondary: "#527f91" },
  },
  {
    key: "VITAL_GREEN",
    name: "Verde Vital",
    description: "Natural, saudável e com uma presença mais viva.",
    colors: { primary: "#235846", deep: "#183f34", medium: "#397760", accent: "#63a17f", soft: "#e7f2eb", chartSecondary: "#557f91" },
  },
  {
    key: "WELCOMING_LAVENDER",
    name: "Lavanda Acolhedora",
    description: "Suave e humano para psicologia e terapias.",
    colors: { primary: "#54486f", deep: "#3d3455", medium: "#6f6091", accent: "#9b88bd", soft: "#f0ecf6", chartSecondary: "#6d849e" },
  },
  {
    key: "HUMAN_TERRACOTTA",
    name: "Terracota Humana",
    description: "Quente, próxima e voltada ao cuidado pessoal.",
    colors: { primary: "#75483d", deep: "#57362f", medium: "#925c4e", accent: "#bd806e", soft: "#f6ece8", chartSecondary: "#527b78" },
  },
  {
    key: "TECHNOLOGICAL_GRAPHITE",
    name: "Grafite Tecnológico",
    description: "Contemporâneo, compacto e de caráter corporativo.",
    colors: { primary: "#33484f", deep: "#26383e", medium: "#496871", accent: "#78a1a8", soft: "#eaf0f1", chartSecondary: "#607e93" },
  },
];

export function normalizeClinicTheme(theme?: ClinicTheme): ClinicTheme {
  return CLINIC_THEMES.some((option) => option.key === theme) ? theme! : DEFAULT_CLINIC_THEME;
}

export function clinicTheme(theme?: ClinicTheme) {
  const normalized = normalizeClinicTheme(theme);
  return CLINIC_THEMES.find((option) => option.key === normalized)!;
}

export function applyClinicTheme(theme?: ClinicTheme) {
  document.documentElement.dataset.theme = normalizeClinicTheme(theme);
}

export function hexToRgb(hex: string): [number, number, number] {
  const value = hex.replace("#", "");
  return [
    Number.parseInt(value.slice(0, 2), 16),
    Number.parseInt(value.slice(2, 4), 16),
    Number.parseInt(value.slice(4, 6), 16),
  ];
}
