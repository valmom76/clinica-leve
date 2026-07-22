import { useCallback, useEffect, useMemo, useState } from "react";
import {
  CalendarDays,
  CheckCircle2,
  ClipboardPlus,
  History,
  Save,
  Stethoscope,
} from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import type { Appointment, ClinicalEncounter, Session } from "../../types";
import { ClinicalDocumentsPanel } from "./ClinicalDocumentsPanel";

type EncounterWorkspaceProps = {
  session: Session;
  appointments: Appointment[];
};

type EncounterForm = Pick<
  ClinicalEncounter,
  | "chiefComplaint"
  | "subjectiveNotes"
  | "objectiveNotes"
  | "assessment"
  | "carePlan"
  | "additionalNotes"
>;

const emptyForm: EncounterForm = {
  chiefComplaint: "",
  subjectiveNotes: "",
  objectiveNotes: "",
  assessment: "",
  carePlan: "",
  additionalNotes: "",
};

export function EncounterWorkspace({ session, appointments }: EncounterWorkspaceProps) {
  const [encounters, setEncounters] = useState<ClinicalEncounter[]>([]);
  const [encounter, setEncounter] = useState<ClinicalEncounter>();
  const [appointmentId, setAppointmentId] = useState("");
  const [form, setForm] = useState<EncounterForm>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");

  const availableAppointments = useMemo(() => appointments.filter((appointment) =>
    !["CANCELLED", "NO_SHOW"].includes(appointment.status)
      && !encounters.some((item) => item.appointmentId === appointment.id),
  ), [appointments, encounters]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const list = await api.clinicalEncounters(session);
      setEncounters(list);
      setEncounter((current) => current
        ? list.find((item) => item.id === current.id) ?? list[0]
        : list[0]);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar atendimentos");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    setForm(encounter ? {
      chiefComplaint: encounter.chiefComplaint ?? "",
      subjectiveNotes: encounter.subjectiveNotes ?? "",
      objectiveNotes: encounter.objectiveNotes ?? "",
      assessment: encounter.assessment ?? "",
      carePlan: encounter.carePlan ?? "",
      additionalNotes: encounter.additionalNotes ?? "",
    } : emptyForm);
  }, [encounter]);

  function merge(saved: ClinicalEncounter) {
    setEncounters((current) => {
      const next = current.some((item) => item.id === saved.id)
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [saved, ...current];
      return next.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    });
    setEncounter(saved);
  }

  function setField(key: keyof EncounterForm, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function startEncounter() {
    if (!appointmentId) return;
    setBusy("start");
    setError("");
    try {
      merge(await api.startClinicalEncounter(session, appointmentId));
      setAppointmentId("");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao iniciar atendimento");
    } finally {
      setBusy("");
    }
  }

  async function save() {
    if (!encounter) return;
    setBusy("save");
    setError("");
    try {
      merge(await api.updateClinicalEncounter(session, {
        id: encounter.id,
        lockVersion: encounter.lockVersion,
        ...form,
      }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar atendimento");
    } finally {
      setBusy("");
    }
  }

  async function finalizeEncounter() {
    if (!encounter || !window.confirm("Finalizar este atendimento? O registro clínico ficará imutável.")) return;
    setBusy("finalize");
    setError("");
    try {
      let current = encounter;
      const changed = (Object.keys(form) as Array<keyof EncounterForm>)
        .some((key) => (form[key] ?? "") !== (encounter[key] ?? ""));
      if (changed) {
        current = await api.updateClinicalEncounter(session, {
          id: encounter.id,
          lockVersion: encounter.lockVersion,
          ...form,
        });
      }
      merge(await api.finalizeClinicalEncounter(session, current.id));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao finalizar atendimento");
    } finally {
      setBusy("");
    }
  }

  const canFinalize = Boolean(
    encounter && session.user.professionalId === encounter.professionalId,
  );

  return (
    <div className="clinical-workspace">
      <aside className="panel encounter-sidebar">
        <div className="encounter-start">
          <span><ClipboardPlus size={18} /></span>
          <div>
            <strong>Iniciar atendimento</strong>
            <small>Escolha um agendamento da agenda atual.</small>
          </div>
          <select value={appointmentId} onChange={(event) => setAppointmentId(event.target.value)}>
            <option value="">Selecione o agendamento</option>
            {availableAppointments.map((appointment) => (
              <option key={appointment.id} value={appointment.id}>
                {formatDateTime(appointment.startAt)} · {appointment.patientName}
              </option>
            ))}
          </select>
          <button className="primary-button" disabled={!appointmentId || Boolean(busy)} onClick={() => void startEncounter()}>
            <Stethoscope size={16} />{busy === "start" ? "Iniciando..." : "Iniciar"}
          </button>
        </div>

        <div className="encounter-list-heading">
          <strong>Atendimentos</strong>
          <small>{encounters.length} registro(s)</small>
        </div>
        <div className="encounter-list">
          {encounters.map((item) => (
            <button
              key={item.id}
              className={item.id === encounter?.id ? "active" : ""}
              onClick={() => setEncounter(item)}
            >
              <span><CalendarDays size={17} /></span>
              <div>
                <strong>{item.patientName}</strong>
                <small>{item.professionalName}</small>
                <em>{formatDateTime(item.createdAt)}</em>
              </div>
              <i className={item.status === "FINALIZED" ? "finalized" : "draft"}>
                {item.status === "FINALIZED" ? "Finalizado" : "Em edição"}
              </i>
            </button>
          ))}
          {loading && <Empty text="Carregando atendimentos..." />}
          {!loading && encounters.length === 0 && <Empty text="Nenhum atendimento iniciado." />}
        </div>
      </aside>

      <div className="encounter-main">
        {error && <div className="page-error">{error}<button onClick={() => void load()}>Recarregar</button></div>}
        {!encounter ? (
          <article className="panel clinical-welcome">
            <Stethoscope size={30} />
            <h2>Selecione ou inicie um atendimento</h2>
            <p>O prontuário, os documentos e as versões ficarão reunidos neste espaço.</p>
          </article>
        ) : (
          <>
            <article className="panel encounter-editor">
              <header>
                <div>
                  <span className="eyebrow">PRONTUÁRIO DO ATENDIMENTO</span>
                  <h2>{encounter.patientName}</h2>
                  <p>{encounter.professionalName}{encounter.professionalCouncil ? ` · ${encounter.professionalCouncil}` : ""}</p>
                </div>
                <small className={`status ${encounter.status === "FINALIZED" ? "completed" : "in_progress"}`}>
                  {encounter.status === "FINALIZED" ? "Finalizado" : "Rascunho"}
                </small>
              </header>

              {encounter.status === "FINALIZED" && (
                <div className="clinical-lock-note">
                  <History size={18} />Registro finalizado e preservado no histórico de versões.
                </div>
              )}

              <div className="encounter-form-grid">
                <label className="full">
                  Queixa principal / motivo do atendimento
                  <textarea rows={3} value={form.chiefComplaint} onChange={(event) => setField("chiefComplaint", event.target.value)} disabled={encounter.status === "FINALIZED"} />
                </label>
                <label>
                  Subjetivo — relato e sintomas
                  <textarea rows={7} value={form.subjectiveNotes} onChange={(event) => setField("subjectiveNotes", event.target.value)} disabled={encounter.status === "FINALIZED"} />
                </label>
                <label>
                  Objetivo — exame e observações
                  <textarea rows={7} value={form.objectiveNotes} onChange={(event) => setField("objectiveNotes", event.target.value)} disabled={encounter.status === "FINALIZED"} />
                </label>
                <label>
                  Avaliação / hipótese profissional
                  <textarea rows={7} value={form.assessment} onChange={(event) => setField("assessment", event.target.value)} disabled={encounter.status === "FINALIZED"} />
                </label>
                <label>
                  Plano e conduta
                  <textarea rows={7} value={form.carePlan} onChange={(event) => setField("carePlan", event.target.value)} disabled={encounter.status === "FINALIZED"} />
                </label>
                <label className="full">
                  Observações adicionais
                  <textarea rows={4} value={form.additionalNotes} onChange={(event) => setField("additionalNotes", event.target.value)} disabled={encounter.status === "FINALIZED"} />
                </label>
              </div>

              {encounter.status === "DRAFT" && (
                <footer className="encounter-editor-actions">
                  <button className="secondary-button" disabled={Boolean(busy)} onClick={() => void save()}>
                    <Save size={16} />{busy === "save" ? "Salvando..." : "Salvar rascunho"}
                  </button>
                  <button className="primary-button" disabled={!canFinalize || Boolean(busy)} onClick={() => void finalizeEncounter()}>
                    <CheckCircle2 size={16} />Finalizar atendimento
                  </button>
                  {!canFinalize && <small>Somente o profissional responsável vinculado pode finalizar.</small>}
                </footer>
              )}
            </article>

            <article className="panel">
              <ClinicalDocumentsPanel
                session={session}
                encounter={encounter}
                canFinalize={canFinalize}
              />
            </article>
          </>
        )}
      </div>
    </div>
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
