import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { FinancialEntry, Session } from "../../types";
import { currency } from "./financeUtils";

export function SettleModal({ session, entry, onClose, onSaved }: {
  session: Session; entry: FinancialEntry; onClose: () => void; onSaved: (entry: FinancialEntry) => void;
}) {
  const [saving, setSaving] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(""); const data = new FormData(event.currentTarget);
    try { onSaved(await api.settleFinancialEntry(session, entry.id, {
      paymentDate: String(data.get("paymentDate")), paymentMethod: String(data.get("paymentMethod")),
    })); } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao baixar lançamento"); }
    finally { setSaving(false); }
  }
  return <Modal title="Baixar lançamento" description={`${entry.description} · ${currency(entry.amount)}`} onClose={onClose}>
    <form onSubmit={submit}>{error && <div className="form-error">{error}</div>}
      <div className="form-grid">
        <label>Data da baixa<input name="paymentDate" type="date" defaultValue={new Date().toISOString().slice(0, 10)} required /></label>
        <label>Forma de pagamento<select name="paymentMethod" required><option value="PIX">PIX</option><option value="DINHEIRO">Dinheiro</option><option value="CARTAO">Cartão</option><option value="TRANSFERENCIA">Transferência</option><option value="BOLETO">Boleto</option><option value="OUTRO">Outro</option></select></label>
      </div><ModalActions saving={saving} onClose={onClose} label="Confirmar baixa" />
    </form>
  </Modal>;
}
