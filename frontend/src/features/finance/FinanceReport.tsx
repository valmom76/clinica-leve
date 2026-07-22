import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowDownCircle, ArrowUpCircle, CircleDollarSign, Download, FileText, History, Scale } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { ReportIdentity } from "../../components/ui/ReportIdentity";
import type {
  FinanceContextReport,
  FinancialCategory,
  FinancialEntryStatus,
  FinancialEntryType,
  Session,
} from "../../types";
import { downloadCsv, firstDayOfMonth, localDate as reportDate } from "../../utils/reporting";
import { clinicToday } from "../../utils/reporting";
import { downloadReportPdf } from "../../utils/reportPdf";
import { currency, localDate, statusLabel, typeLabel } from "./financeUtils";

type TypeFilter = FinancialEntryType | "ALL";
type StatusFilter = FinancialEntryStatus | "ALL";

export function FinanceReport({ session, categories }: { session: Session; categories: FinancialCategory[] }) {
  const today = clinicToday(session.clinic.timezone);
  const [from, setFrom] = useState(firstDayOfMonth(today));
  const [to, setTo] = useState(today);
  const [type, setType] = useState<TypeFilter>("ALL");
  const [categoryId, setCategoryId] = useState("");
  const [status, setStatus] = useState<StatusFilter>("ALL");
  const [filters, setFilters] = useState({ from, to, type: "ALL" as TypeFilter, categoryId: "", status: "ALL" as StatusFilter });
  const [report, setReport] = useState<FinanceContextReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const availableCategories = useMemo(
    () => categories.filter((category) => type === "ALL" || category.type === type),
    [categories, type],
  );

  const categoryTotals = useMemo(() => {
    if (!report) return [];
    const totals = new Map<string, { name: string; type: FinancialEntryType; amount: number }>();
    report.entries.filter((entry) => entry.status !== "CANCELLED").forEach((entry) => {
      const key = `${entry.type}:${entry.categoryId}`;
      const current = totals.get(key) ?? { name: entry.categoryName, type: entry.type, amount: 0 };
      current.amount += entry.amount;
      totals.set(key, current);
    });
    return [...totals.values()].sort((a, b) => b.amount - a.amount).slice(0, 6);
  }, [report]);
  const maxCategoryAmount = categoryTotals[0]?.amount ?? 0;

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setReport(await api.financeReport(session, {
        from: filters.from,
        to: filters.to,
        type: filters.type === "ALL" ? undefined : filters.type,
        categoryId: filters.categoryId || undefined,
        status: filters.status === "ALL" ? undefined : filters.status,
      }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao gerar relatório financeiro");
    } finally {
      setLoading(false);
    }
  }, [filters, session]);

  useEffect(() => { void load(); }, [load]);

  function changeType(nextType: TypeFilter) {
    setType(nextType);
    if (categoryId && nextType !== "ALL" && categories.find((category) => category.id === categoryId)?.type !== nextType) {
      setCategoryId("");
    }
  }

  function apply() {
    if (!from || !to) return;
    setFilters({ from, to, type, categoryId, status });
  }

  function exportCsv() {
    if (!report) return;
    downloadCsv(`financeiro-${report.from}-${report.to}.csv`, [
      ["Relatório financeiro"],
      ["Período", `${reportDate(report.from)} a ${reportDate(report.to)}`],
      ["Tipo", filters.type === "ALL" ? "Receitas e despesas" : typeLabel[filters.type]],
      ["Categoria", categories.find((category) => category.id === filters.categoryId)?.name ?? "Todas"],
      ["Status", filters.status === "ALL" ? "Todos" : statusLabel[filters.status]],
      [],
      ["Vencimento", "Pagamento", "Descrição", "Tipo", "Categoria", "Favorecido / pagador", "Valor", "Status", "Forma de pagamento"],
      ...report.entries.map((entry) => [
        reportDate(entry.dueDate),
        entry.paymentDate ? reportDate(entry.paymentDate) : "",
        entry.description,
        typeLabel[entry.type],
        entry.categoryName,
        entry.counterparty ?? "",
        entry.amount.toFixed(2).replace(".", ","),
        statusLabel[entry.status],
        entry.paymentMethod ?? "",
      ]),
    ]);
  }

  async function exportPdf() {
    if (!report) return;
    try {
      await downloadReportPdf({
        session,
        filename: `financeiro-${report.from}-${report.to}.pdf`,
        title: "Relatório financeiro",
        subtitle: `${reportDate(report.from)} a ${reportDate(report.to)}`,
        filters: [
          ["Tipo", filters.type === "ALL" ? "Receitas e despesas" : typeLabel[filters.type]],
          ["Categoria", categories.find((category) => category.id === filters.categoryId)?.name ?? "Todas"],
          ["Status", filters.status === "ALL" ? "Todos" : statusLabel[filters.status]],
        ],
        summary: [
          ["Recebido", currency(report.received)],
          ["Pago", currency(report.paid)],
          ["Saldo realizado", currency(report.net)],
          ["A receber", currency(report.receivable)],
          ["A pagar", currency(report.payable)],
        ],
        columns: ["Vencimento", "Pagamento", "Lançamento", "Tipo", "Categoria", "Valor", "Status"],
        rows: report.entries.map((entry) => [
          reportDate(entry.dueDate),
          entry.paymentDate ? reportDate(entry.paymentDate) : "—",
          entry.description,
          typeLabel[entry.type],
          entry.categoryName,
          currency(entry.amount),
          statusLabel[entry.status],
        ]),
        landscape: true,
      });
    } catch {
      setError("Não foi possível gerar o PDF do relatório");
    }
  }

  return (
    <section className="context-report">
      <article className="panel module-report-filter finance-report-filter">
        <ReportIdentity session={session} title="Movimentação financeira" description="Analise o realizado e as obrigações dentro do período." />
        <div className="module-report-fields finance-report-fields">
          <label>Tipo<select value={type} onChange={(event) => changeType(event.target.value as TypeFilter)}><option value="ALL">Receitas e despesas</option><option value="INCOME">Receitas</option><option value="EXPENSE">Despesas</option></select></label>
          <label>Categoria<select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}><option value="">Todas as categorias</option>{availableCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
          <label>Status<select value={status} onChange={(event) => setStatus(event.target.value as StatusFilter)}><option value="ALL">Todos os status</option><option value="OPEN">Em aberto</option><option value="OVERDUE">Atrasados</option><option value="PAID">Baixados</option><option value="CANCELLED">Cancelados</option></select></label>
          <label>De<input type="date" value={from} max={to} onChange={(event) => setFrom(event.target.value)} /></label>
          <label>Até<input type="date" value={to} min={from} onChange={(event) => setTo(event.target.value)} /></label>
          <button className="primary-button" onClick={apply}>Gerar relatório</button>
          <button className="secondary-button" disabled={!report} onClick={() => void exportPdf()}><FileText size={17} />PDF</button>
          <button className="secondary-button" disabled={!report} onClick={exportCsv}><Download size={17} />CSV</button>
        </div>
      </article>

      {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}
      {loading && <div className="loading-state"><span /><p>Apurando movimentação financeira...</p></div>}

      {!loading && report && <>
        <section className="kpis context-report-kpis">
          <Kpi icon={ArrowDownCircle} label="Recebido" value={currency(report.received)} tone="sage" />
          <Kpi icon={ArrowUpCircle} label="Pago" value={currency(report.paid)} tone="terracotta" />
          <Kpi icon={Scale} label="Saldo realizado" value={currency(report.net)} tone={report.net >= 0 ? "sage" : "terracotta"} />
          <Kpi icon={CircleDollarSign} label="A receber" value={currency(report.receivable)} tone="blue" />
        </section>
        <div className="report-period-note"><History size={17} />A pagar no período: <strong>{currency(report.payable)}</strong> · {report.entryCount} lançamento(s)</div>

        {categoryTotals.length > 0 && <article className="panel finance-category-report">
          <div className="panel-heading"><div><h2>Distribuição por categoria</h2><p>Maiores valores entre os lançamentos filtrados, exceto cancelados</p></div><CircleDollarSign size={21} /></div>
          <div className="finance-category-bars">{categoryTotals.map((category) => <div key={`${category.type}:${category.name}`}>
            <div><strong>{category.name}</strong><span className={`finance-type ${category.type.toLowerCase()}`}>{typeLabel[category.type]}</span><b>{currency(category.amount)}</b></div>
            <i><span className={category.type === "INCOME" ? "income" : "expense"} style={{ width: `${maxCategoryAmount ? Math.max(5, category.amount / maxCategoryAmount * 100) : 0}%` }} /></i>
          </div>)}</div>
        </article>}

        <article className="panel data-panel context-report-table">
          <div className="panel-heading"><div><h2>Detalhamento financeiro</h2><p>{localDate(report.from)} a {localDate(report.to)}</p></div><History size={21} /></div>
          <div className="table-scroll"><table><thead><tr><th>Vencimento</th><th>Pagamento</th><th>Lançamento</th><th>Tipo</th><th>Categoria</th><th>Valor</th><th>Status</th></tr></thead>
            <tbody>{report.entries.map((entry) => <tr key={entry.id}>
              <td>{localDate(entry.dueDate)}</td><td>{localDate(entry.paymentDate)}</td>
              <td><div className="material-name"><strong>{entry.description}</strong><small>{entry.counterparty ?? "Sem favorecido"}</small></div></td>
              <td><span className={`finance-type ${entry.type.toLowerCase()}`}>{typeLabel[entry.type]}</span></td><td>{entry.categoryName}</td>
              <td><strong className={entry.type === "INCOME" ? "money-income" : "money-expense"}>{currency(entry.amount)}</strong></td>
              <td><small className={`status finance-${entry.status.toLowerCase()}`}>{statusLabel[entry.status]}</small></td>
            </tr>)}</tbody></table>
            {report.entries.length === 0 && <Empty text="Nenhum lançamento encontrado para os filtros." />}
          </div>
        </article>
      </>}
    </section>
  );
}
