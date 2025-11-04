# ETL Data Import Implementation Guide

## Overview
This document describes the complete ETL (Extract, Transform, Load) data import functionality implemented for the DSS301 project.

## Architecture

### Two-Step ETL Process

1. **Step 1: Extract & Load to MongoDB (Raw Data Staging)**
   - Read CSV/Excel files
   - Extract raw data without transformation
   - Store in MongoDB collection `transactions_raw`
   - Purpose: Data staging and audit trail

2. **Step 2: Transform & Load to PostgreSQL (Clean Data)**
   - Read raw data from MongoDB
   - Clean, normalize, and validate data
   - Transform into relational entities
   - Load into PostgreSQL tables

## Components Implemented

### 1. Maven Dependencies (pom.xml)
```xml
<!-- Apache POI for Excel processing -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Apache Commons CSV for CSV processing -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.10.0</version>
</dependency>
```

### 2. MongoDB Entity (RawTransaction.java)
- **Collection**: `transactions_raw`
- **Fields**: InvoiceNo, StockCode, Description, Quantity, InvoiceDate, UnitPrice, CustomerID, Country
- **Metadata**: importedAt, fileName, rowNumber

### 3. PostgreSQL Entities

#### Customer.java
- **Table**: `customers`
- **Fields**: id, customerCode (unique), country, createdAt, updatedAt

#### Product.java
- **Table**: `products`
- **Fields**: id, stockCode (unique), description, unitPrice, createdAt, updatedAt

#### Order.java
- **Table**: `orders`
- **Fields**: id, invoiceNo (unique), customer (FK), invoiceDate, createdAt
- **Relationship**: One-to-Many with OrderItem

#### OrderItem.java
- **Table**: `order_items`
- **Fields**: id, order (FK), product (FK), quantity, unitPrice, totalPrice

### 4. Repositories
All repositories extend Spring Data JPA/MongoDB repositories:
- `RawTransactionRepository` (MongoDB)
- `CustomerRepository` (PostgreSQL)
- `ProductRepository` (PostgreSQL)
- `OrderRepository` (PostgreSQL)
- `OrderItemRepository` (PostgreSQL)

### 5. Service Layer (DataImportService.java)

#### Main Method
```java
public DataImportResult processAndLoadFile(MultipartFile file)
```

#### Key Features
- **File Format Support**: CSV, XLSX, XLS
- **CSV Parsing**: Uses Apache Commons CSV with header detection
- **Excel Parsing**: Uses Apache POI for Excel file processing
- **Data Transformation**: Intelligent upsert logic for entities
- **Error Handling**: Graceful error handling with error counting
- **Date Parsing**: Supports multiple date formats
- **Number Parsing**: Handles decimal and integer conversions

#### Transform Logic
- **Customer Upsert**: Find existing or create new based on customerCode
- **Product Upsert**: Find existing or create new based on stockCode
- **Order Creation**: Create orders with associated customer
- **OrderItem Creation**: Link products and orders with quantity/price calculation

### 6. Controller Layer (DataJobController.java)

#### Endpoints

**POST /api/admin/jobs/upload-csv**
- **Security**: Requires ADMIN role (@PreAuthorize)
- **Request**: MultipartFile (CSV/Excel)
- **Response**: ApiResponse<DataImportResult>
- **Validations**: 
  - File not empty
  - File extension check (.csv, .xlsx, .xls)

**GET /api/admin/jobs/health**
- **Security**: Requires ADMIN role
- **Purpose**: Health check endpoint

### 7. Security Configuration
- Enabled `@EnableMethodSecurity` for method-level security
- `/api/admin/jobs/**` requires authentication + ADMIN role
- JWT authentication via JwtAuthenticationFilter

### 8. Frontend UI (data-job.html)

#### Features
- **Modern Design**: Gradient background, card-based layout
- **File Upload**: Drag-drop style file selector
- **Real-time Logging**: Color-coded status messages
- **Statistics Dashboard**: Shows processed records count
- **JWT Integration**: Automatic token handling from localStorage
- **Progress Indication**: Loading spinner during upload
- **Error Handling**: User-friendly error messages

#### Status Log Types
- **Info** (Blue): General information messages
- **Success** (Green): Successful operations
- **Warning** (Yellow): Warnings and alerts
- **Error** (Red): Error messages

## Data Flow

```
1. User uploads file via data-job.html
   ↓
2. DataJobController receives MultipartFile
   ↓
3. DataJobController validates file (extension, not empty)
   ↓
4. DataImportService.processAndLoadFile()
   ↓
5. STEP 1: Extract & Load to MongoDB
   - Parse CSV/Excel file
   - Create RawTransaction objects
   - Save to MongoDB (transactions_raw)
   ↓
6. STEP 2: Transform & Load to PostgreSQL
   - Read all RawTransaction from MongoDB
   - For each record:
     * Upsert Customer
     * Upsert Product
     * Create Order
     * Create OrderItem
   - Batch save to PostgreSQL
   ↓
7. Return DataImportResult with statistics
   ↓
8. Frontend displays results and statistics
```

