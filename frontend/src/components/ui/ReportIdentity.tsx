import { HeartPulse } from "lucide-react";
import type { Session } from "../../types";

export function ReportIdentity({ session, title, description }: {
  session: Session;
  title: string;
  description: string;
}) {
  return <div className="report-identity">
    <span className={session.clinic.logoUrl ? "has-logo" : ""}>
      {session.clinic.logoUrl ? <img src={session.clinic.logoUrl} alt={`Logomarca ${session.clinic.name}`} /> : <HeartPulse size={23} />}
    </span>
    <div><small>{session.clinic.name}</small><h2>{title}</h2><p>{description}</p></div>
  </div>;
}
