# 💰 0xRupex - Privacy-First Personal Finance Manager

**0xRupex** is a privacy-focused personal finance manager for Android that automatically tracks your expenses from SMS and UPI notifications. Your financial data stays on YOUR device and YOUR server - no third-party access.

## ✨ Features

### 📱 Auto-Transaction Capture

- **SMS Parsing** - Automatically reads bank SMS and extracts transaction details (amount, merchant, bank, type)
- **UPI Notification Listener** - Captures transactions from GPay, PhonePe, Paytm, Amazon Pay, BHIM, and CRED
- **Cross-Source Deduplication** - Prevents duplicate entries when both SMS and notification capture the same transaction
- **Smart Merchant Detection** - Enriches merchant names from notifications when SMS has generic info

### 💳 Manual Transactions

- Add income/expense transactions manually
- Assign categories and add notes
- Edit existing transactions (category, type, notes)
- Swipe-to-delete with confirmation

### 📊 Analytics & Insights

- **Monthly Summary** - Total income, expense, and net balance
- **Category Breakdown** - Visual pie charts showing spending by category
- **Transaction History** - Searchable, filterable list of all transactions

### 🔄 Backend Sync

- Full sync with self-hosted Node.js backend
- Real-time updates when editing transactions
- Fetch transactions from backend on login
- Secure JWT authentication with refresh tokens

### 🔒 Privacy First

- **Self-Hosted Backend** - Run your own server, own your data
- **No Third-Party Services** - No analytics, no tracking, no ads
- **Encrypted Storage** - Tokens stored in Android EncryptedSharedPreferences
- **Local-First** - Works offline, syncs when connected

---

## 🏗️ Architecture

```
0xRupex/
├── android/                 # Android App (Java)
│   ├── app/src/main/java/com/rupex/app/
│   │   ├── data/           # Data layer
│   │   │   ├── local/      # Room Database, DAOs, Entities
│   │   │   ├── remote/     # Retrofit API, DTOs
│   │   │   └── model/      # Domain models
│   │   ├── notification/   # UPI notification listener
│   │   ├── sms/            # SMS parser
│   │   ├── sync/           # Background sync worker
│   │   ├── ui/             # Activities, Fragments, ViewModels
│   │   └── util/           # Utilities (TokenManager, etc.)
│   └── build.gradle
│
├── backend/                 # Node.js Backend
│   ├── src/
│   │   ├── api/            # Controllers, Routes, Middlewares
│   │   ├── config/         # Database, Auth config
│   │   └── services/       # Business logic
│   ├── migrations/         # Knex database migrations
│   └── package.json
│
└── screenshots/            # App screenshots
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Arctic Fox or later)
- **Node.js** v18+
- **PostgreSQL** 14+
- **Android Device** with SMS permissions

### Backend Setup

#### Option 1: Self-Hosted (Local/VPS)

1. **Clone the repository**

   ```bash
   git clone https://github.com/MANOJ-80/0xRupex.git
   cd 0xRupex/backend
   ```

2. **Install dependencies**

   ```bash
   npm install
   ```

3. **Configure environment**

   ```bash
   cp .env.example .env
   # Edit .env with your database credentials
   ```

4. **Run migrations**

   ```bash
   npm run migrate
   ```

5. **Start the server**

   ```bash
   npm run dev
   ```

   Server runs on `http://localhost:3000`

#### Option 2: Deploy to Vercel (Recommended for Production)

