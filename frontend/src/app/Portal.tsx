import { lazy, Suspense, useState } from "react";
import { Sidebar } from "../components/layout/Sidebar";
import { Topbar } from "../components/layout/Topbar";
import { AppointmentModal } from "../features/agenda/AppointmentModal";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { PatientModal } from "../features/patients/PatientModal";
import { PatientsPage } from "../features/patients/PatientsPage";
import { ProfessionalsPage } from "../features/professionals/ProfessionalsPage";
import { UsersPage } from "../features/users/UsersPage";
import { useClinicData } from "../hooks/useClinicData";
import type { Appointment, ClinicBranding, Patient, Professional, Session } from "../types";
import { canAccessClinicalData, type ViewKey } from "./navigation";

type PortalProps = {
  session: Session;
  onSessionChange: (session: Session) => void;
  onLogout: () => void;
};

const AgendaPage = lazy(() => import("../features/agenda/AgendaPage").then((module) => ({ default: module.AgendaPage })));
const InventoryPage = lazy(() => import("../features/inventory/InventoryPage").then((module) => ({ default: module.InventoryPage })));
const FinancePage = lazy(() => import("../features/finance/FinancePage").then((module) => ({ default: module.FinancePage })));
const TimeClockPage = lazy(() => import("../features/timeclock/TimeClockPage").then((module) => ({ default: module.TimeClockPage })));
const ManagementDashboardPage = lazy(() => import("../features/reports/ReportsPage").then((module) => ({ default: module.ManagementDashboardPage })));
const ClinicSettingsPage = lazy(() => import("../features/settings/ClinicSettingsPage").then((module) => ({ default: module.ClinicSettingsPage })));
const BillingPage = lazy(() => import("../features/billing/BillingPage").then((module) => ({ default: module.BillingPage })));
const ClinicalPage = lazy(() => import("../features/clinical/ClinicalPage").then((module) => ({ default: module.ClinicalPage })));

export function Portal({ session, onSessionChange, onLogout }: PortalProps) {
  const [view, setView] = useState<ViewKey>(() =>
    new URLSearchParams(window.location.search).has("billing") ? "billing" : "dashboard",
  );
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [patientModal, setPatientModal] = useState<Patient | "NEW" | null>(null);
  const [appointmentModal, setAppointmentModal] = useState<Appointment | "NEW" | null>(null);
  const [newUserProfessional, setNewUserProfessional] = useState<Professional>();
  const [toast, setToast] = useState("");
  const clinicData = useClinicData(session);
  const clinicalAccess = canAccessClinicalData(session.user.role);
  const managementAccess = ["ADMIN", "MANAGER"].includes(session.user.role);

  function navigate(nextView: ViewKey) {
    setView(nextView);
    setSidebarOpen(false);
  }

  function notify(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(""), 2600);
  }

  function updateBranding(branding: ClinicBranding) {
    onSessionChange({
      ...session,
      clinic: {
        ...session.clinic,
        name: branding.clinicName,
        slug: branding.clinicSlug,
        logoUrl: branding.logoUrl,
        themeKey: branding.themeKey,
      },
    });
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
                managementAccess ? <Suspense fallback={<div className="loading-state"><span /><p>Carregando dashboard...</p></div>}><ManagementDashboardPage session={session} /></Suspense> : <DashboardPage
                  patients={clinicData.patients}
                  professionals={clinicData.professionals}
                  appointments={clinicData.appointments}
                  onNewAppointment={
                    clinicalAccess ? () => setAppointmentModal("NEW") : undefined
                  }
                  onNavigate={navigate}
                />
              )}
              {view === "agenda" && (
                <Suspense fallback={<div className="loading-state"><span /><p>Carregando agenda...</p></div>}>
                  <AgendaPage
                    session={session}
                    appointments={clinicData.appointments}
                    professionals={clinicData.professionals}
                    specialties={clinicData.specialties}
                    onNewAppointment={() => setAppointmentModal("NEW")}
                    onAppointmentSelect={setAppointmentModal}
                    onAppointmentChanged={clinicData.upsertAppointment}
                    onNotify={notify}
                  />
                </Suspense>
              )}
              {view === "patients" && (
                <PatientsPage
                  patients={clinicData.patients}
                  onNewPatient={() => setPatientModal("NEW")}
                  onPatientSelect={setPatientModal}
                />
              )}
              {view === "professionals" && (
                <ProfessionalsPage
                  session={session}
                  professionals={clinicData.professionals}
                  specialties={clinicData.specialties}
                  onProfessionalChanged={clinicData.upsertProfessional}
                  onSpecialtyCreated={clinicData.addSpecialty}
                  onCreateAccess={session.user.role === "ADMIN" ? (professional) => {
                    setNewUserProfessional(professional);
                    navigate("users");
                  } : undefined}
                />
              )}
              {view === "clinical" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando central clínica...</p></div>}><ClinicalPage session={session} appointments={clinicData.appointments} /></Suspense>}
              {view === "inventory" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando estoque...</p></div>}><InventoryPage session={session} /></Suspense>}
              {view === "finance" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando financeiro...</p></div>}><FinancePage session={session} /></Suspense>}
              {view === "time" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando controle de ponto...</p></div>}><TimeClockPage session={session} /></Suspense>}
              {view === "users" && <UsersPage
                session={session}
                newProfessional={newUserProfessional}
                onNewProfessionalHandled={() => setNewUserProfessional(undefined)}
              />}
              {view === "billing" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando assinatura...</p></div>}><BillingPage session={session} /></Suspense>}
              {view === "settings" && <Suspense fallback={<div className="loading-state"><span /><p>Carregando personalização...</p></div>}><ClinicSettingsPage session={session} onBrandingChange={updateBranding} /></Suspense>}
            </>
          )}
        </main>
      </div>

      {patientModal && (
        <PatientModal
          session={session}
          patient={patientModal === "NEW" ? undefined : patientModal}
          onClose={() => setPatientModal(null)}
          onSaved={(patient) => {
            const created = patientModal === "NEW";
            clinicData.upsertPatient(patient);
            setPatientModal(null);
            notify(created ? "Paciente cadastrado com sucesso." : "Paciente atualizado.");
          }}
        />
      )}

      {appointmentModal && (
        <AppointmentModal
          session={session}
          appointment={appointmentModal === "NEW" ? undefined : appointmentModal}
          patients={clinicData.patients}
          professionals={clinicData.professionals}
          specialties={clinicData.specialties}
          onClose={() => setAppointmentModal(null)}
          onSaved={(appointment) => {
            const created = appointmentModal === "NEW";
            clinicData.upsertAppointment(appointment);
            setAppointmentModal(null);
            notify(created ? "Agendamento criado com sucesso." : appointment.status === "CANCELLED" ? "Agendamento cancelado." : "Agendamento atualizado.");
          }}
        />
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
