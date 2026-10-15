#!/usr/bin/env bash
set -euo pipefail
# Restore PLACEHOLDER files and fix R imports
for f in \
  app/src/main/java/com/example/MainActivity.kt \
  app/src/main/java/com/example/ui/NotificationWorker.kt \
  app/src/main/java/com/example/ui/screens/SettingsScreen.kt \
  app/src/main/java/com/example/ui/AppViewModel.kt
do
  if [ -f "$f" ] && grep -q '^PLACEHOLDER$' "$f" 2>/dev/null; then
    git show origin/main:"$f" > "$f"
  fi
  if [ -f "$f" ]; then
    sed -i 's/com\.example\.R/com.baqiyat.azkar.R/g' "$f"
  fi
done
# Harden deleteAccount messages in ViewModel if not already
if ! grep -q 'يمكنك إعادة المحاولة بأمان' app/src/main/java/com/example/ui/AppViewModel.kt 2>/dev/null; then
  sed -i 's/تعذر حذف الحساب\. حاول لاحقاً\./تعذر حذف الحساب. حاول لاحقاً (يمكنك إعادة المحاولة بأمان)./' app/src/main/java/com/example/ui/AppViewModel.kt || true
fi
echo "R import fix applied"
