import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowDownToLine,
  ArrowUpFromLine,
  BarChart3,
  Boxes,
  CalendarClock,
  FolderPlus,
  FileSpreadsheet,
  History,
  PackagePlus,
  Pencil,
  Search,
  Tags,
} from "lucide-react";
import { api } from "../../api";
import { Empty } from "../../components/ui/Empty";
import { Kpi } from "../../components/ui/Kpi";
import { PageTitle } from "../../components/ui/PageTitle";
import { ModuleTabs } from "../../components/ui/ModuleTabs";
import type {
  MaterialCategory,
  Session,
  StockMaterial,
  StockMovementType,
} from "../../types";
import { CategoryModal } from "./CategoryModal";
import { formatDate, formatQuantity, expiresWithin } from "./inventoryUtils";
import { MaterialModal } from "./MaterialModal";
import { InventoryMovementReport } from "./InventoryMovementReport";
import { InventoryImportModal } from "./InventoryImportModal";
import { MovementHistoryModal } from "./MovementHistoryModal";
import { MovementModal } from "./MovementModal";

type MovementSelection = {
  material: StockMaterial;
  type: StockMovementType;
};

type InventorySection = "STOCK" | "REPORT";

export function InventoryPage({ session }: { session: Session }) {
  const [categories, setCategories] = useState<MaterialCategory[]>([]);
  const [materials, setMaterials] = useState<StockMaterial[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [categoryModal, setCategoryModal] = useState(false);
  const [materialModal, setMaterialModal] = useState<StockMaterial | "new" | null>(null);
  const [movement, setMovement] = useState<MovementSelection | null>(null);
  const [history, setHistory] = useState<StockMaterial | null>(null);
  const [section, setSection] = useState<InventorySection>("STOCK");
  const [importModal, setImportModal] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [categoryList, materialList] = await Promise.all([
        api.materialCategories(session),
        api.materials(session),
      ]);
      setCategories(categoryList);
      setMaterials(materialList);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao carregar estoque");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = useMemo(() => {
    const term = search.toLowerCase();
    return materials.filter((material) =>
      [material.name, material.categoryName, material.sku ?? ""].some((value) =>
        value.toLowerCase().includes(term),
      ),
    );
  }, [materials, search]);

  const lowStock = materials.filter((material) => material.lowStock).length;
  const expiring = materials.filter((material) =>
    expiresWithin(material.nearestExpiration, 30),
  ).length;

  function updateMaterial(updated: StockMaterial) {
    setMaterials((current) => {
      const exists = current.some((material) => material.id === updated.id);
      const next = exists
        ? current.map((material) => material.id === updated.id ? updated : material)
        : [...current, updated];
      return next.sort((a, b) => a.name.localeCompare(b.name));
    });
    setMaterialModal(null);
    setMovement(null);
  }

  return (
    <>
      <PageTitle
        eyebrow="SUPRIMENTOS"
        title="Estoque e materiais"
        description="Saldos, lotes e movimentações isolados para esta clínica."
        action={section === "STOCK" ? (
          <div className="inventory-title-actions">
            <button className="secondary-button" onClick={() => setCategoryModal(true)}>
              <FolderPlus size={17} />Categoria
            </button>
            <button className="secondary-button" onClick={() => setImportModal(true)}>
              <FileSpreadsheet size={17} />Importar Excel
            </button>
            <button className="primary-button" onClick={() => setMaterialModal("new")}>
              <PackagePlus size={17} />Novo material
            </button>
          </div>
        ) : undefined}
      />

      <ModuleTabs<InventorySection>
        active={section}
        onChange={setSection}
        items={[
          { key: "STOCK", label: "Estoque atual", icon: Boxes },
          { key: "REPORT", label: "Relatório de movimentações", icon: BarChart3 },
        ]}
      />

      {section === "REPORT" ? (
        <InventoryMovementReport session={session} materials={materials} />
      ) : <>
      <section className="kpis">
        <Kpi icon={Boxes} label="Materiais ativos" value={String(materials.length)} tone="sage" />
        <Kpi icon={AlertTriangle} label="Estoque baixo" value={String(lowStock)} tone="terracotta" />
        <Kpi icon={CalendarClock} label="Validade em até 30 dias" value={String(expiring)} tone="blue" />
        <Kpi icon={Tags} label="Categorias" value={String(categories.length)} tone="sage" />
      </section>

      {error && (
        <div className="page-error">{error}<button onClick={() => void load()}>Tentar novamente</button></div>
      )}

      <article className="panel data-panel">
        <div className="table-toolbar">
          <div className="field-search">
            <Search size={16} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar material, categoria ou código" />
          </div>
          <span>{visible.length} material(is)</span>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Material</th>
                <th>Categoria</th>
                <th>Saldo</th>
                <th>Mínimo</th>
                <th>Próxima validade</th>
                <th>Status</th>
                <th aria-label="Ações" />
              </tr>
            </thead>
            <tbody>
              {!loading && visible.map((material) => (
                <tr key={material.id}>
                  <td>
                    <div className="material-name">
                      <strong>{material.name}</strong>
                      <small>{material.sku ?? (material.lotControlled ? "Controle por lote" : "Sem código")}</small>
                    </div>
                  </td>
                  <td>{material.categoryName}</td>
                  <td><strong className="stock-balance">{formatQuantity(material.currentStock, material.unit)}</strong></td>
                  <td>{formatQuantity(material.minimumStock, material.unit)}</td>
                  <td className={expiresWithin(material.nearestExpiration, 30) ? "expiration-warning" : ""}>
                    {formatDate(material.nearestExpiration)}
                  </td>
                  <td>
                    <small className={`status ${material.lowStock ? "cancelled" : "confirmed"}`}>
                      {material.lowStock ? "Repor" : "Regular"}
                    </small>
                  </td>
                  <td>
                    <div className="inventory-row-actions">
                      <button title="Editar material" onClick={() => setMaterialModal(material)}><Pencil size={15} /></button>
                      <button title="Registrar entrada" onClick={() => setMovement({ material, type: "ENTRY" })}><ArrowDownToLine size={15} /></button>
                      <button title="Registrar saída" onClick={() => setMovement({ material, type: "EXIT" })}><ArrowUpFromLine size={15} /></button>
                      <button title="Ver histórico" onClick={() => setHistory(material)}><History size={15} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <Empty text="Carregando materiais..." />}
          {!loading && visible.length === 0 && <Empty text="Nenhum material encontrado." />}
        </div>
      </article>

      {categoryModal && (
        <CategoryModal
          session={session}
          onClose={() => setCategoryModal(false)}
          onCreated={(category) => {
            setCategories((current) => [...current, category].sort((a, b) => a.name.localeCompare(b.name)));
            setCategoryModal(false);
          }}
        />
      )}
      {materialModal && (
        <MaterialModal
          session={session}
          categories={categories}
          material={materialModal === "new" ? undefined : materialModal}
          onClose={() => setMaterialModal(null)}
          onCreated={updateMaterial}
        />
      )}
      {movement && (
        <MovementModal
          session={session}
          material={movement.material}
          initialType={movement.type}
          onClose={() => setMovement(null)}
          onSaved={updateMaterial}
        />
      )}
      {history && (
        <MovementHistoryModal session={session} material={history} onClose={() => setHistory(null)} />
      )}
      {importModal && (
        <InventoryImportModal session={session} onClose={() => setImportModal(false)} onImported={load} />
      )}
      </>}
    </>
  );
}
