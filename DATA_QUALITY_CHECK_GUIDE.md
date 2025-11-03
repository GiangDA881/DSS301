# 📊 Data Quality Check Tool - Hướng Dẫn Sử Dụng

## 🎯 Mục đích

Công cụ này giúp bạn **kiểm tra chất lượng file CSV trước khi import** vào database. Nó sẽ báo cáo chi tiết các vấn đề trong dữ liệu để bạn có thể quyết định:
- ✅ Import ngay nếu chất lượng tốt
- 🔧 Sửa file trước khi import nếu có nhiều lỗi
- ❌ Từ chối file nếu quá nhiều dữ liệu lỗi

## 🚀 Cách Sử dụng

### Bước 1: Khởi động Spring Boot
```bash
cd E:\DSS301
mvn spring-boot:run
```

### Bước 2: Mở trang Data Job
1. Truy cập: `http://localhost:8080/data-job.html`
2. Login với tài khoản **ADMIN**

### Bước 3: Analyze File
1. Click nút **"Choose File"** và chọn file CSV
2. Click nút **"🔍 Analyze Data Quality First"** (màu xanh dương)
3. Đợi khoảng 30 giây - 2 phút (tùy kích thước file)
4. Xem báo cáo chi tiết trong Status Log

## 📋 Báo Cáo Sẽ Hiển Thị

### 1. Thống Kê Tổng Quan
```
✅ Total rows: 541,909
✅ Valid rows: 536,552 (99.01%)
❌ Invalid rows: 5,357 (0.99%)
```

### 2. Vấn Đề Cụ Thể
```
⚠️ Missing Customer IDs: 2,500 rows (các order này sẽ bị skip)
⚠️ Invalid dates: 1,200 rows
⚠️ Invalid prices: 500 rows
⚠️ Invalid quantities: 157 rows
⚠️ Empty descriptions: 1,000 rows
```

### 3. Chất Lượng Từng Cột (Quality Score 0-100%)
```
📋 Column Quality Scores:
  • InvoiceNo: 100.00% (0 invalid, 0 empty) ✅
  • StockCode: 100.00% (0 invalid, 0 empty) ✅
  • Description: 99.82% (0 invalid, 1000 empty) ⚠️
  • Quantity: 99.97% (157 invalid, 0 empty) ✅
  • InvoiceDate: 99.78% (1200 invalid, 0 empty) ⚠️
  • UnitPrice: 99.91% (500 invalid, 0 empty) ✅
  • CustomerID: 99.54% (0 invalid, 2500 empty) ⚠️
  • Country: 100.00% (0 invalid, 0 empty) ✅
```

### 4. Sample Issues (10 ví dụ đầu tiên)
```
🔍 Sample Issues (showing first 10 of 2,500):
  Row 1523: CustomerID - Missing customer ID - order will be skipped (value: "")
  Row 2045: InvoiceDate - Invalid date format (value: "SUGAR")
  Row 2046: Quantity - Invalid quantity format (value: "COFFEE")
  Row 3102: UnitPrice - Invalid price format (value: "18/08/2011 8:49")
  ...
```

### 5. Khuyến Nghị
```
✅ File quality is excellent! Ready to import.
💡 File quality is good. Some rows will be skipped but most data will import successfully.
⚠️ Warning: High number of invalid rows. Consider cleaning your data before importing.
```

## 🔍 Các Loại Lỗi Được Phát Hiện

### 1. Missing Data
- **InvoiceNo trống**: Order không thể được tạo
- **StockCode trống**: Product không thể được tạo
- **CustomerID trống**: Order sẽ bị skip (vì `customer_id NOT NULL`)

### 2. Invalid Format
- **Invalid Date**: Ngày không đúng format (ví dụ: text xuất hiện ở cột date)
- **Invalid Price**: Giá không phải số thập phân hợp lệ
- **Invalid Quantity**: Số lượng không phải số nguyên hợp lệ

### 3. Data Quality Issues
- **Empty Description**: Mô tả sản phẩm bị trống
- **Missing Country**: Không có thông tin quốc gia

## 💡 Cách Xử Lý Các Vấn Đề

### Nếu Quality Score > 95%
✅ **Tốt!** Import trực tiếp, không cần sửa gì.

### Nếu Quality Score 80-95%
⚠️ **Chấp nhận được**. Một số records sẽ bị skip nhưng phần lớn dữ liệu OK.
- Xem sample issues để biết records nào bị lỗi
- Quyết định có muốn sửa không

