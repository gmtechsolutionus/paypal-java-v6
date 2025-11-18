# Quick Deployment Guide - Render

## Easiest Method: Render Dashboard

1. **Sign up/Login**: Go to https://render.com and create a free account

2. **Create New Web Service**:
   - Click "New +" button → "Web Service"
   - Connect your Git repository (GitHub/GitLab/Bitbucket) OR
   - Use "Public Git repository" and paste your repo URL

3. **Configure**:
   - **Name**: `paypal-java-v6` (or any name you prefer)
   - **Environment**: `Docker`
   - Render will auto-detect the `Dockerfile` and `render.yaml`
   - **Plan**: Free (or choose a paid plan for better performance)

4. **Deploy**:
   - Click "Create Web Service"
   - Render will build and deploy automatically
   - Your app will be live at `https://your-app-name.onrender.com`

## Using Render CLI (Alternative)

If you prefer CLI deployment:

```bash
# Login to Render
render login

# Deploy (requires Git repository)
render deploy
```

## Important Notes

- **Free Tier**: Apps on free tier spin down after 15 minutes of inactivity (first request may be slow)
- **Environment Variables**: No additional env vars needed - the app uses in-memory credential storage
- **Port**: Render automatically sets the `PORT` environment variable - the Dockerfile is configured to use it
- **HTTPS**: Render provides HTTPS automatically

## Testing After Deployment

1. Visit your Render URL (e.g., `https://paypal-java-v6.onrender.com`)
2. Enter your PayPal sandbox credentials (Client ID and Secret)
3. Test with a sandbox card from https://developer.paypal.com/tools/sandbox/card-testing/

## Troubleshooting

- **Build fails**: Check Render logs in the dashboard
- **App won't start**: Verify the Dockerfile builds correctly locally first
- **Slow first request**: Normal on free tier (app spins up from sleep)

