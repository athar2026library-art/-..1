const test = require("node:test");
const assert = require("node:assert/strict");
const { uniqueTokens, buildUserPrompt } = require("../lib/helpers");

test("uniqueTokens removes blanks, non-strings, and duplicates", () => {
  assert.deepEqual(
    uniqueTokens(["token-a", "", " token-a ", null, 42, "token-b"]),
    ["token-a", " token-a ", "token-b"],
  );
});

test("uniqueTokens accepts missing input", () => {
  assert.deepEqual(uniqueTokens(), []);
});

test("buildUserPrompt creates an explanation prompt", () => {
  const prompt = buildUserPrompt("explain", "آية الكرسي");
  assert.match(prompt, /آية الكرسي/);
  assert.match(prompt, /شرح وتدبر/);
});

test("buildUserPrompt creates a suggestion prompt with required sections", () => {
  const prompt = buildUserPrompt("suggest", "القلق");
  assert.match(prompt, /القلق/);
  assert.match(prompt, /الذكر: \[النص\]/);
  assert.match(prompt, /المصدر: \[المرجع\]/);
});
