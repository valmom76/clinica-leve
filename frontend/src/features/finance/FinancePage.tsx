import { useCallback, useEffect, useMemo, useState } from "react";
import { CheckCircle2, CircleDollarSign, FolderPlus, Pencil, Plus, RotateCcw, Search, XCircle } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { PageTitle } from "../../components/ui/PageTitle";
import type { FinancialCategory, FinancialEntry, FinancialEntryStatus, FinancialEntryType, Session } from "../../types";
import { FinanceCategoryModal } from "./FinanceCategoryModal";
import { currency, localDate, statusLabel, typeLabel } from "./financeUtils";
import { FinancialEntryModal } from "./FinancialEntryModal";
import { SettleModal } from "./SettleModal";

export function FinancePage({ session }: { session: Session }) {
  const [categories, setCategories] = useState<FinancialCategory[]>([]);
  const [entries, setEntries] = useState<FinancialEntry[]>([]);
  const [search, setSearch] = useState("");
  const [type, setType] = useState<FinancialEntryType | "ALL">("ALL");
  const [status, setStatus] = useState<FinancialEntryStatus | "ALL">("ALL");
  const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const [categoryModal, setCategoryModal] = useState(false);
  const [entryModal, setEntryModal] = useState<FinancialEntry | "new" | null>(null);
  const [settle, setSettle] = useState<FinancialEntry | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try { const [c, e] = await Promise.all([api.financialCategories(session), api.financialEntries(session)]); setCategories(c); setEntries(e); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao carregar financeiro"); }
    finally { setLoading(false); }
  }, [session]);
  useEffect(() => { void load(); }, [load]);

  const visible = useMemo(() => entries.filter((entry) => {
    const matchesSearch = [entry.description, entry.categoryName, entry.counterparty ?? ""].some((v) => v.toLowerCase().includes(search.toLowerCase()));
    return matchesSearch && (type === "ALL" || entry.type === type) && (status === "ALL" || entry.status === status);
  }), [entries, search, type, status]);

  const active = entries.filter((e) => e.status !== "CANCELLED");
  const receivable = active.filter((e) => e.type === "INCOME" && ["OPEN", "OVERDUE"].includes(e.status)).reduce((s, e) => s + e.amount, 0);
  const payable = active.filter((e) => e.type === "EXPENSE" && ["OPEN", "OVERDUE"].includes(e.status)).reduce((s, e) => s + e.amount, 0);
  const received = active.filter((e) => e.type === "INCOME" && e.status === "PAID").reduce((s, e) => s + e.amount, 0);
  const paid = active.filter((e) => e.type === "EXPENSE" && e.status === "PAID").reduce((s, e) => s + e.amount, 0);

  function update(saved: FinancialEntry) {
    setEntries((current) => {
      const next = current.some((e) => e.id === saved.id) ? current.map((e) => e.id === saved.id ? saved : e) : [saved, ...current];
      return next.sort((a, b) => b.dueDate.localeCompare(a.dueDate));
    }); setEntryModal(null); setSettle(null);
  }
  async function reopen(entry: FinancialEntry) { try { update(await api.reopenFinancialEntry(session, entry.id)); } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao reabrir"); } }
  async function cancel(entry: FinancialEntry) {
    if (!window.confirm(`Cancelar o lançamento “${entry.description}”?`)) return;
    try { update(await api.cancelFinancialEntry(session, entry.id)); } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao cancelar"); }
  }

  return <>
    <PageTitle eyebrow="GESTÃO FINANCEIRA" title="Financeiro" description="Contas a pagar, receber e fluxo realizado desta clínica."
      action={<div className="inventory-title-actions">
        <button className="secondary-button" onClick={() => setCategoryModal(true)}><FolderPlus size={17} />Categoria</button>
        <button className="primary-button" onClick={() => setEntryModal("new")}><Plus size={17} />Novo lançamento</button>
      </div>} />
    <section className="kpis">
      <Kpi icon={CircleDollarSign} label="A receber" value={currency(receivable)} tone="sage" />
      <Kpi icon={CircleDollarSign} label="A pagar" value={currency(payable)} tone="terracotta" />
      <Kpi icon={CheckCircle2} label="Recebido" value={currency(received)} tone="blue" />
      <Kpi icon={CheckCircle2} label="Saldo realizado" value={currency(received - paid)} tone="sage" />
    </section>
    {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}
    <article className="panel data-panel">
      <div className="finance-toolbar">
        <div className="field-search"><Search size={16} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar lançamento" /></div>
        <select value={type} onChange={(e) => setType(e.target.value as FinancialEntryType | "ALL")}><option value="ALL">Todos os tipos</option><option value="INCOME">Receitas</option><option value="EXPENSE">Despesas</option></select>
        <select value={status} onChange={(e) => setStatus(e.target.value as FinancialEntryStatus | "ALL")}><option value="ALL">Todos os status</option><option value="OPEN">Em aberto</option><option value="OVERDUE">Atrasados</option><option value="PAID">Baixados</option><option value="CANCELLED">Cancelados</option></select>
        <span>{visible.length} lançamento(s)</span>
      </div>
      <div className="table-scroll"><table><thead><tr><th>Lançamento</th><th>Tipo</th><th>Categoria</th><th>Vencimento</th><th>Valor</th><th>Status</th><th aria-label="Ações" /></tr></thead>
        <tbody>{!loading && visible.map((entry) => <tr key={entry.id}>
          <td><div className="material-name"><strong>{entry.description}</strong><small>{entry.counterparty ?? "Sem favorecido"}</small></div></td>
          <td><span className={`finance-type ${entry.type.toLowerCase()}`}>{typeLabel[entry.type]}</span></td>
          <td>{entry.categoryName}</td><td>{localDate(entry.dueDate)}</td>
          <td><strong className={entry.type === "INCOME" ? "money-income" : "money-expense"}>{currency(entry.amount)}</strong></td>
          <td><small className={`status finance-${entry.status.toLowerCase()}`}>{statusLabel[entry.status]}</small></td>
          <td><div className="inventory-row-actions">
            {["OPEN", "OVERDUE"].includes(entry.status) && <><button title="Editar" onClick={() => setEntryModal(entry)}><Pencil size={15} /></button><button title="Baixar" onClick={() => setSettle(entry)}><CheckCircle2 size={15} /></button><button title="Cancelar" onClick={() => void cancel(entry)}><XCircle size={15} /></button></>}
            {["PAID", "CANCELLED"].includes(entry.status) && <button title="Reabrir" onClick={() => void reopen(entry)}><RotateCcw size={15} /></button>}
          </div></td>
        </tr>)}</tbody></table>
        {loading && <Empty text="Carregando lançamentos..." />}{!loading && visible.length === 0 && <Empty text="Nenhum lançamento encontrado." />}
      </div>
    </article>
    {categoryModal && <FinanceCategoryModal session={session} onClose={() => setCategoryModal(false)} onCreated={(c) => { setCategories((v) => [...v, c]); setCategoryModal(false); }} />}
    {entryModal && <FinancialEntryModal session={session} categories={categories} entry={entryModal === "new" ? undefined : entryModal} onClose={() => setEntryModal(null)} onSaved={update} />}
    {settle && <SettleModal session={session} entry={settle} onClose={() => setSettle(null)} onSaved={update} />}
  </>;
}
