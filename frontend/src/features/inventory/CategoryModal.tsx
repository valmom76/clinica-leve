import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { MaterialCategory, Session } from "../../types";

type CategoryModalProps = {
  session: Session;
  onClose: () => void;
  onCreated: (category: MaterialCategory) => void;
};

export function CategoryModal({ session, onClose, onCreated }: CategoryModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    try {
      onCreated(await api.createMaterialCategory(session, String(data.get("name"))));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao criar categoria");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title="Nova categoria"
      description="Crie uma classificação própria para os materiais da clínica."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label className="full">Nome da categoria<input name="name" required maxLength={120} /></label>
        </div>
        <ModalActions saving={saving} onClose={onClose} label="Criar categoria" />
      </form>
    </Modal>
  );
}
