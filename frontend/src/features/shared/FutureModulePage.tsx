import { Box } from "lucide-react";
import type { ViewKey } from "../../app/navigation";
import { PageTitle } from "../../components/ui/PageTitle";

const moduleNames: Partial<Record<ViewKey, string>> = {
  inventory: "Estoque",
  finance: "Financeiro",
  time: "Controle de ponto",
  reports: "Relatórios",
};

export function FutureModulePage({ view }: { view: ViewKey }) {
  return (
    <>
      <PageTitle
        eyebrow="PRÓXIMO MÓDULO"
        title={moduleNames[view] ?? "Novo módulo"}
        description="A estrutura multiempresa já está preparada para receber este domínio."
      />
      <article className="panel future-module">
        <Box size={30} />
        <h2>Módulo planejado para o próximo marco</h2>
        <p>
          Ele utilizará o mesmo tenant do usuário autenticado e nunca
          compartilhará registros entre clínicas.
        </p>
      </article>
    </>
  );
}
