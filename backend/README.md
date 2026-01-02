# 0xRupex Backend

Personal Finance Manager API - Node.js + Express + PostgreSQL

## Quick Start

### 1. Start Database

```bash
docker compose up -d
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Run Migrations & Seed

```bash
npm run migrate
npm run seed
```

### 4. Start Server

```bash
# Development
npm run dev

# Production
npm start
```

## Test User

After seeding, you can login with:
- **Email:** test@rupex.dev
- **Password:** Test@123

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh tokens
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/me` - Get current user

### Accounts
- `GET /api/v1/accounts` - List accounts
- `POST /api/v1/accounts` - Create account
- `PUT /api/v1/accounts/:id` - Update account
- `DELETE /api/v1/accounts/:id` - Delete account

### Categories
- `GET /api/v1/categories` - List categories
- `GET /api/v1/categories/stats` - Category statistics
- `POST /api/v1/categories` - Create category
- `PUT /api/v1/categories/:id` - Update category
- `DELETE /api/v1/categories/:id` - Delete category

### Transactions
- `GET /api/v1/transactions` - List transactions (with filters)
- `GET /api/v1/transactions/summary` - Monthly summary
- `GET /api/v1/transactions/analytics` - Analytics data
- `POST /api/v1/transactions` - Create transaction
- `POST /api/v1/transactions/sync` - Sync from Android app
- `PUT /api/v1/transactions/:id` - Update transaction
- `DELETE /api/v1/transactions/:id` - Delete transaction

## Environment Variables

See `.env.example` for all available options.
