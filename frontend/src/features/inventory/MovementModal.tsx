import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { Session, StockMaterial, StockMovementType } from "../../types";
import { formatQuantity } from "./inventoryUtils";

type MovementModalProps = {
  session: Session;
  material: StockMaterial;
  initialType: StockMovementType;
  onClose: () => void;
  onSaved: (material: StockMaterial) => void;
};

export function MovementModal({
  session,
  material,
  initialType,
  onClose,
  onSaved,
}: MovementModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [type, setType] = useState<StockMovementType>(initialType);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    try {
      onSaved(await api.moveStock(session, material.id, {
        type,
        quantity: Number(data.get("quantity")),
        reason: String(data.get("reason")),
        lotNumber: type === "ENTRY" ? String(data.get("lotNumber")) || undefined : undefined,
        expirationDate: type === "ENTRY" ? String(data.get("expirationDate")) || undefined : undefined,
      }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao movimentar estoque");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title={`Movimentar · ${material.name}`}
      description={`Saldo atual: ${formatQuantity(material.currentStock, material.unit)}`}
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="movement-selector">
          <button type="button" className={type === "ENTRY" ? "active" : ""} onClick={() => setType("ENTRY")}>Entrada</button>
          <button type="button" className={type === "EXIT" ? "active exit" : ""} onClick={() => setType("EXIT")}>Saída</button>
        </div>
        <div className="form-grid">
          <label>
            Quantidade ({material.unit})
            <input name="quantity" type="number" min="0.001" step="0.001" required />
          </label>
          <label>
            Motivo
            <input name="reason" placeholder={type === "ENTRY" ? "Compra ou reposição" : "Uso em atendimento"} required maxLength={300} />
          </label>
          {type === "ENTRY" && material.lotControlled && (
            <>
              <label>Lote<input name="lotNumber" required maxLength={80} /></label>
              <label>Validade<input name="expirationDate" type="date" /></label>
            </>
          )}
          {type === "EXIT" && material.lotControlled && (
            <p className="form-hint full">A saída consumirá primeiro os lotes com validade mais próxima.</p>
          )}
        </div>
        <ModalActions
          saving={saving}
          onClose={onClose}
          label={type === "ENTRY" ? "Registrar entrada" : "Registrar saída"}
        />
      </form>
    </Modal>
  );
}
