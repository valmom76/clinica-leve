import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { MaterialCategory, Session, StockMaterial } from "../../types";

type MaterialModalProps = {
  session: Session;
  categories: MaterialCategory[];
  material?: StockMaterial;
  onClose: () => void;
  onCreated: (material: StockMaterial) => void;
};

const units = ["un", "cx", "pct", "kit", "par", "rolo", "frasco", "ml", "l", "g", "kg"];

export function MaterialModal({
  session,
  categories,
  material,
  onClose,
  onCreated,
}: MaterialModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    try {
      const payload = {
        name: String(data.get("name")),
        categoryId: String(data.get("categoryId")),
        sku: String(data.get("sku")) || undefined,
        unit: String(data.get("unit")),
        minimumStock: Number(data.get("minimumStock")),
        lotControlled: data.get("lotControlled") === "on",
      };
      onCreated(material
        ? await api.updateMaterial(session, material.id, payload)
        : await api.createMaterial(session, payload));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao cadastrar material");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title={material ? "Editar material" : "Novo material"}
      description="Defina o cadastro e quando o sistema deverá alertar reposição."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label className="full">Nome<input name="name" defaultValue={material?.name} required maxLength={160} /></label>
          <label>
            Categoria
            <select name="categoryId" required defaultValue={material?.categoryId ?? ""}>
              <option value="" disabled>Selecione</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <label>Código interno<input name="sku" defaultValue={material?.sku} maxLength={80} /></label>
          <label>
            Unidade
            <select name="unit" defaultValue={material?.unit ?? "un"} required>
              {units.map((unit) => <option key={unit} value={unit}>{unit}</option>)}
            </select>
          </label>
          <label>
            Estoque mínimo
            <input name="minimumStock" type="number" min="0" step="0.001" defaultValue={material?.minimumStock ?? 0} required />
          </label>
          <label className="access-checkbox full">
            <input name="lotControlled" type="checkbox" defaultChecked={material?.lotControlled} />
            <span>Controlar lote e validade nas entradas</span>
          </label>
        </div>
        <ModalActions saving={saving} onClose={onClose} label={material ? "Salvar alterações" : "Cadastrar material"} />
      </form>
    </Modal>
  );
}
