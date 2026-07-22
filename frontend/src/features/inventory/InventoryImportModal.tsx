import { useMemo, useState } from "react";
import { AlertTriangle, CheckCircle2, Download, FileSpreadsheet, SearchCheck, Upload } from "lucide-react";
import { api } from "../../api";
import { Modal } from "../../components/ui/Modal";
import type {
  InventoryImportAction,
  InventoryImportDecision,
  InventoryImportPreview,
  InventoryImportPreviewRow,
  InventoryImportResult,
  Session,
} from "../../types";

type ChoiceMap = Record<number, string>;

const actionLabel = {
  CREATE: "Novo",
  UPDATE: "Alteração",
  REVIEW: "Revisar",
  UNCHANGED: "Sem alteração",
  ERROR: "Com erro",
} as const;

export function InventoryImportModal({ session, onClose, onImported }: {
  session: Session;
  onClose: () => void;
  onImported: () => void | Promise<void>;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<InventoryImportPreview | null>(null);
  const [choices, setChoices] = useState<ChoiceMap>({});
  const [result, setResult] = useState<InventoryImportResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState("");

  const selectedCount = useMemo(
    () => Object.values(choices).filter((choice) => choice !== "SKIP").length,
    [choices],
  );

  async function downloadTemplate() {
    setDownloading(true);
    setError("");
    try {
      const blob = await api.inventoryImportTemplate(session);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "modelo-importacao-materiais.xlsx";
      anchor.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao baixar o modelo");
    } finally {
      setDownloading(false);
    }
  }

  function selectFile(selected: File | null) {
    setError("");
    setPreview(null);
    setChoices({});
    setResult(null);
    if (selected && !selected.name.toLowerCase().endsWith(".xlsx")) {
      setFile(null);
      setError("Selecione um arquivo XLSX gerado a partir do modelo.");
      return;
    }
    if (selected && selected.size > 8 * 1024 * 1024) {
      setFile(null);
      setError("A planilha deve ter no máximo 8 MB.");
      return;
    }
    setFile(selected);
  }

  async function analyze() {
    if (!file) return;
    setLoading(true);
    setError("");
    try {
      const nextPreview = await api.previewInventoryImport(session, file);
      setPreview(nextPreview);
      setChoices(Object.fromEntries(nextPreview.rows.map((row) => [row.rowNumber, initialChoice(row)])));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao analisar a planilha");
    } finally {
      setLoading(false);
    }
  }

  async function confirm() {
    if (!file || !preview || selectedCount === 0) return;
    setLoading(true);
    setError("");
    try {
      const decisions = preview.rows.map((row) => decision(row.rowNumber, choices[row.rowNumber] ?? "SKIP"));
      const imported = await api.confirmInventoryImport(session, file, decisions);
      setResult(imported);
      await onImported();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao confirmar a importação");
    } finally {
      setLoading(false);
    }
  }

  return <Modal
    className="inventory-import-modal"
    title="Importação de materiais por Excel"
    description="Baixe o modelo, preencha os dados e revise o merge antes de alterar o estoque."
    onClose={onClose}
  >
    {error && <div className="form-error import-error">{error}</div>}

    {result ? <div className="import-result">
      <span><CheckCircle2 size={34} /></span>
      <h3>Importação concluída</h3>
      <p>{result.created} produto(s) incluído(s), {result.updated} atualizado(s) e {result.skipped} ignorado(s).</p>
      <button className="primary-button" onClick={onClose}>Concluir</button>
    </div> : <>
      <section className="import-start-grid">
        <article>
          <span>1</span><div><strong>Baixe o modelo atualizado</strong><p>Se já existirem produtos, eles virão preenchidos com o ID usado para o merge.</p></div>
          <button className="secondary-button" disabled={downloading} onClick={() => void downloadTemplate()}><Download size={17} />{downloading ? "Gerando..." : "Baixar modelo Excel"}</button>
        </article>
        <article>
          <span>2</span><div><strong>Envie a planilha preenchida</strong><p>O saldo atual é somente leitura; entradas e saídas continuam auditadas separadamente.</p></div>
          <label className="secondary-button import-file-button"><FileSpreadsheet size={17} />{file?.name ?? "Selecionar planilha"}<input type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onClick={(event) => { event.currentTarget.value = ""; }} onChange={(event) => selectFile(event.target.files?.[0] ?? null)} /></label>
          <button className="primary-button" disabled={!file || loading} onClick={() => void analyze()}><SearchCheck size={17} />{loading ? "Analisando..." : "Analisar planilha"}</button>
        </article>
      </section>

      {preview && <>
        <section className="import-preview-summary">
          <div className="create"><strong>{preview.createCount}</strong><span>novos</span></div>
          <div className="update"><strong>{preview.updateCount}</strong><span>alterações</span></div>
          <div className="review"><strong>{preview.reviewCount}</strong><span>para revisar</span></div>
          <div><strong>{preview.unchangedCount}</strong><span>sem mudança</span></div>
          <div className="error"><strong>{preview.errorCount}</strong><span>com erro</span></div>
        </section>
        {preview.reviewCount > 0 && <div className="import-review-warning"><AlertTriangle size={18} /><span>Descrições parecidas começam como “Ignorar”. Escolha conscientemente entre cadastrar um novo produto ou atualizar um dos semelhantes.</span></div>}
        <div className="import-preview-table"><table><thead><tr><th>Linha</th><th>Produto da planilha</th><th>Diagnóstico</th><th>Avisos</th><th>Decisão</th></tr></thead>
          <tbody>{preview.rows.map((row) => <tr key={row.rowNumber} className={`import-row-${row.suggestedAction.toLowerCase()}`}>
            <td>{row.rowNumber}</td>
            <td><div className="material-name"><strong>{row.name || "Descrição ausente"}</strong><small>{row.categoryName || "Sem categoria"} · {row.sku || "sem código"} · {row.unit || "sem unidade"}</small></div></td>
            <td><span className={`import-action-badge ${row.suggestedAction.toLowerCase()}`}>{actionLabel[row.suggestedAction]}</span>{row.targetMaterialName && <small className="import-match">{row.matchReason}: {row.targetMaterialName}</small>}</td>
            <td><div className="import-messages">{row.errors.map((message) => <small className="error" key={message}>{message}</small>)}{row.warnings.map((message) => <small className="warning" key={message}>{message}</small>)}</div></td>
            <td><DecisionSelect row={row} value={choices[row.rowNumber] ?? "SKIP"} onChange={(value) => setChoices((current) => ({ ...current, [row.rowNumber]: value }))} /></td>
          </tr>)}</tbody></table></div>
        <div className="import-confirm-bar">
          <div><strong>{selectedCount} alteração(ões) selecionada(s)</strong><small>Linhas ignoradas, sem mudança ou com erro não serão gravadas.</small></div>
          <button className="secondary-button" onClick={() => { setPreview(null); setChoices({}); }}>Trocar arquivo</button>
          <button className="primary-button" disabled={selectedCount === 0 || loading} onClick={() => void confirm()}><Upload size={17} />{loading ? "Aplicando merge..." : "Confirmar importação"}</button>
        </div>
      </>}
    </>}
  </Modal>;
}

function DecisionSelect({ row, value, onChange }: {
  row: InventoryImportPreviewRow;
  value: string;
  onChange: (value: string) => void;
}) {
  if (row.suggestedAction === "ERROR") return <select disabled value="SKIP"><option value="SKIP">Ignorar — corrija a planilha</option></select>;
  if (row.suggestedAction === "UNCHANGED") return <select disabled value="SKIP"><option value="SKIP">Ignorar — sem alterações</option></select>;
  return <select value={value} onChange={(event) => onChange(event.target.value)}>
    <option value="SKIP">Ignorar esta linha</option>
    {row.suggestedAction === "CREATE" && <option value="CREATE">Cadastrar como novo</option>}
    {row.suggestedAction === "UPDATE" && row.targetMaterialId && <option value={`UPDATE:${row.targetMaterialId}`}>Atualizar {row.targetMaterialName}</option>}
    {row.suggestedAction === "REVIEW" && <>
      <option value="CREATE">Cadastrar mesmo assim como novo</option>
      {row.similarMaterials.map((candidate) => <option key={candidate.materialId} value={`UPDATE:${candidate.materialId}`}>Atualizar {candidate.materialName} ({candidate.similarityPercent}% semelhante)</option>)}
    </>}
  </select>;
}

function initialChoice(row: InventoryImportPreviewRow) {
  if (row.suggestedAction === "CREATE") return "CREATE";
  if (row.suggestedAction === "UPDATE" && row.targetMaterialId) return `UPDATE:${row.targetMaterialId}`;
  return "SKIP";
}

function decision(rowNumber: number, choice: string): InventoryImportDecision {
  if (choice === "CREATE") return { rowNumber, action: "CREATE" };
  if (choice.startsWith("UPDATE:")) return { rowNumber, action: "UPDATE", targetMaterialId: choice.slice(7) };
  return { rowNumber, action: "SKIP" as InventoryImportAction };
}
