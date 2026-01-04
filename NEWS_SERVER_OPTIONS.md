### Server Options Table

| Option | Pros | Cons | Free Tier? | Difficulty | Recommendation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. GitHub Actions** | **Completely Free**, No credit card needed, Easy to setup (just a YAML file). | Runs on schedule (min every 5 mins), not truly "always on" (good enough for news). | ✅ Yes (2000 mins/month) | ⭐ Easy | **Highest** (Best for this project) |
| **2. Google Cloud Functions** | Native integration with Firebase, Professional standard. | Need to enable billing (credit card), slight learning curve. | ✅ Yes (2M calls/month) | ⭐⭐ Medium | **High** (If you want to be professional) |
| **3. Home PC / Raspberry Pi** | Total control, $0 cost if you have hardware. | PC must be on 24/7, unstable if power/net cuts out. | ✅ Yes | ⭐⭐⭐ Hard (Maintenance) | Low |
| **4. Android App (WorkManager)** | No external server needed. | Drains battery, unreliable (OS kills bg processes), limited processing power. | ✅ Yes | ⭐ Easy | Low (Current pain point) |

### Why GitHub Actions?

For a news aggregator that updates every 15-30 minutes, you do **not** need a 24/7 server running. You just need something to "wake up, fetch news, save to DB, and sleep."
**GitHub Actions** is perfect for this. We can create a workflow that runs a Python script on a schedule (CRON).

**Cost:** $0.
**Maintenance:** Zero (handled by GitHub).
**Scalability:** High.

If you proceed, I will write the **GitHub Action YAML file** and the **Python script** for you.
