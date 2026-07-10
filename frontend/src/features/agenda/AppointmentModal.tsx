import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type {
  Appointment,
  Patient,
  Professional,
  Session,
  Specialty,
} from "../../types";
import { tomorrowAtNine } from "../../utils/dates";

type AppointmentModalProps = {
  session: Session;
  patients: Patient[];
  professionals: Professional[];
  specialties: Specialty[];
  onClose: () => void;
  onCreated: (appointment: Appointment) => void;
};

export function AppointmentModal({
  session,
  patients,
  professionals,
  specialties,
  onClose,
  onCreated,
}: AppointmentModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const start = new Date(String(data.get("startAt")));
    const end = new Date(start.getTime() + 60 * 60 * 1000);

    try {
      const appointment = await api.createAppointment(session, {
        patientId: String(data.get("patientId")),
        professionalId: String(data.get("professionalId")),
        specialtyId: String(data.get("specialtyId")),
        startAt: start.toISOString(),
        endAt: end.toISOString(),
        status: "CONFIRMED",
        notes: String(data.get("notes")) || undefined,
      });
      onCreated(appointment);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao agendar");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title="Novo agendamento"
      description="O horário será validado no backend antes de ser salvo."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label className="full">
            Paciente
            <select name="patientId" required defaultValue="">
              <option value="" disabled>Selecione</option>
              {patients.map((patient) => (
                <option key={patient.id} value={patient.id}>{patient.name}</option>
              ))}
            </select>
          </label>
          <label>
            Profissional
            <select name="professionalId" required defaultValue="">
              <option value="" disabled>Selecione</option>
              {professionals.map((professional) => (
                <option key={professional.id} value={professional.id}>
                  {professional.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Especialidade
            <select name="specialtyId" required defaultValue="">
              <option value="" disabled>Selecione</option>
              {specialties.map((specialty) => (
                <option key={specialty.id} value={specialty.id}>
                  {specialty.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Data e horário
            <input
              name="startAt"
              type="datetime-local"
              defaultValue={tomorrowAtNine()}
              required
            />
          </label>
          <label className="full">
            Observações
            <textarea name="notes" rows={3} />
          </label>
        </div>
        <ModalActions
          saving={saving}
          onClose={onClose}
          label="Confirmar agendamento"
        />
      </form>
    </Modal>
  );
}
