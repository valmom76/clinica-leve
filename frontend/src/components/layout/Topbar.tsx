import { Bell, Menu, Search } from "lucide-react";

type TopbarProps = {
  clinicName: string;
  onOpenSidebar: () => void;
};

export function Topbar({ clinicName, onOpenSidebar }: TopbarProps) {
  return (
    <header className="topbar">
      <button
        className="mobile-only icon-button"
        onClick={onOpenSidebar}
        aria-label="Abrir menu"
      >
        <Menu size={19} />
      </button>
      <div className="global-search">
        <Search size={17} />
        <input
          aria-label="Busca global"
          placeholder="Buscar pacientes, profissionais ou registros..."
        />
      </div>
      <button className="icon-button notification" aria-label="Notificações">
        <Bell size={19} /><i />
      </button>
      <span className="clinic-chip">{clinicName}</span>
    </header>
  );
}
