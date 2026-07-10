import { useMemo } from "react";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import ptBrLocale from "@fullcalendar/core/locales/pt-br";
import { Plus } from "lucide-react";
import { PageTitle } from "../../components/ui/PageTitle";
import type { Appointment } from "../../types";

type AgendaPageProps = {
  appointments: Appointment[];
  onNewAppointment: () => void;
};

export function AgendaPage({
  appointments,
  onNewAppointment,
}: AgendaPageProps) {
  const events = useMemo(
    () =>
      appointments.map((appointment) => ({
        id: appointment.id,
        title: `${appointment.patientName} · ${appointment.specialtyName}`,
        start: appointment.startAt,
        end: appointment.endAt,
        backgroundColor: appointment.color,
        borderColor: appointment.color,
      })),
    [appointments],
  );

  return (
    <>
      <PageTitle
        eyebrow="ATENDIMENTOS"
        title="Agenda clínica"
        description="Horários persistidos e isolados para esta clínica."
        action={
          <button className="primary-button" onClick={onNewAppointment}>
            <Plus size={17} />Novo agendamento
          </button>
        }
      />
      <article className="panel calendar-panel">
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="timeGridWeek"
          locale={ptBrLocale}
          headerToolbar={{
            left: "prev,next today",
            center: "title",
            right: "dayGridMonth,timeGridWeek,timeGridDay",
          }}
          buttonText={{
            today: "Hoje",
            month: "Mês",
            week: "Semana",
            day: "Dia",
          }}
          allDaySlot={false}
          slotMinTime="07:00:00"
          slotMaxTime="20:00:00"
          nowIndicator
          height="auto"
          events={events}
        />
      </article>
    </>
  );
}
