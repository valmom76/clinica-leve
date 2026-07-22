import { type ChangeEvent, type CSSProperties, useEffect, useRef, useState } from "react";
import { Check, ImagePlus, Palette, ShieldCheck, Trash2, Upload } from "lucide-react";
import { api } from "../../api";
import { PageTitle } from "../../components/ui/PageTitle";
import type { ClinicBranding, ClinicTheme, Session } from "../../types";
import {
  applyClinicTheme,
  CLINIC_THEMES,
  normalizeClinicTheme,
} from "../../utils/clinicThemes";

export function ClinicSettingsPage({ session, onBrandingChange }: {
  session: Session;
  onBrandingChange: (branding: ClinicBranding) => void;
}) {
  const initialTheme = normalizeClinicTheme(session.clinic.themeKey);
  const [branding, setBranding] = useState<ClinicBranding>({
    clinicName: session.clinic.name,
    clinicSlug: session.clinic.slug,
    logoUrl: session.clinic.logoUrl,
    themeKey: initialTheme,
  });
  const [selectedTheme, setSelectedTheme] = useState<ClinicTheme>(initialTheme);
  const savedTheme = useRef<ClinicTheme>(initialTheme);
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    api.clinicBranding(session).then((current) => {
      setBranding(current);
      setSelectedTheme(current.themeKey);
      savedTheme.current = current.themeKey;
      applyClinicTheme(current.themeKey);
    }).catch(() => undefined);
  }, [session.accessToken]);

  useEffect(() => {
    applyClinicTheme(selectedTheme);
  }, [selectedTheme]);

  useEffect(() => () => applyClinicTheme(savedTheme.current), []);

  useEffect(() => {
    if (!file) {
      setPreviewUrl("");
      return;
    }
    const nextUrl = URL.createObjectURL(file);
    setPreviewUrl(nextUrl);
    return () => URL.revokeObjectURL(nextUrl);
  }, [file]);

  function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    setError("");
    setSuccess("");
    setFile(event.target.files?.[0] ?? null);
  }

  function acceptBranding(updated: ClinicBranding) {
    setBranding(updated);
    setSelectedTheme(updated.themeKey);
    savedTheme.current = updated.themeKey;
    onBrandingChange(updated);
  }

  async function upload() {
    if (!file) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await api.uploadClinicLogo(session, file);
      acceptBranding(updated);
      setFile(null);
      setSuccess("Logomarca atualizada em toda a plataforma.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao enviar a logomarca");
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!branding.logoUrl || !window.confirm("Remover a logomarca personalizada da clínica?")) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await api.removeClinicLogo(session);
      acceptBranding(updated);
      setFile(null);
      setSuccess("Logomarca removida. A identidade padrão voltou a ser usada.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao remover a logomarca");
    } finally {
      setSaving(false);
    }
  }

  async function saveTheme() {
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await api.updateClinicTheme(session, selectedTheme);
      acceptBranding(updated);
      setSuccess(`Tema ${CLINIC_THEMES.find((theme) => theme.key === updated.themeKey)?.name} aplicado em toda a plataforma.`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao aplicar o tema");
    } finally {
      setSaving(false);
    }
  }

  const visibleLogo = previewUrl || branding.logoUrl;
  const themeChanged = selectedTheme !== branding.themeKey;

  return <>
    <PageTitle eyebrow="CONFIGURAÇÕES DA CLÍNICA" title="Personalização" description="Aplique a identidade visual da clínica às áreas institucionais e aos relatórios." />
    {(error || success) && <div className={`settings-feedback ${error ? "form-error" : "form-success"}`}>{error || success}</div>}
    <section className="settings-grid">
      <article className="panel theme-settings-card">
        <div className="panel-heading"><div><h2>Esquema de cores</h2><p>Escolha um tema validado para preservar contraste e legibilidade</p></div><Palette size={21} /></div>
        <div className="theme-options" role="radiogroup" aria-label="Esquema de cores da clínica">
          {CLINIC_THEMES.map((theme) => {
            const selected = selectedTheme === theme.key;
            const style = {
              "--theme-card-primary": theme.colors.primary,
              "--theme-card-deep": theme.colors.deep,
              "--theme-card-medium": theme.colors.medium,
              "--theme-card-accent": theme.colors.accent,
              "--theme-card-soft": theme.colors.soft,
            } as CSSProperties;
            return <button
              type="button"
              role="radio"
              aria-checked={selected}
              className={`theme-option ${selected ? "selected" : ""}`}
              style={style}
              key={theme.key}
              onClick={() => {
                setError("");
                setSuccess("");
                setSelectedTheme(theme.key);
              }}
            >
              <span className="theme-option-preview"><i /><b /><em /><small /></span>
              <span className="theme-option-copy"><strong>{theme.name}</strong><small>{theme.description}</small></span>
              <span className="theme-option-check"><Check size={15} /></span>
            </button>;
          })}
        </div>
        <div className="theme-settings-actions">
          <div><strong>{themeChanged ? "Prévia ativa" : "Tema atual"}</strong><small>{themeChanged ? "A escolha ainda não foi salva para os demais usuários." : "Este esquema já está aplicado à clínica."}</small></div>
          <button className="primary-button" disabled={!themeChanged || saving} onClick={() => void saveTheme()}><Palette size={17} />{saving ? "Aplicando..." : "Aplicar esquema"}</button>
        </div>
      </article>

      <article className="panel branding-settings-card">
        <div className="panel-heading"><div><h2>Logomarca da clínica</h2><p>PNG ou JPG, com tamanho máximo de 2 MB</p></div><ImagePlus size={21} /></div>
        <div className="branding-settings-content">
          <div className={`branding-preview ${visibleLogo ? "has-logo" : ""}`}>
            {visibleLogo ? <img src={visibleLogo} alt={`Prévia da logomarca ${branding.clinicName}`} /> : <ImagePlus size={42} />}
          </div>
          <div className="branding-upload-fields">
            <div><strong>{branding.clinicName}</strong><small>Identificador: {branding.clinicSlug}</small></div>
            <label className="secondary-button file-button"><ImagePlus size={17} />Selecionar imagem<input type="file" accept="image/png,image/jpeg" onChange={chooseFile} /></label>
            <div className="branding-actions">
              <button className="primary-button" disabled={!file || saving} onClick={() => void upload()}><Upload size={17} />{saving ? "Enviando..." : "Aplicar logomarca"}</button>
              {branding.logoUrl && <button className="danger-button" disabled={saving} onClick={() => void remove()}><Trash2 size={17} />Remover</button>}
            </div>
            {file && <small className="selected-file">Selecionado: {file.name}</small>}
          </div>
        </div>
      </article>
      <article className="panel branding-security-card">
        <span><ShieldCheck size={24} /></span>
        <div><h2>Alteração protegida</h2><p>Somente administradores podem alterar a identidade visual. Logomarca e tema permanecem isolados por clínica e aparecem também no login e nos relatórios.</p></div>
      </article>
    </section>
  </>;
}
