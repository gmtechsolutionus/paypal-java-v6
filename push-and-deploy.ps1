# Push to GitHub and Deploy to Render Script

Write-Host "=== PayPal Java V6 - Push and Deploy ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check GitHub authentication
Write-Host "Step 1: Checking GitHub authentication..." -ForegroundColor Yellow
$env:PATH="C:\Program Files\GitHub CLI;$env:PATH"
gh auth status 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "GitHub authentication required!" -ForegroundColor Red
    Write-Host "Please run: gh auth login" -ForegroundColor Yellow
    Write-Host "Then run this script again." -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ GitHub authenticated" -ForegroundColor Green
Write-Host ""

# Step 2: Create GitHub repository
Write-Host "Step 2: Creating GitHub repository..." -ForegroundColor Yellow
$repoName = "paypal-java-v6"
gh repo create $repoName --public --source=. --remote=origin --push 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Repository created and code pushed to GitHub" -ForegroundColor Green
    $repoUrl = (gh repo view $repoName --json url -q .url)
    Write-Host "  Repository URL: $repoUrl" -ForegroundColor Cyan
} else {
    Write-Host "Repository might already exist or there was an error." -ForegroundColor Yellow
    Write-Host "Trying to push to existing remote..." -ForegroundColor Yellow
    
    # Check if remote exists
    $remoteExists = git remote get-url origin 2>&1
    if ($LASTEXITCODE -eq 0) {
        git branch -M main 2>&1 | Out-Null
        git push -u origin main 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Code pushed to GitHub" -ForegroundColor Green
        } else {
            Write-Host "Error pushing to GitHub. Please check your remote configuration." -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "No remote configured. Please create a GitHub repository manually and add it as remote." -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Step 3: Deploy to Render
Write-Host "Step 3: Deploying to Render..." -ForegroundColor Yellow
Write-Host ""
Write-Host "To deploy on Render:" -ForegroundColor Cyan
Write-Host "1. Go to https://render.com and sign in" -ForegroundColor White
Write-Host "2. Click 'New +' → 'Web Service'" -ForegroundColor White
Write-Host "3. Connect your GitHub account" -ForegroundColor White
Write-Host "4. Select the '$repoName' repository" -ForegroundColor White
Write-Host "5. Render will auto-detect the Dockerfile" -ForegroundColor White
Write-Host "6. Click 'Create Web Service'" -ForegroundColor White
Write-Host ""
Write-Host "Or use Render CLI:" -ForegroundColor Cyan
Write-Host "  render login" -ForegroundColor White
Write-Host "  render deploy" -ForegroundColor White
Write-Host ""

Write-Host "=== Done! ===" -ForegroundColor Green

