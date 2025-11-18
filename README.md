## PayPal Java SDK v6 Direct Card Demo

A lightweight Spring Boot backend plus static UI that lets operators bind their own PayPal REST credentials and run direct card captures using the latest Checkout Java SDK (v6). The app emphasizes frictionless payments and keeps security intentionally minimal per requirements.

### Stack
- Java 8, Spring Boot 2.7 (REST + validation)
- PayPal Checkout Java SDK `com.paypal.sdk:checkout-sdk:1.0.5`
- Static HTML/JS frontend (served by Spring Boot)
- Optional Docker packaging + Vercel deployment

### Local setup
1. Ensure Java 8+ and Maven are installed (or run via Docker).
2. Copy your PayPal client ID/secret (sandbox or live).
3. Start the app:
   ```bash
   mvn spring-boot:run
   ```
4. Open `http://localhost:8080` and follow the two-step flow (bind credentials, then run a card payment).

> Credentials are cached in-memory for ~12h only. Restarting the app clears them.

### API
- `POST /api/credentials/validate`
  ```json
  {
    "clientId": "PAYPAL_CLIENT_ID",
    "clientSecret": "PAYPAL_SECRET",
    "environment": "SANDBOX|LIVE"
  }
  ```
  Returns `{ valid, credentialToken, environment }`. Use the token for subsequent payments.

- `POST /api/payment/process`
  ```json
  {
    "credentialToken": "uuid-from-validate",
    "amount": 10.50,
    "currencyCode": "USD",
    "cardNumber": "4111111111111111",
    "expiry": "2026-12",
    "securityCode": "123",
    "cardholderName": "Test User",
    "billingAddress": { "addressLine1": "...", "countryCode": "US" }
  }
  ```
  Responds with `{ status, orderId, rawResponse }` so operators can retry quickly.

### Frontend behavior
- Plain forms plus a minimal status area for feedback/retries.
- After credentials pass validation the page dynamically injects the PayPal JS SDK tag: `https://www.paypal.com/sdk/js?client-id=<bound-id>`.
- No authentication, throttling, or persistence layers are added so the operator can iterate quickly.

### Switching sandbox/live
- Choose the desired environment in step 1 of the UI or payload.
- Use sandbox cards from https://developer.paypal.com/tools/sandbox/card-testing/ during testing.
- For live mode you must onboard the PayPal account for Advanced Credit & Debit Card / direct card support, otherwise PayPal will return `INSTRUMENT_DECLINED` or `PAYER_ACTION_REQUIRED`.

### Docker build
```bash
docker build -t paypal-java-v6 .
docker run -p 8080:8080 -e JAVA_OPTS="-Xms256m -Xmx512m" paypal-java-v6
```

### Deploying on Render

Render provides excellent Docker support for Spring Boot applications. Deploy using one of these methods:

#### Option 1: Using Render Dashboard (Recommended)
1. Sign up/login at [render.com](https://render.com)
2. Click "New +" → "Web Service"
3. Connect your Git repository (GitHub/GitLab/Bitbucket)
4. Render will auto-detect the `Dockerfile` and `render.yaml`
5. Click "Create Web Service"
6. Your app will be live at `https://your-app-name.onrender.com`

#### Option 2: Using Render CLI
```bash
npm install -g render-cli
render login
render deploy
```

The `render.yaml` file configures:
- Docker-based deployment
- Free tier plan
- Automatic port binding (Render sets PORT env var)
- Health check endpoint

#### Option 3: Manual Docker Deploy
```bash
# Build and push to a container registry, then deploy via Render dashboard
docker build -t paypal-java-v6 .
docker tag paypal-java-v6 your-registry/paypal-java-v6
docker push your-registry/paypal-java-v6
```

The build pipeline will:
1. Build the JAR through Maven (multi-stage Dockerfile)
2. Run the Spring Boot server on the port provided by Render (automatically configured)

### Operational notes
- Because 3DS and risk checks are disabled whenever PayPal allows it, expect higher decline rates for issuers that demand SCA. The API still surfaces PayPal’s full response so operators can decide whether to retry.
- HTTPS, authentication, persistence, and rate-limiters are intentionally omitted; deploy behind trusted networks only.
- Monitor PayPal dashboard for error spikes; the app does not add analytics/alerts beyond HTTP responses.

### Next steps
- Add optional persistence (Redis) for credential tokens if multi-node operation is needed.
- Implement audit logging or minimal alerting before going live.
- Consider splitting the frontend to a static host if you need CDN caching; the backend already exposes simple JSON endpoints for easy integration.

