# 🚀 Deploying 0xRupex to Cloud (Free Tier)

This guide will help you deploy the **0xRupex Backend** to **Vercel** and the **Database** to **Supabase**. Both have generous free tiers that are perfect for personal use.

---

## 1️⃣ Setup Database (Supabase)

1.  **Sign up** at [Supabase.com](https://supabase.com/).
2.  **Create a new project**.
    *   Give it a name (e.g., `rupex-db`).
    *   Set a strong database password (save this!).
    *   Choose a region close to you.
3.  **Get Connection Strings**:
    *   Go to **Project Settings** -> **Database**.
    *   Under **Connection string**, select **URI**.
    
    **For Vercel (use Transaction Pooler - port 6543):**
    ```
    postgresql://postgres.[project-ref]:[YOUR-PASSWORD]@aws-0-[region].pooler.supabase.com:6543/postgres
    ```
    
    **For Migrations (use Direct Connection - port 5432):**
    ```
    postgresql://postgres.[project-ref]:[YOUR-PASSWORD]@aws-0-[region].pooler.supabase.com:5432/postgres
    ```
    
    *   Replace `[YOUR-PASSWORD]` with the password you created.

---

## 2️⃣ Deploy Backend (Vercel)

1.  **Install Vercel CLI** (optional, or use the website):
    ```bash
    npm install -g vercel
    ```
2.  **Login to Vercel**:
    ```bash
    vercel login
    ```
3.  **Deploy**:
    Run this command from the `backend` folder:
    ```bash
    cd backend
    vercel
    ```
    *   Set up and deploy? **Yes**
    *   Which scope? **(Select your account)**
    *   Link to existing project? **No**
    *   Project name? **0xrupex-backend**
    *   Directory? **.** (Current directory)

4.  **Configure Environment Variables**:
    *   Go to your Vercel Dashboard -> Select Project -> **Settings** -> **Environment Variables**.
    *   Add the following:
        *   `DATABASE_URL`: (Paste your Supabase connection string - **use port 6543**)
        *   `JWT_SECRET`: (Generate: `openssl rand -hex 32`)
        *   `JWT_ACCESS_EXPIRY`: `15m`
        *   `JWT_REFRESH_EXPIRY`: `7d`
        *   `TEST_USER_EMAIL`: `test@rupex.dev`
        *   `TEST_USER_PASSWORD`: `Test@123`
        *   `TEST_USER_NAME`: `Test User`
        *   `NODE_ENV`: `production`

5.  **Redeploy**:
    *   Go to **Deployments** tab in Vercel and click **Redeploy** (or run `vercel --prod` in terminal) to make sure the new env vars are picked up.

---

## 3️⃣ Run Database Migrations

Since Vercel is serverless, we run migrations from your local machine connecting to the remote Supabase DB.

1.  In your local `backend` folder, set the DATABASE_URL (use **port 5432** for direct connection):
    ```bash
    export DATABASE_URL="postgresql://postgres.[project-ref]:[YOUR-PASSWORD]@aws-0-[region].pooler.supabase.com:5432/postgres"
    export NODE_ENV=production
    ```
2.  Run the migration command:
    ```bash
    npx knex migrate:latest --env production
    ```
3.  (Optional) Seed the test user:
    ```bash
    npx knex seed:run --env production
    ```

---

## 4️⃣ Connect Your App

1.  Get your **Vercel URL** (e.g., `https://0xrupex-backend.vercel.app`).
2.  Open the **0xRupex Android App**.
3.  On the "Connect to Server" screen, enter:
    `https://0xrupex-backend.vercel.app/api/v1`
4.  Login/Register and enjoy! 🎉

---

## 💡 Troubleshooting

*   **Database Connection Error**: Ensure you added `?sslmode=require` to the end of your `DATABASE_URL` if you face issues, though the code is configured to handle SSL automatically.
*   **Cold Starts**: Vercel functions "sleep" after inactivity. The first request might take 2-3 seconds. This is normal for the free tier.
