import { useCallback, useEffect, useMemo, useState } from "react";
import {
  BadgeCheck,
  CalendarClock,
  Check,
  CreditCard,
  ExternalLink,
  FileText,
  Landmark,
  LockKeyhole,
  RefreshCw,
  Save,
  ShieldCheck,
  Sparkles,
  UsersRound,
} from "lucide-react";
import { api } from "../../api";
import { PageTitle } from "../../components/ui/PageTitle";
import type {
  BillingOverview,
  BillingPaymentMethod,
  BillingProfilePayload,
  Session,
  SubscriptionPlan,
  SubscriptionStatus,
} from "../../types";

const statusLabels: Record<SubscriptionStatus, string> = {
  TRIAL: "Período gratuito",
  PENDING: "Aguardando pagamento",
  ACTIVE: "Ativa",
  PAST_DUE: "Pagamento em atraso",
  SUSPENDED: "Somente consulta",
  CANCELED: "Cancelada",
};

const paymentStatusLabels: Record<string, string> = {
  PENDING: "Pendente",
  RECEIVED: "Recebida",
  CONFIRMED: "Confirmada",
  OVERDUE: "Vencida",
  REFUNDED: "Estornada",
};

const emptyProfile: BillingProfilePayload = {
  legalName: "",
  cpfCnpj: "",
  email: "",
  phone: "",
  postalCode: "",
  address: "",
  addressNumber: "",
  complement: "",
  province: "",
};

