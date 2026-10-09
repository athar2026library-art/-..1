const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { buildUserPrompt } = require("./lib/helpers");

initializeApp();

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GEMINI_MODEL = "gemini-2.5-flash";

const SYSTEM_INSTRUCTION =
  "أنت مساعد إسلامي متخصص في الأذكار والدعاء. " +
  "اعتمد فقط على الأحاديث الصحيحة وكتاب حصن المسلم. " +
  "قدم إجاباتك بالعربية بأسلوب ميسر ومختصر وهادئ. " +
  "لا تفتِ ولا تصدر أحكاماً شرعية من عندك. " +
  "لا تخترع أحاديث أو أذكاراً من عندك.";

exports.generateGemini = onCall(
  {
    secrets: [GEMINI_API_KEY],
    region: "us-central1",
    enforceAppCheck: true,
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "يجب تسجيل الدخول.");
    }

    const mode = String(request.data?.mode || "suggest");
    const text = String(request.data?.text || "").trim().slice(0, 800);

    if (text.length < 2) {
      throw new HttpsError("invalid-argument", "النص قصير جداً.");
    }

    const apiKey = GEMINI_API_KEY.value();
    if (!apiKey) {
      throw new HttpsError("failed-precondition", "مفتاح Gemini غير مُعد.");
    }

    const userPrompt = buildUserPrompt(mode, text);

    const url =
      `https://generativelanguage.googleapis.com/v1beta/models/` +
      `${GEMINI_MODEL}:generateContent?key=${encodeURIComponent(apiKey)}`;

    let response;
    try {
      response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [
            {
              parts: [{ text: userPrompt }],
            },
          ],
          systemInstruction: {
            parts: [{ text: SYSTEM_INSTRUCTION }],
          },
          generationConfig: {
            thinkingConfig: {
              thinkingLevel: "low",
            },
            maxOutputTokens: 320,
          },
        }),
      });
    } catch (err) {
      console.error("Gemini network error", err);
      throw new HttpsError("unavailable", "تعذر الاتصال بـ Gemini.");
    }

    if (!response.ok) {
      const body = await response.text().catch(() => "");
      console.error("Gemini HTTP error", response.status, body);
      if (response.status === 429) {
        throw new HttpsError("resource-exhausted", "حد الاستخدام.");
      }
      throw new HttpsError("internal", "فشل طلب Gemini.");
    }

    const json = await response.json();
    const out =
      json?.candidates?.[0]?.content?.parts
        ?.map((p) => p.text)
        .filter(Boolean)
        .join("\n")
        ?.trim() || "";

    if (!out) {
      throw new HttpsError("internal", "استجابة فارغة من النموذج.");
    }

    return { text: out };
  }
);
