import { type FormEvent, useEffect, useState } from "react";
import { CheckCircle2, MessageCircle, ShieldCheck, Smartphone } from "lucide-react";
import { api } from "../../api";
import type { AppointmentMessagingSettings, Session } from "../../types";

export function AgendaMessagingSettings({ session }: { session: Session }) {
  const [settings, setSettings] = useState<AppointmentMessagingSettings>();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    api.appointmentMessagingSettings(session)
      .then(setSettings)
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Falha ao carregar a automação"))
      .finally(() => setLoading(false));
  }, [session]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    const data = new FormData(event.currentTarget);
    const secondReminder = String(data.get("secondReminderHours") ?? "").trim();
    try {
      const saved = await api.saveAppointmentMessagingSettings(session, {
        whatsappEnabled: data.get("whatsappEnabled") === "on",
        confirmationTemplateName: String(data.get("confirmationTemplateName")),
        reminderTemplateName: String(data.get("reminderTemplateName")),
        languageCode: String(data.get("languageCode")),
        confirmationPreview: String(data.get("confirmationPreview")),
        reminderPreview: String(data.get("reminderPreview")),
        firstReminderHours: Number(data.get("firstReminderHours")),
        secondReminderHours: secondReminder ? Number(secondReminder) : undefined,
        maxAttempts: Number(data.get("maxAttempts")),
        retryMinutes: Number(data.get("retryMinutes")),
      });
      setSettings(saved);
      setSuccess("Automação da agenda atualizada.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar a automação");
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="loading-state"><span /><p>Carregando automação da agenda...</p></div>;
  if (!settings) return <div className="page-error">{error || "Configurações indisponíveis"}</div>;

  return <section className="agenda-automation-grid">
    <article className="panel agenda-automation-intro">
      <span><MessageCircle size={22} /></span>
      <div><h2>Confirmação automática</h2><p>Confirmações e lembretes entram em uma fila com novas tentativas em caso de falha.</p></div>
      <ul>
        <li><CheckCircle2 size={16} />Botões para confirmar ou pedir reagendamento</li>
        <li><ShieldCheck size={16} />Webhook validado pela assinatura da Meta</li>
        <li><Smartphone size={16} />Estrutura pronta para um futuro provedor de SMS</li>
      </ul>
    </article>

    <article className="panel agenda-automation-form">
      <div className="panel-heading"><div><h2>WhatsApp da clínica</h2><p>Modelos e frequência dos lembretes</p></div><MessageCircle size={21} /></div>
      <form onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        {success && <div className="form-success">{success}</div>}
        {!settings.platformConfigured && <div className="integration-warning">As credenciais da Meta ainda não estão configuradas no servidor. Preencha o arquivo <strong>.env</strong> antes de ativar.</div>}
        <label className="access-checkbox full">
          <input name="whatsappEnabled" type="checkbox" defaultChecked={settings.whatsappEnabled} disabled={!settings.platformConfigured} />
          <span>Ativar confirmações e lembretes por WhatsApp</span>
        </label>
        <div className="form-grid">
          <label>Modelo de confirmação<input name="confirmationTemplateName" defaultValue={settings.confirmationTemplateName} required pattern="[a-z0-9_]+" /></label>
          <label>Modelo de lembrete<input name="reminderTemplateName" defaultValue={settings.reminderTemplateName} required pattern="[a-z0-9_]+" /></label>
          <label>Idioma do modelo<input name="languageCode" defaultValue={settings.languageCode} required /></label>
          <label>Primeiro lembrete<input name="firstReminderHours" type="number" min="1" max="168" defaultValue={settings.firstReminderHours} required /><small>horas antes da consulta</small></label>
          <label>Segundo lembrete<input name="secondReminderHours" type="number" min="1" max="168" defaultValue={settings.secondReminderHours ?? ""} /><small>opcional; deve ser mais próximo da consulta</small></label>
          <label>Máximo de tentativas<input name="maxAttempts" type="number" min="1" max="10" defaultValue={settings.maxAttempts} required /></label>
          <label>Intervalo entre tentativas<input name="retryMinutes" type="number" min="1" max="1440" defaultValue={settings.retryMinutes} required /><small>em minutos</small></label>
          <label className="full">Prévia da confirmação<textarea name="confirmationPreview" rows={3} maxLength={1000} defaultValue={settings.confirmationPreview} required /></label>
          <label className="full">Prévia do lembrete<textarea name="reminderPreview" rows={3} maxLength={1000} defaultValue={settings.reminderPreview} required /></label>
        </div>
        <p className="template-note"><strong>Importante:</strong> estas prévias documentam o texto usado pela clínica. Os nomes informados precisam corresponder a modelos já criados e aprovados no WhatsApp Manager da Meta, com os parâmetros na ordem paciente, clínica, profissional e data/hora.</p>
        <div className="modal-actions"><button className="primary-button" disabled={saving}>{saving ? "Salvando..." : "Salvar automação"}</button></div>
      </form>
    </article>
  </section>;
}
