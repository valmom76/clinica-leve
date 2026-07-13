import { lazy, Suspense, useState } from "react";
import { Sidebar } from "../components/layout/Sidebar";
import { Topbar } from "../components/layout/Topbar";
import { AppointmentModal } from "../features/agenda/AppointmentModal";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { PatientModal } from "../features/patients/PatientModal";
import { PatientsPage } from "../features/patients/PatientsPage";
import { ProfessionalsPage } from "../features/professionals/ProfessionalsPage";
import { FutureModulePage } from "../features/shared/FutureModulePage";
import { UsersPage } from "../features/users/UsersPage";
import { useClinicData } from "../hooks/useClinicData";
import type { Session } from "../types";
import { canAccessClinicalData, type ViewKey } from "./navigation";

type PortalProps = {
  session: Session;
  onLogout: () => void;
};

const futureViews: ViewKey[] = [];
const AgendaPage = lazy(() => import("../features/agenda/AgendaPage").then((module) => ({ default: module.AgendaPage })));
const InventoryPage = lazy(() => import("../features/inventory/InventoryPage").then((module) => ({ default: module.InventoryPage })));
const FinancePage = lazy(() => import("../features/finance/FinancePage").then((module) => ({ default: module.FinancePage })));
const TimeClockPage = lazy(() => import("../features/timeclock/TimeClockPage").then((module) => ({ default: module.TimeClockPage })));
const ReportsPage = lazy(() => import("../features/reports/ReportsPage").then((module) => ({ default: module.ReportsPage })));

export function Portal({ session, onLogout }: PortalProps) {
  const [view, setView] = useState<ViewKey>("dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [patientModal, setPatientModal] = useState(false);
  const [appointmentModal, setAppointmentModal] = useState(false);
  const [toast, setToast] = useState("");
  const clinicData = useClinicData(session);
  const clinicalAccess = canAccessClinicalData(session.user.role);

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
                  onNewAppointment={
                    clinicalAccess ? () => setAppointmentModal(true) : undefined
                  }
                  onNavigate={navigate}
                />
              )}
              {view === "agenda" && (
                <Suspense fallback={<div className="loading-state"><span /><p>Carregando agenda...</p></div>}>
                  <AgendaPage
                    appointments={clinicData.appointments}
                    onNewAppointment={() => setAppointmentModal(true)}
                  />
                </Suspense>
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
              {view === "inventory" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando estoque...</p></div>}><InventoryPage session={session} /></Suspense>}
              {view === "finance" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando financeiro...</p></div>}><FinancePage session={session} /></Suspense>}
              {view === "time" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando controle de ponto...</p></div>}><TimeClockPage session={session} /></Suspense>}
              {view === "reports" && <Suspense fallback={<div className="loading-state"><span /><p>Gerando relatórios...</p></div>}><ReportsPage session={session} /></Suspense>}
              {view === "users" && <UsersPage session={session} />}
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
