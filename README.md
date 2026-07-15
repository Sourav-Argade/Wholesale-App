# 📦 Wholesale App

A **Route & Customer Management App** for wholesale businessmen who travel fixed sales routes and sell products to shopkeepers/customers.

Built with **Java + Spring Boot** (backend) and a **mobile-responsive HTML/CSS/JS** frontend using Leaflet.js + OpenStreetMap for free map features.

---

## 📖 App Overview

### What Problem Does This Solve?

If you run a wholesale business and visit different shopkeepers on fixed routes, you've probably faced these problems:

- 🧠 Forgetting customer names, shop locations, or which route they're on
- 💰 Not remembering the last rate you quoted a customer for each product
- 📋 Manually tracking daily visits with pen and paper
- 🤝 Needing to share data with partners who ride along on different routes

This app solves all of that — **in one place, on your own laptop, no internet required**. You and your partners access it from your phones over your local Wi-Fi/hotspot.

---

## 🧭 How the App Works (End-to-End Workflow)

### 1. First-Time Setup

You (the Admin) start by setting up your master data:

```
[Login as Admin] → [Add Products] → [Add Customers] → [Create Routes] → [Assign Customers to Routes]
```

- **Products**: What you sell (e.g., "Cooking Oil - 15kg tin" at ₹2,450 default price)
- **Customers**: Who you sell to (shop name, phone, GPS location — pinned right from your phone)
- **Routes**: How you organize visits (e.g., "Monday - Sector 5", "Tuesday - Downtown")
- **Route ordering**: Arrange customers in the order you visit them

### 2. Daily Use (In the Field)

When you or your partners head out for the day:

```
[Login on Phone] → [Open Route View] → [See customers & their latest prices] → [Check-in at each shop] → [End of day]
```

**Before leaving**: Open the route to see all customers, their addresses, and last quoted prices.
**At each shop**:
  1. Open the **Map View** to navigate to the shop's GPS location
  2. Tap **Check In** — the app logs the visit with date/time and GPS coordinates
  3. Set a **new price** for any product if the rate changed today (old rate stays in history)

### 3. End of Day / Reporting

- **Dashboard** shows today's visits at a glance
- **Price History** lets you review what you charged each customer
- **Excel Export** gives you a complete backup file you can save on Google Drive, email to yourself, or print

### 4. Multi-User Access

| Role | Can Do |
|---|---|
| **Admin** (you) | Add/edit/delete everything — customers, products, routes, prices |
| **Partner** (your staff) | View all data + check in visits only (cannot modify master data) |

Partners connect from their phone browser via your laptop's local IP (e.g., `http://192.168.1.5:8080`).

### 5. Data Safety

- All data lives in an **H2 file database** on your laptop (`./data/wholesale.mv.db`)
- **Export to Excel** anytime for offline backup
- **Import from Excel** to bulk-load existing customer data
- The app is **not deployed to any cloud** — privacy is guaranteed

---

## 🏗️ Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────┐
│  PHONE BROWSER (any phone on same Wi-Fi)                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Single Page App (HTML/CSS/JS)                      │   │
│  │  • Login page    • Dashboard    • Customer mgmt    │   │
│  │  • Product mgmt  • Route mgmt   • Map (Leaflet)    │   │
│  │  • Check-in      • Price history • Excel backup    │   │
│  └──────────┬──────────────────────────────────────────┘   │
└─────────────┼───────────────────────────────────────────────┘
              │ HTTP (REST API) over local network
              ▼
┌─────────────────────────────────────────────────────────────┐
│  YOUR LAPTOP (Spring Boot on port 8080)                     │
│  ┌─────────┐  ┌──────────┐  ┌────────────┐  ┌──────────┐   │
│  │Controllers│→│ Services │→│ Repositories│→│  H2 DB   │   │
│  │(REST API) │  │(Business │  │ (Data      │  │(file)    │   │
│  │           │  │  Logic)  │  │  Access)   │  │          │   │
│  └─────────┘  └──────────┘  └────────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
              ┌──────────────────────────────┐
              │  📁 ./data/wholesale.mv.db    │
              │  📁 ./wholesale_backup.xlsx    │
              └──────────────────────────────┘
