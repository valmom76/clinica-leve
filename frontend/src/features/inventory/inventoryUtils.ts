export function formatQuantity(value: number, unit: string) {
  return `${new Intl.NumberFormat("pt-BR", {
    maximumFractionDigits: 3,
  }).format(value)} ${unit}`;
}

export function formatDate(value?: string) {
  if (!value) return "—";
  return new Date(`${value}T12:00:00`).toLocaleDateString("pt-BR");
}

export function expiresWithin(value: string | undefined, days: number) {
  if (!value) return false;
  const expiration = new Date(`${value}T23:59:59`).getTime();
  const limit = Date.now() + days * 86_400_000;
  return expiration <= limit;
}
