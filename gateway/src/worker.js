const MAX_BODY_BYTES = 12 * 1024 * 1024;
const MAX_IMAGE_BYTES = 8 * 1024 * 1024;
const ALLOWED_USDA_TYPES = new Set(["Foundation", "SR Legacy"]);
const ALLOWED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"]);

export const NUTRIENT_UNITS = Object.freeze({
  ENERGY: "kcal",
  ENERGY_FROM_FAT: "kcal",
  PROTEIN: "g",
  TOTAL_CARBOHYDRATE: "g",
  TOTAL_FAT: "g",
  SATURATED_FAT: "g",
  MONOUNSATURATED_FAT: "g",
  POLYUNSATURATED_FAT: "g",
  UNSATURATED_FAT: "g",
  TRANS_FAT: "g",
  DIETARY_FIBER: "g",
  TOTAL_SUGAR: "g",
  ADDED_SUGAR: "g",
  SODIUM: "mg",
  CHOLESTEROL: "mg",
  CAFFEINE: "mg",
  WATER: "g",
  CALCIUM: "mg",
  CHLORIDE: "mg",
  CHROMIUM: "mcg",
  COPPER: "mg",
  FOLATE: "mcg",
  FOLIC_ACID: "mcg",
  IODINE: "mcg",
  IRON: "mg",
  MAGNESIUM: "mg",
  MANGANESE: "mg",
  MOLYBDENUM: "mcg",
  NIACIN: "mg",
  PANTOTHENIC_ACID: "mg",
  PHOSPHORUS: "mg",
  POTASSIUM: "mg",
  RIBOFLAVIN: "mg",
  SELENIUM: "mcg",
  THIAMIN: "mg",
  VITAMIN_A: "mcg",
  VITAMIN_B6: "mg",
  VITAMIN_B12: "mcg",
  VITAMIN_C: "mg",
  VITAMIN_D: "mcg",
  VITAMIN_E: "mg",
  VITAMIN_K: "mcg",
  ZINC: "mg",
  BIOTIN: "mcg",
});

const nutrientProperties = Object.fromEntries(
  Object.entries(NUTRIENT_UNITS).map(([key, unit]) => [key, {
    type: "object",
    additionalProperties: false,
    required: ["value", "unit", "basis", "source"],
    properties: {
      value: { type: ["number", "null"], minimum: 0, maximum: 100000 },
      unit: { type: "string", const: unit },
      basis: { type: ["string", "null"], maxLength: 180 },
      source: { type: "string", enum: ["PACKAGE_LABEL", "AI_ESTIMATE"] },
    },
  }]),
);

export const PHOTO_SCHEMA = Object.freeze({
  type: "object",
  additionalProperties: false,
  required: [
    "schema_version", "source_type", "name", "generic_name", "usda_query", "kind",
    "amount_value", "amount_unit", "meal_type", "confidence", "nutrients", "warnings",
  ],
  properties: {
    schema_version: { type: "integer", const: 1 },
    source_type: { type: "string", enum: ["INGREDIENT", "PACKAGE"] },
    name: { type: "string", minLength: 1, maxLength: 120 },
    generic_name: { type: "string", maxLength: 120 },
    usda_query: { type: "string", maxLength: 160 },
    kind: { type: "string", enum: ["FOOD", "DRINK"] },
    amount_value: { type: "number", exclusiveMinimum: 0, maximum: 100000 },
    amount_unit: { type: "string", enum: ["GRAM", "MILLILITRE", "SERVING"] },
    meal_type: { type: "string", enum: ["BREAKFAST", "LUNCH", "DINNER", "SNACK", "LATE_NIGHT", "COOKING_OIL", "UNKNOWN"] },
    confidence: { type: "number", minimum: 0, maximum: 1 },
    nutrients: {
      type: "object",
      additionalProperties: false,
      required: Object.keys(NUTRIENT_UNITS),
      properties: nutrientProperties,
    },
    warnings: { type: "array", maxItems: 12, items: { type: "string", maxLength: 240 } },
  },
});

