import { useState } from "react";
import { Sidebar } from "../components/layout/Sidebar";
import { Topbar } from "../components/layout/Topbar";
import { AgendaPage } from "../features/agenda/AgendaPage";
import { AppointmentModal } from "../features/agenda/AppointmentModal";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { PatientModal } from "../features/patients/PatientModal";
import { PatientsPage } from "../features/patients/PatientsPage";
import { ProfessionalsPage } from "../features/professionals/ProfessionalsPage";
import { FutureModulePage } from "../features/shared/FutureModulePage";
import { useClinicData } from "../hooks/useClinicData";
import type { Session } from "../types";
import type { ViewKey } from "./navigation";

type PortalProps = {
  session: Session;
  onLogout: () => void;
};

const futureViews: ViewKey[] = ["inventory", "finance", "time", "reports"];

export function Portal({ session, onLogout }: PortalProps) {
  const [view, setView] = useState<ViewKey>("dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [patientModal, setPatientModal] = useState(false);
  const [appointmentModal, setAppointmentModal] = useState(false);
  const [toast, setToast] = useState("");
  const clinicData = useClinicData(session);

  function navigate(nextView: ViewKey) {
    setView(nextView);
    setSidebarOpen(false);
  }

  function notify(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(""), 2600);
  }

  return (
    <div className="portal">
      <Sidebar
        session={session}
        view={view}
        open={sidebarOpen}
        onNavigate={navigate}
        onClose={() => setSidebarOpen(false)}
        onLogout={onLogout}
      />

      {sidebarOpen && (
        <button
          className="backdrop"
          aria-label="Fechar menu"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <div className="workspace">
        <Topbar
          clinicName={session.clinic.name}
          onOpenSidebar={() => setSidebarOpen(true)}
        />

        <main className="content">
          {clinicData.error && (
            <div className="page-error">
              {clinicData.error}
              <button onClick={() => void clinicData.refresh()}>
                Tentar novamente
              </button>
            </div>
          )}

          {clinicData.loading ? (
            <div className="loading-state">
              <span /><p>Carregando dados da clínica...</p>
            </div>
          ) : (
            <>
              {view === "dashboard" && (
                <DashboardPage
                  patients={clinicData.patients}
                  professionals={clinicData.professionals}
                  appointments={clinicData.appointments}
                  onNewAppointment={() => setAppointmentModal(true)}
                  onNavigate={navigate}
                />
              )}
              {view === "agenda" && (
                <AgendaPage
                  appointments={clinicData.appointments}
                  onNewAppointment={() => setAppointmentModal(true)}
                />
              )}
              {view === "patients" && (
                <PatientsPage
                  patients={clinicData.patients}
                  onNewPatient={() => setPatientModal(true)}
                />
              )}
              {view === "professionals" && (
                <ProfessionalsPage
                  professionals={clinicData.professionals}
                />
              )}
              {futureViews.includes(view) && (
                <FutureModulePage view={view} />
              )}
            </>
          )}
        </main>
      </div>

      {patientModal && (
        <PatientModal
          session={session}
          onClose={() => setPatientModal(false)}
          onCreated={(patient) => {
            clinicData.addPatient(patient);
            setPatientModal(false);
            notify("Paciente cadastrado com sucesso.");
          }}
        />
      )}

      {appointmentModal && (
        <AppointmentModal
          session={session}
          patients={clinicData.patients}
          professionals={clinicData.professionals}
          specialties={clinicData.specialties}
          onClose={() => setAppointmentModal(false)}
          onCreated={(appointment) => {
            clinicData.addAppointment(appointment);
            setAppointmentModal(false);
            notify("Agendamento criado com sucesso.");
          }}
        />
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