```

---

## Summary of User Journeys

| 👤 Who | 🎯 Goal | 🚶 Steps in the App |
|---|---|---|
| **Admin** | Set up products | Login → Products → Add Product (name, unit, price) |
| **Admin** | Add a new customer | Login → Customers → Add Customer → fill details → 📍 Pin GPS → Save |
| **Admin** | Create a daily route | Login → Routes → Create Route → Add Customers (in visit order) |
| **Admin** | Record a price quote | Login → Customers → tap customer → Set Price → select product, enter rate → Save |
| **Either** | Check in at a shop | Login → Check-in → find customer → tap Check In → auto-captures GPS |
| **Either** | See map of today's route | Login → Routes → tap route → see customers listed → Map View for pins |
| **Either** | Export backup | Login → Backup/Excel → Export to Excel → downloads `.xlsx` file |
| **Partner** | See today's visits | Login → Dashboard → view today's check-in activity |

---

## Features

| Feature | Description |
|---|---|
| **Customer Management** | Add/edit/delete customers with name, shop name, phone, address, GPS location |
| **Product Catalog** | Maintain products with units and default prices |
| **Price History** | Track per-customer, per-product pricing with full history (never overwritten) |
| **Routes** | Group customers into named routes with visit ordering |
| **Map View** | See all customers on an interactive OpenStreetMap, color-coded by route |
| **Visit Check-in** | One-tap check-in when you visit a shop, with GPS location capture |
| **Multi-user** | Admin and Partner roles with simple username/password login |
| **Search** | Search customers by name or shop name |
| **Excel Backup** | Export all data to Excel; import customers from Excel |
| **Local Network** | Partners connect from their phones over Wi-Fi/hotspot — no internet needed |

## Quick Start

### Prerequisites

- **Java 17** or later ([Download](https://adoptium.net/))
- **Maven** (or use the Maven Wrapper — automatically downloaded on first run)

### Run the App

```bash
# Clone the repository
git clone https://github.com/Sourav-Argade/Wholesale-App.git
cd Wholesale-App

# Build and start (Linux/macOS)
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run

# Or if you have Maven installed globally
mvn spring-boot:run
```

The app starts at **http://localhost:8080**

### Default Login Credentials

| Username | Password | Role | Description |
|---|---|---|---|
| `admin` | `admin123` | **Admin** | Full access — add/edit/delete everything |
| `partner` | `partner123` | **Partner** | View + add visit logs only |

> **Important:** Change the default passwords after first login! Edit the `AuthService.java` `createDefaultUsers()` method or use the H2 console.

## How Partners Connect (Local Network)

1. **Find your laptop's local IP address:**
   - **Windows:** Open Command Prompt → type `ipconfig` → look for `IPv4 Address` (e.g., `192.168.1.5`)
   - **macOS/Linux:** Open Terminal → type `ifconfig` or `ip addr` → look for `inet` (e.g., `192.168.1.5`)

2. **Make sure your laptop and phones are on the same Wi-Fi/hotspot network.**

3. **Partners open this URL on their phone browser:**
   ```
   http://<YOUR_LAPTOP_IP>:8080
   ```
   Example: `http://192.168.1.5:8080`

4. **Log in with the partner credentials.**

> 💡 **Tip:** If your laptop's firewall blocks connections, allow inbound traffic on port 8080 (TCP).

## Project Structure

```
wholesale-app/
├── pom.xml                          # Maven build file
├── src/
│   ├── main/
│   │   ├── java/com/wholesale/
│   │   │   ├── WholesaleApplication.java    # Main entry point
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java       # Spring Security + token auth
│   │   │   │   └── WebConfig.java            # CORS + default data init
│   │   │   ├── model/
│   │   │   │   ├── Customer.java             # Customer entity
│   │   │   │   ├── Product.java              # Product entity
│   │   │   │   ├── CustomerPrice.java        # Price history entity
│   │   │   │   ├── Route.java                # Route entity
│   │   │   │   ├── RouteCustomer.java        # Route-customer mapping
│   │   │   │   ├── VisitLog.java             # Visit check-in entity
│   │   │   │   └── User.java                 # User entity
│   │   │   ├── repository/                   # Spring Data JPA repositories
│   │   │   ├── service/                      # Business logic
│   │   │   ├── controller/                   # REST API endpoints
│   │   │   └── dto/                          # Data Transfer Objects
│   │   └── resources/
│   │       ├── application.properties        # App configuration
│   │       └── static/                       # Frontend files
│   │           ├── index.html                # Main HTML page
│   │           ├── css/app.css               # Mobile-responsive styles
│   │           └── js/app.js                 # SPA JavaScript
│   └── test/
└── data/                                     # H2 database files (auto-created)
```

