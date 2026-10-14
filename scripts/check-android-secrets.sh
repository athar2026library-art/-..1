#!/usr/bin/env bash
set -euo pipefail

# Gemini credentials must stay in server-side Secret Manager, never in Android BuildConfig.
if git grep -nE 'buildConfigField.*GEMINI_API_KEY|BuildConfig\\.GEMINI_API_KEY|geminiApiKey' -- app; then
  echo "::error::Gemini key configuration found in Android client sources."
  exit 1
fi

# The Android validation workflow must not write the Gemini secret into local.properties.
if grep -nE 'secrets\\.GEMINI_API_KEY.*local\\.properties|local\\.properties.*secrets\\.GEMINI_API_KEY' .github/workflows/android-validation.yml; then
  echo "::error::Gemini secret must not be injected into the Android CI build."
  exit 1
fi

echo "Security guard passed: no Gemini secret injection into Android client/build."
