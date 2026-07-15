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

## 🌐 GitHub Pages — Static App Preview

The app's **static frontend shell** is auto-deployed to GitHub Pages so you can preview the UI without running the backend:

👉 **https://sourav-argade.github.io/Wholesale-App/**

### What works on GitHub Pages

| Feature | Status |
|---|---|
| ✅ Landing page with feature showcase | Works |
| ✅ App UI preview (navigation, layout) | Works |
| ✅ Responsive mobile design | Works |
| ❌ Login / Authentication | Backend required |
| ❌ Customer data & CRUD | Backend required |
| ❌ Map with customer pins | Backend required |
| ❌ Check-in, prices, Excel export | Backend required |

> ⚠️ GitHub Pages is a **static hosting service** — it cannot run Java or a database.
> The full app with all features works when you run `mvn spring-boot:run` on your laptop.
> The landing page automatically detects if the backend is running and switches to the live app.

### How Deployment Works

The `docs/` folder at the repository root is automatically deployed to GitHub Pages via GitHub Actions.

**Files in `docs/`** (frontend only):
- `index.html` — Landing page + app shell
- `css/app.css` — Mobile-responsive styles
- `js/app.js` — App logic with backend detection

When you push to `main`, the **GitHub Actions workflow** (`.github/workflows/deploy-pages.yml`) automatically deploys the latest frontend.

### Enabling GitHub Pages (One-Time Setup)

If this hasn't been done yet:
1. Go to your repo on GitHub → **Settings** → **Pages**
2. Under "Build and deployment" → **Source**: select **GitHub Actions**
3. The workflow file at `.github/workflows/deploy-pages.yml` will handle the rest

## ☁️ Deploy to Railway (Free Cloud Hosting)

You can deploy the full Spring Boot backend to Railway's free tier so the app is accessible from **anywhere** (not just local Wi-Fi). No credit card required for the free tier.

### One-Click Deploy from GitHub

Railway auto-detects Spring Boot projects — no configuration needed beyond connecting your repo.

### Step 1: Create a Railway Account

1. Go to **[Railway.app](https://railway.app)** and sign up (GitHub login supported)
2. No credit card required for the free tier

### Step 2: Connect & Deploy

1. Click **New Project** → **Deploy from GitHub repo**
2. Select the `Sourav-Argade/Wholesale-App` repository
3. Railway will automatically:
   - Detect the `pom.xml` and use the built-in Java builder
   - Run `./mvnw clean package -DskipTests` to build
   - Start the app with `java -jar target/*.jar`
4. Go to the **Deployments** tab to watch the build logs

### Step 3: Get Your Public URL

1. Once the deployment succeeds, go to the **Settings** tab of your service
2. Find the **Networking** section
3. Click **Generate Domain** — you'll get a URL like:
   ```
   https://wholesale-app.up.railway.app
   ```

### Step 4: 🎉 You're Live!

Share this URL with your partners — anyone can access the full app from any browser, anywhere in the world.

| User | Login |
|---|---|
| **Admin** | `admin` / `admin123` |
| **Partner** | `partner` / `partner123` |

> ⚠️ **Data Note**: H2 data resets when Railway redeploys. For persistent data, add a free Railway PostgreSQL add-on (see Advanced Options below).

### Connect GitHub Pages to Railway (Optional)

Once deployed, the GitHub Pages landing page can connect to your Railway backend. Edit `docs/js/app.js` and set:

```js
const API_REMOTE_BASE = 'https://wholesale-app.up.railway.app'; // Your Railway URL
```

Now visitors to the GitHub Pages site will automatically connect to the live cloud backend.

### Railway Configuration Files

The project includes these files for Railway deployment:

| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage build (Maven → JRE) for alternative deployment |
| `railway.toml` | Railway-specific build/deploy configuration |
| `.mvn/wrapper/` | Maven Wrapper for zero-config builds |

### Deploy to Render (Alternative)

[Render](https://render.com) also supports Spring Boot:
1. Create a Render account
2. New **Web Service** → Connect your GitHub repo
3. Set **Build Command**: `./mvnw clean package -DskipTests`
4. Set **Start Command**: `java -jar target/*.jar`
5. Choose the free plan and deploy

## Advanced Options

### Run with Docker (Local)

1. **Install Docker** on your laptop
2. Build and run:
   ```bash
   docker build -t wholesale-app .
   docker run -p 8080:8080 -v ./data:/app/data wholesale-app
   ```

### Add Persistent PostgreSQL on Railway

For data that persists across Railway deploys:
1. In your Railway project, click **New** → **Database** → **Add PostgreSQL**
2. Go to the PostgreSQL service **Connect** tab → copy the `DATABASE_URL`
3. Add this **Environment Variable** to your Spring Boot service: `DATABASE_URL` with the copied value
4. The app will automatically switch from H2 to PostgreSQL (requires code changes for PostgreSQL dialect)

> For now, H2 works great for a small single-user/small-team app. Export to Excel regularly for backup.

### Access H2 Console (for debugging)

Open `http://localhost:8080/h2-console` in your browser.

- **JDBC URL:** `jdbc:h2:file:./data/wholesale`
- **User:** `sourav`
- **Password:** `1234`

### Changing Server Port

Edit `src/main/resources/application.properties`:
```properties
server.port=${PORT:8080}  # PORT env var takes priority, falls back to 8080
```

## Troubleshooting

| Problem | Solution |
|---|---|
| **Can't start — port 8080 in use** | Set env var `PORT=9090` or change `application.properties` |
| **Partners can't connect (local)** | Check firewall settings; ensure same Wi-Fi network |
| **Railway deploy fails** | Check build logs in Railway dashboard for Maven errors |
| **"No suitable driver" error** | Delete the `data/` folder and restart (fresh database) |
| **Map doesn't load** | Internet connection needed for OpenStreetMap tiles (maps only — no API key) |
| **Data lost after Railway redeploy** | Add a Railway PostgreSQL add-on for persistent storage |

---

## GitHub — Backup & Deployment

This repository serves two purposes:
- **Version control** for your source code
- **Auto-deployment** to GitHub Pages (static shell) and Railway (full backend)

```bash
# After making changes, push to GitHub — deploys happen automatically
git add .
git commit -m "Describe your changes"
git push origin main
```

## License

Private project — for internal business use.
