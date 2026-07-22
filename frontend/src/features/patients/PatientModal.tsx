import { type FormEvent, useState } from "react";
import { MessageCircle } from "lucide-react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { Patient, Session } from "../../types";

type PatientModalProps = {
  session: Session;
  patient?: Patient;
  onClose: () => void;
  onSaved: (patient: Patient) => void;
};

export function PatientModal({ session, patient, onClose, onSaved }: PatientModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const payload = {
      name: String(data.get("name")),
      phone: String(data.get("phone")),
      email: String(data.get("email")) || undefined,
      cpf: String(data.get("cpf")) || undefined,
      birthDate: String(data.get("birthDate")) || undefined,
      whatsappOptIn: data.get("whatsappOptIn") === "on",
    };

    try {
      onSaved(patient
        ? await api.updatePatient(session, patient.id, payload)
        : await api.createPatient(session, payload));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar o paciente");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title={patient ? "Editar paciente" : "Novo paciente"}
      description="Os dados e a autorização de contato ficam vinculados somente a esta clínica."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label className="full">Nome<input name="name" required defaultValue={patient?.name ?? ""} /></label>
          <label>Telefone<input name="phone" required defaultValue={patient?.phone ?? ""} /></label>
          <label>E-mail<input name="email" type="email" defaultValue={patient?.email ?? ""} /></label>
          <label>CPF<input name="cpf" defaultValue={patient?.cpf ?? ""} /></label>
          <label>Data de nascimento<input name="birthDate" type="date" defaultValue={patient?.birthDate ?? ""} /></label>
          <label className="access-checkbox full">
            <input name="whatsappOptIn" type="checkbox" defaultChecked={patient?.whatsappOptIn ?? false} />
            <span><MessageCircle size={16} />O paciente autorizou confirmações e lembretes pelo WhatsApp</span>
          </label>
          <p className="form-hint full">Marque somente após registrar uma manifestação clara do paciente. A data da autorização será gravada pelo servidor.</p>
        </div>
        <ModalActions saving={saving} onClose={onClose} label={patient ? "Salvar alterações" : "Cadastrar paciente"} />
      </form>
    </Modal>
  );
}
