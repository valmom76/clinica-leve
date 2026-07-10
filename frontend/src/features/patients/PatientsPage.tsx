import { useState } from "react";
import { Plus, Search } from "lucide-react";
import { PageTitle } from "../../components/ui/PageTitle";
import type { Patient } from "../../types";

type PatientsPageProps = {
  patients: Patient[];
  onNewPatient: () => void;
};

export function PatientsPage({
  patients,
  onNewPatient,
}: PatientsPageProps) {
  const [search, setSearch] = useState("");
  const visible = patients.filter((patient) =>
    [patient.name, patient.phone, patient.email ?? ""].some((value) =>
      value.toLowerCase().includes(search.toLowerCase()),
    ),
  );

  return (
    <>
      <PageTitle
        eyebrow="CADASTROS"
        title="Pacientes"
        description="Dados pertencentes exclusivamente à clínica conectada."
        action={
          <button className="primary-button" onClick={onNewPatient}>
            <Plus size={17} />Novo paciente
          </button>
        }
      />
      <article className="panel data-panel">
        <div className="table-toolbar">
          <div className="field-search">
            <Search size={16} />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Buscar paciente"
            />
          </div>
          <span>{visible.length} registro(s)</span>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Paciente</th>
                <th>Telefone</th>
                <th>E-mail</th>
                <th>Nascimento</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((patient) => (
                <tr key={patient.id}>
                  <td>
                    <div className="person">
                      <span>
                        {patient.name
                          .split(" ")
                          .map((part) => part[0])
                          .slice(0, 2)
                          .join("")}
                      </span>
                      <strong>{patient.name}</strong>
                    </div>
                  </td>
                  <td>{patient.phone}</td>
                  <td>{patient.email ?? "—"}</td>
                  <td>
                    {patient.birthDate
                      ? new Date(
                          `${patient.birthDate}T12:00:00`,
                        ).toLocaleDateString("pt-BR")
                      : "—"}
                  </td>
                  <td><small className="status confirmed">Ativo</small></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>
    </>
  );
}
