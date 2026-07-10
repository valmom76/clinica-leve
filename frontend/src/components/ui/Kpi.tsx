import type { LucideIcon } from "lucide-react";

type KpiProps = {
  icon: LucideIcon;
  label: string;
  value: string;
  tone: string;
};

export function Kpi({ icon: Icon, label, value, tone }: KpiProps) {
  return (
    <article className="kpi">
      <span className={tone}><Icon size={21} /></span>
      <div><small>{label}</small><strong>{value}</strong></div>
    </article>
  );
}
