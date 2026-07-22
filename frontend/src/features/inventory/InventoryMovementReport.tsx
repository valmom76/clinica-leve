import { useCallback, useEffect, useState } from "react";
import { ArrowDownToLine, ArrowUpFromLine, Boxes, Download, FileText, History } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { ReportIdentity } from "../../components/ui/ReportIdentity";
import type { InventoryMovementReport as InventoryMovementReportData, Session, StockMaterial, StockMovementType } from "../../types";
import { clinicToday, downloadCsv, firstDayOfMonth, localDate, localDateTime } from "../../utils/reporting";
import { downloadReportPdf } from "../../utils/reportPdf";
import { formatQuantity } from "./inventoryUtils";

type MovementTypeFilter = StockMovementType | "ALL";

export function InventoryMovementReport({ session, materials }: { session: Session; materials: StockMaterial[] }) {
  const today = clinicToday(session.clinic.timezone);
  const [from, setFrom] = useState(firstDayOfMonth(today));
  const [to, setTo] = useState(today);
  const [materialId, setMaterialId] = useState("");
  const [type, setType] = useState<MovementTypeFilter>("ALL");
  const [filters, setFilters] = useState({ from, to, materialId: "", type: "ALL" as MovementTypeFilter });
  const [report, setReport] = useState<InventoryMovementReportData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setReport(await api.inventoryMovementReport(session, {
        from: filters.from,
        to: filters.to,
        materialId: filters.materialId || undefined,
        type: filters.type === "ALL" ? undefined : filters.type,
      }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao gerar relatório de movimentações");
    } finally {
      setLoading(false);
    }
  }, [filters, session]);

  useEffect(() => { void load(); }, [load]);

  function apply() {
    if (!from || !to) return;
    setFilters({ from, to, materialId, type });
  }

  function exportCsv() {
    if (!report) return;
    downloadCsv(`movimentacoes-estoque-${report.from}-${report.to}.csv`, [
      ["Relatório de movimentações de estoque"],
      ["Período", `${localDate(report.from)} a ${localDate(report.to)}`],
      ["Material", report.materialName ?? "Todos os materiais"],
      [],
      ["Data e hora", "Material", "Tipo", "Quantidade", "Unidade", "Saldo após", "Lote", "Motivo", "Registrado por"],
      ...report.movements.map((movement) => [
        localDateTime(movement.occurredAt), movement.materialName,
        movement.type === "ENTRY" ? "Entrada" : "Saída", movement.quantity,
        movement.unit, movement.balanceAfter, movement.lotNumber ?? "", movement.reason,
        movement.createdByUserName,
      ]),
    ]);
  }

  async function exportPdf() {
    if (!report) return;
    try {
      await downloadReportPdf({
        session,
        filename: `movimentacoes-estoque-${report.from}-${report.to}.pdf`,
        title: "Relatório de movimentações de estoque",
        subtitle: `${localDate(report.from)} a ${localDate(report.to)}`,
        filters: [
          ["Material", report.materialName ?? "Todos os materiais"],
          ["Movimentação", filters.type === "ALL" ? "Entradas e saídas" : filters.type === "ENTRY" ? "Entradas" : "Saídas"],
        ],
        summary: [
          ["Movimentações", String(report.movementCount)],
          ["Entradas", String(report.entryCount)],
          ["Saídas", String(report.exitCount)],
          ["Materiais", String(report.distinctMaterials)],
        ],
        columns: ["Data e hora", "Material", "Tipo", "Quantidade", "Saldo após", "Lote", "Motivo", "Registrado por"],
        rows: report.movements.map((movement) => [
          localDateTime(movement.occurredAt),
          movement.materialName,
          movement.type === "ENTRY" ? "Entrada" : "Saída",
          formatQuantity(movement.quantity, movement.unit),
          formatQuantity(movement.balanceAfter, movement.unit),
          movement.lotNumber ?? "—",
          movement.reason,
          movement.createdByUserName,
        ]),
        landscape: true,
      });
    } catch {
      setError("Não foi possível gerar o PDF do relatório");
    }
  }

  return (
    <section className="context-report">
      <article className="panel module-report-filter">
        <ReportIdentity session={session} title="Entradas e saídas" description="Consulte a rastreabilidade das movimentações do estoque." />
        <div className="module-report-fields inventory-report-fields">
          <label>Material<select value={materialId} onChange={(event) => setMaterialId(event.target.value)}><option value="">Todos os materiais</option>{materials.map((material) => <option key={material.id} value={material.id}>{material.name}</option>)}</select></label>
          <label>Movimentação<select value={type} onChange={(event) => setType(event.target.value as MovementTypeFilter)}><option value="ALL">Entradas e saídas</option><option value="ENTRY">Somente entradas</option><option value="EXIT">Somente saídas</option></select></label>
          <label>De<input type="date" value={from} max={to} onChange={(event) => setFrom(event.target.value)} /></label>
          <label>Até<input type="date" value={to} min={from} max={today} onChange={(event) => setTo(event.target.value)} /></label>
          <button className="primary-button" onClick={apply}>Gerar relatório</button>
          <button className="secondary-button" disabled={!report} onClick={() => void exportPdf()}><FileText size={17} />PDF</button>
          <button className="secondary-button" disabled={!report} onClick={exportCsv}><Download size={17} />CSV</button>
        </div>
      </article>

      {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}
      {loading && <div className="loading-state"><span /><p>Consultando movimentações...</p></div>}

      {!loading && report && <>
        <section className="kpis context-report-kpis">
          <Kpi icon={History} label="Movimentações" value={String(report.movementCount)} tone="blue" />
          <Kpi icon={ArrowDownToLine} label="Registros de entrada" value={String(report.entryCount)} tone="sage" />
          <Kpi icon={ArrowUpFromLine} label="Registros de saída" value={String(report.exitCount)} tone="terracotta" />
          <Kpi icon={Boxes} label="Materiais movimentados" value={String(report.distinctMaterials)} tone="sage" />
        </section>
        {report.materialId && <div className="report-period-note"><Boxes size={17} />
          {report.materialName}: entradas {formatQuantity(report.totalEntryQuantity ?? 0, report.unit ?? "un")} · saídas {formatQuantity(report.totalExitQuantity ?? 0, report.unit ?? "un")}
        </div>}
        <article className="panel data-panel context-report-table">
          <div className="panel-heading"><div><h2>Histórico de movimentações</h2><p>{localDate(report.from)} a {localDate(report.to)} · {report.materialName ?? "Todos os materiais"}</p></div><History size={21} /></div>
          <div className="table-scroll"><table><thead><tr><th>Data e hora</th><th>Material</th><th>Tipo</th><th>Quantidade</th><th>Saldo após</th><th>Lote</th><th>Motivo</th><th>Registrado por</th></tr></thead>
            <tbody>{report.movements.map((movement) => <tr key={movement.id}>
              <td>{localDateTime(movement.occurredAt)}</td><td><strong>{movement.materialName}</strong></td>
              <td><span className={`movement-kind ${movement.type === "EXIT" ? "exit" : ""}`}>{movement.type === "ENTRY" ? "Entrada" : "Saída"}</span></td>
              <td><strong>{formatQuantity(movement.quantity, movement.unit)}</strong></td><td>{formatQuantity(movement.balanceAfter, movement.unit)}</td>
              <td>{movement.lotNumber ?? "—"}</td><td>{movement.reason}</td><td>{movement.createdByUserName}</td>
            </tr>)}</tbody></table>
            {report.movements.length === 0 && <Empty text="Nenhuma movimentação encontrada para os filtros." />}
          </div>
        </article>
      </>}
    </section>
  );
}
