import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { Patient, Session } from "../../types";

type PatientModalProps = {
  session: Session;
  onClose: () => void;
  onCreated: (patient: Patient) => void;
};

export function PatientModal({
  session,
  onClose,
  onCreated,
}: PatientModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);

    try {
      const patient = await api.createPatient(session, {
        name: String(data.get("name")),
        phone: String(data.get("phone")),
        email: String(data.get("email")) || undefined,
        cpf: String(data.get("cpf")) || undefined,
        birthDate: String(data.get("birthDate")) || undefined,
      });
      onCreated(patient);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao cadastrar");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title="Novo paciente"
      description="O cadastro será vinculado somente a esta clínica."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label className="full">Nome<input name="name" required /></label>
          <label>Telefone<input name="phone" required /></label>
          <label>E-mail<input name="email" type="email" /></label>
          <label>CPF<input name="cpf" /></label>
          <label>
            Data de nascimento
            <input name="birthDate" type="date" />
          </label>
        </div>
        <ModalActions
          saving={saving}
          onClose={onClose}
          label="Cadastrar paciente"
        />
      </form>
    </Modal>
  );
}
