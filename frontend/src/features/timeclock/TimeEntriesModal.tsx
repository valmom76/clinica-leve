import { Pencil, Plus, Trash2 } from "lucide-react";
import { Modal } from "../../components/ui/Modal";
import type { TimeDaySummary, TimeEntry } from "../../types";
import { formatMinutes, timeEntryLabel, timeFromLocalDateTime } from "./timeClockUtils";

type TimeEntriesModalProps = {
  summary: TimeDaySummary;
  deletingId?: string;
  onClose: () => void;
  onAdd: () => void;
  onEdit: (entry: TimeEntry) => void;
  onDelete: (entry: TimeEntry) => void;
};

export function TimeEntriesModal({
  summary,
  deletingId,
  onClose,
  onAdd,
  onEdit,
  onDelete,
}: TimeEntriesModalProps) {
  return (
    <Modal
      title={`Marcações de ${summary.userName}`}
      description={`${summary.date.split("-").reverse().join("/")} · ${formatMinutes(summary.workedMinutes)} trabalhadas`}
      onClose={onClose}
    >
      <div className="time-modal-actions">
        <button className="primary-button" onClick={onAdd}>
          <Plus size={17} />Adicionar marcação
        </button>
      </div>
      <div className="time-entry-list management-list">
        {summary.entries.length === 0 && (
          <p className="time-empty-message">Nenhuma marcação registrada neste dia.</p>
        )}
        {summary.entries.map((entry) => (
          <article key={entry.id}>
            <span className={`time-entry-dot ${entry.type.toLowerCase()}`} />
            <time>{timeFromLocalDateTime(entry.occurredAt)}</time>
            <div>
              <strong>{timeEntryLabel[entry.type]}</strong>
              <small>
                {entry.source === "MANUAL" ? "Ajuste manual" : "Registrado pelo funcionário"}
                {entry.edited ? " · Editado" : ""}
              </small>
              {entry.notes && <p>{entry.notes}</p>}
            </div>
            <div className="inventory-row-actions">
              <button title="Editar marcação" aria-label="Editar marcação" onClick={() => onEdit(entry)}>
                <Pencil size={16} />
              </button>
              <button
                title="Excluir marcação"
                aria-label="Excluir marcação"
                disabled={deletingId === entry.id}
                onClick={() => onDelete(entry)}
              >
                <Trash2 size={16} />
              </button>
            </div>
          </article>
        ))}
      </div>
    </Modal>
  );
}