export function BillingPage({ session }: { session: Session }) {
  const [overview, setOverview] = useState<BillingOverview | null>(null);
  const [profile, setProfile] = useState<BillingProfilePayload>({
    ...emptyProfile,
    legalName: session.clinic.name,
    email: session.user.email,
  });
  const [selectedPlan, setSelectedPlan] = useState("CLINICA_LEVE_MONTHLY");
  const [paymentMethod, setPaymentMethod] = useState<BillingPaymentMethod>("CREDIT_CARD");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const current = await api.billingOverview(session);
      setOverview(current);
      setSelectedPlan(current.subscription.planCode || "CLINICA_LEVE_MONTHLY");
      if (current.subscription.paymentMethod) setPaymentMethod(current.subscription.paymentMethod);
      if (current.profile) {
        const { synchronizedWithAsaas: _synchronized, ...billingProfile } = current.profile;
        setProfile(billingProfile);
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar a assinatura");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
    const callback = new URLSearchParams(window.location.search).get("billing");
    if (callback === "success") setSuccess("Pagamento enviado. A confirmação será atualizada automaticamente pelo Asaas.");
    if (callback === "cancel") setError("O checkout foi cancelado. Você pode retomá-lo por esta página.");
    if (callback === "expired") setError("O checkout expirou. Atualize a situação para gerar uma nova contratação.");
    if (callback) window.history.replaceState({}, document.title, window.location.pathname);
  }, [load]);

  const currentPlan = useMemo(
    () => overview?.plans.find((plan) => plan.code === selectedPlan),
    [overview?.plans, selectedPlan],
  );

  function updateProfile(field: keyof BillingProfilePayload, value: string) {
    setProfile((current) => ({ ...current, [field]: value }));
    setError("");
    setSuccess("");
  }

  async function saveProfile(showFeedback = true) {
    const saved = await api.saveBillingProfile(session, profile);
    const { synchronizedWithAsaas: _synchronized, ...nextProfile } = saved;
    setProfile(nextProfile);
    setOverview((current) => current ? { ...current, profile: saved } : current);
    if (showFeedback) setSuccess("Dados de faturamento salvos com segurança.");
    return saved;
  }

  async function handleSaveProfile() {
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      await saveProfile();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao salvar os dados de faturamento");
    } finally {
      setSaving(false);
    }
  }

  async function subscribe() {
    if (!currentPlan || !overview?.billingConfigured) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      await saveProfile(false);
      const result = await api.startSubscription(session, {
        planCode: currentPlan.code,
        paymentMethod,
      });
      setSuccess(result.message);
      await load();
      if (result.paymentUrl) window.location.assign(result.paymentUrl);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao iniciar a assinatura");
    } finally {
      setSaving(false);
    }
  }

  async function refresh() {
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      setOverview(await api.refreshSubscription(session));
      setSuccess("Situação financeira atualizada.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao atualizar a assinatura");
    } finally {
      setSaving(false);
    }
  }

  async function cancel() {
    if (!window.confirm("Cancelar a renovação da assinatura? O acesso pago continuará até o fim do período atual.")) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      setOverview(await api.cancelSubscription(session));
      setSuccess("Cancelamento registrado. Cobranças futuras não serão geradas.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao cancelar a assinatura");
    } finally {
      setSaving(false);
    }
  }

  if (loading && !overview) {
    return <div className="loading-state"><span /><p>Carregando assinatura...</p></div>;
  }

  const subscription = overview?.subscription;
  const canContract = subscription && ["TRIAL", "SUSPENDED", "CANCELED"].includes(subscription.status);
  const canCancel = subscription && ["PENDING", "ACTIVE", "PAST_DUE"].includes(subscription.status) && !subscription.cancelAtPeriodEnd;

  return <>
    <PageTitle
      eyebrow="CONTA DA CLÍNICA"
      title="Minha assinatura"
      description="Plano, faturamento e cobranças do Clínica Leve — sem cobrança por usuário."
      action={<button className="secondary-button" disabled={saving} onClick={() => void refresh()}><RefreshCw size={17} />Atualizar situação</button>}
    />

    {(error || success) && <div className={`billing-feedback ${error ? "form-error" : "form-success"}`}>{error || success}</div>}

    {!overview?.billingConfigured && <div className="billing-environment-warning">
      <LockKeyhole size={21} />
      <div><strong>Asaas ainda não configurado neste ambiente</strong><p>A tela está pronta para avaliação, mas a contratação será liberada depois de preencher as variáveis do Sandbox no backend.</p></div>
      <span>SANDBOX</span>
    </div>}

    {overview && <section className="billing-status-grid">
      <article className={`panel subscription-status-card status-${subscription?.status.toLowerCase()}`}>
        <div className="subscription-status-heading">
          <span><BadgeCheck size={25} /></span>
          <div><small>ASSINATURA ATUAL</small><h2>{subscription?.planName}</h2></div>
          <b>{subscription ? statusLabels[subscription.status] : "—"}</b>
        </div>
        <div className="subscription-status-data">
          <div><small>Valor contratado</small><strong>{money(subscription?.amount ?? 0)}<em>/{subscription?.billingCycle === "YEARLY" ? "ano" : "mês"}</em></strong></div>
          <div><small>{subscription?.status === "TRIAL" ? "Teste gratuito até" : "Próximo vencimento"}</small><strong>{date(subscription?.status === "TRIAL" ? subscription.trialEndsAt : subscription?.nextDueDate)}</strong></div>
          <div><small>Acesso</small><strong>{subscription?.accessMode === "FULL" ? "Completo" : "Somente consulta"}</strong></div>
        </div>
        {subscription?.cancelAtPeriodEnd && <p className="billing-cancel-note"><CalendarClock size={16} />Renovação cancelada. O acesso permanece ativo até {date(subscription.nextDueDate)}.</p>}
        {subscription?.status === "PAST_DUE" && <p className="billing-overdue-note"><CalendarClock size={16} />Período de tolerância até {date(subscription.graceEndsAt)}. Depois disso, os dados permanecem disponíveis somente para consulta e exportação.</p>}
        <div className="subscription-status-actions">
          {subscription?.paymentUrl && subscription.status === "PENDING" && <a className="primary-button" href={subscription.paymentUrl} target="_blank" rel="noreferrer">Continuar pagamento<ExternalLink size={16} /></a>}
          {canCancel && <button className="danger-button" disabled={saving} onClick={() => void cancel()}>Cancelar renovação</button>}
        </div>
      </article>

      <article className="panel billing-promise-card">
        <span><UsersRound size={27} /></span>
        <small>NOSSO COMPROMISSO</small>
        <h2>Preço por clínica.<br />Não por usuário.</h2>
        <p>Cadastre profissionais, recepcionistas e gestores sem transformar cada novo acesso em uma nova mensalidade.</p>
        <div><Check size={16} />Usuários ilimitados</div>
        <div><Check size={16} />Todos os módulos atuais</div>
        <div><Check size={16} />Uma clínica ou unidade</div>
      </article>
    </section>}

    {canContract && overview && <section className="billing-section">
      <div className="billing-section-heading"><div><small>ESCOLHA DO PLANO</small><h2>Uma condição simples para a sua clínica</h2></div><span><Sparkles size={20} />30 dias gratuitos</span></div>
      <div className="billing-plan-grid">
        {overview.plans.map((plan) => <PlanCard
          key={plan.code}
          plan={plan}
          selected={selectedPlan === plan.code}
          onSelect={() => plan.available && setSelectedPlan(plan.code)}
        />)}
      </div>
    </section>}

    <section className="billing-content-grid">
      <article className="panel billing-profile-card">
        <div className="panel-heading"><div><h2>Dados de faturamento</h2><p>Cadastro da clínica pagadora enviado ao Asaas somente pelo backend</p></div><FileText size={21} /></div>
        <div className="billing-profile-form">
          <label className="full">Razão social ou nome do responsável<input value={profile.legalName} onChange={(event) => updateProfile("legalName", event.target.value)} /></label>
          <label>CPF ou CNPJ<input value={profile.cpfCnpj} onChange={(event) => updateProfile("cpfCnpj", event.target.value)} placeholder="Somente números ou formatado" /></label>
          <label>Telefone com DDD<input value={profile.phone} onChange={(event) => updateProfile("phone", event.target.value)} /></label>
          <label className="full">E-mail financeiro<input type="email" value={profile.email} onChange={(event) => updateProfile("email", event.target.value)} /></label>
          <label>CEP <small>opcional</small><input value={profile.postalCode ?? ""} onChange={(event) => updateProfile("postalCode", event.target.value)} /></label>
          <label>Logradouro <small>opcional</small><input value={profile.address ?? ""} onChange={(event) => updateProfile("address", event.target.value)} /></label>
          <label>Número <small>opcional</small><input value={profile.addressNumber ?? ""} onChange={(event) => updateProfile("addressNumber", event.target.value)} /></label>
          <label>Bairro <small>opcional</small><input value={profile.province ?? ""} onChange={(event) => updateProfile("province", event.target.value)} /></label>
          <label className="full">Complemento <small>opcional</small><input value={profile.complement ?? ""} onChange={(event) => updateProfile("complement", event.target.value)} /></label>
        </div>
        <div className="billing-profile-actions"><span><ShieldCheck size={16} />A chave do Asaas nunca é enviada ao navegador.</span><button className="secondary-button" disabled={saving} onClick={() => void handleSaveProfile()}><Save size={16} />Salvar dados</button></div>
      </article>

      {canContract && currentPlan ? <article className="panel billing-checkout-card">
        <div className="panel-heading"><div><h2>Forma de pagamento</h2><p>Escolha como deseja renovar o plano</p></div><CreditCard size={21} /></div>
        <div className="billing-methods">
          <button className={paymentMethod === "CREDIT_CARD" ? "selected" : ""} onClick={() => setPaymentMethod("CREDIT_CARD")}><span><CreditCard size={21} /></span><div><strong>Cartão de crédito</strong><small>Renovação automática em checkout seguro</small></div><i>{paymentMethod === "CREDIT_CARD" && <Check size={14} />}</i></button>
          <button className={paymentMethod === "PIX" ? "selected" : ""} onClick={() => setPaymentMethod("PIX")}><span><Landmark size={21} /></span><div><strong>Pix recorrente</strong><small>Uma cobrança Pix será gerada a cada período</small></div><i>{paymentMethod === "PIX" && <Check size={14} />}</i></button>
        </div>
        <div className="billing-order-summary">
          <div><span>{currentPlan.name}</span><strong>{money(currentPlan.price)}</strong></div>
          <small>{currentPlan.billingCycle === "YEARLY" ? "Cobrança anual" : "Cobrança mensal"} · usuários ilimitados</small>
        </div>
        <button className="primary-button billing-subscribe-button" disabled={saving || !overview?.billingConfigured || !currentPlan.available} onClick={() => void subscribe()}>{saving ? "Preparando..." : `Assinar por ${money(currentPlan.price)}`}<ExternalLink size={16} /></button>
        <p className="billing-checkout-security"><LockKeyhole size={14} />Dados de cartão são preenchidos diretamente no ambiente do Asaas.</p>
      </article> : <PaymentHistory overview={overview} />}
    </section>

    {canContract && <PaymentHistory overview={overview} wide />}
  </>;
}

