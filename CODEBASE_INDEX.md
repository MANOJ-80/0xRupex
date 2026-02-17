# 0xRupex Codebase Index

> Privacy-First Personal Finance Manager for Android with Node.js Backend (MongoDB Edition)

## Project Overview

**0xRupex** is a personal finance tracking application that automatically captures transactions from:
- Bank SMS messages
- UPI payment notifications (GPay, PhonePe, Paytm, Amazon Pay, BHIM, CRED)
- Manual entry

**Key Principle**: Financial data stays on user's device and server - no third-party access.

---

## Architecture

```
0xRupex/
├── android/                    # Android App (Java)
│   └── app/src/main/java/com/rupex/app/
│       ├── data/
│       │   ├── local/         # Room Database, DAOs, Entities
│       │   ├── remote/        # Retrofit API, DTOs
│       │   ├── model/         # Domain models
│       │   └── repository/    # Data repository
│       ├── notification/      # UPI notification listener
│       ├── sms/               # SMS parser
│       ├── sync/              # Background sync worker
│       ├── ui/                # Activities, Fragments, ViewModels
│       └── util/              # Utilities
│
├── backend/                    # Node.js Backend
│   ├── api/                   # Vercel serverless entry point
│   └── src/
│       ├── api/
│       │   ├── controllers/   # Request handlers
│       │   ├── middleware/    # Auth, validation, error handling
│       │   └── routes/        # API route definitions
│       ├── config/            # Database, Auth config
│       ├── database/          # Seed file
│       ├── models/            # Mongoose models
│       ├── services/          # Business logic
│       └── utils/             # Helpers (JWT, response, errors)
│
└── deploy/                     # Deployment configs
```

---

## Backend API Reference

### Base URL: `/api/v1`

### Authentication Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/auth/register` | Register new user (requires name) | No |
| POST | `/auth/login` | Login user | No |
| POST | `/auth/refresh` | Refresh tokens | No |
| POST | `/auth/logout` | Logout current session | Yes |
| POST | `/auth/logout-all` | Logout all sessions | Yes |
| GET | `/auth/me` | Get current user | Yes |
| POST | `/auth/change-password` | Change password | Yes |

### Account Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/accounts` | Get all accounts + totalBalance | Yes |
| GET | `/accounts/:id` | Get account by ID | Yes |
| POST | `/accounts` | Create account | Yes |
| PUT | `/accounts/:id` | Update account | Yes |
| DELETE | `/accounts/:id` | Delete account | Yes |

### Category Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/categories` | Get all categories | Yes |
| GET | `/categories/stats` | Get category statistics | Yes |
| GET | `/categories/:id` | Get category by ID | Yes |
| POST | `/categories` | Create category | Yes |
| PUT | `/categories/:id` | Update category | Yes |
| DELETE | `/categories/:id` | Delete category | Yes |

### Transaction Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/transactions` | Get transactions (paginated) | Yes |
| GET | `/transactions/summary` | Get monthly summary | Yes |
| GET | `/transactions/analytics` | Get analytics data | Yes |
| GET | `/transactions/:id` | Get transaction by ID | Yes |
| POST | `/transactions` | Create transaction | Yes |
| POST | `/transactions/sync` | Sync bulk transactions | Yes |
| PUT | `/transactions/:id` | Update transaction | Yes |
| DELETE | `/transactions/:id` | Delete transaction | Yes |

---

## Database Schema (MongoDB)

### User Schema
```javascript
{
  _id: ObjectId,
  email: String (unique, required),
  passwordHash: String (required),
  name: String (required),
  phone: String,
  currency: String (default: 'INR'),
  timezone: String (default: 'Asia/Kolkata'),
  lastLogin: Date,
  isActive: Boolean (default: true),
  createdAt: Date,
  updatedAt: Date
}
```

### Account Schema
```javascript
{
  _id: ObjectId,
  user: ObjectId (ref: 'User', required),
  name: String (required),
  type: String (enum: ['bank', 'wallet', 'cash', 'credit_card']),
  balance: Number (default: 0),
  institution: String,
  accountNumber: String,
  last4Digits: String,
  color: String (default: '#6366f1'),
  icon: String (default: 'wallet'),
  isActive: Boolean (default: true),
  createdAt: Date,
  updatedAt: Date
}
// Indexes: { user: 1, name: 1 } (unique)
```

### Category Schema
```javascript
{
  _id: ObjectId,
  user: ObjectId (ref: 'User', required),
  name: String (required),
  type: String (enum: ['income', 'expense']),
  icon: String (default: 'tag'),
  color: String (default: '#8b5cf6'),
  parent: ObjectId (ref: 'Category'),
  isSystem: Boolean (default: false),
  createdAt: Date,
  updatedAt: Date
}
// Indexes: { user: 1, name: 1, type: 1 } (unique)
```

### Transaction Schema
```javascript
{
  _id: ObjectId,
  user: ObjectId (ref: 'User', required),
  account: ObjectId (ref: 'Account'),
  category: ObjectId (ref: 'Category'),
  type: String (enum: ['income', 'expense', 'transfer']),
  amount: Number (required, min: 0.01),
  description: String,
  merchant: String,
  referenceId: String,
  source: String (enum: ['manual', 'sms', 'api', 'recurring']),
  transactionAt: Date (required),
  location: String,
  tags: [String],
  notes: String,
  isRecurring: Boolean (default: false),
  smsHash: String (unique, sparse),
  createdAt: Date,
  updatedAt: Date
}
// Indexes: { user: 1, transactionAt: -1 }, { user: 1, category: 1 }, { user: 1, account: 1 }, { smsHash: 1 }
```

