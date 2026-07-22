import { useCallback, useEffect, useState, type CSSProperties } from "react";
import { BarChart3, Box, CalendarCheck2, Clock3, PackageSearch, TrendingUp, WalletCards } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { PageTitle } from "../../components/ui/PageTitle";
import type { ManagementReport, Session } from "../../types";
import { GroupedBarChart } from "./GroupedBarChart";
import { clinicToday, currency, firstDayOfMonth, hours, shiftDays } from "./reportUtils";

type Preset = "MONTH" | "30" | "90" | "YEAR" | "CUSTOM";

export function ManagementDashboardPage({ session }: { session: Session }) {
  const today = clinicToday(session.clinic.timezone);
  const [from, setFrom] = useState(firstDayOfMonth(today));
  const [to, setTo] = useState(today);
  const [appliedFrom, setAppliedFrom] = useState(from);
  const [appliedTo, setAppliedTo] = useState(to);
  const [preset, setPreset] = useState<Preset>("MONTH");
  const [report, setReport] = useState<ManagementReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setReport(await api.managementReport(session, appliedFrom, appliedTo));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao gerar o relatório");
    } finally {
      setLoading(false);
    }
  }, [appliedFrom, appliedTo, session]);

  useEffect(() => { void load(); }, [load]);

  function choosePreset(next: Exclude<Preset, "CUSTOM">) {
    const nextFrom = next === "MONTH"
      ? firstDayOfMonth(today)
      : shiftDays(today, next === "30" ? -29 : next === "90" ? -89 : -364);
    setFrom(nextFrom);
    setTo(today);
    setAppliedFrom(nextFrom);
    setAppliedTo(today);
    setPreset(next);
  }

  function apply() {
    if (!from || !to) return;
    setAppliedFrom(from);
    setAppliedTo(to);
    setPreset("CUSTOM");
  }

  return (
    <>
      <PageTitle
        eyebrow="INTELIGÊNCIA DE GESTÃO"
        title="Dashboard gerencial"
        description="Atendimentos, financeiro, estoque e equipe reunidos para uma leitura executiva."
      />

      <section className="panel report-filter-panel">
        <div className="report-presets" aria-label="Períodos rápidos">
          <button className={preset === "MONTH" ? "active" : ""} onClick={() => choosePreset("MONTH")}>Este mês</button>
          <button className={preset === "30" ? "active" : ""} onClick={() => choosePreset("30")}>30 dias</button>
          <button className={preset === "90" ? "active" : ""} onClick={() => choosePreset("90")}>90 dias</button>
          <button className={preset === "YEAR" ? "active" : ""} onClick={() => choosePreset("YEAR")}>12 meses</button>
        </div>
        <div className="report-date-fields">
          <label>De<input type="date" value={from} max={to} onChange={(event) => { setFrom(event.target.value); setPreset("CUSTOM"); }} /></label>
          <label>Até<input type="date" value={to} min={from} max={today} onChange={(event) => { setTo(event.target.value); setPreset("CUSTOM"); }} /></label>
          <button className="primary-button" onClick={apply}>Aplicar período</button>
        </div>
      </section>

      {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}
      {loading && <div className="loading-state"><span /><p>Consolidando dados da clínica...</p></div>}

      {!loading && report && (
        <>
          <div className="report-period-note">
            <BarChart3 size={17} />{report.period.days} dia(s) analisados · visualização {report.period.granularity === "DAILY" ? "diária" : "mensal"}
          </div>

          <section className="kpis report-kpis">
            <Kpi icon={CalendarCheck2} label="Atendimentos" value={String(report.appointments.total)} tone="sage" />
            <Kpi icon={TrendingUp} label="Comparecimento" value={`${report.appointments.attendanceRate.toFixed(1)}%`} tone="blue" />
            <Kpi icon={WalletCards} label="Recebido" value={currency(report.finance.received)} tone="sage" />
            <Kpi icon={WalletCards} label="Saldo realizado" value={currency(report.finance.net)} tone={report.finance.net >= 0 ? "sage" : "terracotta"} />
          </section>

          <section className="report-summary-grid">
            <SummaryCard icon={CalendarCheck2} title="Atendimentos" items={[
              ["Concluídos", report.appointments.completed],
              ["Cancelados", report.appointments.cancelled],
              ["Faltas", report.appointments.noShows],
            ]} />
            <SummaryCard icon={WalletCards} title="Financeiro" items={[
              ["A receber", currency(report.finance.receivable)],
              ["A pagar", currency(report.finance.payable)],
              ["Pago", currency(report.finance.paid)],
            ]} />
            <SummaryCard icon={PackageSearch} title="Estoque atual" warning={report.inventory.lowStock + report.inventory.expiredBatches > 0} items={[
              ["Materiais", report.inventory.activeMaterials],
              ["Estoque baixo", report.inventory.lowStock],
              ["Lotes vencidos", report.inventory.expiredBatches],
            ]} />
            <SummaryCard icon={Clock3} title="Jornada registrada" items={[
              ["Funcionários", report.time.employeesWithRecords],
              ["Dias com ponto", report.time.daysWithRecords],
              ["Saldo", hours(report.time.balanceMinutes)],
            ]} />
          </section>

          <section className="report-charts-grid">
            <article className="panel report-chart-panel">
              <div className="panel-heading"><div><h2>Evolução dos atendimentos</h2><p>Total agendado e concluído no período</p></div><CalendarCheck2 size={21} /></div>
              <GroupedBarChart
                points={report.trend.map((point) => ({ key: point.key, label: point.label, first: point.appointments, second: point.completed }))}
                firstLabel="Agendados" secondLabel="Concluídos" firstClass="chart-sage-soft" secondClass="chart-sage"
                formatValue={(value) => String(value)} ariaLabel="Gráfico de atendimentos agendados e concluídos" />
            </article>
            <article className="panel report-chart-panel">
              <div className="panel-heading"><div><h2>Fluxo financeiro realizado</h2><p>Valores recebidos e pagos por competência de baixa</p></div><WalletCards size={21} /></div>
              <GroupedBarChart
                points={report.trend.map((point) => ({ key: point.key, label: point.label, first: point.received, second: point.paid }))}
                firstLabel="Recebido" secondLabel="Pago" firstClass="chart-blue" secondClass="chart-terracotta"
                formatValue={currency} ariaLabel="Gráfico de valores recebidos e pagos" />
            </article>
          </section>

          <section className="report-detail-grid">
            <article className="panel specialty-report-panel">
              <div className="panel-heading"><div><h2>Desempenho por especialidade</h2><p>Comparecimento entre atendimentos concluídos e faltas</p></div><TrendingUp size={21} /></div>
              <div className="specialty-performance-list">
                {report.specialties.map((item) => (
                  <article key={item.specialtyName} style={{ "--specialty-color": item.color } as CSSProperties}>
                    <div><strong>{item.specialtyName}</strong><small>{item.total} atendimento(s) · {item.noShows} falta(s)</small></div>
                    <span>{item.attendanceRate.toFixed(1)}%</span>
                    <div className="specialty-progress"><i style={{ width: `${item.attendanceRate}%` }} /></div>
                  </article>
                ))}
                {report.specialties.length === 0 && <Empty text="Nenhum atendimento encontrado no período." />}
              </div>
            </article>

            <article className="panel inventory-report-panel">
              <div className="panel-heading"><div><h2>Alertas de estoque</h2><p>Posição atual, independente do período selecionado</p></div><Box size={21} /></div>
              <div className="inventory-report-numbers">
                <RiskNumber label="Estoque baixo" value={report.inventory.lowStock} warning />
                <RiskNumber label="Lotes vencidos" value={report.inventory.expiredBatches} warning />
                <RiskNumber label="A vencer em 30 dias" value={report.inventory.expiringIn30Days} warning={report.inventory.expiringIn30Days > 0} />
                <RiskNumber label="Materiais ativos" value={report.inventory.activeMaterials} />
              </div>
            </article>
          </section>

          <article className="panel data-panel employee-hours-panel">
            <div className="panel-heading"><div><h2>Horas registradas pela equipe</h2><p>Jornada prevista calculada somente nos dias que possuem marcações</p></div><Clock3 size={21} /></div>
            <div className="table-scroll"><table><thead><tr><th>Funcionário</th><th>Dias registrados</th><th>Trabalhado</th><th>Previsto</th><th>Saldo</th></tr></thead>
              <tbody>{report.employeeHours.map((employee) => (
                <tr key={employee.userId}><td><div className="person"><span>{initials(employee.userName)}</span><strong>{employee.userName}</strong></div></td><td>{employee.daysWithRecords}</td><td>{hours(employee.workedMinutes).replace("+", "")}</td><td>{hours(employee.expectedMinutes).replace("+", "")}</td><td><strong className={employee.balanceMinutes >= 0 ? "positive-balance" : "negative-balance"}>{hours(employee.balanceMinutes)}</strong></td></tr>
              ))}</tbody></table>
              {report.employeeHours.length === 0 && <Empty text="Nenhuma jornada registrada no período." />}
            </div>
          </article>
        </>
      )}
    </>
  );
}

function SummaryCard({ icon: Icon, title, items, warning = false }: {
  icon: typeof CalendarCheck2;
  title: string;
  items: Array<[string, string | number]>;
  warning?: boolean;
}) {
  return <article className={`panel report-summary-card ${warning ? "warning" : ""}`}><div><span><Icon size={20} /></span><strong>{title}</strong></div><dl>{items.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></article>;
}

function RiskNumber({ label, value, warning = false }: { label: string; value: number; warning?: boolean }) {
  return <div className={warning && value > 0 ? "warning" : ""}><strong>{value}</strong><span>{label}</span></div>;
}

function initials(name: string) {
  return name.split(" ").filter(Boolean).map((part) => part[0]).slice(0, 2).join("");
}
