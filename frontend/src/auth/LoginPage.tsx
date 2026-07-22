import { type FormEvent, useEffect, useState } from "react";
import { HeartPulse } from "lucide-react";
import { api } from "../api";
import type { ClinicBranding, Session } from "../types";
import { applyClinicTheme } from "../utils/clinicThemes";

type LoginPageProps = {
  onAuthenticated: (session: Session) => void;
};

export function LoginPage({ onAuthenticated }: LoginPageProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [clinicSlug, setClinicSlug] = useState("clinica-demo");
  const [branding, setBranding] = useState<ClinicBranding | null>(null);

  useEffect(() => {
    const normalized = clinicSlug.trim().toLowerCase();
    if (normalized.length < 2) {
      setBranding(null);
      return;
    }
    const timeout = window.setTimeout(() => {
      api.publicBranding(normalized).then(setBranding).catch(() => setBranding(null));
    }, 350);
    return () => window.clearTimeout(timeout);
  }, [clinicSlug]);

  useEffect(() => {
    applyClinicTheme(branding?.themeKey);
  }, [branding?.themeKey]);

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
          <span className={`brand-symbol ${branding?.logoUrl ? "clinic-logo" : ""}`}>
            {branding?.logoUrl ? <img src={branding.logoUrl} alt={`Logomarca ${branding.clinicName}`} /> : <HeartPulse size={30} />}
          </span>
          <strong>{branding?.clinicName ?? "Clínica Leve"}</strong>
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
          {branding && <div className="login-clinic-identity">
            {branding.logoUrl ? <img src={branding.logoUrl} alt={`Logomarca ${branding.clinicName}`} /> : <span><HeartPulse size={21} /></span>}
            <strong>{branding.clinicName}</strong>
          </div>}
          <span className="eyebrow">BEM-VINDO</span>
          <h2>Acesse sua clínica</h2>
          <p>Informe o identificador da empresa e suas credenciais.</p>
          {error && <div className="form-error">{error}</div>}
          <label>
            Clínica
            <input name="clinicSlug" value={clinicSlug} onChange={(event) => setClinicSlug(event.target.value)} required />
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
