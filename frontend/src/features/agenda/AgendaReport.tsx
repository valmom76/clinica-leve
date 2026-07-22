import { useCallback, useEffect, useState } from "react";
import { CalendarCheck2, CalendarDays, CheckCircle2, Download, FileText, UserX, XCircle } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { ReportIdentity } from "../../components/ui/ReportIdentity";
import type {
  AppointmentContextReport,
  AppointmentStatus,
  Professional,
  Session,
  Specialty,
} from "../../types";
import { statusLabel } from "../../utils/appointments";
import { clinicToday, downloadCsv, firstDayOfMonth, localDate } from "../../utils/reporting";
import { downloadReportPdf } from "../../utils/reportPdf";

type StatusFilter = AppointmentStatus | "ALL";

export function AgendaReport({ session, professionals, specialties }: {
  session: Session;
  professionals: Professional[];
  specialties: Specialty[];
}) {
  const today = clinicToday(session.clinic.timezone);
  const [from, setFrom] = useState(firstDayOfMonth(today));
  const [to, setTo] = useState(today);
  const [professionalId, setProfessionalId] = useState("");
  const [specialtyId, setSpecialtyId] = useState("");
  const [status, setStatus] = useState<StatusFilter>("ALL");
  const [filters, setFilters] = useState({
    from,
    to,
    professionalId: "",
    specialtyId: "",
    status: "ALL" as StatusFilter,
  });
  const [report, setReport] = useState<AppointmentContextReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setReport(await api.appointmentReport(session, {
        from: filters.from,
        to: filters.to,
        professionalId: filters.professionalId || undefined,
        specialtyId: filters.specialtyId || undefined,
        status: filters.status === "ALL" ? undefined : filters.status,
      }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao gerar relatório de atendimentos");
    } finally {
      setLoading(false);
    }
  }, [filters, session]);

  useEffect(() => { void load(); }, [load]);

  function apply() {
    if (!from || !to) return;
    setFilters({ from, to, professionalId, specialtyId, status });
  }

  function exportCsv() {
    if (!report) return;
    downloadCsv(`agenda-${report.from}-${report.to}.csv`, [
      ["Relatório de atendimentos"],
      ["Período", `${localDate(report.from)} a ${localDate(report.to)}`],
      ["Profissional", selectedName(professionals, filters.professionalId) ?? "Todos"],
      ["Especialidade", selectedName(specialties, filters.specialtyId) ?? "Todas"],
      ["Status", filters.status === "ALL" ? "Todos" : statusLabel[filters.status]],
      [],
      ["Data e hora", "Paciente", "Profissional", "Especialidade", "Status", "Duração (min)", "Observações"],
      ...report.appointments.map((appointment) => [
        clinicDateTime(appointment.startAt, session.clinic.timezone),
        appointment.patientName,
        appointment.professionalName,
        appointment.specialtyName,
        statusLabel[appointment.status],
        durationMinutes(appointment.startAt, appointment.endAt),
        appointment.notes ?? "",
      ]),
    ]);
  }

  async function exportPdf() {
    if (!report) return;
    try {
      await downloadReportPdf({
        session,
        filename: `agenda-${report.from}-${report.to}.pdf`,
        title: "Relatório de atendimentos",
        subtitle: `${localDate(report.from)} a ${localDate(report.to)}`,
        filters: [
          ["Profissional", selectedName(professionals, filters.professionalId) ?? "Todos"],
          ["Especialidade", selectedName(specialties, filters.specialtyId) ?? "Todas"],
          ["Status", filters.status === "ALL" ? "Todos" : statusLabel[filters.status]],
        ],
        summary: [
          ["Agendamentos", String(report.total)],
          ["Concluídos", String(report.completed)],
          ["Cancelados", String(report.cancelled)],
          ["Comparecimento", `${number(report.attendanceRate)}%`],
        ],
        columns: ["Data e hora", "Paciente", "Profissional", "Especialidade", "Duração", "Status"],
        rows: report.appointments.map((appointment) => [
          clinicDateTime(appointment.startAt, session.clinic.timezone),
          appointment.patientName,
          appointment.professionalName,
          appointment.specialtyName,
          `${durationMinutes(appointment.startAt, appointment.endAt)} min`,
          statusLabel[appointment.status],
        ]),
        landscape: true,
      });
    } catch {
      setError("Não foi possível gerar o PDF do relatório");
    }
  }

  return (
    <section className="context-report">
      <article className="panel module-report-filter agenda-report-filter">
        <ReportIdentity session={session} title="Atendimentos da agenda" description="Acompanhe presença, cancelamentos e faltas por profissional." />
        <div className="module-report-fields agenda-report-fields">
          <label>Profissional<select value={professionalId} onChange={(event) => setProfessionalId(event.target.value)}><option value="">Todos os profissionais</option>{professionals.map((professional) => <option key={professional.id} value={professional.id}>{professional.name}</option>)}</select></label>
          <label>Especialidade<select value={specialtyId} onChange={(event) => setSpecialtyId(event.target.value)}><option value="">Todas as especialidades</option>{specialties.map((specialty) => <option key={specialty.id} value={specialty.id}>{specialty.name}</option>)}</select></label>
          <label>Status<select value={status} onChange={(event) => setStatus(event.target.value as StatusFilter)}><option value="ALL">Todos os status</option>{Object.entries(statusLabel).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
          <label>De<input type="date" value={from} max={to} onChange={(event) => setFrom(event.target.value)} /></label>
          <label>Até<input type="date" value={to} min={from} onChange={(event) => setTo(event.target.value)} /></label>
          <button className="primary-button" onClick={apply}>Gerar relatório</button>
          <button className="secondary-button" disabled={!report} onClick={() => void exportPdf()}><FileText size={17} />PDF</button>
          <button className="secondary-button" disabled={!report} onClick={exportCsv}><Download size={17} />CSV</button>
        </div>
      </article>

      {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}
      {loading && <div className="loading-state"><span /><p>Consultando atendimentos...</p></div>}

      {!loading && report && <>
        <section className="kpis context-report-kpis">
          <Kpi icon={CalendarDays} label="Agendamentos" value={String(report.total)} tone="blue" />
          <Kpi icon={CheckCircle2} label="Concluídos" value={String(report.completed)} tone="sage" />
          <Kpi icon={XCircle} label="Cancelados" value={String(report.cancelled)} tone="terracotta" />
          <Kpi icon={UserX} label="Faltas" value={String(report.noShows)} tone="terracotta" />
        </section>
        <div className="report-period-note"><CalendarCheck2 size={17} />Taxa de comparecimento: <strong>{number(report.attendanceRate)}%</strong> · {localDate(report.from)} a {localDate(report.to)}</div>
        <article className="panel data-panel context-report-table">
          <div className="panel-heading"><div><h2>Detalhamento dos atendimentos</h2><p>{report.total} registro(s) conforme os filtros aplicados</p></div><CalendarDays size={21} /></div>
          <div className="table-scroll"><table><thead><tr><th>Data e hora</th><th>Paciente</th><th>Profissional</th><th>Especialidade</th><th>Duração</th><th>Status</th></tr></thead>
            <tbody>{report.appointments.map((appointment) => <tr key={appointment.id}>
              <td><strong>{clinicDateTime(appointment.startAt, session.clinic.timezone)}</strong></td><td>{appointment.patientName}</td><td>{appointment.professionalName}</td>
              <td><span className="specialty-report-name"><i style={{ background: appointment.color }} />{appointment.specialtyName}</span></td>
              <td>{durationMinutes(appointment.startAt, appointment.endAt)} min</td><td><small className={`status ${appointment.status.toLowerCase()}`}>{statusLabel[appointment.status]}</small></td>
            </tr>)}</tbody></table>
            {report.appointments.length === 0 && <Empty text="Nenhum atendimento encontrado para os filtros." />}
          </div>
        </article>
      </>}
    </section>
  );
}

function selectedName(items: Array<{ id: string; name: string }>, id: string) {
  return items.find((item) => item.id === id)?.name;
}

function clinicDateTime(value: string, timeZone: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    timeZone,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function durationMinutes(start: string, end: string) {
  return Math.max(0, Math.round((new Date(end).getTime() - new Date(start).getTime()) / 60000));
}

function number(value: number) {
  return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(value);
}
