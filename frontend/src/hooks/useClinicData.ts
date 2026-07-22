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

  useEffect(() => {
    if (!canAccessClinicalData(session.user.role)) return;
    const timer = window.setInterval(() => {
      const range = appointmentDateRange();
      api.appointments(session, range.from, range.to).then(setAppointments).catch(() => undefined);
    }, 30_000);
    return () => window.clearInterval(timer);
  }, [session]);

  function addPatient(patient: Patient) {
    setPatients((current) =>
      [...current, patient].sort((a, b) => a.name.localeCompare(b.name)),
    );
  }

  function upsertPatient(patient: Patient) {
    setPatients((current) => {
      const next = current.some((item) => item.id === patient.id)
        ? current.map((item) => item.id === patient.id ? patient : item)
        : [...current, patient];
      return next.sort((a, b) => a.name.localeCompare(b.name));
    });
  }

  function addAppointment(appointment: Appointment) {
    setAppointments((current) =>
      [...current, appointment].sort((a, b) =>
        a.startAt.localeCompare(b.startAt),
      ),
    );
  }

  function upsertAppointment(appointment: Appointment) {
    setAppointments((current) => {
      const next = current.some((item) => item.id === appointment.id)
        ? current.map((item) => item.id === appointment.id ? appointment : item)
        : [...current, appointment];
      return next.sort((a, b) => a.startAt.localeCompare(b.startAt));
    });
  }

  function addSpecialty(specialty: Specialty) {
    setSpecialties((current) => {
      const next = current.some((item) => item.id === specialty.id)
        ? current.map((item) => item.id === specialty.id ? specialty : item)
        : [...current, specialty];
      return next.filter((item) => item.active).sort((a, b) => a.name.localeCompare(b.name));
    });
  }

  function upsertProfessional(professional: Professional) {
    setProfessionals((current) => {
      const withoutCurrent = current.filter((item) => item.id !== professional.id);
      return professional.active
        ? [...withoutCurrent, professional].sort((a, b) => a.name.localeCompare(b.name))
        : withoutCurrent;
    });
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
    upsertPatient,
    addAppointment,
    upsertAppointment,
    addSpecialty,
    upsertProfessional,
  };
}