const USDA_NAMES = Object.freeze({
  "energy": "ENERGY",
  "energy (atwater general factors)": "ENERGY",
  "energy (atwater specific factors)": "ENERGY",
  "protein": "PROTEIN",
  "total lipid (fat)": "TOTAL_FAT",
  "fatty acids, total saturated": "SATURATED_FAT",
  "fatty acids, total monounsaturated": "MONOUNSATURATED_FAT",
  "fatty acids, total polyunsaturated": "POLYUNSATURATED_FAT",
  "fatty acids, total trans": "TRANS_FAT",
  "carbohydrate, by difference": "TOTAL_CARBOHYDRATE",
  "fiber, total dietary": "DIETARY_FIBER",
  "sugars, total including nlea": "TOTAL_SUGAR",
  "sugars, total": "TOTAL_SUGAR",
  "sugars, added": "ADDED_SUGAR",
  "sodium, na": "SODIUM",
  "cholesterol": "CHOLESTEROL",
  "caffeine": "CAFFEINE",
  "water": "WATER",
  "calcium, ca": "CALCIUM",
  "chlorine, cl": "CHLORIDE",
  "chromium, cr": "CHROMIUM",
  "copper, cu": "COPPER",
  "folate, total": "FOLATE",
  "folic acid": "FOLIC_ACID",
  "iodine, i": "IODINE",
  "iron, fe": "IRON",
  "magnesium, mg": "MAGNESIUM",
  "manganese, mn": "MANGANESE",
  "molybdenum, mo": "MOLYBDENUM",
  "niacin": "NIACIN",
  "pantothenic acid": "PANTOTHENIC_ACID",
  "phosphorus, p": "PHOSPHORUS",
  "potassium, k": "POTASSIUM",
  "riboflavin": "RIBOFLAVIN",
  "selenium, se": "SELENIUM",
  "thiamin": "THIAMIN",
  "vitamin a, rae": "VITAMIN_A",
  "vitamin b-6": "VITAMIN_B6",
  "vitamin b-12": "VITAMIN_B12",
  "vitamin c, total ascorbic acid": "VITAMIN_C",
  "vitamin d (d2 + d3)": "VITAMIN_D",
  "vitamin e (alpha-tocopherol)": "VITAMIN_E",
  "vitamin k (phylloquinone)": "VITAMIN_K",
  "zinc, zn": "ZINC",
  "biotin": "BIOTIN",
});
const jsonHeaders = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "x-content-type-options": "nosniff",
  "referrer-policy": "no-referrer",
};

function jsonResponse(value, status = 200) {
  return new Response(JSON.stringify(value), { status, headers: jsonHeaders });
}

async function readJson(request) {
  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_BODY_BYTES) throw new HttpError(413, "Request too large");
  const bytes = await request.arrayBuffer();
  if (bytes.byteLength > MAX_BODY_BYTES) throw new HttpError(413, "Request too large");
  try {
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    throw new HttpError(400, "Invalid JSON");
  }
}

class HttpError extends Error {
  constructor(status, publicMessage) {
    super(publicMessage);
    this.status = status;
    this.publicMessage = publicMessage;
  }
}

async function secureEqual(left, right) {
  const encode = (value) => new TextEncoder().encode(value);
  const [a, b] = await Promise.all([
    crypto.subtle.digest("SHA-256", encode(left)),
    crypto.subtle.digest("SHA-256", encode(right)),
  ]);
  const aa = new Uint8Array(a);
  const bb = new Uint8Array(b);
  let difference = 0;
  for (let index = 0; index < aa.length; index += 1) difference |= aa[index] ^ bb[index];
  return difference === 0;
}

async function authorized(request, env) {
  if (!env.APP_ACCESS_TOKEN || env.APP_ACCESS_TOKEN.length < 16) return false;
  const header = request.headers.get("authorization") || "";
  if (!header.startsWith("Bearer ")) return false;
  return secureEqual(header.slice(7), env.APP_ACCESS_TOKEN);
}

