#!/bin/bash

echo "=== VoiceGram Android Project - GitHub Setup ==="
echo ""

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI not found. Please install it first:"
    echo "   brew install gh"
    exit 1
fi

# Check authentication
echo "🔍 Checking GitHub authentication..."
if ! gh auth status &> /dev/null; then
    echo "❌ Not authenticated with GitHub. Please run:"
    echo "   gh auth login"
    echo ""
    echo "Then run this script again."
    exit 1
fi

echo "✅ GitHub authentication confirmed"
echo ""

# Get GitHub username
GITHUB_USERNAME=$(gh api user --jq '.login')
echo "👤 GitHub username: $GITHUB_USERNAME"
echo ""

# Ask for repository name
read -p "📦 Enter repository name (default: voicegram): " REPO_NAME
REPO_NAME=${REPO_NAME:-voicegram}
echo ""

# Clean up any partial git initialization
echo "🧹 Cleaning up any partial git initialization..."
rm -rf .git 2>/dev/null || true
echo "✅ Cleanup complete"
echo ""

# Initialize git repository
echo "🔧 Initializing git repository..."
git init
git add .
git commit -m "Initial VoiceGram Android project with GitHub Actions

- Complete Android project structure
- MVVM architecture with service, manager, and model layers
- GitHub Actions workflow for automatic APK builds
- Gradle build configuration
- Basic UI and resource files"
echo "✅ Git repository initialized"
echo ""

# Create GitHub repository
echo "🌐 Creating GitHub repository: $GITHUB_USERNAME/$REPO_NAME..."
gh repo create "$REPO_NAME" --public --source=. --remote=origin --description "VoiceGram - Voice-to-text Telegram Android application" --push
echo "✅ Repository created and code pushed"
echo ""

echo "🎉 Setup complete!"
echo ""
echo "📱 Your repository is available at: https://github.com/$GITHUB_USERNAME/$REPO_NAME"
echo ""
echo "🔄 GitHub Actions will automatically start building your Android APK."
echo "📥 Download the APK from: https://github.com/$GITHUB_USERNAME/$REPO_NAME/actions"
echo ""
echo "Next steps:"
echo "1. Wait for the GitHub Actions build to complete"
echo "2. Download the APK from the Actions tab"
echo "3. Install on your Android device"
