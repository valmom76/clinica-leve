import { useEffect, useState } from "react";
import { api, loadSession, saveSession, SESSION_EXPIRED_EVENT } from "./api";
import { Portal } from "./app/Portal";
import { LoginPage } from "./auth/LoginPage";
import { ResetPasswordPage } from "./auth/ResetPasswordPage";
import { DocumentVerificationPage } from "./features/clinical/DocumentVerificationPage";
import type { Session } from "./types";
import { applyClinicTheme } from "./utils/clinicThemes";

export default function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession());
  const verificationCode = window.location.pathname.match(/^\/verify\/([^/]+)\/?$/)?.[1];
  const resetPassword = window.location.pathname === "/reset-password";

  useEffect(() => {
    if (session) applyClinicTheme(session.clinic.themeKey);
  }, [session]);

  useEffect(() => {
    const expire = () => setSession(null);
    window.addEventListener(SESSION_EXPIRED_EVENT, expire);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expire);
  }, []);

  if (verificationCode) {
    return <DocumentVerificationPage code={decodeURIComponent(verificationCode)} />;
  }

  if (resetPassword) {
    return <ResetPasswordPage />;
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
        void api.logoutAll(session).catch(() => undefined);
        saveSession(null);
        setSession(null);
      }}
    />
  );
}