function requiredEnv(env, name) {
  const value = env[name];
  if (typeof value !== "string" || !value.trim()) throw new HttpError(503, "Gateway provider is not configured");
  return value.trim();
}
export function usdaApiKey(env) {
  const value = env.USDA_API_KEY;
  return typeof value === "string" && value.trim() ? value.trim() : "DEMO_KEY";
}

export default {
  async fetch(request, env) {
    try {
      if (request.method !== "POST") return jsonResponse({ error: "Not found" }, 404);
      if (!(await authorized(request, env))) return jsonResponse({ error: "Unauthorized" }, 401);
      const path = new URL(request.url).pathname.replace(/\/+$/, "");
      if (path === "/v1/photo/analyze") return await analyzePhoto(request, env);
      if (path === "/v1/usda/search") return await searchUsda(request, env);
      if (path === "/v1/usda/food") return await getUsdaFood(request, env);
      return jsonResponse({ error: "Not found" }, 404);
    } catch (error) {
      if (error instanceof HttpError) return jsonResponse({ error: error.publicMessage }, error.status);
      return jsonResponse({ error: "Gateway request failed" }, 500);
    }
  },
};

async function analyzePhoto(request, env) {
  const body = await readJson(request);
  if (body?.schema_version !== 1 || !ALLOWED_IMAGE_TYPES.has(body?.mime_type)) {
    throw new HttpError(400, "Invalid photo request");
  }
  if (typeof body.image_base64 !== "string" || !/^[A-Za-z0-9+/]+={0,2}$/.test(body.image_base64)) {
    throw new HttpError(400, "Invalid photo request");
  }
  const decodedSize = Math.floor(body.image_base64.length * 3 / 4) - (body.image_base64.endsWith("==") ? 2 : body.image_base64.endsWith("=") ? 1 : 0);
  if (decodedSize <= 0 || decodedSize > MAX_IMAGE_BYTES) throw new HttpError(413, "Photo is too large");

  const apiKey = requiredEnv(env, "OPENAI_API_KEY");
  const model = requiredEnv(env, "OPENAI_MODEL");
  const openAiRequest = {
    model,
    store: false,
    input: [{
      role: "user",
      content: [
        {
          type: "input_text",
          text: [
            "Analyze this single food or drink photo for a review-before-save diet log.",
            "First classify it as INGREDIENT or PACKAGE. For PACKAGE, transcribe only nutrition values visibly present on the label as PACKAGE_LABEL; missing fields must be null, never inferred from a generic product.",
            "For INGREDIENT, cautious estimates may use AI_ESTIMATE. A visible zero is numeric zero; unknown is null.",
            "Return amounts for the photographed portion, choose a generic USDA search query, and add uncertainty warnings. Never make medical claims.",
          ].join(" "),
        },
        { type: "input_image", image_url: `data:${body.mime_type};base64,${body.image_base64}`, detail: "high" },
      ],
    }],
    text: {
      format: {
        type: "json_schema",
        name: "diet_photo_draft",
        strict: true,
        schema: PHOTO_SCHEMA,
      },
    },
  };
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      authorization: `Bearer ${apiKey}`,
      "content-type": "application/json",
    },
    body: JSON.stringify(openAiRequest),
  });
  if (!response.ok) {
    await response.text();
    throw new HttpError(502, "OpenAI analysis failed");
  }
  const payload = await response.json();
  const text = extractOutputText(payload);
  let result;
  try {
    result = JSON.parse(text);
  } catch {
    throw new HttpError(502, "OpenAI returned an invalid structured result");
  }
  validatePhotoResult(result);
  return jsonResponse(result);
}

function extractOutputText(payload) {
  for (const item of payload?.output || []) {
    for (const content of item?.content || []) {
      if (content?.type === "output_text" && typeof content.text === "string") return content.text;
    }
  }
  throw new HttpError(502, "OpenAI returned no structured result");
}

