import { type FormEvent, useState } from "react";
import { HeartPulse } from "lucide-react";
import { api } from "../api";

export function ResetPasswordPage() {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const token = new URLSearchParams(window.location.search).get("token") ?? "";

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const password = String(data.get("password"));
    const confirmation = String(data.get("confirmation"));
    if (password !== confirmation) {
      setError("As senhas não coincidem");
      setSaving(false);
      return;
    }
    try {
      await api.resetPassword({ token, newPassword: password });
      setSuccess(true);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível criar a senha");
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="login-page simple-login-page">
      <section className="login-form-panel reset-password-panel">
        <form className="login-form" onSubmit={submit}>
          <div className="login-clinic-identity">
            <span><HeartPulse size={21} /></span>
            <strong>Clínica Leve</strong>
          </div>
          <span className="eyebrow">ACESSO SEGURO</span>
          <h2>{success ? "Senha criada" : "Crie sua nova senha"}</h2>
          {success ? (
            <>
              <p>Sua senha foi atualizada e as sessões anteriores foram encerradas.</p>
              <a className="primary-button login-button link-button" href="/">Ir para o login</a>
            </>
          ) : (
            <>
              <p>Use pelo menos 10 caracteres, com maiúscula, minúscula, número e símbolo.</p>
              {!token && <div className="form-error">Este endereço não contém um token válido.</div>}
              {error && <div className="form-error">{error}</div>}
              <label>
                Nova senha
                <input name="password" type="password" minLength={10} autoComplete="new-password" required />
              </label>
              <label>
                Confirme a nova senha
                <input name="confirmation" type="password" minLength={10} autoComplete="new-password" required />
              </label>
              <button className="primary-button login-button" disabled={saving || !token}>
                {saving ? "Salvando..." : "Criar nova senha"}
              </button>
            </>
          )}
        </form>
      </section>
    </main>
  );
}
