import assert from "node:assert/strict";
import test from "node:test";
import worker, { NUTRIENT_UNITS, PHOTO_SCHEMA, normalizeUsdaFood } from "../src/worker.js";

test("strict photo schema covers every Android nutrient", () => {
  assert.equal(PHOTO_SCHEMA.additionalProperties, false);
  assert.deepEqual(new Set(PHOTO_SCHEMA.properties.nutrients.required), new Set(Object.keys(NUTRIENT_UNITS)));
  assert.equal(PHOTO_SCHEMA.properties.nutrients.additionalProperties, false);
});

test("USDA normalization keeps allowed type, units, and missing fields", () => {
  const food = normalizeUsdaFood({
    fdcId: 123,
    dataType: "Foundation",
    description: "Test oats",
    foodNutrients: [
      { nutrient: { name: "Energy", unitName: "KCAL" }, amount: 389 },
      { nutrient: { name: "Protein", unitName: "G" }, amount: 16.9 },
      { nutrient: { name: "Vitamin B-12", unitName: "UG" }, amount: 0 },
      { nutrient: { name: "Sodium, Na", unitName: "G" }, amount: 1 },
    ],
  });
  assert.equal(food.data_type, "Foundation");
  assert.equal(food.nutrients.ENERGY, 389);
  assert.equal(food.nutrients.PROTEIN, 16.9);
  assert.equal(food.nutrients.VITAMIN_B12, 0);
  assert.equal(food.nutrients.SODIUM, undefined);
});

test("USDA normalization rejects branded foods", () => {
  assert.throws(() => normalizeUsdaFood({ fdcId: 9, dataType: "Branded", description: "x" }));
});

test("gateway rejects missing bearer token before any provider call", async () => {
  const response = await worker.fetch(
    new Request("https://gateway.example/v1/photo/analyze", { method: "POST", body: "{}" }),
    { APP_ACCESS_TOKEN: "1234567890123456" },
  );
  assert.equal(response.status, 401);
  assert.equal(response.headers.get("cache-control"), "no-store");
});