import { type FormEvent, useState } from "react";
import { Plus } from "lucide-react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { Professional, Session, Specialty } from "../../types";

type ProfessionalModalProps = {
  session: Session;
  professional?: Professional;
  specialties: Specialty[];
  canCreateAccess: boolean;
  onClose: () => void;
  onSpecialtyCreated: (specialty: Specialty) => void;
  onSaved: (professional: Professional, createAccess: boolean) => void;
};

export function ProfessionalModal({
  session,
  professional,
  specialties: initialSpecialties,
  canCreateAccess,
  onClose,
  onSpecialtyCreated,
  onSaved,
}: ProfessionalModalProps) {
  const [specialties, setSpecialties] = useState(initialSpecialties.filter((item) => item.active));
  const [specialtyId, setSpecialtyId] = useState(professional?.specialtyId ?? initialSpecialties[0]?.id ?? "");
  const [showNewSpecialty, setShowNewSpecialty] = useState(false);
  const [specialtyName, setSpecialtyName] = useState("");
  const [specialtyColor, setSpecialtyColor] = useState("#4f887b");
  const [specialtySaving, setSpecialtySaving] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function createSpecialty() {
    if (!specialtyName.trim()) {
      setError("Informe o nome da nova especialidade");
      return;
    }
    setSpecialtySaving(true);
    setError("");
    try {
      const created = await api.createSpecialty(session, {
        name: specialtyName.trim(),
        color: specialtyColor,
      });
      setSpecialties((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)));
      setSpecialtyId(created.id);
      setSpecialtyName("");
      setShowNewSpecialty(false);
      onSpecialtyCreated(created);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao criar especialidade");
    } finally {
      setSpecialtySaving(false);
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const payload = {
      name: String(data.get("name")),
      specialtyId,
      council: String(data.get("council")) || undefined,
      email: String(data.get("email")) || undefined,
      phone: String(data.get("phone")) || undefined,
    };
    try {
      const saved = professional
        ? await api.updateProfessional(session, professional.id, {
            ...payload,
            active: data.get("active") === "on",
          })
        : await api.createProfessional(session, payload);
      onSaved(saved, !professional && data.get("createAccess") === "on");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar profissional");
    } finally {
      setSaving(false);
    }
  }

  return <Modal
    title={professional ? "Editar profissional" : "Novo profissional"}
    description="Cadastro clínico usado na agenda, nos atendimentos e na assinatura digital."
    onClose={onClose}
  >
    <form onSubmit={(event) => void submit(event)}>
      {error && <div className="form-error">{error}</div>}
      <div className="form-grid professional-form">
        <label className="full">Nome completo<input name="name" required maxLength={160} defaultValue={professional?.name} /></label>
        <label className="full">
          Especialidade
          <div className="specialty-select-row">
            <select required value={specialtyId} onChange={(event) => setSpecialtyId(event.target.value)}>
              <option value="">Selecione uma especialidade</option>
              {specialties.map((specialty) => <option key={specialty.id} value={specialty.id}>{specialty.name}</option>)}
            </select>
            <button type="button" className="secondary-button" onClick={() => setShowNewSpecialty((current) => !current)}><Plus size={16} />Nova</button>
          </div>
        </label>
        {showNewSpecialty && <div className="quick-specialty full">
          <label>Nome<input value={specialtyName} maxLength={120} onChange={(event) => setSpecialtyName(event.target.value)} placeholder="Ex.: Cardiologia" /></label>
          <label>Cor<input type="color" value={specialtyColor} onChange={(event) => setSpecialtyColor(event.target.value)} /></label>
          <button type="button" className="secondary-button" disabled={specialtySaving} onClick={() => void createSpecialty()}>{specialtySaving ? "Criando..." : "Adicionar especialidade"}</button>
        </div>}
        <label className="full">Conselho e número<input name="council" maxLength={80} defaultValue={professional?.council} placeholder="Ex.: CRM-CE 12345, CRO-CE 1234 ou CRP 11/12345" /></label>
        <label>E-mail<input name="email" type="email" maxLength={190} defaultValue={professional?.email} /></label>
        <label>Telefone<input name="phone" maxLength={30} defaultValue={professional?.phone} /></label>
        {professional && <label className="access-checkbox full"><input name="active" type="checkbox" defaultChecked={professional.active} /><span>Profissional ativo e disponível para novos agendamentos</span></label>}
        {professional?.active && <p className="professional-inactive-warning full">Para inativar um profissional que possui acesso ao sistema, desative primeiro o usuário correspondente em “Equipe e acessos”. O histórico clínico será preservado.</p>}
        {!professional && canCreateAccess && <label className="access-checkbox full"><input name="createAccess" type="checkbox" defaultChecked /><span>Abrir a criação do acesso deste profissional após salvar</span></label>}
      </div>
      <ModalActions saving={saving} onClose={onClose} label={professional ? "Salvar alterações" : "Cadastrar profissional"} />
    </form>
  </Modal>;
}
