<div align="center">

<img src="img/carzone_logo.png" alt="CarZone Logo" width="180"/>

# CarZone — Garage Management System

**Internal desktop software for automotive garage staff**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![SQL Server](https://img.shields.io/badge/SQL_Server-2022-CC2927?style=flat-square&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/en-us/sql-server)
[![Maven](https://img.shields.io/badge/Maven-3-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![FlatLaf](https://img.shields.io/badge/FlatLaf-3.7-4A90D9?style=flat-square)](https://www.formdev.com/flatlaf/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-Academic-lightgrey?style=flat-square)](./docs/)

> Academic capstone project — Application Development course  
> Group 01 · Ho Chi Minh City University of Industry (IUH)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [Role-Based Access](#role-based-access)
- [Business Rules](#business-rules)
- [UI Design](#ui-design)
- [Contributing](#contributing)

---

## Overview

CarZone is an **in-house desktop application** built with Java Swing, designed to digitize and streamline the full operational workflow of an automotive repair garage. It covers the entire service lifecycle — from vehicle intake and repair order management, through spare-parts inventory control, all the way to invoicing, payment, and revenue reporting.

The system supports **three staff roles** with distinct navigation menus and permissions:

| Role | Vietnamese | Responsibilities |
|------|-----------|------------------|
| **Store Owner** | Chủ cửa hàng | Full access — management, reporting, analytics |
| **Receptionist / Cashier** | Tiếp nhận viên | Vehicle intake, repair orders, invoicing & payment |
| **Technician** | Kỹ thuật viên | Repair execution, parts requests |

---

## Features

### 🔐 Authentication & Session
- Secure login with SHA-256 hex password hashing
- Role-based sidebar navigation (menu items filtered per role)
- Singleton `UserSession` stores authenticated employee info globally

### 📊 Dashboard
- KPI cards: daily revenue, active orders, pending requests, low-stock alerts
- Pending parts-request queue with live status counts
- Today's revenue chart (4 time-slot breakdown) and payment-method breakdown
- Low-inventory component alerts

### 🔧 Repair Order Management
- Four-stage workflow: `Waiting → In Repair ⇄ Awaiting Parts → Completed`
- One assigned technician per repair slip
- Automatic parts deduction from inventory (DB trigger)
- Invoice generation only allowed on `Completed` orders

### 🧾 Invoicing & Payment
- Auto-calculated totals (no manual entry)
- Dynamic tax rate: services-only 7% · parts-only 1.5% · mixed 4.5%
- Max 1 active promotion per invoice (validated against expiry & item scope)
- Bank-transfer payments require a transaction ID (`maGiaoDich`)
- `PAID` invoices are immutable (no edit / delete / status rollback)
- 7-day return window; refunds deducted from the originating day's revenue

### 📦 Inventory & Parts Management
- Parts requests lifecycle: `Pending → Approved ↔ Received / Rejected`
- Actual-received quantity recorded separately from requested quantity (V5 migration)
- DB trigger adds the real received amount to stock
- Inventory confirmation dialog with before/after stock preview

### 💅 Services Catalogue
- Add / edit / toggle-status for garage services
- Auto-generated service codes (`DV00001`, `DV00002`, …) via stored procedure

### 🏷️ Promotions
- Date-ranged promotions with optional item-scope list
- Empty scope list = applies to all items
- Cannot delete a promotion already linked to a paid invoice

### 👥 Employee & Account Management
- Employee CRUD with role assignment; codes generated via `sp_ThemNhanVien`
- Account creation / password reset / soft-delete (keeps employee record)

### 📈 Revenue Reporting
- Configurable date range with smart grouping (day / week / month / year)
- Bar chart, donut payment-mix chart, summary stats and detail table
- One-click CSV export (UTF-8) of the current report view

### 🔍 Look-up (Store Owner)
- Read-only tabs: Parts inventory · Customer profiles · Repair history
- Customer detail view with linked vehicle and service history

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | **17** (`maven.compiler.release=17`) |
| UI Framework | Java Swing + FlatLaf (Light) | **3.7** |
| Database | Microsoft SQL Server | **2022** (Developer Edition) |
| JDBC Driver | `mssql-jdbc` | **12.6.4.jre11** |
| Build Tool | Apache Maven | 3.x |
| Packaging | maven-shade-plugin (fat JAR) | 3.6.0 |
| Schema Migration | Flyway (runs inside Docker) | **10** |
| Container Runtime | Docker Compose | v2 |
| Unit Testing | JUnit Jupiter | 5.10.2 |

> **No ORM.** Plain JDBC via `DriverManager` — no Hibernate, no JPA, no `persistence.xml`.

---

## Architecture

The project follows a strict **four-layer architecture**:

```
UI  ──►  Controller  ──►  DAO  ──►  Entity / Enum
                │
          UserSession  (Singleton, cross-cutting)
                │
           connectDB   (DatabaseConfig + DbUtil)
```

| Layer | Responsibility |
|-------|---------------|
| `ui` | Build Swing components, wire event listeners, run all DB calls on a `SwingWorker` background thread. **No SQL here.** |
| `controller` | Receive UI requests, check permissions from `UserSession`, call DAO, orchestrate business flow |
| `dao` | Pure JDBC queries using `DbUtil.getConnection()` · one class per table/group · always `try-with-resources` |
| `entity` | Plain POJOs · business validation in normal setters · raw setters (`setXxxRaw`) for DAO `ResultSet` mapping |
| `session` | `UserSession` — double-checked locking Singleton, holds `TaiKhoan` + `NhanVien` + login timestamp |
| `connectDB` | `DatabaseConfig` reads `infra/docker/.env`; `DbUtil` wraps `DriverManager` with a 300 ms port check cache |

### Key Design Decisions
- DB stores triggers and stored procedures for: code generation (`DV`, `LK`, `NV` prefixes), inventory deduction, invoice total calculation.
- Java validates early, checks permissions, and translates DB error codes (50xxx) to Vietnamese messages.
- `java.util.Date` and `double` are used throughout entities (finalized, not migrating to `LocalDateTime`/`BigDecimal`).

---

## Project Structure

```
CarZone/
├── src/
│   ├── app/              # Entry point — App.java
│   ├── connectDB/        # DatabaseConfig.java · DbUtil.java
│   ├── session/          # UserSession.java (Singleton)
│   ├── controller/       # 11 controllers (auth, dashboard, services, parts, …)
│   ├── dao/              # 11 DAO classes (one per table/group, plain JDBC)
│   ├── entity/           # 14 POJO entities
│   ├── enums/            # 6 enums (VaiTro, TrangThaiPhieu, …)
│   └── ui/               # 16 Swing screens + dialogs
│
├── img/                  # Shared image assets (logo, nav icons, illustrations)
│
├── infra/
│   ├── docker/
│   │   ├── compose.yml   # SQL Server 2022 → db-init → Flyway pipeline
│   │   └── .env          # DB credentials & port (intentionally tracked for team use)
│   └── database/
│       ├── fly_way_migrations/
│       │   ├── V1__init_CarZone_Database.sql
│       │   ├── V2__update_KhachHang_Entity.sql
│       │   ├── V3__quan_ly_dich_vu.sql
│       │   ├── V4__quan_ly_linh_kien.sql
│       │   └── V5__so_luong_nhap_thuc_te.sql
│       └── seed/
│           └── CarZone_Demo.sql   # Demo data (not a migration)
│
├── docs/                 # Project report (Word)
└── pom.xml
```

---

## Database Schema

The schema is managed exclusively by **Flyway** (runs inside Docker on `compose up`):

| Migration | Description |
|-----------|-------------|
| `V1` | Full schema: 15+ tables, sequences, triggers, stored procedures (`sp_LapHoaDon`, `sp_ThanhToan`, `sp_ThemNhanVien`, `sp_TaoKhachHang`), views (`vw_DoanhThuTheoNgay`, …), and seed catalogue/employee data |
| `V2` | Add `thanhVienThanThiet`, `diemTichLuy` columns to `KhachHang` |
| `V3` | Stored procedure `sp_ThemDichVu` — auto-generates `DV00001`-style codes with `sp_getapplock` |
| `V4` | Stored procedure for part code generation (`LK00001`) and initial stock |
| `V5` | Add `soLuongNhapThucTe` to `PhieuYeuCauNhapLinhKien`; update trigger to use actual received quantity |

**Collation:** `Vietnamese_CI_AS` (case-insensitive, accent-sensitive Vietnamese)  
**Time zone:** `Asia/Ho_Chi_Minh` (GMT+7)

---

## Prerequisites

| Requirement | Minimum Version |
|-------------|----------------|
| JDK | 17 |
| Apache Maven | 3.8+ |
| Docker Desktop | 4.x (with Docker Compose v2) |

> **Windows users:** SQL Server may already occupy port 1433. The default `.env` maps CarZone to port **1434**. Adjust `SQLSERVER_PORT` in `infra/docker/.env` if needed.

---

## Getting Started

### 1 — Clone the repository

```bash
git clone <repository-url>
cd CarZone
```

### 2 — Start the database

```bash
docker compose -f infra/docker/compose.yml up -d
```

This single command:
1. Starts **SQL Server 2022** (Developer Edition)
2. Creates the `CarZone` database (if it does not exist)
3. Runs **Flyway** to apply all schema migrations (V1 → V5)

> **First-run note:** If Flyway exits with a `sp_getapplock` DLL error (code 126), simply restart it:
> ```bash
> docker restart carzone_flyway
> ```

To verify all migrations applied:
```bash
docker logs carzone_flyway
```

### 3 — (Optional) Load demo data

```bash
docker exec -i carzone_sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -C -S localhost -U sa -P "<your-sa-password>" \
  -i /dev/stdin < infra/database/seed/CarZone_Demo.sql
```

The demo dataset includes 6 customers, 6 vehicles, 7 services, 9 parts (2 with zero stock to trigger alerts), repair slips in all 4 states, paid invoices (cash & bank transfer), and 5 employee accounts.

### 4 — Reset the database (clean slate)

```bash
docker compose -f infra/docker/compose.yml down -v
docker compose -f infra/docker/compose.yml up -d
```

---

## Running the Application

### Option A — Run directly with Maven

```bash
mvn exec:java
```

### Option B — Build a fat JAR and run

```bash
mvn package
java -jar target/carzone-1.0.0.jar
```

The fat JAR bundles all dependencies (JDBC driver, FlatLaf) via `maven-shade-plugin`.

### Option C — Compile only (no run)

```bash
mvn -q compile
```

> The application reads `infra/docker/.env` at runtime to resolve the JDBC connection URL and credentials. The working directory must be the project root when running the JAR directly.

---

## Application Startup Flow

```
App.main()
  └─ FlatLightLaf.setup()            ← Apply light theme (arc 12px, SansSerif 13pt)
       └─ KhoiDongUI (Splash Screen) ← Animated startup screen
            └─ DangNhapUI (Login)    ← SHA-256 auth against DB
                 └─ MainFrame        ← Header + Role-filtered Sidebar + CardLayout content
```

---

## Role-Based Access

| Screen | Store Owner | Receptionist | Technician |
|--------|:-----------:|:------------:|:----------:|
| Dashboard | ✅ | ✅ | ✅ |
| Repair Orders | ✅ | ✅ | ✅ |
| Invoicing & Payment | ✅ | ✅ | — |
| Services Catalogue | ✅ | — | — |
| Parts & Inventory | ✅ | — | — |
| Parts Requests | ✅ | — | ✅ |
| Promotions | ✅ | — | — |
| Employee Management | ✅ | — | — |
| Account Management | ✅ | — | — |
| Revenue Reports | ✅ | — | — |
| Look-up (read-only) | ✅ | — | — |
| Personal Profile | ✅ | ✅ | ✅ |

---

## Business Rules

Key invariants enforced by both the Java layer (early validation) and the database layer (triggers / stored procedures / CHECK constraints):

- **Repair order states:** `CHO_SUA → DANG_SUA ⇄ CHO_LINH_KIEN → HOAN_TAT` — only one primary technician per order.
- **Invoice creation:** only allowed when the repair order is in `HOAN_TAT` state; totals are system-calculated.
- **Inventory:** parts usage cannot exceed current stock; stock is decremented by a DB trigger.
- **Paid invoices:** `DA_THANH_TOAN` status is final — no edits, no deletions, no rollback.
- **Bank transfer:** requires a non-empty transaction reference (`maGiaoDich`).
- **Returns:** within 7 days of a paid invoice; refunds are deducted from the originating day's revenue.
- **Tax rates:** services-only → 7%; parts-only → 1.5%; mixed → 4.5%.
- **Promotions:** max 1 active promotion per invoice; empty applicable-items list means "applies to everything."



---

## UI Design

The interface follows the official Figma spec:  
🎨 **[GaraOTO_CarZone on Figma](https://www.figma.com/design/DgNDrY8qVy5NyzHHjDzyVH/GaraOTO_CarZone)**

Design constraints implemented in code:

- **Theme:** `FlatLightLaf` (light mode only — `FlatLightLaf.setup()`)
- **Palette:** inspected from Figma (`#F4F7FB` background · `#14233D` primary text · `#2563EB` accent · `#CA4850` alert)
- **Components:** rounded buttons (12 px arc) using custom `RoundedButton` with anti-aliased `Graphics2D`
- **Layout:** `BoxLayout(Y_AXIS)` children share `alignmentX = 0.5f`; horizontal button rows use `BoxLayout(X_AXIS)` with `RigidArea` gaps
- **Threading:** every DB call runs on a `SwingWorker` background thread; EDT is never blocked
- **UX:** first field auto-focused on open · `Enter` moves focus / submits · `DocumentListener` clears error borders on re-type

---

## Contributing

This is an academic group project. Work happens on the `develop` branch (and individual `feature/*` branches). The `main` branch holds stable releases.

```
develop          ← active development
feature/<name>   ← individual feature branches
main             ← stable / tagged releases
```

---

<div align="center">
  <sub>Built with ☕ Java 17 · FlatLaf · SQL Server 2022 · Docker · Flyway</sub><br/>
  <sub>Ho Chi Minh City University of Industry — Application Development Course — Group 01</sub>
</div>
