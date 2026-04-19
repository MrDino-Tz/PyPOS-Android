package com.dtcteam.pypos.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.dtcteam.pypos.R;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
            String fileName = "Pawin_Stationery_Items_" + System.currentTimeMillis() + ".pdf";
            File file = getOutputFile(context, fileName);
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

addLogosWithTitle(document, context, "Pawin Stationery - Items");
            
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
            
            saveToDownloads(context, file, fileName);
            
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
            String fileName = "Pawin_Stationery_Categories_" + System.currentTimeMillis() + ".pdf";
            File file = getOutputFile(context, fileName);
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

addLogosWithTitle(document, context, "Pawin Stationery - Categories");
            
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
            
            saveToDownloads(context, file, fileName);
            
        } catch (Exception e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportReports(Context context, Map<String, Object> dailyData, Map<String, Object> monthlyData, double totalRevenue, int totalTransactions) {
        try {
            String fileName = "Pawin_Stationery_Report_" + System.currentTimeMillis() + ".pdf";
            File file = getOutputFile(context, fileName);
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

addLogosWithTitle(document, context, "Pawin Stationery Report");
            
            document.add(new Paragraph("\n"));
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
            
Table dailyTable = new Table(UnitValue.createPercentArray(new float[]{20, 25, 20, 20, 20}));
            dailyTable.setWidth(UnitValue.createPercentValue(100));
            dailyTable.addHeaderCell(createHeaderCell("Receipt #"));
            dailyTable.addHeaderCell(createHeaderCell("Item Name"));
            dailyTable.addHeaderCell(createHeaderCell("Unit Price"));
            dailyTable.addHeaderCell(createHeaderCell("Qty"));
            dailyTable.addHeaderCell(createHeaderCell("Amount"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dailySales = (List<Map<String, Object>>) dailyData.get("sales");
            if (dailySales != null && !dailySales.isEmpty()) {
                int receiptNum = 1;
                for (Map<String, Object> sale : dailySales) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> saleItems = (List<Map<String, Object>>) sale.get("items");
                    if (saleItems != null && !saleItems.isEmpty()) {
                        for (Map<String, Object> item : saleItems) {
                            dailyTable.addCell(createCell(receiptNum == 1 ? "#" + String.valueOf(sale.get("id")) : ""));
                            dailyTable.addCell(createCell(String.valueOf(item.get("name") != null ? item.get("name") : "-")));
                            
                            Object priceObj = item.get("price");
                            double price = 0.0;
                            if (priceObj != null) {
                                if (priceObj instanceof Double) price = (Double) priceObj;
                                else if (priceObj instanceof Integer) price = ((Integer) priceObj).doubleValue();
                                else if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();
                            }
                            dailyTable.addCell(createCell(String.format("TZS %.2f", price)));
                            
                            Object qtyObj = item.get("quantity");
                            int qty = 0;
                            if (qtyObj != null) {
                                if (qtyObj instanceof Integer) qty = (Integer) qtyObj;
                                else if (qtyObj instanceof Number) qty = ((Number) qtyObj).intValue();
                            }
                            dailyTable.addCell(createCell(String.valueOf(qty)));
                            
                            Object subtotalObj = item.get("subtotal");
                            double subtotal = 0.0;
                            if (subtotalObj != null) {
                                if (subtotalObj instanceof Double) subtotal = (Double) subtotalObj;
                                else if (subtotalObj instanceof Integer) subtotal = ((Integer) subtotalObj).doubleValue();
                                else if (subtotalObj instanceof Number) subtotal = ((Number) subtotalObj).doubleValue();
                            }
                            dailyTable.addCell(createCell(String.format("TZS %.2f", subtotal)));
                            
                            receiptNum++;
                        }
                    }
                }
            } else {
                dailyTable.addCell(createCell("- No sales data -"));
            }
            document.add(dailyTable);

            document.close();
            
            saveToDownloads(context, file, fileName);
            
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
            
            openPdf(context, file);
            
            Toast.makeText(context, "Saved to Downloads/PyPOS/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private static void openPdf(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(uri, "application/pdf");
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(viewIntent);
        } catch (Exception e) {
            // Silently fail if no PDF viewer available
        }
    }

    private static void addLogosWithTitle(Document document, Context context, String title) {
        try {
            try {
                Bitmap pawinBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pawin_logo);
                if (pawinBitmap != null) {
                    java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                    pawinBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    Image pawinImage = new Image(ImageDataFactory.create(byteArray));
                    pawinImage.setWidth(UnitValue.createPercentValue(20));
                    pawinImage.setHeight(UnitValue.createPercentValue(20));
                    
                    Paragraph logoPara = new Paragraph().add(pawinImage);
                    logoPara.setTextAlignment(TextAlignment.CENTER);
                    document.add(logoPara);
                }
            } catch (Exception e) {
                // silently fail
            }
            
            Paragraph titlePara = new Paragraph(title).setFontSize(11).setBold().setTextAlignment(TextAlignment.CENTER);
            titlePara.setMarginTop(2f);
            titlePara.setMarginBottom(1f);
            document.add(titlePara);
            
            document.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1));
        } catch (Exception e) {
            document.add(new Paragraph(title).setFontSize(11).setBold().setTextAlignment(TextAlignment.CENTER));
        }
    }
}