### Nếu Quality Score < 80%
❌ **Nên sửa!** Quá nhiều dữ liệu lỗi.

**Cách sửa file CSV:**
1. Mở file trong Excel/LibreOffice
2. Tìm các row bị lỗi (dựa vào row number trong report)
3. Sửa các vấn đề:
   - Điền CustomerID cho các row thiếu
   - Sửa format ngày về `dd/MM/yyyy H:mm` (ví dụ: `18/08/2011 6:30`)
   - Sửa giá về số thập phân (ví dụ: `2.55`)
   - Sửa số lượng về số nguyên (ví dụ: `6`)
4. Save file và analyze lại

## 🐛 Các Vấn Đề Phổ Biến & Cách Fix

### Vấn đề 1: Data bị lộn cột
**Triệu chứng:**
```
Row 2045: InvoiceDate - Invalid date format (value: "SUGAR")
Row 2046: Quantity - Invalid quantity format (value: "COFFEE")
```

**Nguyên nhân:** Description có chứa dấu `;` hoặc `,` → làm lộn cột

**Cách fix:**
- Mở file CSV trong text editor
- Đảm bảo Description có dấu ngoặc kép: `"COFFEE SUGAR"`
- Hoặc replace `;` trong Description bằng `:` hoặc `-`

### Vấn đề 2: Missing CustomerID
**Triệu chứng:**
```
⚠️ Missing Customer IDs: 2,500 rows (các order này sẽ bị skip)
```

**Nguyên nhân:** File gốc có transactions không gắn với customer

**Cách fix:**
- **Option 1:** Tạo CustomerID mặc định (ví dụ: `GUEST001`)
- **Option 2:** Xóa các rows không có CustomerID (nếu không quan trọng)
- **Option 3:** Import luôn - system sẽ tự động skip các orders này

### Vấn đề 3: Invalid Date Format
**Triệu chứng:**
```
⚠️ Invalid dates: 1,200 rows
```

**Nguyên nhân:** Date không đúng format hoặc bị lộn cột

**Cách fix:**
- Đảm bảo format ngày là: `dd/MM/yyyy H:mm` (ví dụ: `18/08/2011 6:30`)
- Trong Excel: Format Cells → Custom → `dd/mm/yyyy h:mm`
- Kiểm tra xem có text lạ ở cột InvoiceDate không

## 📊 API Endpoint

Nếu muốn gọi trực tiếp từ code:

```bash
POST /api/admin/jobs/analyze-file
Authorization: Bearer {JWT_TOKEN}
Content-Type: multipart/form-data

file: [CSV/Excel file]
```

**Response:**
```json
{
  "success": true,
  "message": "File analyzed successfully",
  "data": {
    "fileName": "data.csv",
    "totalRows": 541909,
    "validRows": 536552,
    "invalidRows": 5357,
    "missingCustomerIds": 2500,
    "invalidDates": 1200,
    "invalidPrices": 500,
    "invalidQuantities": 157,
    "emptyDescriptions": 1000,
    "columnQuality": {
      "InvoiceNo": {
        "columnName": "InvoiceNo",
        "totalValues": 541909,
        "nullValues": 0,
        "emptyValues": 0,
        "invalidValues": 0,
        "qualityScore": 100.0
      },
      ...
    },
    "issues": [
      {
        "rowNumber": 1523,
        "columnName": "CustomerID",
        "issueType": "MISSING",
        "value": "",
        "description": "Missing customer ID - order will be skipped"
      },
      ...
    ]
  }
}
```

## 🎯 Kết Luận

Công cụ Data Quality Check này giúp bạn:
- ✅ **Phát hiện lỗi TRƯỚC KHI import** → tiết kiệm thời gian
- ✅ **Biết chính xác** bao nhiêu records sẽ bị skip
- ✅ **Xác định vị trí** các row bị lỗi để sửa
- ✅ **Đánh giá chất lượng** file trước khi quyết định import

**Workflow khuyến nghị:**
1. 🔍 **Analyze** file trước
2. 📊 **Xem report** và đánh giá
3. 🔧 **Sửa file** nếu cần (hoặc chấp nhận skip một số records)
4. 📤 **Upload** và import vào database

Chúc bạn import dữ liệu thành công! 🎉