### RefreshToken Schema
```javascript
{
  _id: ObjectId,
  user: ObjectId (ref: 'User', required),
  tokenHash: String (required),
  expiresAt: Date (required, TTL indexed),
  revoked: Boolean (default: false),
  createdAt: Date
}
// TTL Index: { expiresAt: 1 } (expireAfterSeconds: 0)
```

---

## Key Files Reference

### Backend Models

| File | Purpose |
|------|---------|
| `backend/src/models/User.js` | User schema with password hashing |
| `backend/src/models/Account.js` | Account schema with balance tracking |
| `backend/src/models/Category.js` | Category schema with system defaults |
| `backend/src/models/Transaction.js` | Transaction schema with populated fields |
| `backend/src/models/RefreshToken.js` | JWT refresh token storage |

### Backend Services

| File | Purpose |
|------|---------|
| `auth.service.js` | User registration, login, token management |
| `transaction.service.js` | CRUD operations, sync, analytics |
| `account.service.js` | Account management, balance updates |
| `category.service.js` | Category management, default initialization |

### Backend Controllers

| File | Endpoints |
|------|-----------|
| `auth.controller.js` | register, login, refresh, logout, me, changePassword |
| `transaction.controller.js` | getTransactions, createTransaction, syncTransactions, updateTransaction, deleteTransaction |
| `account.controller.js` | getAccounts, createAccount, updateAccount, deleteAccount |
| `category.controller.js` | getCategories, createCategory, getCategoryStats |

### Backend Middleware

| File | Purpose |
|------|---------|
| `auth.middleware.js` | JWT token verification, user extraction |
| `validate.middleware.js` | Request validation with express-validator |
| `error.middleware.js` | Global error handling, MongoDB error handling |

### Android DTOs

| File | Purpose |
|------|---------|
| `AccountDto.java` | Account data (String id) |
| `CategoryDto.java` | Category data (String id) |
| `TransactionDto.java` | Transaction data with populated fields |
| `LoginRequest.java` | Login payload (email, password) |
| `RegisterRequest.java` | Register payload (email, password, name) |
| `ApiResponse.java` | Standard response wrapper |
| `PaginatedApiResponse.java` | Paginated list response |

---

## API Request/Response Formats

### Standard Response
```json
{
  "success": true,
  "message": "Optional message",
  "data": { ... }
}
```

### Accounts Response (Special)
```json
{
  "success": true,
  "accounts": [...],
  "totalBalance": 10000.00
}
```

### Categories Response (Special)
```json
{
  "success": true,
  "categories": [...]
}
```

### Paginated Response
```json
{
  "success": true,
  "data": [...],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 100,
    "totalPages": 5
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": "Error message",
  "errors": [{ "field": "email", "message": "Invalid email" }]
}
```

---

## Authentication Flow

1. **Register**: `POST /auth/register` with `{ email, password, name }` → Returns user + tokens
2. **Login**: `POST /auth/login` → Returns user + tokens
3. **Access Protected Routes**: Include `Authorization: Bearer <accessToken>`
4. **Token Expired**: Use `POST /auth/refresh` with `{ refreshToken }`
5. **Logout**: `POST /auth/logout` (revokes refresh token)

### Token Details
- Access Token: 15 minutes expiry
- Refresh Token: 7 days expiry
- Tokens stored hashed in MongoDB

---

## Environment Variables

### Backend Required
```
MONGODB_URI=your-mongodb-connection-string-here
MONGODB_DB_NAME=rupex
JWT_SECRET=your-secret-key
NODE_ENV=production
```

### Backend Optional
```
PORT=3000
CORS_ORIGINS=*
JWT_ACCESS_EXPIRY=15m
JWT_REFRESH_EXPIRY=7d
```

---

## Tech Stack

### Android
- **Language**: Java
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **Background Sync**: WorkManager
- **Security**: EncryptedSharedPreferences

### Backend
- **Runtime**: Node.js 18+
- **Framework**: Express.js
- **Database**: MongoDB Atlas
- **ODM**: Mongoose 8+
- **Auth**: JWT + Refresh Tokens
- **Security**: Helmet, CORS, bcryptjs

---

## Commands

### Backend
```bash
cd backend
npm install                # Install dependencies
npm run dev               # Start development server
npm start                 # Start production server
npm run seed              # Seed database with test user
npm test                  # Run tests
```

### Android
```bash
cd android
./gradlew testDebugUnitTest    # Unit tests
./gradlew assembleDebug        # Build debug APK
```

---

## Migration Notes (v2.0.0)

### Changed from PostgreSQL to MongoDB
- Replaced Knex + pg with Mongoose
- All IDs are now MongoDB ObjectId strings (24 chars)
- No migrations needed - Mongoose auto-creates collections
- Schema validation at application level

### API Changes
- Registration now requires `name` field
- IDs are MongoDB ObjectId strings, not UUIDs
- Response formats standardized
- All field names in responses are camelCase

### Fixed Issues from Audit
- DEFAULT_CATEGORIES export fixed
- Registration includes name field
- Response wrappers unified
- ID types aligned (String in Android)
- Sensitive data no longer logged
- MongoDB error handling added

---

*Updated: 2026-02-17 - MongoDB Migration Complete*
