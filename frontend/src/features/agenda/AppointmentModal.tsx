import { type FormEvent, useEffect, useState } from "react";
import { Clock3, MessageCircle, RotateCcw, Trash2 } from "lucide-react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type {
  Appointment,
  AppointmentMessage,
  AppointmentStatus,
  Patient,
  Professional,
  Session,
  Specialty,
} from "../../types";
import { statusLabel } from "../../utils/appointments";
import { tomorrowAtNine } from "../../utils/dates";

type AppointmentModalProps = {
  session: Session;
  appointment?: Appointment;
  patients: Patient[];
  professionals: Professional[];
  specialties: Specialty[];
  onClose: () => void;
  onSaved: (appointment: Appointment) => void;
};

const editableStatuses: AppointmentStatus[] = [
  "SCHEDULED",
  "CONFIRMED",
  "RESCHEDULE_REQUESTED",
  "WAITING",
  "IN_PROGRESS",
  "COMPLETED",
  "NO_SHOW",
];

const messageStatus: Record<AppointmentMessage["status"], string> = {
  PENDING: "Na fila",
  PROCESSING: "Enviando",
  SENT: "Enviada",
  DELIVERED: "Entregue",
  READ: "Lida",
  RESPONDED: "Respondida",
  FAILED: "Falhou",
  CANCELLED: "Cancelada",
};

