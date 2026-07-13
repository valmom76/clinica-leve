import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { Session, TimeDaySummary, TimeEntry, TimeEntryType } from "../../types";
import { clinicDateTime, timeEntryLabel } from "./timeClockUtils";

type TimeEntryModalProps = {
  session: Session;
  summary: TimeDaySummary;
  entry?: TimeEntry;
  onClose: () => void;
  onSaved: () => void;
};

export function TimeEntryModal({ session, summary, entry, onClose, onSaved }: TimeEntryModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const defaultType = entry?.type ?? suggestedType(summary);
  const now = clinicDateTime(session.clinic.timezone);
  const defaultOccurredAt = entry?.occurredAt.slice(0, 16)
    ?? (now.startsWith(summary.date) ? now : `${summary.date}T08:00`);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const occurredAt = String(data.get("occurredAt"));
    const payload = {
      type: String(data.get("type")) as TimeEntryType,
      occurredAt: occurredAt.length === 16 ? `${occurredAt}:00` : occurredAt,
      notes: String(data.get("notes")) || undefined,
    };

    try {
      if (entry) {
        await api.updateTimeEntry(session, entry.id, payload);
      } else {
        await api.createTimeEntry(session, { ...payload, userId: summary.userId });
      }
      onSaved();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar a marcação");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title={entry ? "Corrigir marcação" : "Adicionar marcação"}
      description={`${summary.userName} · ${summary.date.split("-").reverse().join("/")}`}
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label>
            Tipo de marcação
            <select name="type" defaultValue={defaultType} required>
              {(Object.keys(timeEntryLabel) as TimeEntryType[]).map((type) => (
                <option key={type} value={type}>{timeEntryLabel[type]}</option>
              ))}
            </select>
          </label>
          <label>
            Data e hora
            <input
              name="occurredAt"
              type="datetime-local"
              defaultValue={defaultOccurredAt}
              min={`${summary.date}T00:00`}
              max={`${summary.date}T23:59`}
              required
            />
          </label>
          <label className="full">
            Motivo ou observação
            <textarea name="notes" rows={3} defaultValue={entry?.notes} maxLength={500} />
          </label>
          <p className="form-hint full">
            Ajustes manuais ficam identificados no histórico para facilitar a conferência da jornada.
          </p>
        </div>
        <ModalActions saving={saving} onClose={onClose} label="Salvar marcação" />
      </form>
    </Modal>
  );
}

function suggestedType(summary: TimeDaySummary): TimeEntryType {
  if (summary.status === "NOT_STARTED") return "CLOCK_IN";
  if (summary.status === "ON_BREAK") return "BREAK_END";
  if (summary.status === "WORKING") return "CLOCK_OUT";
  return "BREAK_START";
}
