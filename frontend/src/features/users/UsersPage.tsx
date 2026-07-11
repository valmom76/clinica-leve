import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, Search } from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { PageTitle } from "../../components/ui/PageTitle";
import type { ClinicUser, Session } from "../../types";
import { roleLabel } from "./roleOptions";
import { UserModal } from "./UserModal";

export function UsersPage({ session }: { session: Session }) {
  const [users, setUsers] = useState<ClinicUser[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState<ClinicUser | "new" | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setUsers(await api.users(session));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar acessos");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = users.filter((user) =>
    [user.name, user.email, roleLabel[user.role]].some((value) =>
      value.toLowerCase().includes(search.toLowerCase()),
    ),
  );

  function saveUser(saved: ClinicUser) {
    setUsers((current) => {
      const exists = current.some((user) => user.id === saved.id);
      const next = exists
        ? current.map((user) => (user.id === saved.id ? saved : user))
        : [...current, saved];
      return next.sort((a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name));
    });
    setSelected(null);
  }

  return (
    <>
      <PageTitle
        eyebrow="SEGURANÇA E EQUIPE"
        title="Equipe e acessos"
        description="Perfis, credenciais e situação dos usuários desta clínica."
        action={
          <button className="primary-button" onClick={() => setSelected("new")}>
            <Plus size={17} />Novo acesso
          </button>
        }
      />

      {error && <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>}

      <article className="panel data-panel">
        <div className="table-toolbar">
          <div className="field-search">
            <Search size={16} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar usuário" />
          </div>
          <span>{visible.length} acesso(s)</span>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Usuário</th>
                <th>E-mail</th>
                <th>Perfil</th>
                <th>Status</th>
                <th aria-label="Ações" />
              </tr>
            </thead>
            <tbody>
              {!loading && visible.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className="person">
                      <span>{user.name.split(" ").map((part) => part[0]).slice(0, 2).join("")}</span>
                      <strong>{user.name}</strong>
                    </div>
                  </td>
                  <td>{user.email}</td>
                  <td>{roleLabel[user.role]}</td>
                  <td>
                    <small className={`status ${user.active ? "confirmed" : "cancelled"}`}>
                      {user.active ? "Ativo" : "Inativo"}
                    </small>
                  </td>
                  <td>
                    <button className="text-button" onClick={() => setSelected(user)}>
                      <Pencil size={14} />Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <Empty text="Carregando acessos..." />}
          {!loading && visible.length === 0 && <Empty text="Nenhum acesso encontrado." />}
        </div>
      </article>

      {selected && (
        <UserModal
          session={session}
          user={selected === "new" ? undefined : selected}
          onClose={() => setSelected(null)}
          onSaved={saveUser}
        />
      )}
    </>
  );
}