export function AppointmentModal({
  session,
  appointment,
  patients,
  professionals,
  specialties,
  onClose,
  onSaved,
}: AppointmentModalProps) {
  const [saving, setSaving] = useState(false);
  const [messageAction, setMessageAction] = useState(false);
  const [messages, setMessages] = useState<AppointmentMessage[]>([]);
  const [historyLoading, setHistoryLoading] = useState(Boolean(appointment));
  const [error, setError] = useState("");
  const [patientId, setPatientId] = useState(appointment?.patientId ?? "");
  const [dirty, setDirty] = useState(false);
  const editing = Boolean(appointment);
  const selectedPatient = patients.find((patient) => patient.id === patientId);
  const persistedPatient = patients.find((patient) => patient.id === appointment?.patientId);

  useEffect(() => {
    if (!appointment) return;
    const load = () => api.appointmentMessages(session, appointment.id)
      .then(setMessages)
      .catch(() => undefined)
      .finally(() => setHistoryLoading(false));
    void load();
    const timer = window.setInterval(() => void load(), 10_000);
    return () => window.clearInterval(timer);
  }, [appointment, session]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const start = new Date(String(data.get("startAt")));
    const duration = Number(data.get("durationMinutes"));
    const end = new Date(start.getTime() + duration * 60_000);
    const payload = {
      patientId: String(data.get("patientId")),
      professionalId: String(data.get("professionalId")),
      specialtyId: String(data.get("specialtyId")),
      startAt: start.toISOString(),
      endAt: end.toISOString(),
      status: (appointment ? String(data.get("status")) : "SCHEDULED") as AppointmentStatus,
      notes: String(data.get("notes")) || undefined,
    };

    try {
      const saved = appointment
        ? await api.updateAppointment(session, appointment.id, payload)
        : await api.createAppointment(session, payload);
      onSaved(saved);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar o agendamento");
    } finally {
      setSaving(false);
    }
  }

  async function cancelAppointment() {
    if (!appointment || !window.confirm("Cancelar este agendamento e os lembretes pendentes?")) return;
    setSaving(true);
    setError("");
    try {
      onSaved(await api.cancelAppointment(session, appointment.id));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao cancelar o agendamento");
    } finally {
      setSaving(false);
    }
  }

  async function sendConfirmation() {
    if (!appointment) return;
    setMessageAction(true);
    setError("");
    try {
      const queued = await api.sendAppointmentConfirmation(session, appointment.id);
      setMessages((current) => [queued, ...current]);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao incluir a confirmação na fila");
    } finally {
      setMessageAction(false);
    }
  }

  const duration = appointment
    ? Math.max(15, Math.round((new Date(appointment.endAt).getTime() - new Date(appointment.startAt).getTime()) / 60_000))
    : 60;
  const canMessage = appointment && !["CANCELLED", "COMPLETED", "NO_SHOW"].includes(appointment.status);

  return (
    <Modal
      className="appointment-modal"
      title={editing ? "Detalhes do agendamento" : "Novo agendamento"}
      description={editing
        ? "Edite o horário, acompanhe a confirmação ou cancele o atendimento."
        : "O horário será validado antes de ser salvo."}
      onClose={onClose}
    >
      <form onSubmit={submit} onChange={() => setDirty(true)}>
        {error && <div className="form-error">{error}</div>}
        {appointment?.status === "RESCHEDULE_REQUESTED" && (
          <div className="agenda-attention"><RotateCcw size={18} /><span>O paciente pediu reagendamento. Escolha o novo horário e salve.</span></div>
        )}
        <div className="form-grid">
          <label className="full">
            Paciente
            <select name="patientId" required value={patientId} onChange={(event) => setPatientId(event.target.value)}>
              <option value="" disabled>Selecione</option>
              {patients.map((patient) => <option key={patient.id} value={patient.id}>{patient.name}</option>)}
            </select>
          </label>
          {selectedPatient && !selectedPatient.whatsappOptIn && <p className="form-hint full">Este paciente ainda não autorizou contato pelo WhatsApp. A consulta será salva, mas nenhuma mensagem entrará na fila até o cadastro ser atualizado.</p>}
          <label>
            Profissional
            <select name="professionalId" required defaultValue={appointment?.professionalId ?? ""}>
              <option value="" disabled>Selecione</option>
              {professionals.map((professional) => (
                <option key={professional.id} value={professional.id}>{professional.name}</option>
              ))}
            </select>
          </label>
          <label>
            Especialidade
            <select name="specialtyId" required defaultValue={appointment?.specialtyId ?? ""}>
              <option value="" disabled>Selecione</option>
              {specialties.map((specialty) => <option key={specialty.id} value={specialty.id}>{specialty.name}</option>)}
            </select>
          </label>
          <label>
            Data e horário
            <input name="startAt" type="datetime-local" defaultValue={appointment ? localInput(appointment.startAt) : tomorrowAtNine()} required />
          </label>
          <label>
            Duração
            <select name="durationMinutes" defaultValue={String(duration)}>
              {[15, 30, 45, 60, 90, 120].map((minutes) => <option key={minutes} value={minutes}>{minutes} minutos</option>)}
            </select>
          </label>
          {appointment && <label className="full">
            Status
            <select name="status" defaultValue={appointment.status}>
              {editableStatuses.map((status) => <option key={status} value={status}>{statusLabel[status]}</option>)}
            </select>
          </label>}
          <label className="full">
            Observações
            <textarea name="notes" rows={3} defaultValue={appointment?.notes ?? ""} />
          </label>
        </div>

        {appointment && <div className="appointment-quick-actions">
          {canMessage && <button type="button" className="secondary-button" disabled={messageAction || saving || dirty || !persistedPatient?.whatsappOptIn} title={dirty ? "Salve as alterações antes de enviar" : !persistedPatient?.whatsappOptIn ? "Registre a autorização no cadastro do paciente" : undefined} onClick={() => void sendConfirmation()}>
            <MessageCircle size={17} />{messageAction ? "Incluindo..." : "Enviar confirmação agora"}
          </button>}
          {appointment.status !== "CANCELLED" && appointment.status !== "COMPLETED" && (
            <button type="button" className="danger-button" disabled={saving} onClick={() => void cancelAppointment()}>
              <Trash2 size={17} />Cancelar agendamento
            </button>
          )}
        </div>}

        <ModalActions saving={saving} onClose={onClose} label={editing ? "Salvar alterações" : "Criar agendamento"} />
      </form>

      {appointment && <section className="message-history">
        <div><h3><Clock3 size={17} />Histórico de mensagens</h3><small>WhatsApp · confirmações e lembretes</small></div>
        {historyLoading ? <p>Carregando histórico...</p> : messages.length === 0 ? <p>Nenhuma mensagem programada para esta consulta.</p> : (
          <ul>{messages.map((message) => <li key={message.id}>
            <i className={`message-state ${message.status.toLowerCase()}`} />
            <span><strong>{message.purpose === "CONFIRMATION" ? "Confirmação" : "Lembrete"}</strong><small>{formatDate(message.scheduledAt)} · {messageStatus[message.status]}{message.responseAction ? ` · ${message.responseAction === "CONFIRMED" ? "Paciente confirmou" : "Pediu reagendamento"}` : ""}</small>{message.errorMessage && <em>{message.errorMessage}</em>}</span>
          </li>)}</ul>
        )}
      </section>}
    </Modal>
  );
}

function localInput(value: string) {
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
}
