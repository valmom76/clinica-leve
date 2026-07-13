import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { FinancialCategory, FinancialEntry, FinancialEntryType, Session } from "../../types";

export function FinancialEntryModal({ session, categories, entry, onClose, onSaved }: {
  session: Session; categories: FinancialCategory[]; entry?: FinancialEntry;
  onClose: () => void; onSaved: (entry: FinancialEntry) => void;
}) {
  const [type, setType] = useState<FinancialEntryType>(entry?.type ?? "INCOME");
  const [saving, setSaving] = useState(false); const [error, setError] = useState("");
  const available = categories.filter((category) => category.type === type);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(""); const data = new FormData(event.currentTarget);
    const payload = {
      description: String(data.get("description")), type, categoryId: String(data.get("categoryId")),
      amount: Number(data.get("amount")), dueDate: String(data.get("dueDate")),
      counterparty: String(data.get("counterparty")) || undefined, notes: String(data.get("notes")) || undefined,
    };
    try { onSaved(entry ? await api.updateFinancialEntry(session, entry.id, payload) : await api.createFinancialEntry(session, payload)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao salvar lançamento"); }
    finally { setSaving(false); }
  }
  return <Modal title={entry ? "Editar lançamento" : "Novo lançamento"} description="Registre uma conta a pagar ou a receber." onClose={onClose}>
    <form onSubmit={submit}>{error && <div className="form-error">{error}</div>}
      <div className="movement-selector">
        <button type="button" className={type === "INCOME" ? "active" : ""} onClick={() => setType("INCOME")}>Receita</button>
        <button type="button" className={type === "EXPENSE" ? "active exit" : ""} onClick={() => setType("EXPENSE")}>Despesa</button>
      </div>
      <div className="form-grid">
        <label className="full">Descrição<input name="description" defaultValue={entry?.description} required maxLength={180} /></label>
        <label>Categoria<select key={type} name="categoryId" defaultValue={entry?.type === type ? entry.categoryId : ""} required>
          <option value="" disabled>Selecione</option>{available.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select></label>
        <label>Valor<input name="amount" type="number" min="0.01" step="0.01" defaultValue={entry?.amount} required /></label>
        <label>Vencimento<input name="dueDate" type="date" defaultValue={entry?.dueDate ?? new Date().toISOString().slice(0, 10)} required /></label>
        <label>Cliente/fornecedor<input name="counterparty" defaultValue={entry?.counterparty} maxLength={160} /></label>
        <label className="full">Observações<textarea name="notes" rows={3} defaultValue={entry?.notes} maxLength={500} /></label>
      </div><ModalActions saving={saving} onClose={onClose} label="Salvar lançamento" />
    </form>
  </Modal>;
}
