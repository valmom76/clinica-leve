import { useCallback, useEffect, useMemo, useState } from "react";
import { FilePlus2, Pencil, Search, Star } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import type { ClinicalPlaceholder, ClinicalTemplate, Session } from "../../types";
import { documentTypeLabel } from "./clinicalLabels";
import { TemplateModal } from "./TemplateModal";

export function TemplateLibrary({ session }: { session: Session }) {
  const [templates, setTemplates] = useState<ClinicalTemplate[]>([]);
  const [placeholders, setPlaceholders] = useState<ClinicalPlaceholder[]>([]);
  const [selected, setSelected] = useState<ClinicalTemplate | "new" | null>(null);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [templateList, placeholderList] = await Promise.all([
        api.clinicalTemplates(session),
        api.clinicalPlaceholders(session),
      ]);
      setTemplates(templateList);
      setPlaceholders(placeholderList);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar os modelos");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = useMemo(() => {
    const term = search.toLowerCase();
    return templates.filter((template) =>
      [template.name, documentTypeLabel[template.type]].some((value) =>
        value.toLowerCase().includes(term),
      ),
    );
  }, [search, templates]);

  function save(saved: ClinicalTemplate) {
    setTemplates((current) => {
      const next = current.some((item) => item.id === saved.id)
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [...current, saved];
      return next.sort((a, b) => Number(b.favorite) - Number(a.favorite) || a.name.localeCompare(b.name));
    });
    setSelected(null);
  }

  return (
    <>
      <div className="clinical-section-heading">
        <div>
          <h2>Biblioteca de modelos</h2>
          <p>Modelos são versionados; documentos já emitidos preservam o conteúdo original.</p>
        </div>
        <button className="primary-button" onClick={() => setSelected("new")}>
          <FilePlus2 size={17} />Novo modelo
        </button>
      </div>

      {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}

      <article className="panel clinical-template-panel">
        <div className="table-toolbar">
          <div className="field-search">
            <Search size={16} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar modelo" />
          </div>
          <span>{visible.length} modelo(s)</span>
        </div>
        <div className="clinical-template-grid">
          {!loading && visible.map((template) => (
            <button
              key={template.id}
              className={`clinical-template-card ${template.active ? "" : "inactive"}`}
              onClick={() => setSelected(template)}
            >
              <span>{template.favorite ? <Star size={16} fill="currentColor" /> : <Pencil size={16} />}</span>
              <div>
                <strong>{template.name}</strong>
                <small>{documentTypeLabel[template.type]} · versão {template.versionNumber}</small>
                <p>{template.active ? template.titleTemplate : "Modelo inativo"}</p>
              </div>
            </button>
          ))}
        </div>
        {loading && <Empty text="Carregando modelos..." />}
        {!loading && visible.length === 0 && <Empty text="Nenhum modelo encontrado." />}
      </article>

      {selected && (
        <TemplateModal
          session={session}
          template={selected === "new" ? undefined : selected}
          placeholders={placeholders}
          onClose={() => setSelected(null)}
          onSaved={save}
        />
      )}
    </>
  );
}
