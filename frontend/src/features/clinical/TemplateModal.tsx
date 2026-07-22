import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type {
  ClinicalDocumentType,
  ClinicalPlaceholder,
  ClinicalTemplate,
  Session,
} from "../../types";
import { documentTypeOptions } from "./clinicalLabels";

type TemplateModalProps = {
  session: Session;
  template?: ClinicalTemplate;
  placeholders: ClinicalPlaceholder[];
  onClose: () => void;
  onSaved: (template: ClinicalTemplate) => void;
};

export function TemplateModal({
  session,
  template,
  placeholders,
  onClose,
  onSaved,
}: TemplateModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const payload = {
      type: String(data.get("type")) as ClinicalDocumentType,
      name: String(data.get("name")),
      titleTemplate: String(data.get("titleTemplate")),
      bodyTemplate: String(data.get("bodyTemplate")),
      favorite: data.get("favorite") === "on",
      active: data.get("active") === "on",
    };
    try {
      const saved = template
        ? await api.updateClinicalTemplate(session, template.id, payload)
        : await api.createClinicalTemplate(session, payload);
      onSaved(saved);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar o modelo");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      className="clinical-template-modal"
      title={template ? "Editar modelo" : "Novo modelo"}
      description="Use campos automáticos e deixe apenas o trecho variável para o profissional completar."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label>
            Tipo de documento
            <select name="type" defaultValue={template?.type ?? "CLINICAL_REPORT"} required>
              {documentTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <label>
            Nome do modelo
            <input name="name" defaultValue={template?.name} maxLength={160} required />
          </label>
          <label className="full">
            Título gerado
            <input
              name="titleTemplate"
              defaultValue={template?.titleTemplate ?? "Documento - {{paciente.nome}}"}
              maxLength={240}
              required
            />
          </label>
          <label className="full">
            Conteúdo do modelo
            <textarea name="bodyTemplate" rows={13} defaultValue={template?.bodyTemplate} required />
          </label>
          <div className="clinical-placeholders full">
            <strong>Campos automáticos disponíveis</strong>
            <p>Clique para copiar e cole no título ou conteúdo.</p>
            <div>
              {placeholders.map((placeholder) => (
                <button
                  key={placeholder.key}
                  type="button"
                  title={placeholder.description}
                  onClick={() => void navigator.clipboard.writeText(placeholder.key)}
                >
                  {placeholder.key}
                </button>
              ))}
            </div>
          </div>
          <label className="access-checkbox">
            <input name="favorite" type="checkbox" defaultChecked={template?.favorite} />
            <span>Destacar como favorito</span>
          </label>
          <label className="access-checkbox">
            <input name="active" type="checkbox" defaultChecked={template?.active ?? true} />
            <span>Modelo ativo</span>
          </label>
        </div>
        <ModalActions
          saving={saving}
          onClose={onClose}
          label={template ? "Salvar nova versão" : "Criar modelo"}
        />
      </form>
    </Modal>
  );
}
