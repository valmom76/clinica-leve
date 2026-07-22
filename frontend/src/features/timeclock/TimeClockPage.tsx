import { useCallback, useEffect, useMemo, useState } from "react";
import { BarChart3, CalendarDays, Clock3, Coffee, LogIn, LogOut, PencilLine, TimerReset } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { PageTitle } from "../../components/ui/PageTitle";
import { ModuleTabs } from "../../components/ui/ModuleTabs";
import type { Session, TimeDaySummary, TimeEntry, TimeEntryType } from "../../types";
import { TimeEntriesModal } from "./TimeEntriesModal";
import { TimeEntryModal } from "./TimeEntryModal";
import { EmployeeTimeReport } from "./EmployeeTimeReport";
import {
  clinicDate,
  formatBalance,
  formatMinutes,
  longDate,
  timeEntryLabel,
  timeFromLocalDateTime,
  timeStatusLabel,
} from "./timeClockUtils";

const managerRoles = ["ADMIN", "MANAGER", "HR"];
type TimeSection = "CLOCK" | "REPORT";

export function TimeClockPage({ session }: { session: Session }) {
  const today = useMemo(() => clinicDate(session.clinic.timezone), [session.clinic.timezone]);
  const canManage = managerRoles.includes(session.user.role);
  const [selectedDate, setSelectedDate] = useState(today);
  const [myDay, setMyDay] = useState<TimeDaySummary | null>(null);
  const [team, setTeam] = useState<TimeDaySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [teamLoading, setTeamLoading] = useState(canManage);
  const [punching, setPunching] = useState<TimeEntryType | null>(null);
  const [error, setError] = useState("");
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [editor, setEditor] = useState<{ summary: TimeDaySummary; entry?: TimeEntry } | null>(null);
  const [deletingId, setDeletingId] = useState<string>();
  const [, setClockTick] = useState(0);
  const [section, setSection] = useState<TimeSection>("CLOCK");

  const loadMine = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setMyDay(await api.myTimeDay(session, today));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar seu ponto");
    } finally {
      setLoading(false);
    }
  }, [session, today]);

  const loadTeam = useCallback(async () => {
    if (!canManage) return;
    setTeamLoading(true);
    setError("");
    try {
      setTeam(await api.teamTimeDay(session, selectedDate));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar o ponto da equipe");
    } finally {
      setTeamLoading(false);
    }
  }, [canManage, selectedDate, session]);

  useEffect(() => { void loadMine(); }, [loadMine]);
  useEffect(() => { void loadTeam(); }, [loadTeam]);
  useEffect(() => {
    const timer = window.setInterval(() => setClockTick((value) => value + 1), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const selectedSummary = selectedUserId
    ? team.find((summary) => summary.userId === selectedUserId) ?? null
    : null;

  async function punch(type: TimeEntryType) {
    setPunching(type);
    setError("");
    try {
      setMyDay(await api.punchTime(session, type));
      if (canManage && selectedDate === today) await loadTeam();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao registrar o ponto");
    } finally {
      setPunching(null);
    }
  }

  async function savedManualEntry() {
    setEditor(null);
    await loadTeam();
    if (selectedDate === today) await loadMine();
  }

  async function deleteEntry(entry: TimeEntry) {
    if (!window.confirm(`Excluir a marcação de ${timeEntryLabel[entry.type].toLowerCase()} às ${timeFromLocalDateTime(entry.occurredAt)}?`)) return;
    setDeletingId(entry.id);
    setError("");
    try {
      await api.deleteTimeEntry(session, entry.id);
      await loadTeam();
      if (selectedDate === today) await loadMine();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao excluir a marcação");
    } finally {
      setDeletingId(undefined);
    }
  }

  const clinicTime = new Intl.DateTimeFormat("pt-BR", {
    timeZone: session.clinic.timezone,
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date());

  return (
    <>
      <PageTitle
        eyebrow="JORNADA DA EQUIPE"
        title="Controle de ponto"
        description="Marcações de entrada, intervalos, saída e conferência diária."
      />

      {canManage && (
        <ModuleTabs<TimeSection>
          active={section}
          onChange={setSection}
          items={[
            { key: "CLOCK", label: "Registro diário", icon: Clock3 },
            { key: "REPORT", label: "Relatório por funcionário", icon: BarChart3 },
          ]}
        />
      )}

      {section === "REPORT" && canManage ? (
        <EmployeeTimeReport session={session} />
      ) : <>
      {error && (
        <div className="page-error">
          {error}<button onClick={() => { void loadMine(); void loadTeam(); }}>Tentar novamente</button>
        </div>
      )}

      {loading || !myDay ? (
        <div className="loading-state"><span /><p>Carregando controle de ponto...</p></div>
      ) : (
        <>
          <section className="time-clock-grid">
            <article className="panel punch-card">
              <div className="punch-card-heading">
                <div>
                  <span className="eyebrow">HOJE NA CLÍNICA</span>
                  <strong>{clinicTime}</strong>
                  <p>{longDate(today)}</p>
                </div>
                <span className={`time-status-badge ${myDay.status.toLowerCase()}`}>
                  {timeStatusLabel[myDay.status]}
                </span>
              </div>
              <PunchActions summary={myDay} busy={punching} onPunch={punch} />
              <p className="punch-help">O horário é registrado pelo servidor no fuso de {session.clinic.name}.</p>
            </article>

            <article className="panel my-time-entries">
              <div className="panel-heading">
                <div><h2>Minhas marcações</h2><p>Histórico de hoje</p></div>
                <Clock3 size={20} />
              </div>
              <div className="time-entry-list">
                {myDay.entries.length === 0 && <p className="time-empty-message">Sua primeira marcação será exibida aqui.</p>}
                {myDay.entries.map((entry) => (
                  <article key={entry.id}>
                    <span className={`time-entry-dot ${entry.type.toLowerCase()}`} />
                    <time>{timeFromLocalDateTime(entry.occurredAt)}</time>
                    <div>
                      <strong>{timeEntryLabel[entry.type]}</strong>
                      <small>{entry.source === "MANUAL" ? "Ajuste da gestão" : "Registrado por você"}{entry.edited ? " · Editado" : ""}</small>
                    </div>
                  </article>
                ))}
              </div>
            </article>
          </section>

          <section className="kpis time-kpis">
            <Kpi icon={Clock3} label="Tempo trabalhado" value={formatMinutes(myDay.workedMinutes)} tone="sage" />
            <Kpi icon={TimerReset} label="Jornada prevista" value={formatMinutes(myDay.expectedMinutes)} tone="blue" />
            <Kpi icon={Coffee} label="Intervalos iniciados" value={String(myDay.entries.filter((entry) => entry.type === "BREAK_START").length)} tone="terracotta" />
            <Kpi icon={CalendarDays} label="Saldo do dia" value={formatBalance(myDay.balanceMinutes)} tone={myDay.balanceMinutes >= 0 ? "sage" : "terracotta"} />
          </section>
        </>
      )}

      {canManage && (
        <article className="panel data-panel team-time-panel">
          <div className="team-time-heading">
            <div>
              <span className="eyebrow">GESTÃO E RH</span>
              <h2>Ponto da equipe</h2>
              <p>Confira jornadas e faça ajustes devidamente identificados.</p>
            </div>
            <label>
              Data da conferência
              <input type="date" value={selectedDate} max={today} onChange={(event) => setSelectedDate(event.target.value)} />
            </label>
          </div>
          <div className="table-scroll">
            <table>
              <thead><tr><th>Funcionário</th><th>Situação</th><th>Marcações</th><th>Trabalhado</th><th>Previsto</th><th>Saldo</th><th aria-label="Ações" /></tr></thead>
              <tbody>
                {!teamLoading && team.map((summary) => (
                  <tr key={summary.userId}>
                    <td><div className="person"><span>{initials(summary.userName)}</span><strong>{summary.userName}</strong></div></td>
                    <td><small className={`status time-${summary.status.toLowerCase()}`}>{timeStatusLabel[summary.status]}</small></td>
                    <td>{summary.entries.length}</td>
                    <td><strong>{formatMinutes(summary.workedMinutes)}</strong></td>
                    <td>{formatMinutes(summary.expectedMinutes)}</td>
                    <td><strong className={summary.balanceMinutes >= 0 ? "positive-balance" : "negative-balance"}>{formatBalance(summary.balanceMinutes)}</strong></td>
                    <td><button className="text-button" onClick={() => setSelectedUserId(summary.userId)}><PencilLine size={16} />Conferir</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            {teamLoading && <Empty text="Carregando jornadas da equipe..." />}
            {!teamLoading && team.length === 0 && <Empty text="Nenhum funcionário ativo encontrado." />}
          </div>
        </article>
      )}

      {selectedSummary && !editor && (
        <TimeEntriesModal
          summary={selectedSummary}
          deletingId={deletingId}
          onClose={() => setSelectedUserId(null)}
          onAdd={() => setEditor({ summary: selectedSummary })}
          onEdit={(entry) => setEditor({ summary: selectedSummary, entry })}
          onDelete={(entry) => void deleteEntry(entry)}
        />
      )}
      {editor && (
        <TimeEntryModal
          session={session}
          summary={editor.summary}
          entry={editor.entry}
          onClose={() => setEditor(null)}
          onSaved={() => void savedManualEntry()}
        />
      )}
      </>}
    </>
  );
}

function PunchActions({ summary, busy, onPunch }: {
  summary: TimeDaySummary;
  busy: TimeEntryType | null;
  onPunch: (type: TimeEntryType) => void;
}) {
  if (summary.status === "CLOSED") {
    return <div className="journey-complete"><LogOut size={20} /><span>Jornada encerrada às {timeFromLocalDateTime(summary.entries.at(-1)?.occurredAt ?? "")}</span></div>;
  }
  if (summary.status === "NOT_STARTED") {
    return <button className="primary-button punch-main-button" disabled={Boolean(busy)} onClick={() => onPunch("CLOCK_IN")}><LogIn size={20} />{busy ? "Registrando..." : "Registrar entrada"}</button>;
  }
  if (summary.status === "ON_BREAK") {
    return <button className="primary-button punch-main-button" disabled={Boolean(busy)} onClick={() => onPunch("BREAK_END")}><Coffee size={20} />{busy ? "Registrando..." : "Finalizar intervalo"}</button>;
  }
  return (
    <div className="punch-action-row">
      <button className="secondary-button" disabled={Boolean(busy)} onClick={() => onPunch("BREAK_START")}><Coffee size={19} />Iniciar intervalo</button>
      <button className="primary-button" disabled={Boolean(busy)} onClick={() => onPunch("CLOCK_OUT")}><LogOut size={19} />Encerrar expediente</button>
    </div>
  );
}

function initials(name: string) {
  return name.split(" ").filter(Boolean).map((part) => part[0]).slice(0, 2).join("");
}
