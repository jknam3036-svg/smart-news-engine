# 🧠 Intelligent News Crawler Setup Guide

This folder contains the **Backend Brain** of your news system. It is designed to run on **GitHub Actions** for free, forever.

## Step 1: Firebase Setup (Database)

1. Go to [Firebase Console](https://console.firebase.google.com/).
2. Create a new project named **`Make-Intelligence`**.
3. Go to **Build** -> **Firestore Database** -> **Create Database** (Start in **Production Mode**, select a nearby location like `asia-northeast3`).
4. Go to **Project Settings** (Gear icon) -> **Service accounts**.
5. Click **Generate new private key**. this will download a `.json` file.
   - **IMPORTANT**: Rename this file to `serviceAccountKey.json` and put it in this folder for *local testing*.
   - **DO NOT commit this file to GitHub**. (Add it to `.gitignore`).

## Step 2: GitHub Repository Setup

1. Create a new **Private** Repository on GitHub (e.g., `make-news-engine`).
2. Upload the contents of this `news_crawler` folder to the repository.
   - *Tip*: The `.github` folder must be at the root of the repo.

## Step 3: Configure Secrets (Security)

To let the server access your keys securely:

1. Go to your GitHub Repo -> **Settings** -> **Secrets and variables** -> **Actions**.
2. Click **New repository secret**.
3. Add the following 4 secrets:

### 3-1. GEMINI_API_KEY (AI 뉴스 분석)

- Get from: [Google AI Studio](https://aistudio.google.com/app/apikey)
- Name: `GEMINI_API_KEY`
- Value: Your Gemini API key (e.g., `AIzaSyC...`)

### 3-2. FIREBASE_CREDENTIALS (Database)

- Open your `serviceAccountKey.json` with a text editor.
- Copy the **entire JSON content**.
- Name: `FIREBASE_CREDENTIALS`
- Value: Paste the entire JSON

### 3-3. TWELVE_DATA_API_KEY (글로벌 시장 데이터) - Optional

- Get from: [Twelve Data](https://twelvedata.com/)
- Name: `TWELVE_DATA_API_KEY`
- Value: Your Twelve Data API key

### 3-4. ECOS_API_KEY (한국은행 경제통계) - Optional

- Get from: [한국은행 ECOS](https://ecos.bok.or.kr/)
- Name: `ECOS_API_KEY`
- Value: Your ECOS API key

## Step 4: Launch 🚀

1. Go to the **Actions** tab in your GitHub repo.
2. You should see "Intelligent News Crawler".
3. Click "Run workflow" to test it immediately.
4. Check Firestore in Firebase Console to see the analyzed news appearing in real-time!

## Local Testing (Optional)

If you have Python installed:

1. `pip install -r requirements.txt`
2. Make sure `serviceAccountKey.json` is in the folder.
3. Set your Gemini Key: `export GEMINI_API_KEY="AIza..."` (Linux/Mac) or `$env:GEMINI_API_KEY="AIza..."` (Windows PowerShell).
4. Run `python main.py`.
