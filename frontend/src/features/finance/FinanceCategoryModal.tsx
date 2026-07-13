import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { FinancialCategory, FinancialEntryType, Session } from "../../types";

export function FinanceCategoryModal({ session, onClose, onCreated }: {
  session: Session; onClose: () => void; onCreated: (category: FinancialCategory) => void;
}) {
  const [saving, setSaving] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(""); const data = new FormData(event.currentTarget);
    try { onCreated(await api.createFinancialCategory(session, {
      name: String(data.get("name")), type: String(data.get("type")) as FinancialEntryType,
    })); } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao criar categoria"); }
    finally { setSaving(false); }
  }
  return <Modal title="Nova categoria financeira" description="Classifique receitas e despesas da clínica." onClose={onClose}>
    <form onSubmit={submit}>{error && <div className="form-error">{error}</div>}
      <div className="form-grid">
        <label>Tipo<select name="type"><option value="INCOME">Receita</option><option value="EXPENSE">Despesa</option></select></label>
        <label>Nome<input name="name" required maxLength={120} /></label>
      </div><ModalActions saving={saving} onClose={onClose} label="Criar categoria" />
    </form>
  </Modal>;
}
