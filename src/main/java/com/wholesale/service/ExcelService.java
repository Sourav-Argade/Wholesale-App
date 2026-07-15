package com.wholesale.service;

import com.wholesale.model.*;
import com.wholesale.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class ExcelService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerPriceRepository customerPriceRepository;
    private final RouteRepository routeRepository;
    private final RouteCustomerRepository routeCustomerRepository;
    private final VisitLogRepository visitLogRepository;
    private final UserRepository userRepository;

    public ExcelService(CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        CustomerPriceRepository customerPriceRepository,
                        RouteRepository routeRepository,
                        RouteCustomerRepository routeCustomerRepository,
                        VisitLogRepository visitLogRepository,
                        UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.customerPriceRepository = customerPriceRepository;
        this.routeRepository = routeRepository;
        this.routeCustomerRepository = routeCustomerRepository;
        this.visitLogRepository = visitLogRepository;
        this.userRepository = userRepository;
    }

    public byte[] exportAllToExcel() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Sheet 1: Customers
            Sheet customerSheet = workbook.createSheet("Customers");
            writeCustomersSheet(customerSheet);

            // Sheet 2: Products
            Sheet productSheet = workbook.createSheet("Products");
            writeProductsSheet(productSheet);

            // Sheet 3: Price History
            Sheet priceSheet = workbook.createSheet("Price History");
            writePriceHistorySheet(priceSheet);

            // Sheet 4: Routes
            Sheet routeSheet = workbook.createSheet("Routes");
            writeRoutesSheet(routeSheet);

            // Sheet 5: Visit Logs
            Sheet visitSheet = workbook.createSheet("Visit Logs");
            writeVisitLogsSheet(visitSheet);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public void importCustomersFromExcel(InputStream inputStream) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet("Customers");
            if (sheet == null) return;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Customer customer = new Customer();
                customer.setName(getCellString(row, 0));
                customer.setShopName(getCellString(row, 1));
                customer.setPhone(getCellString(row, 2));
                customer.setAddress(getCellString(row, 3));
                customer.setLatitude(getCellDouble(row, 4));
                customer.setLongitude(getCellDouble(row, 5));
                customer.setNotes(getCellString(row, 6));
                customerRepository.save(customer);
            }
        }
    }

    private void writeCustomersSheet(Sheet sheet) {
        String[] headers = {"Name", "Shop Name", "Phone", "Address", "Latitude", "Longitude", "Notes"};
        writeHeaderRow(sheet, headers);

        List<Customer> customers = customerRepository.findAll();
        int rowNum = 1;
        for (Customer c : customers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getName());
            row.createCell(1).setCellValue(c.getShopName());
            row.createCell(2).setCellValue(c.getPhone());
            row.createCell(3).setCellValue(c.getAddress());
            if (c.getLatitude() != null) row.createCell(4).setCellValue(c.getLatitude());
            if (c.getLongitude() != null) row.createCell(5).setCellValue(c.getLongitude());
            row.createCell(6).setCellValue(c.getNotes());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void writeProductsSheet(Sheet sheet) {
        String[] headers = {"Name", "Unit", "Default Price"};
        writeHeaderRow(sheet, headers);

        List<Product> products = productRepository.findAll();
        int rowNum = 1;
        for (Product p : products) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getName());
            row.createCell(1).setCellValue(p.getUnit());
            if (p.getDefaultPrice() != null) row.createCell(2).setCellValue(p.getDefaultPrice().doubleValue());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void writePriceHistorySheet(Sheet sheet) {
        String[] headers = {"Customer ID", "Customer Name", "Product Name", "Price", "Effective Date", "Notes"};
        writeHeaderRow(sheet, headers);

        List<CustomerPrice> prices = customerPriceRepository.findAll();
        int rowNum = 1;
        for (CustomerPrice cp : prices) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(cp.getCustomerId());
            customerRepository.findById(cp.getCustomerId()).ifPresent(c ->
                row.createCell(1).setCellValue(c.getName()));
            productRepository.findById(cp.getProductId()).ifPresent(p ->
                row.createCell(2).setCellValue(p.getName()));
            row.createCell(3).setCellValue(cp.getPrice().doubleValue());
            if (cp.getEffectiveDate() != null)
                row.createCell(4).setCellValue(cp.getEffectiveDate().toString());
            row.createCell(5).setCellValue(cp.getNotes());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void writeRoutesSheet(Sheet sheet) {
        String[] headers = {"Route Name", "Description", "Customer Name (ordered)"};
        writeHeaderRow(sheet, headers);

        List<Route> routes = routeRepository.findAll();
        int rowNum = 1;
        for (Route r : routes) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getName());
            row.createCell(1).setCellValue(r.getDescription());

            List<RouteCustomer> rcs = routeCustomerRepository.findByRouteIdOrderByVisitOrderAsc(r.getId());
            StringBuilder customerNames = new StringBuilder();
            for (RouteCustomer rc : rcs) {
                if (customerNames.length() > 0) customerNames.append("; ");
                customerRepository.findById(rc.getCustomerId()).ifPresent(c ->
                    customerNames.append(c.getName()));
            }
            row.createCell(2).setCellValue(customerNames.toString());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void writeVisitLogsSheet(Sheet sheet) {
        String[] headers = {"Date", "Customer Name", "User", "Notes", "Latitude", "Longitude"};
        writeHeaderRow(sheet, headers);

        List<VisitLog> logs = visitLogRepository.findAll();
        int rowNum = 1;
        for (VisitLog v : logs) {
            Row row = sheet.createRow(rowNum++);
            if (v.getVisitedDate() != null)
                row.createCell(0).setCellValue(v.getVisitedDate().toString());
            customerRepository.findById(v.getCustomerId()).ifPresent(c ->
                row.createCell(1).setCellValue(c.getName()));
            userRepository.findById(v.getUserId()).ifPresent(u ->
                row.createCell(2).setCellValue(u.getDisplayName()));
            row.createCell(3).setCellValue(v.getNotes());
            if (v.getLatitude() != null) row.createCell(4).setCellValue(v.getLatitude());
            if (v.getLongitude() != null) row.createCell(5).setCellValue(v.getLongitude());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void writeHeaderRow(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String getCellString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        return cell == null ? null : cell.getStringCellValue();
    }

    private Double getCellDouble(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            return cell.getNumericCellValue();
        } catch (Exception e) {
            return null;
        }
    }
}
