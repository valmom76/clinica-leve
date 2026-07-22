import { useMemo, useState } from "react";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import ptBrLocale from "@fullcalendar/core/locales/pt-br";
import { BarChart3, CalendarDays, LoaderCircle, MessageCircle, Move, Plus, RotateCcw } from "lucide-react";
import { api } from "../../api";
import { ModuleTabs } from "../../components/ui/ModuleTabs";
import { PageTitle } from "../../components/ui/PageTitle";
import type { Appointment, Professional, Session, Specialty } from "../../types";
import { AgendaReport } from "./AgendaReport";
import { AgendaMessagingSettings } from "./AgendaMessagingSettings";

type AgendaPageProps = {
  session: Session;
  appointments: Appointment[];
  professionals: Professional[];
  specialties: Specialty[];
  onNewAppointment: () => void;
  onAppointmentSelect: (appointment: Appointment) => void;
  onAppointmentChanged: (appointment: Appointment) => void;
  onNotify: (message: string) => void;
};

type AgendaTab = "CALENDAR" | "REPORT" | "AUTOMATION";
const movableStatuses: Appointment["status"][] = ["SCHEDULED", "CONFIRMED", "RESCHEDULE_REQUESTED"];

export function AgendaPage({
  session,
  appointments,
  professionals,
  specialties,
  onNewAppointment,
  onAppointmentSelect,
  onAppointmentChanged,
  onNotify,
}: AgendaPageProps) {
  const [tab, setTab] = useState<AgendaTab>("CALENDAR");
  const [movingAppointmentId, setMovingAppointmentId] = useState<string>();
  const [moveError, setMoveError] = useState("");
  const managementAccess = ["ADMIN", "MANAGER"].includes(session.user.role);
  const rescheduleRequests = appointments.filter((appointment) => appointment.status === "RESCHEDULE_REQUESTED");
  const events = useMemo(
    () =>
      appointments.map((appointment) => ({
        id: appointment.id,
        title: `${appointment.patientName} · ${appointment.specialtyName}`,
        start: appointment.startAt,
        end: appointment.endAt,
        allDay: false,
        editable: movableStatuses.includes(appointment.status),
        backgroundColor: appointment.status === "CANCELLED" ? "#8b9692" : appointment.status === "RESCHEDULE_REQUESTED" ? "#c97861" : appointment.color,
        borderColor: appointment.status === "CANCELLED" ? "#8b9692" : appointment.status === "RESCHEDULE_REQUESTED" ? "#c97861" : appointment.color,
      })),
    [appointments],
  );

  async function moveAppointment(
    appointment: Appointment,
    start: Date,
    end: Date,
    revert: () => void,
  ) {
    setMovingAppointmentId(appointment.id);
    setMoveError("");
    try {
      const updated = await api.updateAppointment(session, appointment.id, {
        patientId: appointment.patientId,
        professionalId: appointment.professionalId,
        specialtyId: appointment.specialtyId,
        startAt: start.toISOString(),
        endAt: end.toISOString(),
        status: appointment.status,
        notes: appointment.notes,
      });
      onAppointmentChanged(updated);
      onNotify("Horário do agendamento atualizado.");
    } catch (cause) {
      revert();
      setMoveError(cause instanceof Error ? cause.message : "Não foi possível alterar o horário");
    } finally {
      setMovingAppointmentId(undefined);
    }
  }

  return (
    <>
      <PageTitle
        eyebrow="ATENDIMENTOS"
        title="Agenda clínica"
        description="Organize os horários e consulte o desempenho dos atendimentos."
        action={tab === "CALENDAR" ?
          <button className="primary-button" onClick={onNewAppointment}>
            <Plus size={17} />Novo agendamento
          </button>
        : undefined}
      />
      <ModuleTabs<AgendaTab>
        active={tab}
        onChange={setTab}
        items={[
          { key: "CALENDAR", label: "Calendário", icon: CalendarDays },
          { key: "REPORT", label: "Relatório de atendimentos", icon: BarChart3 },
          ...(managementAccess ? [{ key: "AUTOMATION" as const, label: "Automação", icon: MessageCircle }] : []),
        ]}
      />
      {tab === "CALENDAR" ? <>
        {moveError && <div className="page-error agenda-move-error">{moveError}<button onClick={() => setMoveError("")}>Fechar</button></div>}
        {rescheduleRequests.length > 0 && <div className="agenda-reschedule-list">
          <RotateCcw size={19} /><div><strong>{rescheduleRequests.length} solicitação(ões) de reagendamento</strong><span>Abra a consulta para escolher um novo horário.</span></div>
          <div>{rescheduleRequests.slice(0, 3).map((appointment) => <button key={appointment.id} onClick={() => onAppointmentSelect(appointment)}>{appointment.patientName}</button>)}</div>
        </div>}
        <div className="calendar-drag-hint">
          {movingAppointmentId ? <LoaderCircle className="spin" size={18} /> : <Move size={18} />}
          <span>{movingAppointmentId ? "Salvando o novo horário..." : "Clique para editar ou arraste um agendamento para alterar seu horário."}</span>
        </div>
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
          editable={!movingAppointmentId}
          eventStartEditable={!movingAppointmentId}
          eventDurationEditable={false}
          eventDragMinDistance={6}
          events={events}
          eventClick={(info) => {
            const appointment = appointments.find((item) => item.id === info.event.id);
            if (appointment) onAppointmentSelect(appointment);
          }}
          eventDrop={(info) => {
            const appointment = appointments.find((item) => item.id === info.event.id);
            if (!appointment || !info.event.start || !movableStatuses.includes(appointment.status)) {
              info.revert();
              return;
            }
            const originalDuration = new Date(appointment.endAt).getTime() - new Date(appointment.startAt).getTime();
            const end = info.event.end ?? new Date(info.event.start.getTime() + originalDuration);
            void moveAppointment(appointment, info.event.start, end, info.revert);
          }}
        />
      </article></> : tab === "REPORT"
        ? <AgendaReport session={session} professionals={professionals} specialties={specialties} />
        : <AgendaMessagingSettings session={session} />}
    </>
  );
}
