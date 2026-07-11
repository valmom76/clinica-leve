import { type FormEvent, useState } from "react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { ClinicUser, Role, Session } from "../../types";
import { roleOptions } from "./roleOptions";

type UserModalProps = {
  session: Session;
  user?: ClinicUser;
  onClose: () => void;
  onSaved: (user: ClinicUser) => void;
};

export function UserModal({ session, user, onClose, onSaved }: UserModalProps) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

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
            <input name="name" defaultValue={user?.name} required />
          </label>
          <label>
            E-mail
            <input name="email" type="email" defaultValue={user?.email} required />
          </label>
          <label>
            Perfil
            <select name="role" defaultValue={user?.role ?? "RECEPTIONIST"} required>
              {roleOptions.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
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
