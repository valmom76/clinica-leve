import { type FormEvent, useEffect, useState } from "react";
import { Cloud, HardDrive, ShieldCheck } from "lucide-react";
import { api } from "../../api";
import { Modal, ModalActions } from "../../components/ui/Modal";
import type { ClinicalDocument, DocumentSignature, Session, SignatureCredential } from "../../types";

export function SignatureModal({ session, document, onClose, onSigned }: {
  session: Session;
  document: ClinicalDocument;
  onClose: () => void;
  onSigned: (signature: DocumentSignature) => void;
}) {
  const [credentials, setCredentials] = useState<SignatureCredential[]>([]);
  const [credentialId, setCredentialId] = useState("");
  const [secret, setSecret] = useState("");
  const [secondarySecret, setSecondarySecret] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const selected = credentials.find((credential) => credential.id === credentialId);

  useEffect(() => {
    api.signatureCredentials(session).then((items) => {
      const active = items.filter((item) => item.active && new Date(item.validUntil).getTime() >= Date.now());
      setCredentials(active);
      setCredentialId(active[0]?.id ?? "");
    }).catch((cause) => setError(cause instanceof Error ? cause.message : "Falha ao carregar certificados"))
      .finally(() => setLoading(false));
  }, [session]);

  async function submit(event: FormEvent) {
    event.preventDefault(); setSaving(true); setError("");
    try {
      const result = await api.signClinicalDocument(session, document.id, { credentialId, secret, secondarySecret: secondarySecret || undefined });
      if (result.status === "FAILED") throw new Error(result.failureMessage || "A assinatura não foi concluída");
      onSigned(result);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha ao assinar documento"); }
    finally { setSaving(false); }
  }

  return <Modal title="Assinar documento" description={`Assinatura digital de “${document.title}”.`} onClose={onClose}>
    <form onSubmit={(event) => void submit(event)}>
      {error && <div className="form-error">{error}</div>}
      {!loading && credentials.length === 0 ? <div className="signature-empty-modal"><ShieldCheck size={24} /><p>Nenhum certificado ativo. Cadastre um na aba <strong>Certificados</strong>.</p></div> : <div className="form-grid signature-form">
        <label className="full">Certificado<select required disabled={loading} value={credentialId} onChange={(event) => { setCredentialId(event.target.value); setSecret(""); setSecondarySecret(""); }}><option value="">{loading ? "Carregando..." : "Selecione"}</option>{credentials.map((credential) => <option key={credential.id} value={credential.id}>{credential.displayName} · {credential.mode === "LOCAL_PKCS12" ? "A1" : credential.providerName || "Nuvem"}</option>)}</select></label>
        {selected && <div className="full selected-signature-credential">{selected.mode === "LOCAL_PKCS12" ? <HardDrive size={18} /> : <Cloud size={18} />}<span><strong>{selected.subjectName}</strong><small>Válido até {new Date(selected.validUntil).toLocaleDateString("pt-BR")}</small></span></div>}
        <label className="full">{selected?.mode === "LOCAL_PKCS12" ? "Senha do certificado" : selected?.remoteSecretKind === "OTP" ? "Código OTP" : "PIN do certificado"}<input required type="password" autoComplete="new-password" value={secret} onChange={(event) => setSecret(event.target.value)} /></label>
        {selected?.remoteSecretKind === "PIN_OTP" && <label className="full">Código OTP<input required type="password" inputMode="numeric" autoComplete="one-time-code" value={secondarySecret} onChange={(event) => setSecondarySecret(event.target.value)} /></label>}
        <p className="full signature-consent">Ao continuar, você autoriza uma assinatura digital sobre o PDF final. O conteúdo e o hash ficam registrados na trilha de auditoria.</p>
      </div>}
      {credentials.length > 0 && <ModalActions saving={saving} onClose={onClose} label="Assinar e gerar PDF" />}
    </form>
  </Modal>;
}
