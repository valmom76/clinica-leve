import { HeartPulse, LogOut, X } from "lucide-react";
import type { Session } from "../../types";
import { navItems, type ViewKey } from "../../app/navigation";

type SidebarProps = {
  session: Session;
  view: ViewKey;
  open: boolean;
  onNavigate: (view: ViewKey) => void;
  onClose: () => void;
  onLogout: () => void;
};

export function Sidebar({
  session,
  view,
  open,
  onNavigate,
  onClose,
  onLogout,
}: SidebarProps) {
  const initials = session.user.name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("");

  return (
    <aside className={`sidebar ${open ? "open" : ""}`}>
      <div className="brand">
        <span className="brand-symbol"><HeartPulse size={21} /></span>
        <strong>Clínica <em>Leve</em></strong>
        <button
          className="mobile-only icon-button"
          onClick={onClose}
          aria-label="Fechar menu"
        >
          <X size={18} />
        </button>
      </div>

      <nav>
        <span className="nav-caption">OPERAÇÃO</span>
        {navItems.slice(0, 5).map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            className={view === key ? "active" : ""}
            onClick={() => onNavigate(key)}
          >
            <Icon size={18} /><span>{label}</span>
          </button>
        ))}

        <span className="nav-caption management">GESTÃO</span>
        {navItems.slice(5).map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            className={view === key ? "active" : ""}
            onClick={() => onNavigate(key)}
          >
            <Icon size={18} /><span>{label}</span>
          </button>
        ))}
      </nav>

      <div className="sidebar-user">
        <span>{initials}</span>
        <div>
          <strong>{session.user.name}</strong>
          <small>{session.clinic.name}</small>
        </div>
        <button onClick={onLogout} title="Sair" aria-label="Sair">
          <LogOut size={17} />
        </button>
      </div>
    </aside>
  );
}
