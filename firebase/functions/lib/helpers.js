function uniqueTokens(tokens) {
  return [...new Set((tokens || []).filter((token) => typeof token === "string" && token.trim()))];
}

function buildUserPrompt(mode, text) {
  if (mode === "explain") {
    return `قم بشرح وتدبر هذا الذكر بأسلوب إيماني، ميسر ومختصر جداً:\n\n"${text}"`;
  }
  return (
    `أشعر بـ (${text}) أو أحتاج إلى دعاء بهذا الخصوص. اقترح لي ذكراً أو دعاءً من الأحاديث الصحيحة وحصن المسلم يناسب حالتي.\n` +
    "نرجو الرد بالتنسيق التالي حصراً:\nالذكر: [النص]\nفضله: [شرح مبسط]\nالمصدر: [المرجع]"
  );
}

module.exports = { uniqueTokens, buildUserPrompt };
