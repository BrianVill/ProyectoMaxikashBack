package com.loki.estructuraUsuarios.Service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.github.pjfanning.xlsx.StreamingReader;

import java.io.*;
import java.util.*;

@Component
public class ExcelClientesHelper {

    private static final String EXCEL_CREDITOS = "/tmp/uploads/Clientes.xlsx";

    
    /**
     * Sobrescribe la columna “Gestor” en el archivo /tmp/uploads/Clientes.xlsx
     * usando dos mapas ya preparados:
     *   – creditoId  → puestoId
     *   – puestoId   → nombreGestor
     */
    public void actualizarGestorColumn(Map<String, UUID> puestoByCred,
                                       Map<UUID, String> nombreGestor) throws IOException {

        File xls = new File(EXCEL_CREDITOS);
        if (!xls.exists()) return;                         // nada que hacer

        try (FileInputStream fis = new FileInputStream(xls);
             Workbook         wb  = StreamingReader.builder()
                                   .rowCacheSize(100)
                                   .bufferSize(4096)
                                   .open(fis);
             Workbook         out = new SXSSFWorkbook();   // destino en memoria
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet src = wb.getSheetAt(0);
            Sheet dst = out.createSheet(src.getSheetName());

            /* ---------- copiar encabezado ---------- */
            Iterator<Row> rowIt = src.iterator();
            if (!rowIt.hasNext()) return;
            Row srcHeader = rowIt.next();
            Row dstHeader = dst.createRow(0);

            int colIdCred = -1, colGestor = -1;
            for (Iterator<Cell> cit = srcHeader.cellIterator(); cit.hasNext();) {
                Cell sc = cit.next();
                Cell dc = dstHeader.createCell(sc.getColumnIndex(), sc.getCellType());
                dc.setCellValue(sc.getStringCellValue());

                String v = sc.getStringCellValue().trim();
                if ("u_ID_Credito".equalsIgnoreCase(v)) colIdCred  = sc.getColumnIndex();
                if ("Gestor".equalsIgnoreCase(v))       colGestor  = sc.getColumnIndex();
            }
            if (colIdCred < 0 || colGestor < 0)
                throw new IllegalStateException(
                        "El XLS maestro no tiene las columnas esperadas [u_ID_Credito, Gestor]");

            /* ---------- recorrer filas con iterador explícito ---------- */
            int dstRowNum = 1;
            while (rowIt.hasNext()) {
                Row sr = rowIt.next();

                Row dr = dst.createRow(dstRowNum++);

                // Copiar toda la fila
                for (Iterator<Cell> cit = sr.cellIterator(); cit.hasNext();) {
                    Cell sc = cit.next();
                    Cell dc = dr.createCell(sc.getColumnIndex(), sc.getCellType());
                    if (sc.getCellType() == CellType.STRING)
                        dc.setCellValue(sc.getStringCellValue());
                    else if (sc.getCellType() == CellType.NUMERIC)
                        dc.setCellValue(sc.getNumericCellValue());
                }

                /* --- sobrescribir columna Gestor --- */
                String credId = getCellString(sr.getCell(colIdCred));
                if (credId != null && puestoByCred.containsKey(credId)) {
                    UUID   puestoId = puestoByCred.get(credId);
                    String gestor   = nombreGestor.getOrDefault(puestoId, "");
                    dr.getCell(colGestor, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                      .setCellValue(gestor);
                }
            }

            /* ---------- guardar ---------- */
            out.write(bos);
            try (FileOutputStream fos = new FileOutputStream(xls)) {
                fos.write(bos.toByteArray());
            }
        }
    }

    private int findOrCreateColumn(Row header, String name) {
        for (Cell c : header)
            if (name.equalsIgnoreCase(c.getStringCellValue()))
                return c.getColumnIndex();
        int idx = header.getLastCellNum();
        header.createCell(idx).setCellValue(name);
        return idx;
    }

    private String getCellString(Cell c) {
        if (c == null)
            return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> c.getCellFormula();
            default -> null;
        };
    }

    private void setCellString(Row row, int col, String val) {
        Cell c = row.getCell(col);
        if (c == null)
            c = row.createCell(col);
        c.setCellValue(val);
    }
}
