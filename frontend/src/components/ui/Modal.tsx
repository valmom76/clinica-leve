import type { ReactNode } from "react";
import { X } from "lucide-react";

type ModalProps = {
  title: string;
  description: string;
  onClose: () => void;
  children: ReactNode;
};

export function Modal({
  title,
  description,
  onClose,
  children,
}: ModalProps) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-heading">
          <div>
            <span className="eyebrow">CLÍNICA LEVE</span>
            <h2>{title}</h2>
            <p>{description}</p>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Fechar">
            <X size={18} />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}

type ModalActionsProps = {
  saving: boolean;
  onClose: () => void;
  label: string;
};

export function ModalActions({
  saving,
  onClose,
  label,
}: ModalActionsProps) {
  return (
    <div className="modal-actions">
      <button type="button" className="secondary-button" onClick={onClose}>
        Cancelar
      </button>
      <button className="primary-button" disabled={saving}>
        {saving ? "Salvando..." : label}
      </button>
    </div>
  );
}
