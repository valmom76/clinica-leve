import { type FormEvent, useEffect, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { ClinicUser, Professional, Role, Session } from "../../types";
import { roleAccess, roleOptions } from "./roleOptions";

type UserModalProps = {
  session: Session;
  user?: ClinicUser;
  initialProfessional?: Professional;
  onClose: () => void;
  onSaved: (user: ClinicUser) => void;
};

export function UserModal({ session, user, initialProfessional, onClose, onSaved }: UserModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [selectedRole, setSelectedRole] = useState<Role>(user?.role ?? (initialProfessional ? "PROFESSIONAL" : "RECEPTIONIST"));
  const [professionals, setProfessionals] = useState<Professional[]>([]);
  const [professionalId, setProfessionalId] = useState(user?.professionalId ?? initialProfessional?.id ?? "");

  useEffect(() => {
    void api.professionals(session)
      .then(setProfessionals)
      .catch(() => setProfessionals([]));
  }, [session]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const password = String(data.get("password"));

    try {
      const payload = {
        name: String(data.get("name")),
        email: String(data.get("email")),
        role: String(data.get("role")) as Role,
        professionalId: String(data.get("professionalId") || "") || undefined,
        expectedDailyMinutes: Math.round(Number(data.get("expectedDailyHours")) * 60),
      };
      const saved = user
        ? await api.updateUser(session, user.id, {
            ...payload,
            password: password || undefined,
            active: data.get("active") === "on",
          })
        : await api.createUser(session, { ...payload, password });
      onSaved(saved);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar acesso");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title={user ? "Editar acesso" : "Novo acesso"}
      description="O usuário ficará vinculado exclusivamente a esta clínica."
      onClose={onClose}
    >
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        <div className="form-grid">
          <label className="full">
            Nome
            <input name="name" defaultValue={user?.name ?? initialProfessional?.name} required />
          </label>
          <label>
            E-mail
            <input name="email" type="email" defaultValue={user?.email ?? initialProfessional?.email} required />
          </label>
          <label>
            Perfil
            <select name="role" value={selectedRole} onChange={(event) => setSelectedRole(event.target.value as Role)} required>
              {roleOptions.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <div className="role-access-note full"><strong>Acesso deste perfil</strong><span>{roleAccess[selectedRole]}</span></div>
          {(selectedRole === "PROFESSIONAL" || selectedRole === "ADMIN") && (
            <label className="full">
              Vínculo com profissional {selectedRole === "PROFESSIONAL" ? "(obrigatório)" : "(opcional)"}
              <select
                name="professionalId"
                value={professionalId}
                onChange={(event) => setProfessionalId(event.target.value)}
                required={selectedRole === "PROFESSIONAL"}
              >
                <option value="">Selecione um profissional</option>
                {professionals.map((professional) => (
                  <option key={professional.id} value={professional.id}>
                    {professional.name}{professional.council ? ` · ${professional.council}` : ""}
                  </option>
                ))}
              </select>
              <small>Esse vínculo determina quem pode finalizar atendimentos e documentos clínicos.</small>
            </label>
          )}
          <label>
            Jornada diária (horas)
            <input
              name="expectedDailyHours"
              type="number"
              min="1"
              max="12"
              step="0.5"
              defaultValue={(user?.expectedDailyMinutes ?? 480) / 60}
              required
            />
          </label>
          <label className="full">
            {user ? "Nova senha (deixe em branco para manter)" : "Senha temporária"}
            <input
              name="password"
              type="password"
              minLength={8}
              required={!user}
              autoComplete="new-password"
            />
          </label>
          {user && (
            <label className="access-checkbox full">
              <input name="active" type="checkbox" defaultChecked={user.active} />
              <span>Usuário ativo</span>
            </label>
          )}
        </div>
        <ModalActions
          saving={saving}
          onClose={onClose}
          label={user ? "Salvar alterações" : "Criar acesso"}
        />
      </form>
    </Modal>
  );
}
