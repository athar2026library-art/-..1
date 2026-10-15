import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const data = JSON.parse(readFileSync("admin-panel/hisn_al_muslim.json", "utf8"));
const chapters = data.chapters;
assert.equal(chapters.length, 133, "The book should contain 133 ordered chapters.");

const allItems = chapters.flatMap((chapter) => chapter.adhkar);
assert.equal(allItems.length, 302, "The book should contain 302 dhikr records.");
assert.equal(new Set(allItems.map((item) => item.id)).size, allItems.length, "Dhikr IDs must be unique.");

for (const [index, chapter] of chapters.entries()) {
  assert.equal(chapter.order, index + 1, `Chapter order mismatch at index ${index}.`);
  assert.equal(chapter.id, `hisn_${String(index + 1).padStart(3, "0")}`, "Chapter IDs must be stable.");
  assert.ok(chapter.title.trim(), `Chapter ${chapter.order} needs a title.`);
  assert.ok(Array.isArray(chapter.adhkar) && chapter.adhkar.length > 0, `Chapter ${chapter.order} needs dhikr.`);
  assert.ok(/^https:\/\//.test(chapter.audioUrl || ""), `Chapter ${chapter.order} needs an HTTPS audio URL.`);
  for (const [itemIndex, item] of chapter.adhkar.entries()) {
    assert.ok(item.text.trim(), `Missing text in chapter ${chapter.order}, item ${itemIndex + 1}.`);
    assert.ok(item.source.trim(), `Missing reference in chapter ${chapter.order}, item ${itemIndex + 1}.`);
    assert.ok(Number.isInteger(item.repeatCount) && item.repeatCount > 0, "Repeat counts must be positive integers.");
  }
}

const notice = readFileSync("docs/NOTICE-HISN-AL-MUSLIM.md", "utf8");
assert.ok(notice.includes("Abdellah SELLAM"), "Source attribution must remain in the repository.");
assert.ok(notice.includes("MIT License"), "The transcription license must be documented.");

console.log(`Validated ${chapters.length} chapters and ${allItems.length} dhikr entries, including references, counts, audio, and attribution.`);
