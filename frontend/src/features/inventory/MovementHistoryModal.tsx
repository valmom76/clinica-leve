import { useEffect, useState } from "react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Modal } from "../../components/ui/Modal";
import type { Session, StockMaterial, StockMovement } from "../../types";
import { formatDate, formatQuantity } from "./inventoryUtils";

export function MovementHistoryModal({
  session,
  material,
  onClose,
}: {
  session: Session;
  material: StockMaterial;
  onClose: () => void;
}) {
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    api.stockMovements(session, material.id)
      .then(setMovements)
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Falha ao carregar histórico"))
      .finally(() => setLoading(false));
  }, [material.id, session]);

  return (
    <Modal title={`Histórico · ${material.name}`} description="Últimas 100 movimentações do material." onClose={onClose}>
      {error && <div className="form-error">{error}</div>}
      {loading && <Empty text="Carregando movimentações..." />}
      {!loading && movements.length === 0 && <Empty text="Nenhuma movimentação registrada." />}
      <div className="movement-history">
        {movements.map((movement) => (
          <article key={movement.id}>
            <span className={`movement-kind ${movement.type.toLowerCase()}`}>
              {movement.type === "ENTRY" ? "Entrada" : "Saída"}
            </span>
            <div>
              <strong>{formatQuantity(movement.quantity, material.unit)}</strong>
              <small>{movement.reason}</small>
              {movement.lotNumber && <small>Lote {movement.lotNumber} · validade {formatDate(movement.expirationDate)}</small>}
            </div>
            <div className="movement-balance">
              <time>{new Date(movement.occurredAt).toLocaleString("pt-BR")}</time>
              <small>Saldo: {formatQuantity(movement.balanceAfter, material.unit)}</small>
            </div>
          </article>
        ))}
      </div>
    </Modal>
  );
}