## Database Schema (Auto-created)

The app uses **H2 Database in file mode** — no separate database server needed. Tables are created automatically on first run.

| Table | Purpose |
|---|---|
| `users` | Admin/Partner login credentials |
| `customers` | Customer info with GPS coordinates |
| `products` | Product catalog with units & default prices |
| `customer_prices` | Full price history per customer per product |
| `routes` | Named sales routes |
| `route_customers` | Which customers belong to which route (ordered) |
| `visit_logs` | Daily check-in records |

### Why H2 (not Excel) as the Live Database?

Excel is **great for backup/export/offline copies**, but not suitable as a live multi-user database:

| Requirement | H2 Database | Excel File |
|---|---|---|
| **Concurrent writes** (2+ partners logging visits) | ✅ Handles safely | ❌ File corruption / data loss |
| **Relationships** (customer → prices → routes) | ✅ SQL JOINs | ❌ No relational model |
| **Querying** ("show all customers on Monday route") | ✅ Instant | ❌ Manual filtering |
| **Data integrity** (no duplicate prices) | ✅ Constraints | ❌ Easy to break |
| **Backup** | ✅ Export to Excel anytime | ✅ Already Excel format |

You can always **Export to Excel** from the app's Backup page for safe offline storage.

## REST API Endpoints

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Login (returns token) |
| POST | `/api/auth/logout` | Logout (invalidate token) |

### Customers
| Method | Path | Description |
|---|---|---|
| GET | `/api/customers` | List all customers |
| GET | `/api/customers/{id}` | Get customer details |
| GET | `/api/customers/search?q=...` | Search customers |
| POST | `/api/customers` | Create customer |
| PUT | `/api/customers/{id}` | Update customer |
| DELETE | `/api/customers/{id}` | Delete customer |

### Products
| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List products |
| POST | `/api/products` | Create product |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

### Prices
| Method | Path | Description |
|---|---|---|
| GET | `/api/prices?customerId=...` | Get price history |
| POST | `/api/prices` | Set a new price (keeps history) |
| DELETE | `/api/prices/{id}` | Delete a price record |

### Routes
| Method | Path | Description |
|---|---|---|
| GET | `/api/routes` | List routes |
| POST | `/api/routes` | Create route |
| PUT | `/api/routes/{id}` | Update route |
| DELETE | `/api/routes/{id}` | Delete route |
| POST | `/api/routes/{id}/customers` | Add customer to route |
| DELETE | `/api/routes/{id}/customers/{cId}` | Remove customer from route |

### Visits
| Method | Path | Description |
|---|---|---|
| GET | `/api/visits/today` | Today's visits |
| GET | `/api/visits/customer/{id}` | Visit history for a customer |
| POST | `/api/visits/checkin` | Check in at a customer |

### Excel
| Method | Path | Description |
|---|---|---|
| GET | `/api/excel/export` | Download all data as Excel |
| POST | `/api/excel/import` | Import customers from Excel |

## GitHub — For Backup/Version Control Only

This repository on GitHub is for **source code backup and version control** only. The app runs **locally on your laptop** — it is not deployed to any cloud service.

```bash
# After making changes locally, push to GitHub for backup
git add .
git commit -m "Describe your changes"
git push origin main
```

## Advanced Options

### Run with Docker (Optional)

1. **Install Docker** on your laptop
2. Build and run:
   ```bash
   docker build -t wholesale-app .
   docker run -p 8080:8080 -v ./data:/app/data wholesale-app
   ```

### Access H2 Console (for debugging)

Open `http://localhost:8080/h2-console` in your browser.

- **JDBC URL:** `jdbc:h2:file:./data/wholesale`
- **User:** `sa`
- **Password:** *(leave blank)*

### Changing Server Port

Edit `src/main/resources/application.properties`:
```properties
server.port=9090  # Change to any port you prefer
```

## Troubleshooting

| Problem | Solution |
|---|---|
| **Can't start — port 8080 in use** | Change `server.port` in `application.properties` |
| **Partners can't connect** | Check firewall settings; ensure same Wi-Fi network |
| **"No suitable driver" error** | Delete the `data/` folder and restart (fresh database) |
| **Map doesn't load** | Internet connection needed for OpenStreetMap tiles (maps only — no API key) |

## License

Private project — for internal business use.
