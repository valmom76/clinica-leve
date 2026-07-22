import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Cloud, HardDrive, KeyRound, Plus, ShieldCheck, Trash2 } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { CscProvider, Session, SignatureCredential } from "../../types";

const date = (value?: string) => value
  ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" }).format(new Date(value))
  : "—";

export function SignatureCenter({ session }: { session: Session }) {
  const [credentials, setCredentials] = useState<SignatureCredential[]>([]);
  const [providers, setProviders] = useState<CscProvider[]>([]);
  const [modal, setModal] = useState<"LOCAL" | "REMOTE" | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = useCallback(async () => {
    setError("");
    try {
      const [saved, available] = await Promise.all([
        api.signatureCredentials(session),
        api.cscProviders(session),
      ]);
      setCredentials(saved);
      setProviders(available);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível carregar os certificados");
    }
  }, [session]);

  useEffect(() => { void load(); }, [load]);

  async function deactivate(credential: SignatureCredential) {
    if (!window.confirm(`Desativar “${credential.displayName}”? Documentos já assinados não serão alterados.`)) return;
    setBusy(true);
    setError("");
    try {
      await api.deactivateSignatureCredential(session, credential.id);
      setSuccess("Credencial desativada.");
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível desativar a credencial");
    } finally {
      setBusy(false);
    }
  }

  return <>
    <section className="panel signature-center">
      <div className="panel-heading">
        <div><h2>Certificados e assinatura digital</h2><p>Use A1 local ou uma credencial remota compatível com o padrão CSC.</p></div>
        <ShieldCheck size={22} />
      </div>
      <div className="signature-toolbar">
        <div>
          <strong>Arquitetura híbrida</strong>
          <small>A senha do A1 e o PIN/OTP remoto são solicitados somente no momento da assinatura.</small>
        </div>
        <button className="secondary-button" onClick={() => setModal("LOCAL")}><HardDrive size={17} />Adicionar A1</button>
        <button className="primary-button" disabled={providers.length === 0} onClick={() => setModal("REMOTE")}><Cloud size={17} />Conectar nuvem</button>
      </div>
      {(error || success) && <div className={error ? "form-error" : "form-success"}>{error || success}</div>}
      <div className="signature-grid">
        {credentials.map((credential) => {
          const expired = new Date(credential.validUntil).getTime() < Date.now();
          return <article className={`signature-card ${!credential.active ? "inactive" : ""}`} key={credential.id}>
            <span>{credential.mode === "LOCAL_PKCS12" ? <HardDrive size={21} /> : <Cloud size={21} />}</span>
            <div>
              <div className="signature-card-title">
                <strong>{credential.displayName}</strong>
                <i className={!credential.active || expired ? "danger" : "active"}>{!credential.active ? "Inativo" : expired ? "Vencido" : "Ativo"}</i>
              </div>
              <small>{credential.mode === "LOCAL_PKCS12" ? "Certificado A1" : credential.providerName || credential.providerKey}</small>
              <p>{credential.subjectName}</p>
              <dl>
                <div><dt>Validade</dt><dd>{date(credential.validUntil)}</dd></div>
                <div><dt>Emissor</dt><dd>{credential.issuerName}</dd></div>
                <div><dt>Uso recente</dt><dd>{date(credential.lastUsedAt)}</dd></div>
              </dl>
            </div>
            {credential.active && <button className="icon-button danger" disabled={busy} title="Desativar" onClick={() => void deactivate(credential)}><Trash2 size={16} /></button>}
          </article>;
        })}
        {credentials.length === 0 && <Empty text="Nenhum certificado cadastrado para este profissional." />}
      </div>
      <div className="signature-security-note"><KeyRound size={19} /><p><strong>Proteção das credenciais</strong><br />O arquivo A1 e os dados de conexão remota ficam criptografados. A senha/PIN nunca é armazenada.</p></div>
    </section>
    {modal === "LOCAL" && <LocalCredentialModal session={session} onClose={() => setModal(null)} onSaved={async () => { setModal(null); setSuccess("Certificado A1 adicionado."); await load(); }} />}
    {modal === "REMOTE" && <RemoteCredentialModal session={session} providers={providers} onClose={() => setModal(null)} onSaved={async () => { setModal(null); setSuccess("Credencial remota conectada."); await load(); }} />}
  </>;
}

function LocalCredentialModal({ session, onClose, onSaved }: { session: Session; onClose: () => void; onSaved: () => Promise<void> }) {
  const [file, setFile] = useState<File>();
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("Meu certificado A1");
  const [confirmed, setConfirmed] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!file) return setError("Selecione o arquivo .pfx ou .p12");
    setSaving(true); setError("");
    try {
      await api.uploadLocalSignatureCredential(session, { file, password, displayName, ownershipConfirmed: confirmed });
      await onSaved();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao cadastrar certificado"); }
    finally { setSaving(false); }
  }
  return <Modal title="Adicionar certificado A1" description="O arquivo será protegido e a senha não será armazenada." onClose={onClose}>
    <form onSubmit={(event) => void submit(event)}>
      {error && <div className="form-error">{error}</div>}
      <div className="form-grid signature-form">
        <label className="full">Nome de identificação<input required maxLength={160} value={displayName} onChange={(event) => setDisplayName(event.target.value)} /></label>
        <label className="full">Arquivo do certificado<input required type="file" accept=".pfx,.p12,application/x-pkcs12" onChange={(event) => setFile(event.target.files?.[0])} /></label>
        <label className="full">Senha do certificado<input required type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <label className="full check-field"><input required type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} /><span>Confirmo que este certificado pertence ao profissional conectado.</span></label>
      </div>
      <ModalActions saving={saving} onClose={onClose} label="Proteger e adicionar" />
    </form>
  </Modal>;
}

function RemoteCredentialModal({ session, providers, onClose, onSaved }: { session: Session; providers: CscProvider[]; onClose: () => void; onSaved: () => Promise<void> }) {
  const [providerKey, setProviderKey] = useState(providers[0]?.key ?? "");
  const [credentialId, setCredentialId] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [displayName, setDisplayName] = useState("Certificado em nuvem");
  const [confirmed, setConfirmed] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: FormEvent) {
    event.preventDefault(); setSaving(true); setError("");
    try {
      await api.connectRemoteSignatureCredential(session, { providerKey, credentialId, accessToken, displayName, ownershipConfirmed: confirmed });
      await onSaved();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao conectar credencial remota"); }
    finally { setSaving(false); }
  }
  return <Modal title="Conectar certificado em nuvem" description="Conector CSC configurado pelo operador da plataforma." onClose={onClose}>
    <form onSubmit={(event) => void submit(event)}>
      {error && <div className="form-error">{error}</div>}
      <div className="form-grid signature-form">
        <label className="full">Provedor<select required value={providerKey} onChange={(event) => setProviderKey(event.target.value)}>{providers.map((provider) => <option key={provider.key} value={provider.key}>{provider.name}</option>)}</select></label>
        <label className="full">Nome de identificação<input required maxLength={160} value={displayName} onChange={(event) => setDisplayName(event.target.value)} /></label>
        <label>Identificador da credencial<input required value={credentialId} onChange={(event) => setCredentialId(event.target.value)} /></label>
        <label>Token de acesso<input required type="password" autoComplete="new-password" value={accessToken} onChange={(event) => setAccessToken(event.target.value)} /></label>
        <label className="full check-field"><input required type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} /><span>Confirmo que esta credencial pertence ao profissional conectado.</span></label>
      </div>
      <ModalActions saving={saving} onClose={onClose} label="Validar e conectar" />
    </form>
  </Modal>;
}
