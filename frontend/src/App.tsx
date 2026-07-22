import { useEffect, useState } from "react";
import { loadSession, saveSession } from "./api";
import { Portal } from "./app/Portal";
import { LoginPage } from "./auth/LoginPage";
import { DocumentVerificationPage } from "./features/clinical/DocumentVerificationPage";
import type { Session } from "./types";
import { applyClinicTheme } from "./utils/clinicThemes";

export default function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession());
  const verificationCode = window.location.pathname.match(/^\/verify\/([^/]+)\/?$/)?.[1];

  useEffect(() => {
    if (session) applyClinicTheme(session.clinic.themeKey);
  }, [session]);

  if (verificationCode) {
    return <DocumentVerificationPage code={decodeURIComponent(verificationCode)} />;
  }

  if (!session) {
    return (
      <LoginPage
        onAuthenticated={(authenticatedSession) => {
          saveSession(authenticatedSession);
          setSession(authenticatedSession);
        }}
      />
    );
  }

  return (
    <Portal
      session={session}
      onSessionChange={(nextSession) => {
        saveSession(nextSession);
        setSession(nextSession);
      }}
      onLogout={() => {
        saveSession(null);
        setSession(null);
      }}
    />
  );
}