function validatePhotoResult(value) {
  if (value?.schema_version !== 1 || typeof value.name !== "string" || !value.name.trim()) {
    throw new HttpError(502, "OpenAI returned an invalid structured result");
  }
  if (!value.nutrients || Object.keys(NUTRIENT_UNITS).some((key) => !(key in value.nutrients))) {
    throw new HttpError(502, "OpenAI returned an incomplete structured result");
  }
  for (const [key, unit] of Object.entries(NUTRIENT_UNITS)) {
    const item = value.nutrients[key];
    if (!item || item.unit !== unit || (item.value !== null && (!Number.isFinite(item.value) || item.value < 0 || item.value > 100000))) {
      throw new HttpError(502, "OpenAI returned an invalid nutrient field");
    }
  }
}
async function searchUsda(request, env) {
  const body = await readJson(request);
  const query = typeof body?.query === "string" ? body.query.trim() : "";
  if (body?.schema_version !== 1 || query.length < 2 || query.length > 120 || !Array.isArray(body.data_types)) {
    throw new HttpError(400, "Invalid USDA search request");
  }
  if (body.data_types.length === 0 || body.data_types.some((value) => !ALLOWED_USDA_TYPES.has(value))) {
    throw new HttpError(400, "Only USDA Foundation and SR Legacy are allowed");
  }
  const key = usdaApiKey(env);
  const url = new URL("https://api.nal.usda.gov/fdc/v1/foods/search");
  url.searchParams.set("api_key", key);
  const response = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      query,
      dataType: body.data_types,
      pageSize: 25,
      pageNumber: 1,
    }),
  });
  if (!response.ok) {
    await response.text();
    throw new HttpError(502, "USDA search failed");
  }
  const payload = await response.json();
  const foods = (payload?.foods || [])
    .filter((food) => ALLOWED_USDA_TYPES.has(food?.dataType))
    .slice(0, 25)
    .map(normalizeUsdaFood);
  return jsonResponse({ schema_version: 1, foods });
}

async function getUsdaFood(request, env) {
  const body = await readJson(request);
  const id = Number(body?.fdc_id);
  if (body?.schema_version !== 1 || !Number.isSafeInteger(id) || id <= 0) {
    throw new HttpError(400, "Invalid USDA food request");
  }
  const key = usdaApiKey(env);
  const url = new URL(`https://api.nal.usda.gov/fdc/v1/food/${id}`);
  url.searchParams.set("api_key", key);
  const response = await fetch(url);
  if (response.status === 404) return jsonResponse({ schema_version: 1, food: null });
  if (!response.ok) {
    await response.text();
    throw new HttpError(502, "USDA food lookup failed");
  }
  const payload = await response.json();
  if (!ALLOWED_USDA_TYPES.has(payload?.dataType)) throw new HttpError(502, "USDA returned a disallowed data type");
  return jsonResponse({ schema_version: 1, food: normalizeUsdaFood(payload) });
}

export function normalizeUsdaFood(food) {
  const id = Number(food?.fdcId);
  if (!Number.isSafeInteger(id) || id <= 0 || !ALLOWED_USDA_TYPES.has(food?.dataType)) {
    throw new HttpError(502, "USDA returned invalid food data");
  }
  const nutrients = {};
  for (const raw of food.foodNutrients || []) {
    const nutrient = raw?.nutrient || raw;
    const name = String(nutrient?.name ?? raw?.nutrientName ?? "").trim().toLowerCase();
    const key = USDA_NAMES[name];
    if (!key || key in nutrients) continue;
    const amount = Number(raw?.amount ?? raw?.value);
    if (!Number.isFinite(amount) || amount < 0 || amount > 100000) continue;
    const expected = NUTRIENT_UNITS[key];
    const rawUnit = String(nutrient?.unitName ?? raw?.unitName ?? "").trim().toUpperCase().replace("Μ", "U").replace("µ", "U");
    const accepted = expected === "mcg" ? rawUnit === "UG" || rawUnit === "MCG" : rawUnit === expected.toUpperCase();
    if (!accepted) continue;
    nutrients[key] = amount;
  }
  return {
    fdc_id: id,
    description: String(food.description || "USDA food").trim().slice(0, 240),
    data_type: food.dataType,
    nutrients,
  };
}