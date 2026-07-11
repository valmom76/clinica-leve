import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import type {
  Appointment,
  Patient,
  Professional,
  Session,
  Specialty,
} from "../types";
import { appointmentDateRange } from "../utils/dates";
import { canAccessClinicalData } from "../app/navigation";

export function useClinicData(session: Session) {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [professionals, setProfessionals] = useState<Professional[]>([]);
  const [specialties, setSpecialties] = useState<Specialty[]>([]);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    const range = appointmentDateRange();
    const clinicalAccess = canAccessClinicalData(session.user.role);

    try {
      const [patientList, professionalList, specialtyList, appointmentList] =
        await Promise.all([
          clinicalAccess ? api.patients(session) : Promise.resolve([]),
          api.professionals(session),
          api.specialties(session),
          clinicalAccess
            ? api.appointments(session, range.from, range.to)
            : Promise.resolve([]),
        ]);
      setPatients(patientList);
      setProfessionals(professionalList);
      setSpecialties(specialtyList);
      setAppointments(appointmentList);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar dados");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  function addPatient(patient: Patient) {
    setPatients((current) =>
      [...current, patient].sort((a, b) => a.name.localeCompare(b.name)),
    );
  }

  function addAppointment(appointment: Appointment) {
    setAppointments((current) =>
      [...current, appointment].sort((a, b) =>
        a.startAt.localeCompare(b.startAt),
      ),
    );
  }

  return {
    patients,
    professionals,
    specialties,
    appointments,
    loading,
    error,
    refresh,
    addPatient,
    addAppointment,
  };
}