function PlanCard({ plan, selected, onSelect }: { plan: SubscriptionPlan; selected: boolean; onSelect: () => void }) {
  const pioneer = plan.code === "PIONEER_MONTHLY";
  const annual = plan.billingCycle === "YEARLY";
  return <button type="button" className={`billing-plan-card ${selected ? "selected" : ""} ${!plan.available ? "unavailable" : ""}`} onClick={onSelect}>
    <span className="billing-plan-radio">{selected && <Check size={14} />}</span>
    <div className="billing-plan-badges">
      {pioneer && <b>PIONEIROS</b>}
      {annual && <b>2 MESES GRÁTIS</b>}
      {!plan.available && <b>ENCERRADO</b>}
    </div>
    <small>{plan.billingCycle === "YEARLY" ? "PLANO ANUAL" : "PLANO MENSAL"}</small>
    <h3>{plan.name.replace("Clínica Leve ", "")}</h3>
    <p>{plan.description}</p>
    <strong>{money(plan.price)}<em>/{annual ? "ano" : "mês"}</em></strong>
    {plan.remainingSpots !== undefined && plan.available && <span className="billing-plan-spots">{plan.remainingSpots} vaga(s) promocional(is)</span>}
    <div><Check size={15} />Todos os módulos atuais</div>
    <div><Check size={15} />Usuários ilimitados</div>
    <div><Check size={15} />Sem fidelidade no mensal</div>
  </button>;
}