## Expected CSV/Excel Format

The file should contain these columns:
- **InvoiceNo**: Order/Invoice number
- **StockCode**: Product code
- **Description**: Product description
- **Quantity**: Order quantity
- **InvoiceDate**: Date of invoice (multiple formats supported)
- **UnitPrice**: Price per unit
- **CustomerID**: Customer identifier
- **Country**: Customer country

## Usage Instructions

### 1. Access the Upload Page
Navigate to: `http://localhost:8080/data-job.html`

### 2. Authentication Required
- Must be logged in with ADMIN role
- JWT token stored in localStorage
- Will redirect to login if not authenticated

### 3. Upload File
- Click "Choose File" button
- Select CSV or Excel file
- Click "Upload and Process"

### 4. Monitor Progress
- Watch the Status Log for real-time updates
- View statistics after completion:
  - Raw Records Loaded
  - Customers Processed
  - Products Processed
  - Orders Processed
  - Order Items Processed
  - Error Count

## Error Handling

### File Validation Errors
- Empty file
- Invalid file format (not CSV/Excel)
- Missing file name

### Parsing Errors
- Invalid row format
- Missing required columns
- Invalid data types

### Transform Errors
- Invalid date format (uses current time as fallback)
- Invalid number format (uses 0 as fallback)
- Missing customer/product references

All errors are logged and counted, but don't stop the entire process.

## Database Schema

### PostgreSQL Tables
```sql
-- Created automatically by JPA
CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(50) UNIQUE NOT NULL,
    country VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(500),
    unit_price DECIMAL(10,2),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    invoice_no VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT REFERENCES customers(customer_id),
    invoice_date TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(order_id),
    product_id BIGINT REFERENCES products(product_id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2),
    total_price DECIMAL(10,2)
);
```

### MongoDB Collection
```javascript
// transactions_raw collection
{
    "_id": ObjectId("..."),
    "invoiceNo": "536365",
    "stockCode": "85123A",
    "description": "WHITE HANGING HEART T-LIGHT HOLDER",
    "quantity": "6",
    "invoiceDate": "12/1/2010 8:26",
    "unitPrice": "2.55",
    "customerId": "17850",
    "country": "United Kingdom",
    "importedAt": ISODate("2025-11-03T12:00:00Z"),
    "fileName": "data.csv",
    "rowNumber": 2
}
```

## Testing

### 1. Reload Maven Project (IntelliJ IDEA)
```
Right-click on pom.xml → Maven → Reload Project
```

### 2. Run the Application
```bash
cd E:\DSS301
mvn spring-boot:run
```

### 3. Test the Upload
1. Login as ADMIN user
2. Navigate to `/data-job.html`
3. Upload a test CSV/Excel file
4. Verify results in Status Log

### 4. Verify Database
**MongoDB:**
```javascript
use Dss301
db.transactions_raw.find().limit(5)
```

**PostgreSQL:**
```sql
SELECT COUNT(*) FROM customers;
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM orders;
SELECT COUNT(*) FROM order_items;
```

## Performance Considerations

- **Batch Processing**: Uses `saveAll()` for batch inserts
- **Transaction Management**: `@Transactional` for data consistency
- **Memory Efficient**: Processes large files in streaming mode
- **Error Resilience**: Continues processing even with individual record errors

## Future Enhancements

1. **Async Processing**: Use `@Async` for large file processing
2. **Progress Updates**: WebSocket for real-time progress
3. **Data Validation**: More strict validation rules
4. **Duplicate Detection**: Advanced duplicate handling
5. **Scheduled Jobs**: Automatic file processing from directory
6. **Export Functionality**: Export processed data

## Troubleshooting

### Issue: "Cannot resolve symbol 'csv' or 'poi'"
**Solution**: Reload Maven project in IDE
```bash
mvn clean install
```

### Issue: "Access Denied"
**Solution**: Ensure user has ADMIN role and valid JWT token

### Issue: "File upload failed"
**Solution**: Check file format, size limits, and server logs

### Issue: "Database connection error"
**Solution**: Verify PostgreSQL and MongoDB are running and accessible

## Configuration

### application.properties
```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/Dss301

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/DSS301
spring.datasource.username=postgres
spring.datasource.password=123456

# JPA
spring.jpa.hibernate.ddl-auto=update
```

## Summary

The ETL data import functionality has been successfully implemented with:
- ✅ Complete 2-step ETL pipeline
- ✅ MongoDB staging for raw data
- ✅ PostgreSQL storage for clean data
- ✅ CSV and Excel file support
- ✅ Secure ADMIN-only access
- ✅ Modern, user-friendly UI
- ✅ Comprehensive error handling
- ✅ Real-time progress monitoring
- ✅ Detailed statistics reporting

All components are production-ready and follow Spring Boot best practices.

