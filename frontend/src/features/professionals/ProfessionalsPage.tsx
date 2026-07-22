import { useCallback, useEffect, useMemo, useState } from "react";
import { Pencil, Plus, Search, UserRoundCheck, UserRoundX } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { PageTitle } from "../../components/ui/PageTitle";
import type { Professional, Session, Specialty } from "../../types";
import { ProfessionalModal } from "./ProfessionalModal";

type ProfessionalsPageProps = {
  session: Session;
  professionals: Professional[];
  specialties: Specialty[];
  onProfessionalChanged: (professional: Professional) => void;
  onSpecialtyCreated: (specialty: Specialty) => void;
  onCreateAccess?: (professional: Professional) => void;
};

export function ProfessionalsPage({
  session,
  professionals: activeProfessionals,
  specialties,
  onProfessionalChanged,
  onSpecialtyCreated,
  onCreateAccess,
}: ProfessionalsPageProps) {
  const canManage = session.user.role === "ADMIN" || session.user.role === "MANAGER";
  const [professionals, setProfessionals] = useState(activeProfessionals);
  const [availableSpecialties, setAvailableSpecialties] = useState(specialties);
  const [selected, setSelected] = useState<Professional | "new" | null>(null);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"ACTIVE" | "ALL">("ACTIVE");
  const [loading, setLoading] = useState(canManage);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = useCallback(async () => {
    if (!canManage) return;
    setLoading(true);
    setError("");
    try {
      setProfessionals(await api.managedProfessionals(session));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar profissionais");
    } finally {
      setLoading(false);
    }
  }, [canManage, session]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (!canManage) setProfessionals(activeProfessionals);
  }, [activeProfessionals, canManage]);
  useEffect(() => setAvailableSpecialties(specialties), [specialties]);

  const visible = useMemo(() => professionals.filter((professional) => {
    if (status === "ACTIVE" && !professional.active) return false;
    const term = search.trim().toLowerCase();
    return !term || [professional.name, professional.specialtyName, professional.council ?? "", professional.email ?? ""]
      .some((value) => value.toLowerCase().includes(term));
  }), [professionals, search, status]);

  function acceptSpecialty(specialty: Specialty) {
    setAvailableSpecialties((current) => [...current.filter((item) => item.id !== specialty.id), specialty]
      .sort((a, b) => a.name.localeCompare(b.name)));
    onSpecialtyCreated(specialty);
  }

  function acceptProfessional(saved: Professional, createAccess: boolean) {
    setProfessionals((current) => [...current.filter((item) => item.id !== saved.id), saved]
      .sort((a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name)));
    onProfessionalChanged(saved);
    setSelected(null);
    setSuccess(saved.active ? "Profissional salvo com sucesso." : "Profissional inativado; o histórico foi preservado.");
    if (createAccess) onCreateAccess?.(saved);
  }

  return <>
    <PageTitle
      eyebrow="EQUIPE CLÍNICA"
      title="Profissionais"
      description="Cadastros clínicos, especialidades e vínculos da clínica conectada."
      action={canManage ? <button className="primary-button" onClick={() => setSelected("new")}><Plus size={17} />Novo profissional</button> : undefined}
    />

    {(error || success) && <div className={error ? "page-error" : "form-success"}>{error || success}</div>}

    <article className="panel professional-management-panel">
      <div className="professional-toolbar">
        <div className="field-search"><Search size={16} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar profissional ou especialidade" /></div>
        {canManage && <div className="professional-status-filter">
          <button className={status === "ACTIVE" ? "active" : ""} onClick={() => setStatus("ACTIVE")}>Ativos</button>
          <button className={status === "ALL" ? "active" : ""} onClick={() => setStatus("ALL")}>Todos</button>
        </div>}
        <span>{visible.length} profissional(is)</span>
      </div>

      <section className="professional-grid managed">
        {visible.map((professional) => <article className={`professional-card ${professional.active ? "" : "inactive"}`} key={professional.id}>
          <div className="professional-card-heading">
            <span className="professional-avatar" style={{ background: professional.specialtyColor }}>
              {professional.name.split(" ").map((part) => part[0]).slice(0, 2).join("")}
            </span>
            <small className={`status ${professional.active ? "confirmed" : "cancelled"}`}>{professional.active ? <UserRoundCheck size={13} /> : <UserRoundX size={13} />}{professional.active ? "Ativo" : "Inativo"}</small>
          </div>
          <h2>{professional.name}</h2>
          <p>{professional.specialtyName}</p>
          <small>{professional.council ?? "Conselho não informado"}</small>
          <dl>
            <div><dt>Telefone</dt><dd>{professional.phone ?? "—"}</dd></div>
            <div><dt>E-mail</dt><dd>{professional.email ?? "—"}</dd></div>
          </dl>
          {canManage && <button className="secondary-button professional-edit-button" onClick={() => setSelected(professional)}><Pencil size={15} />Editar cadastro</button>}
        </article>)}
        {!loading && visible.length === 0 && <Empty text="Nenhum profissional encontrado." />}
        {loading && <Empty text="Carregando profissionais..." />}
      </section>
    </article>

    {selected && <ProfessionalModal
      session={session}
      professional={selected === "new" ? undefined : selected}
      specialties={availableSpecialties}
      canCreateAccess={Boolean(onCreateAccess)}
      onClose={() => setSelected(null)}
      onSpecialtyCreated={acceptSpecialty}
      onSaved={acceptProfessional}
    />}
  </>;
}
