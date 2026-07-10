import { PageTitle } from "../../components/ui/PageTitle";
import type { Professional } from "../../types";

export function ProfessionalsPage({
  professionals,
}: {
  professionals: Professional[];
}) {
  return (
    <>
      <PageTitle
        eyebrow="EQUIPE CLÍNICA"
        title="Profissionais"
        description="Especialidades e vínculos da empresa conectada."
      />
      <section className="professional-grid">
        {professionals.map((professional) => (
          <article className="panel professional-card" key={professional.id}>
            <span
              className="professional-avatar"
              style={{ background: professional.specialtyColor }}
            >
              {professional.name
                .split(" ")
                .map((part) => part[0])
                .slice(0, 2)
                .join("")}
            </span>
            <h2>{professional.name}</h2>
            <p>{professional.specialtyName}</p>
            <small>{professional.council ?? "Conselho não informado"}</small>
            <dl>
              <div>
                <dt>Telefone</dt>
                <dd>{professional.phone ?? "—"}</dd>
              </div>
              <div>
                <dt>E-mail</dt>
                <dd>{professional.email ?? "—"}</dd>
              </div>
            </dl>
          </article>
        ))}
      </section>
    </>
  );
}
