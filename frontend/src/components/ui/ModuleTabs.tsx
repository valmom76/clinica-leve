import type { LucideIcon } from "lucide-react";

type ModuleTab<T extends string> = {
  key: T;
  label: string;
  icon: LucideIcon;
};

export function ModuleTabs<T extends string>({ items, active, onChange }: {
  items: Array<ModuleTab<T>>;
  active: T;
  onChange: (key: T) => void;
}) {
  return (
    <div className="module-tabs" role="tablist" aria-label="Seções do módulo">
      {items.map(({ key, label, icon: Icon }) => (
        <button
          key={key}
          type="button"
          role="tab"
          aria-selected={active === key}
          className={active === key ? "active" : ""}
          onClick={() => onChange(key)}
        >
          <Icon size={18} />{label}
        </button>
      ))}
    </div>
  );
}
