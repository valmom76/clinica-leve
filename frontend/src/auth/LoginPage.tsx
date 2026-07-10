import { type FormEvent, useState } from "react";
import { HeartPulse } from "lucide-react";
import { api } from "../api";
import type { Session } from "../types";

type LoginPageProps = {
  onAuthenticated: (session: Session) => void;
};

export function LoginPage({ onAuthenticated }: LoginPageProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");
    const data = new FormData(event.currentTarget);

    try {
      const session = await api.login({
        clinicSlug: String(data.get("clinicSlug")),
        email: String(data.get("email")),
        password: String(data.get("password")),
      });
      onAuthenticated(session);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao entrar");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-brand-panel">
        <div className="login-brand">
          <span className="brand-symbol"><HeartPulse size={30} /></span>
          <strong>Clínica Leve</strong>
        </div>
        <div className="login-message">
          <span>GESTÃO MULTIEMPRESA</span>
          <h1>Sua clínica organizada, conectada e mais leve.</h1>
          <p>
            Agenda, pacientes, profissionais e gestão da clínica em uma única
            plataforma.
          </p>
        </div>
        <small>Dados de cada clínica isolados desde a autenticação.</small>
      </section>

      <section className="login-form-panel">
        <form className="login-form" onSubmit={submit}>
          <span className="eyebrow">BEM-VINDO</span>
          <h2>Acesse sua clínica</h2>
          <p>Informe o identificador da empresa e suas credenciais.</p>
          {error && <div className="form-error">{error}</div>}
          <label>
            Clínica
            <input name="clinicSlug" defaultValue="clinica-demo" required />
          </label>
          <label>
            E-mail
            <input
              name="email"
              type="email"
              defaultValue="admin@clinicaleve.local"
              required
            />
          </label>
          <label>
            Senha
            <input
              name="password"
              type="password"
              defaultValue="Admin@123"
              required
            />
          </label>
          <button className="primary-button login-button" disabled={loading}>
            {loading ? "Entrando..." : "Entrar na plataforma"}
          </button>
        </form>
      </section>
    </main>
  );
}
