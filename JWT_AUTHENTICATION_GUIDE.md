# 🔐 JWT Authentication Guide

## ✅ Đã hoàn thành

Hệ thống login giờ đã **TRẢ VỀ JWT TOKEN** khi đăng nhập thành công!

## 📦 Cấu trúc Login Response mới

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "success": true,
    "message": "Login successful",
    "username": "admin",
    "userId": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6ImFkbWluIiwiaWF0IjoxNjk4NzU...",
    "tokenType": "Bearer"
  },
  "timestamp": "2025-10-31T10:30:00"
}
```

## 🔧 Components đã tạo

### 1. **JwtUtil.java** - JWT Utility Class
- `generateToken(username, userId)` - Tạo JWT token
- `extractUsername(token)` - Lấy username từ token
- `extractUserId(token)` - Lấy userId từ token
- `validateToken(token)` - Xác thực token
- Token expire sau 24 giờ (configurable)

### 2. **JwtAuthenticationFilter.java** - Security Filter
- Tự động kiểm tra JWT token trong header `Authorization: Bearer <token>`
- Set authentication vào SecurityContext nếu token hợp lệ

### 3. **Updated LoginResponse.java**
- Thêm field `token` và `tokenType`

### 4. **Updated AuthService.java**
- Tự động sinh JWT token khi login thành công

### 5. **Updated SecurityConfig.java**
- Stateless session management
- JWT filter được thêm vào security chain

## 🚀 Cách sử dụng

### Bước 1: Login và nhận token

```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "username": "admin",
    "userId": 1
  }
}
```

### Bước 2: Sử dụng token để gọi API

```bash
GET http://localhost:8080/api/auth/check
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## 💻 Frontend Integration

### JavaScript/Fetch Example

```javascript
// Login và lưu token
const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
});

const data = await response.json();

if (data.success) {
    // Lưu token vào localStorage
    localStorage.setItem('token', data.data.token);
    localStorage.setItem('tokenType', data.data.tokenType);
}

// Gọi API với token
const token = localStorage.getItem('token');
const apiResponse = await fetch('/api/protected-endpoint', {
    method: 'GET',
    headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
    }
});
```

### Axios Example

```javascript
// Set default header cho mọi request
axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

// Hoặc cho từng request
axios.get('/api/protected-endpoint', {
    headers: {
        'Authorization': `Bearer ${token}`
    }
});
```

## 🧪 Test JWT

### Cách 1: Sử dụng trang test có sẵn
Truy cập: `http://localhost:8080/jwt-test.html`

Trang này cho phép:
- ✅ Test login và nhận token
- ✅ Test API không có token (sẽ fail)
- ✅ Test API có token (sẽ success)
- ✅ Xem token details
- ✅ Clear token

### Cách 2: Sử dụng Postman

**1. Login:**
```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "username": "admin",
  "password": "123456"
}
```

**2. Copy token từ response**

**3. Gọi protected API:**
```
GET http://localhost:8080/api/auth/check
Headers:
Authorization: Bearer YOUR_TOKEN_HERE
```

## ⚙️ Configuration (application.properties)

```properties
# JWT Configuration
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000  # 24 hours in milliseconds
```

**Thay đổi thời gian expire:**
- 1 hour = 3600000
- 12 hours = 43200000
- 24 hours = 86400000 (mặc định)
- 7 days = 604800000

## 🔒 Security Features

✅ **BCrypt Password Hashing** - Password được mã hóa an toàn  
✅ **JWT Token** - Stateless authentication  
✅ **Token Expiration** - Token tự động hết hạn sau 24h  
✅ **Bearer Authentication** - Standard OAuth 2.0 format  
✅ **CSRF Disabled** - Vì sử dụng JWT stateless  
✅ **Stateless Session** - Không lưu session trên server  

## 📋 API Endpoints

### Public Endpoints (Không cần token)
- `POST /api/auth/login` - Đăng nhập
- `POST /api/test/create-user` - Tạo user test
- `GET /login.html` - Trang login
- `GET /jwt-test.html` - Trang test JWT

### Protected Endpoints (Cần token)
- `GET /api/auth/check` - Kiểm tra authentication
- `POST /api/auth/logout` - Đăng xuất
- Tất cả endpoints khác bắt đầu với `/api/*` (trừ `/api/auth/**` và `/api/test/**`)

## 🎯 Token Structure

JWT token bao gồm 3 phần:

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6ImFkbWluIiwiaWF0IjoxNjk4NzU2ODAwLCJleHAiOjE2OTg4NDMyMDB9.signature

[Header].[Payload].[Signature]
```

**Payload chứa:**
- `userId` - ID của user
- `sub` (subject) - Username
- `iat` (issued at) - Thời gian tạo token
- `exp` (expiration) - Thời gian hết hạn

## 🐛 Troubleshooting

### Token không hoạt động?
1. Kiểm tra token có được lưu đúng không: `localStorage.getItem('token')`
2. Kiểm tra header Authorization có đúng format không: `Bearer <token>`
3. Kiểm tra token chưa hết hạn (24h)

### 401 Unauthorized?
- Token không hợp lệ hoặc đã hết hạn
- Cần login lại để lấy token mới

### CORS error?
- SecurityConfig đã disable CSRF
- Đảm bảo `@CrossOrigin(origins = "*")` có trong controller

## 📝 Next Steps

1. **Thêm role-based authorization:**
```java
// Thêm role vào User entity
private String role; // ADMIN, USER, MANAGER

// Thêm role vào token
claims.put("role", user.getRole());

// Check role trong controller
@PreAuthorize("hasRole('ADMIN')")
```

2. **Refresh Token:**
- Implement refresh token để gia hạn token mà không cần login lại

3. **Token Blacklist:**
- Thêm blacklist để logout thực sự vô hiệu hóa token

## 🎉 Summary

Bây giờ hệ thống của bạn đã có:
✅ Login trả về JWT token  
✅ Frontend tự động lưu và sử dụng token  
✅ Backend tự động verify token  
✅ Trang test JWT đầy đủ  
✅ Security với BCrypt + JWT  

**Sử dụng ngay:** Login tại `/login.html` và test JWT tại `/jwt-test.html`!

