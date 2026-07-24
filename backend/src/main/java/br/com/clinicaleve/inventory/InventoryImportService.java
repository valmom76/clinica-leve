package br.com.clinicaleve.inventory;

import br.com.clinicaleve.inventory.InventoryDtos.MaterialRequest;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportAction;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportConfirmRequest;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportPreviewResponse;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportPreviewRow;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportResult;
import br.com.clinicaleve.inventory.InventoryImportDtos.SimilarMaterial;
import br.com.clinicaleve.inventory.InventoryImportDtos.SuggestedAction;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryImportService {
    private static final int MAX_IMPORT_ROWS = 5000;
    private static final long MAX_IMPORT_BYTES = 8L * 1024 * 1024;
    private static final BigDecimal MAXIMUM_MINIMUM_STOCK = new BigDecimal("999999999");
    private static final int SIMILARITY_REVIEW_THRESHOLD = 72;
    private static final List<String> UNITS = List.of(
            "un", "cx", "pct", "kit", "par", "rolo", "frasco", "ml", "l", "g", "kg"
    );
    private static final List<String> HEADERS = List.of(
            "Descrição*",
            "Categoria*",
            "Código interno",
            "Unidade*",
            "Estoque mínimo*",
            "Controla lote (SIM/NÃO)*",
            "Estoque atual (somente leitura)",
            "ID do sistema (não editar)"
    );

    private final MaterialRepository materialRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final MaterialCategoryService categoryService;
    private final InventoryService inventoryService;

    @Transactional
    public byte[] template() {
        var clinicId = TenantAccess.currentClinicId();
        var categories = activeCategories(clinicId);
        var materials = materialRepository.findByClinicIdAndActiveTrueOrderByName(clinicId);

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var products = workbook.createSheet("Produtos");
            var instructions = workbook.createSheet("Instruções");
            var lists = workbook.createSheet("Categorias");
            createInstructions(workbook, instructions);
            createLists(workbook, lists, categories);
            createProducts(workbook, products, categories, materials);
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar a planilha modelo", exception);
        }
    }

    @Transactional
    public ImportPreviewResponse preview(MultipartFile file) {
        return analyze(parse(file));
    }

    @Transactional
    public ImportResult confirm(MultipartFile file, ImportConfirmRequest request) {
        var preview = analyze(parse(file));
        var decisions = request.decisions().stream().collect(Collectors.toMap(
                InventoryImportDtos.ImportDecision::rowNumber,
                Function.identity(),
                (first, ignored) -> first
        ));
        var clinicId = TenantAccess.currentClinicId();
        var categories = activeCategories(clinicId).stream().collect(Collectors.toMap(
                category -> normalize(category.getName()),
                Function.identity()
        ));
        var created = 0;
        var updated = 0;
        var skipped = 0;

        for (var row : preview.rows()) {
            var decision = decisions.get(row.rowNumber());
            if (decision == null
                    || decision.action() == ImportAction.SKIP
                    || row.suggestedAction() == SuggestedAction.ERROR
                    || row.suggestedAction() == SuggestedAction.UNCHANGED) {
                skipped++;
                continue;
            }

            var category = categories.get(normalize(row.categoryName()));
            if (category == null) {
                throw new IllegalStateException("A categoria da linha " + row.rowNumber() + " não está mais disponível");
            }
            var materialRequest = new MaterialRequest(
                    row.name(),
                    category.getId(),
                    row.sku(),
                    row.unit(),
                    row.minimumStock(),
                    Boolean.TRUE.equals(row.lotControlled())
            );

            if (decision.action() == ImportAction.CREATE) {
                if (row.suggestedAction() != SuggestedAction.CREATE
                        && row.suggestedAction() != SuggestedAction.REVIEW) {
                    throw new IllegalArgumentException("A linha " + row.rowNumber() + " não pode ser incluída como nova");
                }
                inventoryService.create(materialRequest);
                created++;
                continue;
            }

            if (decision.action() != ImportAction.UPDATE) {
                throw new IllegalArgumentException("Ação inválida na linha " + row.rowNumber());
            }
            var targetId = clean(decision.targetMaterialId());
            if (row.suggestedAction() == SuggestedAction.UPDATE) {
                targetId = targetId == null ? row.targetMaterialId() : targetId;
                if (!Objects.equals(targetId, row.targetMaterialId())) {
                    throw new IllegalArgumentException("O destino da linha " + row.rowNumber() + " foi alterado");
                }
            } else if (row.suggestedAction() == SuggestedAction.REVIEW) {
                var candidateIds = row.similarMaterials().stream()
                        .map(SimilarMaterial::materialId)
                        .collect(Collectors.toSet());
                if (targetId == null || !candidateIds.contains(targetId)) {
                    throw new IllegalArgumentException("Selecione um produto semelhante válido na linha " + row.rowNumber());
                }
            } else {
                throw new IllegalArgumentException("A linha " + row.rowNumber() + " não pode atualizar um produto");
            }

            if (row.sourceId() != null && !row.sourceId().equals(targetId)) {
                throw new IllegalArgumentException("O ID original da linha " + row.rowNumber() + " não pode ser substituído");
            }
            inventoryService.update(targetId, materialRequest);
            updated++;
        }
        return new ImportResult(created, updated, skipped);
    }

    private ImportPreviewResponse analyze(List<ParsedRow> parsedRows) {
        var clinicId = TenantAccess.currentClinicId();
        var materials = materialRepository.findByClinicIdAndActiveTrueOrderByName(clinicId);
        var categories = activeCategories(clinicId);
        var materialById = materials.stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        var materialBySku = materials.stream()
                .filter(material -> clean(material.getSku()) != null)
                .collect(Collectors.toMap(material -> skuKey(material.getSku()), Function.identity(), (first, ignored) -> first));
        var materialByName = materials.stream().collect(Collectors.toMap(
                material -> normalize(material.getName()), Function.identity(), (first, ignored) -> first));
        var categoryByName = categories.stream().collect(Collectors.toMap(
                category -> normalize(category.getName()), Function.identity()));
        var duplicateIds = duplicates(parsedRows, row -> clean(row.sourceId()));
        var duplicateSkus = duplicates(parsedRows, row -> skuKey(row.sku()));
        var duplicateNewNames = duplicates(
                parsedRows.stream().filter(row -> clean(row.sourceId()) == null).toList(),
                row -> normalizeNullable(row.name())
        );

        var rows = new ArrayList<ImportPreviewRow>();
        for (var parsed : parsedRows) {
            var errors = new ArrayList<String>();
            var warnings = new ArrayList<String>();
            validate(parsed, categoryByName, duplicateIds, duplicateSkus, duplicateNewNames, errors, warnings);

            Material target = null;
            String matchReason = null;
            var sourceId = clean(parsed.sourceId());
            if (sourceId != null) {
                target = materialById.get(sourceId);
                matchReason = "ID do sistema";
                if (target == null) errors.add("O ID informado não pertence a um produto ativo desta clínica");
            }

            var normalizedSku = skuKey(parsed.sku());
            var skuTarget = normalizedSku == null ? null : materialBySku.get(normalizedSku);
            if (target != null && skuTarget != null && !target.getId().equals(skuTarget.getId())) {
                errors.add("O código interno pertence a outro produto: " + skuTarget.getName());
            } else if (target == null && skuTarget != null) {
                target = skuTarget;
                matchReason = "Código interno idêntico";
                warnings.add("O código interno já existe e direcionará a linha para atualização");
            }

            var normalizedName = normalizeNullable(parsed.name());
            var nameTarget = normalizedName == null ? null : materialByName.get(normalizedName);
            if (target == null && nameTarget != null) {
                target = nameTarget;
                matchReason = "Descrição idêntica";
            }

            if (target != null
                    && parsed.lotControlled() != null
                    && target.isLotControlled() != parsed.lotControlled()
                    && target.getCurrentStock().signum() > 0) {
                errors.add("O controle por lote não pode mudar enquanto o produto tiver saldo");
            }
            if (target != null
                    && parsed.currentStock() != null
                    && target.getCurrentStock().compareTo(parsed.currentStock()) != 0) {
                warnings.add("A alteração no estoque atual será ignorada; use uma entrada ou saída para mudar o saldo");
            }

            var similar = similarMaterials(parsed.name(), materials, target == null ? null : target.getId());
            SuggestedAction action;
            if (!errors.isEmpty()) {
                action = SuggestedAction.ERROR;
            } else if (target != null) {
                action = unchanged(parsed, target, categoryByName)
                        ? SuggestedAction.UNCHANGED
                        : SuggestedAction.UPDATE;
            } else if (!similar.isEmpty()
                    && similar.get(0).similarityPercent() >= SIMILARITY_REVIEW_THRESHOLD) {
                action = SuggestedAction.REVIEW;
                warnings.add("Existe uma descrição parecida. Revise antes de incluir ou atualizar");
                matchReason = "Descrição semelhante";
            } else {
                action = SuggestedAction.CREATE;
            }

            rows.add(new ImportPreviewRow(
                    parsed.rowNumber(),
                    sourceId,
                    clean(parsed.name()),
                    clean(parsed.categoryName()),
                    clean(parsed.sku()),
                    clean(parsed.unit()),
                    parsed.minimumStock(),
                    parsed.lotControlled(),
                    parsed.currentStock(),
                    action,
                    target == null ? null : target.getId(),
                    target == null ? null : target.getName(),
                    matchReason,
                    similar,
                    List.copyOf(warnings),
                    List.copyOf(errors)
            ));
        }

        return new ImportPreviewResponse(
                rows.size(),
                count(rows, SuggestedAction.CREATE),
                count(rows, SuggestedAction.UPDATE),
                count(rows, SuggestedAction.REVIEW),
                count(rows, SuggestedAction.UNCHANGED),
                count(rows, SuggestedAction.ERROR),
                rows
        );
    }

    private List<ParsedRow> parse(MultipartFile file) {
        validateFile(file);
        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheet("Produtos");
            if (sheet == null) throw new IllegalArgumentException("A planilha precisa conter a aba Produtos");
            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new IllegalArgumentException("A importação permite no máximo " + MAX_IMPORT_ROWS + " linhas");
            }
            var headers = headers(sheet.getRow(0));
            requireHeaders(headers);
            var rows = new ArrayList<ParsedRow>();
            for (var index = 1; index <= sheet.getLastRowNum(); index++) {
                var row = sheet.getRow(index);
                if (row == null || empty(row, headers)) continue;
                rows.add(new ParsedRow(
                        index + 1,
                        text(row, headers.get("id do sistema nao editar")),
                        text(row, headers.get("descricao")),
                        text(row, headers.get("categoria")),
                        text(row, headers.get("codigo interno")),
                        text(row, headers.get("unidade")),
                        decimal(row, headers.get("estoque minimo")),
                        bool(row, headers.get("controla lote sim nao")),
                        decimal(row, headers.get("estoque atual somente leitura"))
                ));
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("A planilha não possui produtos preenchidos");
            return rows;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo. Use o modelo XLSX fornecido pelo sistema", exception);
        }
    }

    private void validate(
            ParsedRow row,
            Map<String, MaterialCategory> categoryByName,
            Set<String> duplicateIds,
            Set<String> duplicateSkus,
            Set<String> duplicateNewNames,
            List<String> errors,
            List<String> warnings
    ) {
        var name = clean(row.name());
        var categoryName = clean(row.categoryName());
        var unit = clean(row.unit());
        if (name == null) errors.add("Informe a descrição");
        else if (name.length() > 160) errors.add("A descrição deve ter no máximo 160 caracteres");
        if (categoryName == null) errors.add("Informe a categoria");
        else if (!categoryByName.containsKey(normalize(categoryName))) errors.add("Categoria não cadastrada na clínica");
        if (unit == null) errors.add("Informe a unidade");
        else if (unit.length() > 30) errors.add("A unidade deve ter no máximo 30 caracteres");
        if (row.minimumStock() == null || row.minimumStock().signum() < 0) {
            errors.add("Informe um estoque mínimo válido, igual ou maior que zero");
        } else if (row.minimumStock().compareTo(MAXIMUM_MINIMUM_STOCK) > 0) {
            errors.add("O estoque mínimo deve ser menor ou igual a 999.999.999");
        }
        if (row.lotControlled() == null) errors.add("Use SIM ou NÃO no campo de controle por lote");
        if (clean(row.sku()) != null && clean(row.sku()).length() > 80) errors.add("O código interno deve ter no máximo 80 caracteres");
        if (duplicateIds.contains(clean(row.sourceId()))) errors.add("O mesmo ID aparece mais de uma vez na planilha");
        if (duplicateSkus.contains(skuKey(row.sku()))) errors.add("O mesmo código interno aparece mais de uma vez na planilha");
        if (clean(row.sourceId()) == null && duplicateNewNames.contains(normalizeNullable(row.name()))) {
            errors.add("A mesma descrição nova aparece mais de uma vez na planilha");
        }
        if (row.currentStock() != null && clean(row.sourceId()) == null && row.currentStock().signum() != 0) {
            warnings.add("O estoque atual é somente informativo e não será importado");
        }
    }

    private boolean unchanged(
            ParsedRow row,
            Material material,
            Map<String, MaterialCategory> categoryByName
    ) {
        var category = categoryByName.get(normalize(row.categoryName()));
        return normalize(material.getName()).equals(normalize(row.name()))
                && Objects.equals(skuKey(material.getSku()), skuKey(row.sku()))
                && normalize(material.getUnit()).equals(normalize(row.unit()))
                && material.getMinimumStock().compareTo(row.minimumStock()) == 0
                && material.isLotControlled() == row.lotControlled()
                && category != null
                && material.getCategoryId().equals(category.getId());
    }

    private List<SimilarMaterial> similarMaterials(String name, List<Material> materials, String excludedId) {
        if (clean(name) == null) return List.of();
        return materials.stream()
                .filter(material -> !material.getId().equals(excludedId))
                .map(material -> new SimilarMaterial(
                        material.getId(),
                        material.getName(),
                        material.getSku(),
                        similarityPercent(name, material.getName())
                ))
                .filter(candidate -> candidate.similarityPercent() >= 55)
                .sorted(Comparator.comparingInt(SimilarMaterial::similarityPercent).reversed())
                .limit(3)
                .toList();
    }

    private int similarityPercent(String first, String second) {
        var left = normalize(first);
        var right = normalize(second);
        if (left.equals(right)) return 100;
        var maximum = Math.max(left.length(), right.length());
        if (maximum == 0) return 100;
        var levenshtein = 1d - (double) levenshtein(left, right) / maximum;
        var leftTokens = new HashSet<>(List.of(left.split(" ")));
        var rightTokens = new HashSet<>(List.of(right.split(" ")));
        var intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        var union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        var tokenScore = union.isEmpty() ? 0d : (double) intersection.size() / union.size();
        var containsScore = left.contains(right) || right.contains(left) ? 0.88d : 0d;
        return (int) Math.round(Math.max(containsScore, Math.max(levenshtein, tokenScore)) * 100);
    }

    private int levenshtein(String first, String second) {
        var previous = new int[second.length() + 1];
        for (var column = 0; column <= second.length(); column++) previous[column] = column;
        for (var row = 1; row <= first.length(); row++) {
            var current = new int[second.length() + 1];
            current[0] = row;
            for (var column = 1; column <= second.length(); column++) {
                var cost = first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + cost
                );
            }
            previous = current;
        }
        return previous[second.length()];
    }

    private void createProducts(
            XSSFWorkbook workbook,
            Sheet sheet,
            List<MaterialCategory> categories,
            List<Material> materials
    ) {
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, 1);
        var headerStyle = headerStyle(workbook);
        var editableStyle = editableStyle(workbook, false);
        var editableDecimalStyle = editableStyle(workbook, true);
        var readOnlyStyle = readOnlyStyle(workbook, false);
        var readOnlyDecimalStyle = readOnlyStyle(workbook, true);
        var header = sheet.createRow(0);
        header.setHeightInPoints(28);
        for (var index = 0; index < HEADERS.size(); index++) {
            cell(header, index, HEADERS.get(index), headerStyle);
        }

        var rowIndex = 1;
        for (var material : materials) {
            var row = sheet.createRow(rowIndex++);
            var categoryName = categories.stream()
                    .filter(category -> category.getId().equals(material.getCategoryId()))
                    .map(MaterialCategory::getName)
                    .findFirst().orElse("Sem categoria");
            cell(row, 0, material.getName(), editableStyle);
            cell(row, 1, categoryName, editableStyle);
            cell(row, 2, material.getSku(), editableStyle);
            cell(row, 3, material.getUnit(), editableStyle);
            numericCell(row, 4, material.getMinimumStock(), editableDecimalStyle);
            cell(row, 5, material.isLotControlled() ? "SIM" : "NÃO", editableStyle);
            numericCell(row, 6, material.getCurrentStock(), readOnlyDecimalStyle);
            cell(row, 7, material.getId(), readOnlyStyle);
        }
        var preparedRows = Math.max(rowIndex + 100, 151);
        for (var index = rowIndex; index < preparedRows; index++) {
            var row = sheet.createRow(index);
            for (var column = 0; column < 6; column++) {
                row.createCell(column).setCellStyle(column == 4 ? editableDecimalStyle : editableStyle);
            }
            row.createCell(6).setCellStyle(readOnlyDecimalStyle);
            row.createCell(7).setCellStyle(readOnlyStyle);
        }
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, rowIndex - 1), 0, 7));

        var widths = new int[]{42, 27, 22, 14, 19, 27, 28, 42};
        for (var index = 0; index < widths.length; index++) sheet.setColumnWidth(index, widths[index] * 256);

        var helper = sheet.getDataValidationHelper();
        addValidation(sheet, helper.createFormulaListConstraint("CategoriasValidas"), 1, MAX_IMPORT_ROWS, 1);
        addValidation(sheet, helper.createFormulaListConstraint("UnidadesValidas"), 1, MAX_IMPORT_ROWS, 3);
        addValidation(sheet, helper.createExplicitListConstraint(new String[]{"SIM", "NÃO"}), 1, MAX_IMPORT_ROWS, 5);
        addValidation(sheet, helper.createDecimalConstraint(
                org.apache.poi.ss.usermodel.DataValidationConstraint.OperatorType.BETWEEN,
                "0", "999999999"
        ), 1, MAX_IMPORT_ROWS, 4);
    }

    private void createInstructions(XSSFWorkbook workbook, Sheet sheet) {
        sheet.setDisplayGridlines(false);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        var title = sheet.createRow(0);
        cell(title, 0, "Clínica Leve — importação de materiais", titleStyle(workbook));
        var instructions = List.of(
                "1. Edite somente a aba Produtos e mantenha os títulos das colunas.",
                "2. Produtos já cadastrados aparecem com o ID do sistema; não altere esse campo.",
                "3. Para novos produtos, deixe ID e estoque atual vazios.",
                "4. O estoque atual não é importado: saldos continuam sendo alterados apenas por entradas e saídas.",
                "5. Use categorias e unidades listadas na aba Categorias.",
                "6. Após o upload, revise descrições semelhantes antes de confirmar o merge.",
                "7. Nenhuma alteração é aplicada durante a prévia. O banco só muda após a confirmação."
        );
        for (var index = 0; index < instructions.size(); index++) {
            var row = sheet.createRow(index + 2);
            row.setHeightInPoints(25);
            cell(row, 0, instructions.get(index), instructionStyle(workbook));
            sheet.addMergedRegion(new CellRangeAddress(index + 2, index + 2, 0, 5));
        }
        for (var index = 0; index < 6; index++) sheet.setColumnWidth(index, 20 * 256);
    }

    private void createLists(XSSFWorkbook workbook, Sheet sheet, List<MaterialCategory> categories) {
        sheet.setDisplayGridlines(false);
        var headerStyle = headerStyle(workbook);
        var bodyStyle = readOnlyStyle(workbook, false);
        var header = sheet.createRow(0);
        cell(header, 0, "Categorias disponíveis", headerStyle);
        cell(header, 1, "Unidades sugeridas", headerStyle);
        for (var index = 0; index < Math.max(categories.size(), UNITS.size()); index++) {
            var row = sheet.createRow(index + 1);
            if (index < categories.size()) cell(row, 0, categories.get(index).getName(), bodyStyle);
            if (index < UNITS.size()) cell(row, 1, UNITS.get(index), bodyStyle);
        }
        sheet.setColumnWidth(0, 34 * 256);
        sheet.setColumnWidth(1, 22 * 256);

        var categoryName = workbook.createName();
        categoryName.setNameName("CategoriasValidas");
        categoryName.setRefersToFormula("'Categorias'!$A$2:$A$" + (categories.size() + 1));
        var unitName = workbook.createName();
        unitName.setNameName("UnidadesValidas");
        unitName.setRefersToFormula("'Categorias'!$B$2:$B$" + (UNITS.size() + 1));
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.WHITE.getIndex());
        var font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        var font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle instructionStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        style.setWrapText(true);
        var font = workbook.createFont();
        font.setColor(IndexedColors.DARK_TEAL.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle editableStyle(XSSFWorkbook workbook, boolean decimal) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.HAIR);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        if (decimal) style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.000"));
        return style;
    }

    private CellStyle readOnlyStyle(XSSFWorkbook workbook, boolean decimal) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.HAIR);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        if (decimal) style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.000"));
        return style;
    }

    private void addValidation(
            Sheet sheet,
            org.apache.poi.ss.usermodel.DataValidationConstraint constraint,
            int firstRow,
            int lastRow,
            int column
    ) {
        var validation = sheet.getDataValidationHelper().createValidation(
                constraint,
                new CellRangeAddressList(firstRow, lastRow, column, column)
        );
        validation.setShowErrorBox(true);
        validation.createErrorBox("Valor inválido", "Use uma das opções permitidas no modelo.");
        sheet.addValidationData(validation);
    }

    private void cell(Row row, int column, String value, CellStyle style) {
        var cell = row.createCell(column, CellType.STRING);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void numericCell(Row row, int column, BigDecimal value, CellStyle style) {
        var cell = row.createCell(column, CellType.NUMERIC);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private Map<String, Integer> headers(Row header) {
        if (header == null) throw new IllegalArgumentException("A aba Produtos está sem cabeçalho");
        var result = new LinkedHashMap<String, Integer>();
        for (var index = 0; index < header.getLastCellNum(); index++) {
            var key = normalize(text(header.getCell(index)));
            if (!key.isBlank()) result.put(key, index);
        }
        return result;
    }

    private void requireHeaders(Map<String, Integer> headers) {
        var required = List.of("descricao", "categoria", "unidade", "estoque minimo", "controla lote sim nao");
        var missing = required.stream().filter(header -> !headers.containsKey(header)).toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException("O cabeçalho do modelo foi alterado ou está incompleto");
        headers.putIfAbsent("codigo interno", -1);
        headers.putIfAbsent("estoque atual somente leitura", -1);
        headers.putIfAbsent("id do sistema nao editar", -1);
    }

    private boolean empty(Row row, Map<String, Integer> headers) {
        return headers.values().stream()
                .filter(index -> index >= 0)
                .distinct()
                .allMatch(index -> text(row, index).isBlank());
    }

    private String text(Row row, int index) {
        return index < 0 ? "" : text(row.getCell(index));
    }

    private String text(Cell cell) {
        if (cell == null) return "";
        return new DataFormatter(Locale.forLanguageTag("pt-BR")).formatCellValue(cell).trim();
    }

    private BigDecimal decimal(Row row, int index) {
        if (index < 0) return null;
        var cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        var value = text(cell).replace(" ", "").replace(",", ".");
        try {
            return value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Boolean bool(Row row, int index) {
        var value = normalize(text(row, index));
        if (Set.of("sim", "s", "yes", "true", "1").contains(value)) return true;
        if (Set.of("nao", "n", "no", "false", "0").contains(value)) return false;
        return null;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione a planilha preenchida");
        if (file.getSize() > MAX_IMPORT_BYTES) throw new IllegalArgumentException("A planilha deve ter no máximo 8 MB");
        var name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Envie um arquivo no formato XLSX");
        }
    }

    private List<MaterialCategory> activeCategories(String clinicId) {
        var categories = categoryRepository.findByClinicIdAndActiveTrueOrderByName(clinicId);
        if (categories.isEmpty()) {
            categoryService.list();
            categories = categoryRepository.findByClinicIdAndActiveTrueOrderByName(clinicId);
        }
        return categories;
    }

    private Set<String> duplicates(List<ParsedRow> rows, Function<ParsedRow, String> extractor) {
        var counts = new HashMap<String, Integer>();
        for (var row : rows) {
            var value = extractor.apply(row);
            if (value != null && !value.isBlank()) counts.merge(value, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private int count(List<ImportPreviewRow> rows, SuggestedAction action) {
        return Math.toIntExact(rows.stream().filter(row -> row.suggestedAction() == action).count());
    }

    private String normalizeNullable(String value) {
        var cleaned = clean(value);
        return cleaned == null ? null : normalize(cleaned);
    }

    private String skuKey(String value) {
        var cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ParsedRow(
            int rowNumber,
            String sourceId,
            String name,
            String categoryName,
            String sku,
            String unit,
            BigDecimal minimumStock,
            Boolean lotControlled,
            BigDecimal currentStock
    ) {}
}
