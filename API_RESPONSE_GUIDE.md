# 📘 API Response Standardization Guide

## 🎯 Cấu trúc ApiResponse chuẩn

Tất cả API endpoints đều trả về cấu trúc response thống nhất:

```json
{
  "success": true/false,
  "message": "Thông báo",
  "data": { ... },
  "timestamp": "2025-10-31T10:30:00"
}
```

## 📝 Cách sử dụng trong Controller

### 1. Success Response với data

```java
@GetMapping("/users")
public ResponseEntity<ApiResponse<List<User>>> getUsers() {
    List<User> users = userService.getAllUsers();
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách user thành công", users));
}
```

### 2. Success Response không có data

```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Object>> logout() {
    return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
}
```

### 3. Error Response

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
    if (!isValid(request)) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Username hoặc password không hợp lệ"));
    }
    // ...
}
```

### 4. Error Response với data

```java
@PostMapping("/validate")
public ResponseEntity<ApiResponse<Map<String, String>>> validate(@RequestBody Form form) {
    Map<String, String> errors = validateForm(form);
    if (!errors.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Validation failed", errors));
    }
    // ...
}
```

## 🚀 Ví dụ Response thực tế

### ✅ Login thành công
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "success": true,
    "message": "Login successful",
    "username": "admin",
    "userId": 1
  },
  "timestamp": "2025-10-31T10:30:00"
}
```

### ❌ Login thất bại
```json
{
  "success": false,
  "message": "Invalid username or password",
  "data": null,
  "timestamp": "2025-10-31T10:30:00"
}
```

### ✅ Tạo user thành công
```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "userId": 2,
    "username": "newuser"
  },
  "timestamp": "2025-10-31T10:30:00"
}
```

## 🛠️ Helper Methods trong ApiResponse

### ApiResponse.success(String message, T data)
Tạo success response với message và data

### ApiResponse.success(T data)
Tạo success response với message mặc định "Success"

### ApiResponse.error(String message)
Tạo error response với message và data = null

### ApiResponse.error(String message, T data)
Tạo error response với message và data

## 🔥 Global Exception Handler

Tất cả exception không được xử lý sẽ tự động được convert thành ApiResponse format:

```java
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ex.getMessage()));
}
```

## 📋 HTTP Status Codes được sử dụng

- **200 OK**: Success response
- **400 BAD_REQUEST**: Invalid input, validation error
- **401 UNAUTHORIZED**: Authentication failed
- **500 INTERNAL_SERVER_ERROR**: Server error

## 💡 Best Practices

1. ✅ **Luôn trả về ApiResponse** trong tất cả endpoints
2. ✅ **Sử dụng HTTP status code phù hợp** với tình huống
3. ✅ **Message rõ ràng, dễ hiểu** cho người dùng
4. ✅ **Data có thể null** nếu không cần thiết
5. ✅ **Timestamp tự động** được thêm vào mọi response

## 🧪 Test API với Postman

### Login API
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

### Create User API
```
POST http://localhost:8080/api/test/create-user?username=testuser&password=123456
```

### Logout API
```
POST http://localhost:8080/api/auth/logout
```

## 📱 Frontend Integration

Ví dụ xử lý response ở frontend:

```javascript
const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
});

const apiResponse = await response.json();

if (apiResponse.success) {
    console.log('Success:', apiResponse.message);
    console.log('Data:', apiResponse.data);
} else {
    console.error('Error:', apiResponse.message);
}
```

