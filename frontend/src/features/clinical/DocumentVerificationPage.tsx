import { useEffect, useState } from "react";
import { AlertTriangle, CheckCircle2, FileCheck2, LoaderCircle, ShieldCheck, XCircle } from "lucide-react";
import { api } from "../../api";
import type { SignatureVerification } from "../../types";
import { documentTypeLabel } from "./clinicalLabels";

export function DocumentVerificationPage({ code }: { code: string }) {
  const [result, setResult] = useState<SignatureVerification>();
  const [error, setError] = useState("");

  useEffect(() => {
    api.verifySignature(code)
      .then(setResult)
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Não foi possível consultar a assinatura"));
  }, [code]);

  if (!result && !error) return <main className="verification-page"><section className="verification-card loading"><LoaderCircle className="spin" size={28} /><p>Verificando assinatura e integridade…</p></section></main>;
  const valid = Boolean(result?.found && result.signed && result.integrityValid && result.cryptographicSignatureValid);

  return <main className="verification-page">
    <section className="verification-card">
      <header><span><ShieldCheck size={26} /></span><div><small>CLÍNICA LEVE</small><h1>Validação de documento</h1></div></header>
      {error ? <div className="verification-state invalid"><XCircle size={35} /><h2>Consulta indisponível</h2><p>{error}</p></div> : <>
        <div className={`verification-state ${valid ? "valid" : "invalid"}`}>
          {valid ? <CheckCircle2 size={38} /> : result?.found ? <AlertTriangle size={38} /> : <XCircle size={38} />}
          <h2>{valid ? "Documento íntegro e assinado" : result?.found ? "Validação requer atenção" : "Documento não encontrado"}</h2>
          <p>{result?.notice}</p>
        </div>
        {result?.found && <dl className="verification-details">
          <div><dt>Clínica</dt><dd>{result.clinicName || "—"}</dd></div>
          <div><dt>Documento</dt><dd>{result.documentType ? documentTypeLabel[result.documentType] : "—"}</dd></div>
          <div><dt>Assinante</dt><dd>{result.signerSubject || "—"}</dd></div>
          <div><dt>Modalidade</dt><dd>{result.mode === "LOCAL_PKCS12" ? "Certificado A1" : result.providerName || "Certificado em nuvem"}</dd></div>
          <div><dt>Assinado em</dt><dd>{result.signedAt ? new Date(result.signedAt).toLocaleString("pt-BR") : "—"}</dd></div>
          <div><dt>Serial do certificado</dt><dd>{result.certificateSerial || "—"}</dd></div>
          <div className="full"><dt>Hash SHA-256 do PDF</dt><dd>{result.signedPdfHash || "—"}</dd></div>
        </dl>}
      </>}
      <footer><FileCheck2 size={17} /><span>Esta página não exibe dados do paciente. Para validar a cadeia ICP-Brasil e o estado de revogação, use também o serviço VALIDAR do ITI.</span></footer>
    </section>
  </main>;
}
