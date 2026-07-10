import { useState } from "react";
import { loadSession, saveSession } from "./api";
import { Portal } from "./app/Portal";
import { LoginPage } from "./auth/LoginPage";
import type { Session } from "./types";

export default function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession());

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
      onLogout={() => {
        saveSession(null);
        setSession(null);
      }}
    />
  );
}
