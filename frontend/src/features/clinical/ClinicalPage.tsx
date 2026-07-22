import { useState } from "react";
import { FileStack, ShieldCheck, Stethoscope } from "lucide-react";
import { ModuleTabs } from "../../components/ui/ModuleTabs";
import { PageTitle } from "../../components/ui/PageTitle";
import type { Appointment, Session } from "../../types";
import { EncounterWorkspace } from "./EncounterWorkspace";
import { TemplateLibrary } from "./TemplateLibrary";
import { SignatureCenter } from "./SignatureCenter";

type ClinicalSection = "ENCOUNTERS" | "TEMPLATES" | "SIGNATURES";

export function ClinicalPage({
  session,
  appointments,
}: {
  session: Session;
  appointments: Appointment[];
}) {
  const [section, setSection] = useState<ClinicalSection>("ENCOUNTERS");

  return (
    <>
      <PageTitle
        eyebrow="CENTRAL CLÍNICA"
        title="Atendimentos e documentos"
        description="Prontuário versionado, modelos automáticos e documentos com assinatura digital híbrida."
      />

      <ModuleTabs<ClinicalSection>
        active={section}
        onChange={setSection}
        items={[
          { key: "ENCOUNTERS", label: "Atendimentos", icon: Stethoscope },
          { key: "TEMPLATES", label: "Biblioteca de modelos", icon: FileStack },
          { key: "SIGNATURES", label: "Certificados", icon: ShieldCheck },
        ]}
      />

      {section === "ENCOUNTERS" ? (
        <EncounterWorkspace session={session} appointments={appointments} />
      ) : section === "TEMPLATES" ? (
        <TemplateLibrary session={session} />
      ) : (
        <SignatureCenter session={session} />
      )}
    </>
  );
}