1. **Set up Supabase Database**

   - Create a free account at [supabase.com](https://supabase.com)
   - Create a new project
   - Go to **Settings → Database → Connection Pooling**
   - Copy the **Transaction mode** connection string

2. **Deploy to Vercel**

   ```bash
   cd backend
   npm install -g vercel
   vercel login
   vercel --prod
   ```

3. **Set Environment Variables in Vercel Dashboard**

   ```
   DATABASE_URL=postgresql://postgres.xxx:[PASSWORD]@aws-0-region.pooler.supabase.com:6543/postgres
   JWT_SECRET=your-secure-random-secret
   NODE_ENV=production
   ```

4. **Run migrations on Supabase**
   ```bash
   # Set DATABASE_URL locally to Supabase pooler URL, then:
   npm run migrate
   ```

**Production API:** `https://your-project.vercel.app/api/v1/`

### Android Setup

1. **Open in Android Studio**

   ```bash
   cd 0xRupex/android
   # Open with Android Studio
   ```

2. **Configure API URL**

   Edit `app/build.gradle` and set your backend URL:

   ```gradle
   buildConfigField "String", "API_BASE_URL", "\"http://YOUR_SERVER_IP:3000/api/v1/\""
   ```

3. **Build and Run**

   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Grant Permissions**
   - SMS Read permission (for bank SMS parsing)
   - Notification Access (for UPI app notifications)

---

## 📱 App Permissions

| Permission              | Purpose                           |
| ----------------------- | --------------------------------- |
| `READ_SMS`              | Parse bank transaction SMS        |
| `RECEIVE_SMS`           | Real-time SMS detection           |
| `NOTIFICATION_LISTENER` | Capture UPI payment notifications |
| `INTERNET`              | Sync with backend server          |

---

## 🔧 Tech Stack

### Android

- **Language**: Java
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **Background Sync**: WorkManager
- **Charts**: MPAndroidChart

### Backend

- **Runtime**: Node.js
- **Framework**: Express.js
- **Database**: PostgreSQL
- **ORM**: Knex.js
- **Authentication**: JWT + Refresh Tokens

---

## 📊 Database Schema

### Android (Room)

```
pending_transactions
├── id (PK)
├── amount
├── type (income/expense)
├── category
├── merchant
├── bank_name
├── last_4_digits
├── transaction_at
├── note
├── source (sms/notification/manual)
├── sms_hash (dedup key)
├── server_id (FK to backend)
└── synced (boolean)
```

### Backend (PostgreSQL)

```
transactions
├── id (UUID, PK)
├── user_id (FK)
├── account_id (FK)
├── category_id (FK)
├── type
├── amount
├── description
├── merchant
├── notes
├── transaction_at
└── created_at/updated_at
```

---

## 🛣️ Roadmap & Future Improvements

### 🎯 High Priority

- [ ] **Recurring Transactions** - Auto-detect and predict recurring bills/subscriptions
- [ ] **Budget Planning** - Set monthly budgets per category with alerts
- [ ] **Multi-Currency Support** - Handle transactions in different currencies
- [ ] **Export to CSV/PDF** - Download transaction history for records

### 🔧 Technical Improvements

- [ ] **Kotlin Migration** - Migrate Android app from Java to Kotlin
- [ ] **Jetpack Compose UI** - Modern declarative UI toolkit
- [ ] **Offline-First Sync** - Better conflict resolution for offline edits
- [ ] **End-to-End Encryption** - Encrypt data before sending to server
- [ ] **Biometric Lock** - Fingerprint/Face unlock for app access

### 📱 Feature Enhancements

- [ ] **Bill Reminders** - Push notifications for upcoming bills
- [ ] **Spending Insights** - AI-powered spending analysis and tips
- [ ] **Goal Tracking** - Set savings goals and track progress
- [ ] **Split Expenses** - Track shared expenses with friends/family
- [ ] **Bank Account Linking** - Direct bank integration (Open Banking API)
- [ ] **Receipt Scanning** - OCR to capture receipts
- [ ] **Dark Mode** - System-aware dark theme
- [ ] **Widgets** - Home screen widgets for quick balance view

### 🌐 Platform Expansion

- [ ] **iOS App** - Native iOS version
- [ ] **Web Dashboard** - Browser-based analytics dashboard
- [ ] **Desktop App** - Electron-based desktop client

### 🔒 Security Enhancements

- [ ] **2FA Authentication** - TOTP-based two-factor auth
- [ ] **Session Management** - View and revoke active sessions
- [ ] **Audit Logs** - Track all account activities
- [ ] **Self-Destruct** - Remote wipe feature for lost devices

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Guidelines

- Follow existing code style and conventions
- Write meaningful commit messages
- Add comments for complex logic
- Test on real devices when possible

---

## 📜 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 📩 Contact

For questions, suggestions, or feedback:

- **GitHub**: [@MANOJ-80](https://github.com/MANOJ-80)
- **Issues**: [Open an issue](https://github.com/MANOJ-80/0xRupex/issues)

---

<p align="center">
  Made with ❤️ for financial privacy
</p>
