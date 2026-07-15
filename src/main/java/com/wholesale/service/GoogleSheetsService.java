package com.wholesale.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.wholesale.model.*;
import com.wholesale.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Google Sheets integration for Wholesale App.
 * <p>
 * Google Sheet URL: https://docs.google.com/spreadsheets/d/1Z4W9vMRBoVUnRQ_wMF_ry7_6Gh5nc02_87KReNK-hFk/edit
 * Sheet name: "Sheet1"
 * <p>
 * Columns:
 * A - Customer Name
 * B - Shop Name
 * C - Phone
 * D - Address
 * E - Lat
 * F - Lng
 * G - Route
 * H - Product
 * I - Price
 * J - Date
 * K - Notes
 * <p>
 * Each row represents a transaction record. The data is denormalized — customer
 * and product info repeats across rows. This service reads the sheet to populate
 * the H2 database on startup, and writes to the sheet on every app operation.
 */
@Service
public class GoogleSheetsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Wholesale App";
    private static final String SHEET_RANGE = "Sheet1!A:K";
    private static final String VALUE_INPUT_OPTION = "USER_ENTERED";

    // Spreadsheet ID from the shared URL
    private static final String SPREADSHEET_ID = "1Z4W9vMRBoVUnRQ_wMF_ry7_6Gh5nc02_87KReNK-hFk";

    // Column indices (0-based)
    public static final int COL_CUSTOMER_NAME = 0;
    public static final int COL_SHOP_NAME = 1;
    public static final int COL_PHONE = 2;
    public static final int COL_ADDRESS = 3;
    public static final int COL_LAT = 4;
    public static final int COL_LNG = 5;
    public static final int COL_ROUTE = 6;
    public static final int COL_PRODUCT = 7;
    public static final int COL_PRICE = 8;
    public static final int COL_DATE = 9;
    public static final int COL_NOTES = 10;

    private static final int COLUMN_COUNT = 11;

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerPriceRepository priceRepository;
    private final RouteRepository routeRepository;
    private final RouteCustomerRepository routeCustomerRepository;
    private final VisitLogRepository visitLogRepository;
    private final UserRepository userRepository;

    private Sheets sheetsService;
    private boolean configured = false;
    private String lastError = null;

    @Value("${google.sheets.credentials.path:}")
    private String credentialsPath;

    @Value("${google.sheets.credentials.json:}")
    private String credentialsJson;

    public GoogleSheetsService(
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            CustomerPriceRepository priceRepository,
            RouteRepository routeRepository,
            RouteCustomerRepository routeCustomerRepository,
            VisitLogRepository visitLogRepository,
            UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
        this.routeRepository = routeRepository;
        this.routeCustomerRepository = routeCustomerRepository;
        this.visitLogRepository = visitLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Initialize the Google Sheets API client after Spring bootstraps.
     * Credentials can come from:
     * 1. Env var GOOGLE_SHEETS_CREDENTIALS_JSON (the JSON content as a string)
     * 2. A file path in GOOGLE_SHEETS_CREDENTIALS_PATH
     * 3. JVM system properties or application.properties
     */
    @PostConstruct
    public void init() {
        try {
            InputStream credentialsStream = null;

            // Try 1: JSON content from env var / property
            if (credentialsJson != null && !credentialsJson.isEmpty()) {
                credentialsStream = new ByteArrayInputStream(credentialsJson.getBytes());
                log.info("Using Google Sheets credentials from environment variable");
            }
            // Try 2: File path
            else if (credentialsPath != null && !credentialsPath.isEmpty()) {
                File credFile = new File(credentialsPath);
                if (credFile.exists()) {
                    credentialsStream = new FileInputStream(credFile);
                    log.info("Using Google Sheets credentials from file: {}", credentialsPath);
                }
            }
            // Try 3: System env var (fallback)
            else {
                String envJson = System.getenv("GOOGLE_SHEETS_CREDENTIALS_JSON");
                if (envJson != null && !envJson.isEmpty()) {
                    credentialsStream = new ByteArrayInputStream(envJson.getBytes());
                    log.info("Using Google Sheets credentials from system env var");
                }
                String envPath = System.getenv("GOOGLE_SHEETS_CREDENTIALS_PATH");
                if (credentialsStream == null && envPath != null && !envPath.isEmpty()) {
                    File credFile = new File(envPath);
                    if (credFile.exists()) {
                        credentialsStream = new FileInputStream(credFile);
                        log.info("Using Google Sheets credentials from system env var path: {}", envPath);
                    }
                }
            }

            if (credentialsStream == null) {
                log.warn("Google Sheets credentials not configured. Sheet sync is disabled. " +
                        "Set GOOGLE_SHEETS_CREDENTIALS_JSON or GOOGLE_SHEETS_CREDENTIALS_PATH env var.");
                return;
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets"));

            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

            sheetsService = new Sheets.Builder(transport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            configured = true;
            log.info("Google Sheets API client initialized successfully.");

            // Auto-load data from sheet into H2 on startup
            loadDataIntoH2();

        } catch (Exception e) {
            configured = false;
            lastError = e.getMessage();
            log.warn("Failed to initialize Google Sheets: {}", e.getMessage());
        }
    }

    // =====================================================
    //  READING FROM SHEET
    // =====================================================

    /**
     * Read all rows from the sheet. Returns a list of rows, where each row
     * is a list of cell values (as strings). Row 0 is the header.
     */
    public List<List<Object>> readAllRows() {
        if (!configured || sheetsService == null) {
            return Collections.emptyList();
        }
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, SHEET_RANGE)
                    .execute();
            return response.getValues() != null ? response.getValues() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error reading from Google Sheets: {}", e.getMessage());
            lastError = e.getMessage();
            return Collections.emptyList();
        }
    }

    /**
     * Get all data rows (excluding the header row).
     */
    public List<List<Object>> getDataRows() {
        List<List<Object>> allRows = readAllRows();
        if (allRows.size() <= 1) return Collections.emptyList();
        return allRows.subList(1, allRows.size());
    }

    // =====================================================
    //  WRITING TO SHEET
    // =====================================================

    /**
     * Append a new row to the sheet.
     *
     * @param values Array of 11 values matching the column order
     */
    public synchronized void appendRow(String[] values) {
        if (!configured || sheetsService == null) {
            log.warn("Google Sheets not configured, skipping write");
            return;
        }
        try {
            // Pad or trim to exactly COLUMN_COUNT
            List<Object> rowData = new ArrayList<>();
            for (int i = 0; i < COLUMN_COUNT; i++) {
                rowData.add(i < values.length && values[i] != null ? values[i] : "");
            }

            List<List<Object>> rows = Collections.singletonList(rowData);
            ValueRange body = new ValueRange().setValues(rows);

            AppendValuesResponse result = sheetsService.spreadsheets().values()
                    .append(SPREADSHEET_ID, SHEET_RANGE, body)
                    .setValueInputOption(VALUE_INPUT_OPTION)
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

            log.debug("Appended row to Google Sheets: {}", (Object) values);
        } catch (Exception e) {
            log.error("Error appending to Google Sheets: {}", e.getMessage());
            lastError = e.getMessage();
        }
    }

    // =====================================================
    //  DATA TRANSFER: SHEET → H2 (startup restore)
    // =====================================================

    /**
     * Load all data from the Google Sheet into H2 tables.
     * Called on startup. Only populates if the customers table is empty.
     */
    @Transactional
    public void loadDataIntoH2() {
        List<List<Object>> rows = getDataRows();
        if (rows.isEmpty()) {
            log.info("No data rows found in Google Sheet (or sheet not configured)");
            return;
        }

        // Only load if H2 is empty (avoid duplicate on every restart)
        if (customerRepository.count() > 0) {
            log.info("H2 database already has data, skipping sheet load");
            return;
        }

        log.info("Loading {} rows from Google Sheet into H2 database...", rows.size());

        // Track unique entities by name
        Map<String, Customer> customerMap = new LinkedHashMap<>();
        Map<String, Product> productMap = new LinkedHashMap<>();
        Map<String, Route> routeMap = new LinkedHashMap<>();
        // Track price records and visit logs per customer
        List<Object[]> priceRecords = new ArrayList<>();
        List<Object[]> visitRecords = new ArrayList<>();
        // Track customer-route associations
        Map<String, Set<String>> routeCustomerMap = new HashMap<>();

        for (List<Object> row : rows) {
            if (row.isEmpty()) continue;

            String customerName = getCell(row, COL_CUSTOMER_NAME);
            if (customerName.isEmpty()) continue;

            String shopName = getCell(row, COL_SHOP_NAME);
            String phone = getCell(row, COL_PHONE);
            String address = getCell(row, COL_ADDRESS);
            String latStr = getCell(row, COL_LAT);
            String lngStr = getCell(row, COL_LNG);
            String route = getCell(row, COL_ROUTE);
            String product = getCell(row, COL_PRODUCT);
            String priceStr = getCell(row, COL_PRICE);
            String date = getCell(row, COL_DATE);
            String notes = getCell(row, COL_NOTES);

            // --- Build Customers ---
            if (!customerMap.containsKey(customerName)) {
                Customer c = new Customer();
                c.setName(customerName);
                c.setShopName(shopName.isEmpty() ? null : shopName);
                c.setPhone(phone.isEmpty() ? null : phone);
                c.setAddress(address.isEmpty() ? null : address);
                try {
                    if (!latStr.isEmpty()) c.setLatitude(Double.parseDouble(latStr));
                    if (!lngStr.isEmpty()) c.setLongitude(Double.parseDouble(lngStr));
                } catch (NumberFormatException e) {
                    // ignore invalid lat/lng
                }
                c.setNotes(notes.isEmpty() ? null : notes);
                customerMap.put(customerName, c);
            }

            // --- Build Products ---
            if (!product.isEmpty() && !productMap.containsKey(product)) {
                Product p = new Product();
                p.setName(product);
                productMap.put(product, p);
            }

            // --- Build Routes ---
            if (!route.isEmpty() && !routeMap.containsKey(route)) {
                Route r = new Route();
                r.setName(route);
                routeMap.put(route, r);
            }

            // --- Track Customer ↔ Route association ---
            if (!route.isEmpty() && !customerName.isEmpty()) {
                routeCustomerMap.computeIfAbsent(route, k -> new LinkedHashSet<>()).add(customerName);
            }

            // --- Track Price Records ---
            if (!product.isEmpty() && !priceStr.isEmpty()) {
                priceRecords.add(new Object[]{customerName, product, priceStr, date, notes});
            }

            // --- Track Visit Records (rows with a date but no product) ---
            // A row with a date and no product/price is effectively a visit log
            // Also, rows with a date always represent a "record on that date"
            if (!date.isEmpty()) {
                visitRecords.add(new Object[]{customerName, date, notes});
            }
        }

        // --- Save to H2 ---

        // Save products first (needed for price references)
        Map<String, Product> savedProducts = new HashMap<>();
        for (Map.Entry<String, Product> entry : productMap.entrySet()) {
            Product saved = productRepository.save(entry.getValue());
            savedProducts.put(entry.getKey(), saved);
        }
        log.info("Imported {} products from sheet", savedProducts.size());

        // Save customers
        Map<String, Customer> savedCustomers = new HashMap<>();
        for (Map.Entry<String, Customer> entry : customerMap.entrySet()) {
            Customer saved = customerRepository.save(entry.getValue());
            savedCustomers.put(entry.getKey(), saved);
        }
        log.info("Imported {} customers from sheet", savedCustomers.size());

        // Save routes and route-customer associations
        for (Map.Entry<String, Route> entry : routeMap.entrySet()) {
            Route savedRoute = routeRepository.save(entry.getValue());
            Set<String> customerNames = routeCustomerMap.get(entry.getKey());
            if (customerNames != null) {
                int order = 1;
                for (String custName : customerNames) {
                    Customer cust = savedCustomers.get(custName);
                    if (cust != null) {
                        RouteCustomer rc = new RouteCustomer();
                        rc.setRouteId(savedRoute.getId());
                        rc.setCustomerId(cust.getId());
                        rc.setVisitOrder(order++);
                        routeCustomerRepository.save(rc);
                    }
                }
            }
        }
        log.info("Imported {} routes from sheet", routeMap.size());

        // Save price records
        int priceCount = 0;
        for (Object[] record : priceRecords) {
            String custName = (String) record[0];
            String prodName = (String) record[1];
            String priceVal = (String) record[2];
            String dateVal = (String) record[3];
            String notesVal = (String) record[4];

            Customer cust = savedCustomers.get(custName);
            Product prod = savedProducts.get(prodName);
            if (cust == null || prod == null) continue;

            CustomerPrice cp = new CustomerPrice();
            cp.setCustomerId(cust.getId());
            cp.setProductId(prod.getId());
            try {
                cp.setPrice(new BigDecimal(priceVal));
            } catch (NumberFormatException e) {
                continue;
            }
            cp.setEffectiveDate(parseDate(dateVal));
            cp.setNotes(notesVal.isEmpty() ? null : notesVal);
            priceRepository.save(cp);
            priceCount++;
        }
        log.info("Imported {} price records from sheet", priceCount);

        // Save visit records
        // Find the "admin" user ID for historical visits
        Long defaultUserId = 1L;
        try {
            Optional<com.wholesale.model.User> adminUser = userRepository.findByUsername("admin");
            if (adminUser.isPresent()) defaultUserId = adminUser.get().getId();
        } catch (Exception e) {
            // ignore
        }

        int visitCount = 0;
        for (Object[] record : visitRecords) {
            String custName = (String) record[0];
            String dateVal = (String) record[1];
            String notesVal = (String) record[2];

            Customer cust = savedCustomers.get(custName);
            if (cust == null) continue;

            VisitLog vl = new VisitLog();
            vl.setCustomerId(cust.getId());
            vl.setUserId(defaultUserId);
            LocalDate visitDate = parseDate(dateVal);
            if (visitDate == null) continue;

            // Avoid duplicate visits on the same day
            if (visitLogRepository.existsByCustomerIdAndVisitedDate(cust.getId(), visitDate)) continue;

            vl.setVisitedDate(visitDate);
            vl.setNotes(notesVal.isEmpty() ? null : notesVal);
            visitLogRepository.save(vl);
            visitCount++;
        }
        log.info("Imported {} visit records from sheet", visitCount);

        // Ensure default users exist (admin/partner)
        try {
            if (!userRepository.existsByUsername("admin")) {
                com.wholesale.model.User admin = new com.wholesale.model.User("admin",
                        com.wholesale.service.AuthService.hashPassword("admin123"), "ADMIN", "Owner");
                userRepository.save(admin);
            }
            if (!userRepository.existsByUsername("partner")) {
                com.wholesale.model.User partner = new com.wholesale.model.User("partner",
                        com.wholesale.service.AuthService.hashPassword("partner123"), "PARTNER", "Partner");
                userRepository.save(partner);
            }
        } catch (Exception e) {
            log.warn("Could not create default users: {}", e.getMessage());
        }

        log.info("✅ Google Sheet data loaded into H2 successfully!");
    }

    // =====================================================
    //  DATA TRANSFER: APP → SHEET (live sync)
    // =====================================================

    /**
     * Sync a customer creation/update to Google Sheets.
     */
    public void syncCustomer(String customerName, String shopName, String phone,
                             String address, String lat, String lng, String route, String notes) {
        appendRow(new String[]{
                customerName,
                shopName != null ? shopName : "",
                phone != null ? phone : "",
                address != null ? address : "",
                lat != null ? lat : "",
                lng != null ? lng : "",
                route != null ? route : "",
                "",  // Product
                "",  // Price
                LocalDate.now().toString(), // Date (created/updated on this date)
                notes != null ? notes : ""
        });
    }

    /**
     * Sync a price setting to Google Sheets.
     */
    public void syncPrice(String customerName, String shopName, String productName,
                          BigDecimal price, LocalDate date, String notes) {
        appendRow(new String[]{
                customerName,
                shopName != null ? shopName : "",
                "",  // Phone
                "",  // Address
                "",  // Lat
                "",  // Lng
                "",  // Route
                productName,
                price != null ? price.toString() : "",
                date != null ? date.toString() : LocalDate.now().toString(),
                notes != null ? notes : ""
        });
    }

    /**
     * Sync a visit check-in to Google Sheets.
     */
    public void syncCheckIn(String customerName, String shopName, LocalDate date,
                            String notes, String lat, String lng) {
        appendRow(new String[]{
                customerName,
                shopName != null ? shopName : "",
                "",  // Phone
                "",  // Address
                lat != null ? lat : "",
                lng != null ? lng : "",
                "",  // Route
                "",  // Product
                "",  // Price
                date != null ? date.toString() : LocalDate.now().toString(),
                notes != null ? notes : "✅ Visit check-in"
        });
    }

    /**
     * Sync a route assignment to Google Sheets.
     */
    public void syncRouteAssignment(String customerName, String routeName) {
        appendRow(new String[]{
                customerName,
                "", "", "", "", "",
                routeName,
                "", "", "",
                "Assigned to route"
        });
    }

    /**
     * Sync full data snapshot to Google Sheets (clear + rewrite all rows).
     * This is useful for a full export/backup.
     */
    public void exportFullSnapshot() {
        if (!configured || sheetsService == null) return;

        try {
            // Build all rows
            List<List<Object>> allRows = new ArrayList<>();

            // Header row
            allRows.add(Arrays.asList(
                    "Customer Name", "Shop Name", "Phone", "Address",
                    "Lat", "Lng", "Route", "Product", "Price", "Date", "Notes"
            ));

            // Customer rows
            List<Customer> customers = customerRepository.findAll();
            Map<Long, String> customerRouteMap = new HashMap<>();
            List<Route> routes = routeRepository.findAll();
            for (Route route : routes) {
                List<RouteCustomer> rcs = routeCustomerRepository.findByRouteIdOrderByVisitOrderAsc(route.getId());
                for (RouteCustomer rc : rcs) {
                    customerRouteMap.put(rc.getCustomerId(), route.getName());
                }
            }

            for (Customer c : customers) {
                String routeName = customerRouteMap.getOrDefault(c.getId(), "");
                allRows.add(Arrays.asList(
                        c.getName() != null ? c.getName() : "",
                        c.getShopName() != null ? c.getShopName() : "",
                        c.getPhone() != null ? c.getPhone() : "",
                        c.getAddress() != null ? c.getAddress() : "",
                        c.getLatitude() != null ? c.getLatitude().toString() : "",
                        c.getLongitude() != null ? c.getLongitude().toString() : "",
                        routeName,
                        "", "", "",
                        c.getNotes() != null ? c.getNotes() : ""
                ));
            }

            // Price rows
            List<CustomerPrice> prices = priceRepository.findAll();
            for (CustomerPrice cp : prices) {
                String custName = "";
                String prodName = "";
                if (customerRepository.findById(cp.getCustomerId()).isPresent())
                    custName = customerRepository.findById(cp.getCustomerId()).get().getName();
                if (productRepository.findById(cp.getProductId()).isPresent())
                    prodName = productRepository.findById(cp.getProductId()).get().getName();

                allRows.add(Arrays.asList(
                        custName, "", "", "", "", "", "",
                        prodName,
                        cp.getPrice() != null ? cp.getPrice().toString() : "",
                        cp.getEffectiveDate() != null ? cp.getEffectiveDate().toString() : "",
                        cp.getNotes() != null ? cp.getNotes() : ""
                ));
            }

            // Visit rows
            List<VisitLog> visits = visitLogRepository.findAll();
            for (VisitLog v : visits) {
                String custName = "";
                if (customerRepository.findById(v.getCustomerId()).isPresent())
                    custName = customerRepository.findById(v.getCustomerId()).get().getName();

                allRows.add(Arrays.asList(
                        custName, "", "", "",
                        v.getLatitude() != null ? v.getLatitude().toString() : "",
                        v.getLongitude() != null ? v.getLongitude().toString() : "",
                        "", "", "",
                        v.getVisitedDate() != null ? v.getVisitedDate().toString() : "",
                        v.getNotes() != null ? "✅ " + v.getNotes() : "✅ Visit"
                ));
            }

            // Clear sheet and rewrite
            String clearRange = "Sheet1!A:K";
            sheetsService.spreadsheets().values().clear(SPREADSHEET_ID, clearRange,
                    new com.google.api.services.sheets.v4.model.ClearValuesRequest()).execute();

            // Write all rows in one batch
            ValueRange body = new ValueRange().setValues(allRows);
            sheetsService.spreadsheets().values()
                    .update(SPREADSHEET_ID, "Sheet1!A1", body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            log.info("✅ Exported full data snapshot to Google Sheets ({} rows)", allRows.size());
        } catch (Exception e) {
            log.error("Error exporting full snapshot: {}", e.getMessage());
            lastError = e.getMessage();
        }
    }

    // =====================================================
    //  STATUS
    // =====================================================

    public boolean isConfigured() {
        return configured;
    }

    public String getLastError() {
        return lastError;
    }

    public String getSpreadsheetId() {
        return SPREADSHEET_ID;
    }

    public String getSpreadsheetUrl() {
        return "https://docs.google.com/spreadsheets/d/" + SPREADSHEET_ID + "/edit";
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", configured);
        status.put("spreadsheetUrl", getSpreadsheetUrl());
        status.put("spreadsheetId", SPREADSHEET_ID);
        status.put("lastError", lastError);
        if (configured) {
            int rowCount = getDataRows().size();
            status.put("rowCount", rowCount);
        }
        return status;
    }

    // =====================================================
    //  HELPERS
    // =====================================================

    private String getCell(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) return "";
        return row.get(index).toString().trim();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        // Try common formats
        String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd"};
        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        return null;
    }
}
