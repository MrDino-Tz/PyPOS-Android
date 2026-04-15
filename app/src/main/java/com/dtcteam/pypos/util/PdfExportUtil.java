package com.dtcteam.pypos.util;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PdfExportUtil {

    public static void exportItems(Context context, List<Map<String, Object>> items) {
        if (items.isEmpty()) {
            Toast.makeText(context, "No items to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "items_export_" + System.currentTimeMillis() + ".pdf";
            File file = getOutputFile(context, fileName);
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Items Export")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 2, 2, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));

            String[] headers = {"SKU", "Name", "Category", "Unit Price", "Cost", "Stock", "Min"};
            for (String header : headers) {
                table.addHeaderCell(createHeaderCell(header));
            }

            for (Map<String, Object> item : items) {
                table.addCell(createCell(String.valueOf(item.get("sku") != null ? item.get("sku") : "")));
                table.addCell(createCell(String.valueOf(item.get("name") != null ? item.get("name") : "")));
                table.addCell(createCell(String.valueOf(item.get("category") != null ? item.get("category") : "")));
                table.addCell(createCell(String.valueOf(item.get("unit_price") != null ? item.get("unit_price") : "")));
                table.addCell(createCell(String.valueOf(item.get("cost_price") != null ? item.get("cost_price") : "")));
                table.addCell(createCell(String.valueOf(item.get("quantity") != null ? item.get("quantity") : "0")));
                table.addCell(createCell(String.valueOf(item.get("min_stock_level") != null ? item.get("min_stock_level") : "0")));
            }

            document.add(table);
            document.add(new Paragraph("Total Items: " + items.size())
                .setFontSize(10)
                .setMarginTop(10));

            document.close();
            
            openPdf(context, file);
            Toast.makeText(context, "Exported to " + fileName, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportCategories(Context context, List<Map<String, Object>> categories) {
        if (categories.isEmpty()) {
            Toast.makeText(context, "No categories to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "categories_export_" + System.currentTimeMillis() + ".pdf";
            File file = getOutputFile(context, fileName);
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Categories Export")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 5}));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(createHeaderCell("Name"));
            table.addHeaderCell(createHeaderCell("Description"));

            for (Map<String, Object> category : categories) {
                table.addCell(createCell(String.valueOf(category.get("name") != null ? category.get("name") : "")));
                table.addCell(createCell(String.valueOf(category.get("description") != null ? category.get("description") : "")));
            }

            document.add(table);
            document.add(new Paragraph("Total Categories: " + categories.size())
                .setFontSize(10)
                .setMarginTop(10));

            document.close();
            
            openPdf(context, file);
            Toast.makeText(context, "Exported to " + fileName, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportReports(Context context, Map<String, Object> dailyData, Map<String, Object> monthlyData, double totalRevenue, int totalTransactions) {
        try {
            String fileName = "reports_export_" + System.currentTimeMillis() + ".pdf";
            File file = getOutputFile(context, fileName);
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Sales Reports Export")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

            document.add(new Paragraph("Summary")
                .setFontSize(14)
                .setBold()
                .setMarginTop(10));
            
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{2, 2}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));
            
            summaryTable.addCell(createLabelCell("Total Revenue:"));
            summaryTable.addCell(createCell(String.format("TZS %.2f", totalRevenue)));
            summaryTable.addCell(createLabelCell("Total Transactions:"));
            summaryTable.addCell(createCell(String.valueOf(totalTransactions)));
            
            document.add(summaryTable);
            
            document.add(new Paragraph("\nDaily Sales")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));
            
            Table dailyTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2, 2}));
            dailyTable.setWidth(UnitValue.createPercentValue(100));
            dailyTable.addHeaderCell(createHeaderCell("Date"));
            dailyTable.addHeaderCell(createHeaderCell("Amount"));
            dailyTable.addHeaderCell(createHeaderCell("Items"));
            dailyTable.addHeaderCell(createHeaderCell("Status"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dailySales = (List<Map<String, Object>>) dailyData.get("sales");
            if (dailySales != null) {
                for (Map<String, Object> sale : dailySales) {
                    dailyTable.addCell(createCell(String.valueOf(sale.get("date") != null ? sale.get("date") : "")));
                    dailyTable.addCell(createCell(String.format("TZS %.2f", sale.get("amount") != null ? (Double) sale.get("amount") : 0.0)));
                    dailyTable.addCell(createCell(String.valueOf(sale.get("items") != null ? sale.get("items") : "0")));
                    dailyTable.addCell(createCell(String.valueOf(sale.get("status") != null ? sale.get("status") : "completed")));
                }
            }
            document.add(dailyTable);

            document.close();
            
            openPdf(context, file);
            Toast.makeText(context, "Exported to " + fileName, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static Cell createHeaderCell(String text) {
        return new Cell()
            .add(new Paragraph(text).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5);
    }

    private static Cell createCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(4);
    }

    private static Cell createLabelCell(String text) {
        return new Cell()
            .add(new Paragraph(text).setBold())
            .setTextAlignment(TextAlignment.LEFT)
            .setPadding(4);
    }

    private static File getOutputFile(Context context, String fileName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pyposDir = new File(downloadsDir, "PyPOS");
        if (!pyposDir.exists()) {
            pyposDir.mkdirs();
        }
        return new File(pyposDir, fileName);
    }

    public static void saveToDownloads(Context context, File file, String fileName) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(android.net.Uri.fromFile(file));
            context.sendBroadcast(intent);
            Toast.makeText(context, "Saved to Downloads/PyPOS/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}