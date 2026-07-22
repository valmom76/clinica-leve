import { useCallback, useEffect, useState } from "react";
import {
  CheckCircle2,
  Download,
  FilePlus2,
  Files,
  Printer,
  RefreshCcw,
  Save,
  ShieldCheck,
} from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import type {
  ClinicalDocument,
  ClinicalEncounter,
  ClinicalTemplate,
  Session,
} from "../../types";
import { documentTypeLabel } from "./clinicalLabels";
import { outputClinicalDocumentPdf } from "./clinicalDocumentPdf";
import { SignatureModal } from "./SignatureModal";

type ClinicalDocumentsPanelProps = {
  session: Session;
  encounter: ClinicalEncounter;
  canFinalize: boolean;
};

export function ClinicalDocumentsPanel({
  session,
  encounter,
  canFinalize,
}: ClinicalDocumentsPanelProps) {
  const [templates, setTemplates] = useState<ClinicalTemplate[]>([]);
  const [documents, setDocuments] = useState<ClinicalDocument[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [templateId, setTemplateId] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const [signing, setSigning] = useState(false);

  const selected = documents.find((document) => document.id === selectedId);

  const load = useCallback(async () => {
    setError("");
    try {
      const [templateList, documentList] = await Promise.all([
        api.clinicalTemplates(session),
        api.clinicalDocuments(session, encounter.id),
      ]);
      setTemplates(templateList.filter((template) => template.active));
      setTemplateId((current) => current || templateList.find((template) => template.active)?.id || "");
      setDocuments(documentList);
      setSelectedId((current) => current && documentList.some((item) => item.id === current)
        ? current
        : documentList[0]?.id);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar documentos");
    }
  }, [encounter.id, session]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    setTitle(selected?.title ?? "");
    setContent(selected?.content ?? "");
  }, [selected]);

  function merge(saved: ClinicalDocument) {
    setDocuments((current) => {
      const next = current.some((document) => document.id === saved.id)
        ? current.map((document) => document.id === saved.id ? saved : document)
        : [saved, ...current];
      return next.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    });
    setSelectedId(saved.id);
  }

  async function generate() {
    if (!templateId) return;
    setBusy("generate");
    setError("");
    try {
      merge(await api.createClinicalDocument(session, encounter.id, templateId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao gerar documento");
    } finally {
      setBusy("");
    }
  }

  async function saveDraft() {
    if (!selected) return;
    setBusy("save");
    setError("");
    try {
      merge(await api.updateClinicalDocument(session, selected.id, { title, content }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar documento");
    } finally {
      setBusy("");
    }
  }

  async function finalizeDocument() {
    if (!selected || !window.confirm("Finalizar este documento? Depois disso, o conteúdo ficará imutável.")) return;
    setBusy("finalize");
    setError("");
    try {
      let current = selected;
      if (title !== selected.title || content !== selected.content) {
        current = await api.updateClinicalDocument(session, selected.id, { title, content });
      }
      merge(await api.finalizeClinicalDocument(session, current.id));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao finalizar documento");
    } finally {
      setBusy("");
    }
  }

  async function revise() {
    if (!selected) return;
    setBusy("revise");
    setError("");
    try {
      merge(await api.reviseClinicalDocument(session, selected.id));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao criar revisão");
    } finally {
      setBusy("");
    }
  }

  async function output(mode: "download" | "print") {
    if (!selected) return;
    setBusy(mode);
    setError("");
    try {
      if (selected.status === "SIGNED") {
        const blob = await api.signedClinicalDocumentPdf(session, selected.id);
        const url = URL.createObjectURL(blob);
        if (mode === "download") {
          const link = document.createElement("a");
          link.href = url;
          link.download = `${selected.title.replace(/[^a-zA-Z0-9À-ÿ]+/g, "-").toLowerCase()}-assinado.pdf`;
          link.click();
          window.setTimeout(() => URL.revokeObjectURL(url), 1_000);
        } else {
          const target = window.open(url, "_blank");
          if (!target) throw new Error("Permita pop-ups para imprimir o PDF assinado");
          target.opener = null;
          window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
        }
      } else {
        await outputClinicalDocumentPdf(session, selected, mode);
      }
    } catch {
      setError("Não foi possível gerar o PDF deste documento");
    } finally {
      setBusy("");
    }
  }

  return (
    <section className="clinical-documents">
      <div className="clinical-section-heading compact">
        <div>
          <h2>Documentos do atendimento</h2>
          <p>Gere, revise, finalize e imprima sem redigitar dados do paciente.</p>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="clinical-document-create">
        <label>
          Modelo
          <select value={templateId} onChange={(event) => setTemplateId(event.target.value)}>
            {templates.length === 0 && <option value="">Nenhum modelo ativo</option>}
            {templates.map((template) => (
              <option key={template.id} value={template.id}>
                {template.favorite ? "★ " : ""}{template.name}
              </option>
            ))}
          </select>
        </label>
        <button className="secondary-button" disabled={!templateId || Boolean(busy)} onClick={() => void generate()}>
          <FilePlus2 size={17} />{busy === "generate" ? "Gerando..." : "Gerar documento"}
        </button>
      </div>

      <div className="clinical-document-layout">
        <aside className="clinical-document-list">
          {documents.map((document) => (
            <button
              key={document.id}
              className={document.id === selectedId ? "active" : ""}
              onClick={() => setSelectedId(document.id)}
            >
              <Files size={17} />
              <span>
                <strong>{document.title}</strong>
                <small>
                  {documentTypeLabel[document.type]} · revisão {document.revisionNumber}
                </small>
              </span>
              <i className={document.status === "SIGNED" ? "signed" : document.status === "FINALIZED" ? "finalized" : "draft"}>
                {document.status === "SIGNED" ? "Assinado" : document.status === "FINALIZED" ? "Finalizado" : "Rascunho"}
              </i>
            </button>
          ))}
          {documents.length === 0 && <Empty text="Nenhum documento gerado." />}
        </aside>

        <article className="clinical-document-editor">
          {!selected ? <Empty text="Gere ou selecione um documento para editar." /> : (
            <>
              <label>
                Título
                <input
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  disabled={selected.status !== "DRAFT"}
                />
              </label>
              <label>
                Conteúdo
                <textarea
                  rows={17}
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                  disabled={selected.status !== "DRAFT"}
                />
              </label>
              {selected.status !== "DRAFT" && (
                <div className="clinical-integrity-note">
                  {selected.status === "SIGNED" ? <ShieldCheck size={18} /> : <CheckCircle2 size={18} />}
                  <span>
                    <strong>{selected.status === "SIGNED" ? "PDF assinado digitalmente" : "Conteúdo finalizado e imutável"}</strong>
                    <small>
                      {selected.status === "SIGNED"
                        ? `PDF SHA-256: ${selected.signedPdfHash ?? "indisponível"}. O QR Code permite consultar a verificação pública.`
                        : `Conteúdo SHA-256: ${selected.documentHash ?? "indisponível"}. Assine para gerar o PDF PAdES final.`}
                    </small>
                  </span>
                </div>
              )}
              <div className="clinical-editor-actions">
                {selected.status === "DRAFT" ? (
                  <>
                    <button className="secondary-button" disabled={Boolean(busy)} onClick={() => void saveDraft()}>
                      <Save size={16} />Salvar rascunho
                    </button>
                    <button className="primary-button" disabled={!canFinalize || Boolean(busy)} onClick={() => void finalizeDocument()}>
                      <CheckCircle2 size={16} />Finalizar
                    </button>
                  </>
                ) : (
                  <>
                    {selected.status === "FINALIZED" && <button className="primary-button" disabled={!canFinalize || Boolean(busy)} onClick={() => setSigning(true)}>
                      <ShieldCheck size={16} />Assinar digitalmente
                    </button>}
                    <button className="secondary-button" disabled={Boolean(busy)} onClick={() => void revise()}>
                      <RefreshCcw size={16} />Criar revisão
                    </button>
                  </>
                )}
                <button className="secondary-button" disabled={Boolean(busy)} onClick={() => void output("download")}>
                  <Download size={16} />PDF
                </button>
                <button className="secondary-button" disabled={Boolean(busy)} onClick={() => void output("print")}>
                  <Printer size={16} />Imprimir
                </button>
              </div>
              {!canFinalize && selected.status === "DRAFT" && (
                <small className="clinical-permission-hint">
                  A finalização exige que o usuário esteja vinculado ao profissional responsável.
                </small>
              )}
            </>
          )}
        </article>
      </div>
      {signing && selected && <SignatureModal
        session={session}
        document={selected}
        onClose={() => setSigning(false)}
        onSigned={() => {
          setSigning(false);
          void load();
        }}
      />}
    </section>
  );
}
