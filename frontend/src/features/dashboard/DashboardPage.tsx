import {
  AlertTriangle,
  CalendarDays,
  Stethoscope,
  UsersRound,
} from "lucide-react";
import type { ViewKey } from "../../app/navigation";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { PageTitle } from "../../components/ui/PageTitle";
import type { Appointment, Patient, Professional } from "../../types";
import { statusLabel } from "../../utils/appointments";

type DashboardPageProps = {
  patients: Patient[];
  professionals: Professional[];
  appointments: Appointment[];
  onNewAppointment?: () => void;
  onNavigate: (view: ViewKey) => void;
};

export function DashboardPage({
  patients,
  professionals,
  appointments,
  onNewAppointment,
  onNavigate,
}: DashboardPageProps) {
  const upcoming = appointments
    .filter((appointment) => new Date(appointment.endAt) >= new Date())
    .slice(0, 4);

  return (
    <>
      <PageTitle
        eyebrow="OPERAÇÃO DA CLÍNICA"
        title="Visão geral"
        description="Acompanhe os dados persistidos da empresa conectada."
        action={onNewAppointment ? (
          <button className="primary-button" onClick={onNewAppointment}>
            <CalendarDays size={17} />Novo agendamento
          </button>
        ) : undefined}
      />

      <section className="kpis">
        <Kpi
          icon={CalendarDays}
          label="Próximos atendimentos"
          value={String(upcoming.length)}
          tone="sage"
        />
        <Kpi
          icon={UsersRound}
          label="Pacientes ativos"
          value={String(patients.length)}
          tone="blue"
        />
        <Kpi
          icon={Stethoscope}
          label="Profissionais"
          value={String(professionals.length)}
          tone="sage"
        />
        <Kpi
          icon={AlertTriangle}
          label="Pendências operacionais"
          value="0"
          tone="terracotta"
        />
      </section>

      <section className="dashboard-grid">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">AGENDA</span>
              <h2>Próximos atendimentos</h2>
            </div>
            {onNewAppointment && (
              <button className="text-button" onClick={() => onNavigate("agenda")}>
                Abrir calendário
              </button>
            )}
          </div>
          <div className="appointment-list">
            {upcoming.length === 0 && (
              <Empty text="Nenhum atendimento no período." />
            )}
            {upcoming.map((appointment) => (
              <div className="appointment-item" key={appointment.id}>
                <time>
                  {new Date(appointment.startAt).toLocaleTimeString("pt-BR", {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </time>
                <i style={{ background: appointment.color }} />
                <div>
                  <strong>{appointment.patientName}</strong>
                  <span>
                    {appointment.professionalName} · {appointment.specialtyName}
                  </span>
                </div>
                <small className={`status ${appointment.status.toLowerCase()}`}>
                  {statusLabel[appointment.status]}
                </small>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">EQUIPE</span>
              <h2>Profissionais ativos</h2>
            </div>
            <button
              className="text-button"
              onClick={() => onNavigate("professionals")}
            >
              Ver equipe
            </button>
          </div>
          <div className="professional-mini-list">
            {professionals.slice(0, 5).map((professional) => (
              <div key={professional.id}>
                <span style={{ background: professional.specialtyColor }}>
                  {professional.name
                    .split(" ")
                    .map((part) => part[0])
                    .slice(0, 2)
                    .join("")}
                </span>
                <div>
                  <strong>{professional.name}</strong>
                  <small>{professional.specialtyName}</small>
                </div>
              </div>
            ))}
          </div>
        </article>
      </section>
    </>
  );
}