function PaymentHistory({ overview, wide = false }: { overview: BillingOverview | null; wide?: boolean }) {
  return <article className={`panel billing-payments-card ${wide ? "wide" : ""}`}>
    <div className="panel-heading"><div><h2>Histórico de cobranças</h2><p>Mensalidades conciliadas pelo webhook do Asaas</p></div><Landmark size={21} /></div>
    {!overview?.payments.length ? <div className="billing-payment-empty"><FileText size={29} /><strong>Nenhuma cobrança registrada</strong><p>As cobranças aparecerão aqui após a primeira contratação.</p></div> : <div className="table-scroll"><table><thead><tr><th>Vencimento</th><th>Descrição</th><th>Forma</th><th>Valor</th><th>Status</th><th aria-label="Ações" /></tr></thead><tbody>{overview.payments.map((payment) => <tr key={payment.id}><td>{date(payment.dueDate)}</td><td>{payment.description || "Assinatura Clínica Leve"}</td><td>{payment.billingType === "CREDIT_CARD" ? "Cartão" : payment.billingType === "PIX" ? "Pix" : "—"}</td><td>{money(payment.value)}</td><td><span className={`billing-payment-status status-${payment.status.toLowerCase()}`}>{paymentStatusLabels[payment.status] || payment.status}</span></td><td>{(payment.invoiceUrl || payment.bankSlipUrl) && <a className="icon-button" href={payment.invoiceUrl || payment.bankSlipUrl} target="_blank" rel="noreferrer" title="Abrir cobrança"><ExternalLink size={16} /></a>}</td></tr>)}</tbody></table></div>}
  </article>;
}

function money(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

function date(value?: string) {
  if (!value) return "—";
  const normalized = value.length === 10 ? `${value}T12:00:00` : value;
  return new Intl.DateTimeFormat("pt-BR").format(new Date(normalized));
}
