#!/bin/bash

echo "VoiceGram - GitHub Setup Script"
echo "================================"
echo ""

# Check if git is initialized
if [ ! -d ".git" ]; then
    echo "Initializing git repository..."
    git init
    git add .
    git commit -m "Initial commit - VoiceGram Android project"
    echo "✓ Git repository initialized"
else
    echo "✓ Git repository already exists"
fi

echo ""
echo "Next steps:"
echo "1. Create a new repository on GitHub: https://github.com/new"
echo "2. Run the following commands (replace YOUR_USERNAME and REPO_NAME):"
echo ""
echo "   git remote add origin https://github.com/YOUR_USERNAME/REPO_NAME.git"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "3. The GitHub Actions build will start automatically"
echo "4. Download the APK from the Actions tab in your repository"
