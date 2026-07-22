import { useCallback, useEffect, useState } from "react";
import { CalendarCheck2, CheckCircle2, Clock3, Download, FileText, TimerReset } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { ReportIdentity } from "../../components/ui/ReportIdentity";
import type { EmployeeTimeReport as EmployeeTimeReportData, Session, TimeReportEmployee } from "../../types";
import { clinicToday, downloadCsv, firstDayOfMonth, localDate } from "../../utils/reporting";
import { downloadReportPdf } from "../../utils/reportPdf";
import { formatBalance, formatMinutes, timeFromLocalDateTime, timeStatusLabel } from "./timeClockUtils";

export function EmployeeTimeReport({ session }: { session: Session }) {
  const today = clinicToday(session.clinic.timezone);
  const [userId, setUserId] = useState("");
  const [from, setFrom] = useState(firstDayOfMonth(today));
  const [to, setTo] = useState(today);
  const [filters, setFilters] = useState({ userId: "", from, to });
  const [report, setReport] = useState<EmployeeTimeReportData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [employees, setEmployees] = useState<TimeReportEmployee[]>([]);
  const [employeesLoading, setEmployeesLoading] = useState(true);

  useEffect(() => {
    setEmployeesLoading(true);
    api.timeReportEmployees(session)
      .then(setEmployees)
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Falha ao carregar funcionários"))
      .finally(() => setEmployeesLoading(false));
  }, [session]);

  useEffect(() => {
    if (employees.length === 0) return;
    if (!employees.some((employee) => employee.userId === userId)) {
      setUserId(employees[0].userId);
      setFilters((current) => ({ ...current, userId: employees[0].userId }));
    }
  }, [employees, userId]);

  const load = useCallback(async () => {
    if (!filters.userId) return;
    setLoading(true);
    setError("");
    try {
      setReport(await api.employeeTimeReport(session, filters.userId, filters.from, filters.to));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao gerar relatório do funcionário");
    } finally {
      setLoading(false);
    }
  }, [filters, session]);

  useEffect(() => { void load(); }, [load]);

  function apply() {
    if (!userId || !from || !to) return;
    setFilters({ userId, from, to });
  }

  function exportCsv() {
    if (!report) return;
    downloadCsv(`ponto-${report.userName.toLowerCase().replaceAll(" ", "-")}-${report.from}-${report.to}.csv`, [
      ["Relatório de ponto", report.userName],
      ["Período", `${localDate(report.from)} a ${localDate(report.to)}`],
      [],
      ["Data", "Situação", "Primeira marcação", "Última marcação", "Trabalhado", "Previsto", "Saldo"],
      ...report.days.map((day) => [
        localDate(day.date),
        timeStatusLabel[day.status],
        day.entries.length ? timeFromLocalDateTime(day.entries[0].occurredAt) : "—",
        day.entries.length ? timeFromLocalDateTime(day.entries.at(-1)?.occurredAt ?? "") : "—",
        day.workedMinutes,
        day.expectedMinutes,
        day.balanceMinutes,
      ]),
    ]);
  }

  async function exportPdf() {
    if (!report) return;
    try {
      await downloadReportPdf({
        session,
        filename: `ponto-${report.userName.toLowerCase().replaceAll(" ", "-")}-${report.from}-${report.to}.pdf`,
        title: "Relatório de ponto",
        subtitle: `${report.userName} · ${localDate(report.from)} a ${localDate(report.to)}`,
        summary: [
          ["Dias com marcação", String(report.daysWithRecords)],
          ["Jornadas encerradas", String(report.closedDays)],
          ["Tempo trabalhado", formatMinutes(report.workedMinutes)],
          ["Saldo", formatBalance(report.balanceMinutes)],
        ],
        columns: ["Data", "Situação", "Primeira", "Última", "Marcações", "Trabalhado", "Previsto", "Saldo"],
        rows: report.days.map((day) => [
          localDate(day.date),
          timeStatusLabel[day.status],
          day.entries.length ? timeFromLocalDateTime(day.entries[0].occurredAt) : "—",
          day.entries.length ? timeFromLocalDateTime(day.entries.at(-1)?.occurredAt ?? "") : "—",
          day.entries.length,
          formatMinutes(day.workedMinutes),
          formatMinutes(day.expectedMinutes),
          formatBalance(day.balanceMinutes),
        ]),
        landscape: true,
      });
    } catch {
      setError("Não foi possível gerar o PDF do relatório");
    }
  }

  if (employeesLoading) {
    return <div className="loading-state"><span /><p>Carregando funcionários...</p></div>;
  }
  if (employees.length === 0) {
    return <article className="panel"><Empty text="Nenhum funcionário ativo disponível para o relatório." /></article>;
  }

  return (
    <section className="context-report">
      <article className="panel module-report-filter">
        <ReportIdentity session={session} title="Ponto por funcionário" description="Analise somente os dias que possuem marcações registradas." />
        <div className="module-report-fields">
          <label>Funcionário<select value={userId} onChange={(event) => setUserId(event.target.value)}>{employees.map((employee) => <option key={employee.userId} value={employee.userId}>{employee.userName}{employee.active ? "" : " (inativo)"}</option>)}</select></label>
          <label>De<input type="date" value={from} max={to} onChange={(event) => setFrom(event.target.value)} /></label>
          <label>Até<input type="date" value={to} min={from} max={today} onChange={(event) => setTo(event.target.value)} /></label>
          <button className="primary-button" onClick={apply}>Gerar relatório</button>
          <button className="secondary-button" disabled={!report} onClick={() => void exportPdf()}><FileText size={17} />PDF</button>
          <button className="secondary-button" disabled={!report} onClick={exportCsv}><Download size={17} />CSV</button>
        </div>
      </article>

      {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}
      {loading && <div className="loading-state"><span /><p>Apurando jornada do funcionário...</p></div>}

      {!loading && report && (
        <>
          <section className="kpis context-report-kpis">
            <Kpi icon={CalendarCheck2} label="Dias com marcação" value={String(report.daysWithRecords)} tone="sage" />
            <Kpi icon={CheckCircle2} label="Jornadas encerradas" value={String(report.closedDays)} tone="blue" />
            <Kpi icon={Clock3} label="Tempo trabalhado" value={formatMinutes(report.workedMinutes)} tone="sage" />
            <Kpi icon={TimerReset} label="Saldo do período" value={formatBalance(report.balanceMinutes)} tone={report.balanceMinutes >= 0 ? "sage" : "terracotta"} />
          </section>
          <article className="panel data-panel context-report-table">
            <div className="panel-heading"><div><h2>{report.userName}</h2><p>{localDate(report.from)} a {localDate(report.to)}</p></div><Clock3 size={21} /></div>
            <div className="table-scroll"><table><thead><tr><th>Data</th><th>Situação</th><th>Primeira</th><th>Última</th><th>Marcações</th><th>Trabalhado</th><th>Previsto</th><th>Saldo</th></tr></thead>
              <tbody>{report.days.map((day) => <tr key={day.date}>
                <td><strong>{localDate(day.date)}</strong></td>
                <td><small className={`status time-${day.status.toLowerCase()}`}>{timeStatusLabel[day.status]}</small></td>
                <td>{day.entries.length ? timeFromLocalDateTime(day.entries[0].occurredAt) : "—"}</td>
                <td>{day.entries.length ? timeFromLocalDateTime(day.entries.at(-1)?.occurredAt ?? "") : "—"}</td>
                <td>{day.entries.length}</td><td>{formatMinutes(day.workedMinutes)}</td><td>{formatMinutes(day.expectedMinutes)}</td>
                <td><strong className={day.balanceMinutes >= 0 ? "positive-balance" : "negative-balance"}>{formatBalance(day.balanceMinutes)}</strong></td>
              </tr>)}</tbody></table>
              {report.days.length === 0 && <Empty text="Nenhuma marcação encontrada no período." />}
            </div>
          </article>
        </>
      )}
    </section>
  );
}
