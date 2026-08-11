package io.github.kanggod9.diettracker.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.kanggod9.diettracker.domain.AmountUnit
import io.github.kanggod9.diettracker.domain.EntryAmount
import io.github.kanggod9.diettracker.domain.EntryKind
import io.github.kanggod9.diettracker.domain.JournalEntry
import io.github.kanggod9.diettracker.domain.MealType
import io.github.kanggod9.diettracker.domain.QuickFood
import io.github.kanggod9.diettracker.domain.Suggestion
import io.github.kanggod9.diettracker.integration.UsdaCacheEntry
import io.github.kanggod9.diettracker.integration.UsdaDataType
import io.github.kanggod9.diettracker.integration.UsdaFood
import io.github.kanggod9.diettracker.integration.UsdaFoodCache
import java.time.Instant

data class LocalSnapshot(
    val entries: List<JournalEntry>,
    val quickFoods: List<QuickFood>,
    val settings: Map<String, String>,
    val suggestions: List<Suggestion> = emptyList(),
)

interface JournalRepository {
    fun entries(): List<JournalEntry>
    fun save(entry: JournalEntry)
    fun delete(id: String)
    fun deleteAllJournal()

    fun quickFoods(): List<QuickFood>
    fun saveQuickFood(food: QuickFood)
    fun deleteQuickFood(id: String)

    fun setting(key: String): String?
    fun settings(): Map<String, String>
    fun setSetting(key: String, value: String)
    fun removeSetting(key: String)

    fun replaceSuggestions(suggestions: List<Suggestion>)
    fun suggestions(): List<Suggestion>

    fun recordHealthExport(entryId: String, recordType: String, recordId: String, syncedAt: Instant)
    fun lastHealthExport(entryId: String, recordType: String): Instant?

    fun snapshot(): LocalSnapshot = LocalSnapshot(entries(), quickFoods(), settings(), suggestions())
    fun clearAllLocalData()
}

class LocalStore(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION),
    JournalRepository,
    UsdaFoodCache {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE journal_entry(
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                kind TEXT NOT NULL,
                meal_type TEXT NOT NULL,
                serving_description TEXT NOT NULL,
                serving_grams REAL,
                amount_value REAL NOT NULL,
                amount_unit TEXT NOT NULL,
                logged_at INTEGER NOT NULL,
                nutrients TEXT NOT NULL,
                note TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX journal_logged_at_idx ON journal_entry(logged_at)")
        db.execSQL(
            """
            CREATE TABLE quick_food(
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                kind TEXT NOT NULL,
                meal_type TEXT NOT NULL,
                serving_description TEXT NOT NULL,
                serving_grams REAL,
                amount_value REAL NOT NULL,
                amount_unit TEXT NOT NULL,
                nutrients TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE TABLE setting(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE suggestion(id TEXT PRIMARY KEY,message TEXT NOT NULL,source_title TEXT NOT NULL,source_url TEXT NOT NULL)",
        )
        db.execSQL(
            """
            CREATE TABLE health_export(
                entry_id TEXT NOT NULL,
                record_type TEXT NOT NULL,
                record_id TEXT NOT NULL,
                synced_at INTEGER NOT NULL,
                PRIMARY KEY(entry_id, record_type)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE usda_food_cache(
                fdc_id INTEGER PRIMARY KEY,
                description TEXT NOT NULL,
                data_type TEXT NOT NULL,
                nutrients TEXT NOT NULL,
                fetched_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE journal_entry ADD COLUMN amount_value REAL NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE journal_entry ADD COLUMN amount_unit TEXT NOT NULL DEFAULT 'SERVING'")
            db.execSQL("ALTER TABLE quick_food ADD COLUMN amount_value REAL NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE quick_food ADD COLUMN amount_unit TEXT NOT NULL DEFAULT 'SERVING'")
            db.execSQL("ALTER TABLE quick_food ADD COLUMN meal_type TEXT NOT NULL DEFAULT 'SNACK'")
        }
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS health_export(
                    entry_id TEXT NOT NULL,
                    record_type TEXT NOT NULL,
                    record_id TEXT NOT NULL,
                    synced_at INTEGER NOT NULL,
                    PRIMARY KEY(entry_id, record_type)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS journal_logged_at_idx ON journal_entry(logged_at)")
        }
        if (oldVersion < 4) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS usda_food_cache(
                    fdc_id INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    data_type TEXT NOT NULL,
                    nutrients TEXT NOT NULL,
                    fetched_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    override fun entries(): List<JournalEntry> = readableDatabase.query(
        "journal_entry",
        null,
        null,
        null,
        null,
        null,
        "logged_at DESC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val servingGrams = cursor.nullableDouble("serving_grams")
                add(
                    JournalEntry(
                        id = cursor.text("id"),
                        name = cursor.text("name"),
                        kind = EntryKind.valueOf(cursor.text("kind")),
                        mealType = MealType.valueOf(cursor.text("meal_type")),
                        servingDescription = cursor.text("serving_description"),
                        servingGrams = servingGrams,
                        amount = EntryAmount(
                            cursor.double("amount_value"),
                            AmountUnit.valueOf(cursor.text("amount_unit")),
                        ),
                        loggedAt = Instant.ofEpochMilli(cursor.long("logged_at")),
                        nutrients = NutrientCodec.decode(cursor.text("nutrients")),
                        note = cursor.text("note"),
                    ),
                )
            }
        }
    }

    override fun save(entry: JournalEntry) {
        writableDatabase.insertWithOnConflict(
            "journal_entry",
            null,
            ContentValues().apply {
                put("id", entry.id)
                put("name", entry.name)
                put("kind", entry.kind.name)
                put("meal_type", entry.mealType.name)
                put("serving_description", entry.servingDescription)
                entry.servingGrams?.let { put("serving_grams", it) } ?: putNull("serving_grams")
                put("amount_value", entry.amount.value)
                put("amount_unit", entry.amount.unit.name)
                put("logged_at", entry.loggedAt.toEpochMilli())
                put("nutrients", NutrientCodec.encode(entry.nutrients))
                put("note", entry.note)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun delete(id: String) {
        writableDatabase.transaction {
            delete("journal_entry", "id=?", arrayOf(id))
            delete("health_export", "entry_id=?", arrayOf(id))
        }
    }

    override fun deleteAllJournal() {
        writableDatabase.transaction {
            delete("journal_entry", null, null)
            delete("health_export", null, null)
        }
    }

    override fun quickFoods(): List<QuickFood> = readableDatabase.query(
        "quick_food",
        null,
        null,
        null,
        null,
        null,
        "name COLLATE NOCASE",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val servingGrams = cursor.nullableDouble("serving_grams")
                add(
                    QuickFood(
                        id = cursor.text("id"),
                        name = cursor.text("name"),
                        kind = EntryKind.valueOf(cursor.text("kind")),
                        servingDescription = cursor.text("serving_description"),
                        servingGrams = servingGrams,
                        nutrients = NutrientCodec.decode(cursor.text("nutrients")),
                        amount = EntryAmount(
                            cursor.double("amount_value"),
                            AmountUnit.valueOf(cursor.text("amount_unit")),
                        ),
                        mealType = MealType.valueOf(cursor.text("meal_type")),
                    ),
                )
            }
        }
    }

    override fun saveQuickFood(food: QuickFood) {
        writableDatabase.insertWithOnConflict(
            "quick_food",
            null,
            ContentValues().apply {
                put("id", food.id)
                put("name", food.name)
                put("kind", food.kind.name)
                put("meal_type", food.mealType.name)
                put("serving_description", food.servingDescription)
                food.servingGrams?.let { put("serving_grams", it) } ?: putNull("serving_grams")
                put("amount_value", food.amount.value)
                put("amount_unit", food.amount.unit.name)
                put("nutrients", NutrientCodec.encode(food.nutrients))
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun deleteQuickFood(id: String) {
        writableDatabase.delete("quick_food", "id=?", arrayOf(id))
    }

    override fun setting(key: String): String? = readableDatabase.query(
        "setting",
        arrayOf("value"),
        "key=?",
        arrayOf(key),
        null,
        null,
        null,
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    override fun settings(): Map<String, String> = readableDatabase.query(
        "setting",
        arrayOf("key", "value"),
        null,
        null,
        null,
        null,
        "key",
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
        }
    }

    override fun setSetting(key: String, value: String) {
        require(key.matches(SETTING_KEY_PATTERN))
        writableDatabase.insertWithOnConflict(
            "setting",
            null,
            ContentValues().apply {
                put("key", key)
                put("value", value)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun removeSetting(key: String) {
        writableDatabase.delete("setting", "key=?", arrayOf(key))
    }

    override fun replaceSuggestions(suggestions: List<Suggestion>) {
        writableDatabase.transaction {
            delete("suggestion", null, null)
            suggestions.forEach { suggestion ->
                insert(
                    "suggestion",
                    null,
                    ContentValues().apply {
                        put("id", suggestion.id)
                        put("message", suggestion.message)
                        put("source_title", suggestion.sourceTitle)
                        put("source_url", suggestion.sourceUrl)
                    },
                )
            }
        }
    }

    override fun suggestions(): List<Suggestion> = readableDatabase.query(
        "suggestion",
        null,
        null,
        null,
        null,
        null,
        "id",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    Suggestion(
                        id = cursor.text("id"),
                        message = cursor.text("message"),
                        sourceTitle = cursor.text("source_title"),
                        sourceUrl = cursor.text("source_url"),
                    ),
                )
            }
        }
    }

    override fun recordHealthExport(
        entryId: String,
        recordType: String,
        recordId: String,
        syncedAt: Instant,
    ) {
        writableDatabase.insertWithOnConflict(
            "health_export",
            null,
            ContentValues().apply {
                put("entry_id", entryId)
                put("record_type", recordType)
                put("record_id", recordId)
                put("synced_at", syncedAt.toEpochMilli())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun lastHealthExport(entryId: String, recordType: String): Instant? =
        readableDatabase.query(
            "health_export",
            arrayOf("synced_at"),
            "entry_id=? AND record_type=?",
            arrayOf(entryId, recordType),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) Instant.ofEpochMilli(cursor.getLong(0)) else null
        }

    override fun get(fdcId: Long, now: Instant): UsdaCacheEntry? = readableDatabase.query(
        "usda_food_cache",
        null,
        "fdc_id=? AND expires_at>?",
        arrayOf(fdcId.toString(), now.toEpochMilli().toString()),
        null,
        null,
        null,
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        UsdaCacheEntry(
            food = UsdaFood(
                fdcId = cursor.long("fdc_id"),
                description = cursor.text("description"),
                dataType = UsdaDataType.valueOf(cursor.text("data_type")),
                nutrientsPer100g = NutrientCodec.decode(cursor.text("nutrients")),
            ),
            fetchedAt = Instant.ofEpochMilli(cursor.long("fetched_at")),
            expiresAt = Instant.ofEpochMilli(cursor.long("expires_at")),
        )
    }

    override fun put(entry: UsdaCacheEntry) {
        writableDatabase.insertWithOnConflict(
            "usda_food_cache",
            null,
            ContentValues().apply {
                put("fdc_id", entry.food.fdcId)
                put("description", entry.food.description)
                put("data_type", entry.food.dataType.name)
                put("nutrients", NutrientCodec.encode(entry.food.nutrientsPer100g))
                put("fetched_at", entry.fetchedAt.toEpochMilli())
                put("expires_at", entry.expiresAt.toEpochMilli())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun clear() {
        writableDatabase.delete("usda_food_cache", null, null)
    }
    override fun clearAllLocalData() {
        writableDatabase.transaction {
            delete("journal_entry", null, null)
            delete("quick_food", null, null)
            delete("suggestion", null, null)
            delete("setting", null, null)
            delete("health_export", null, null)
            delete("usda_food_cache", null, null)
        }
    }

    private fun Cursor.text(name: String): String = getString(getColumnIndexOrThrow(name))
    private fun Cursor.long(name: String): Long = getLong(getColumnIndexOrThrow(name))
    private fun Cursor.double(name: String): Double = getDouble(getColumnIndexOrThrow(name))
    private fun Cursor.nullableDouble(name: String): Double? =
        getColumnIndexOrThrow(name).let { index -> if (isNull(index)) null else getDouble(index) }

    private inline fun <T> SQLiteDatabase.transaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try {
            block().also { setTransactionSuccessful() }
        } finally {
            endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "diet-tracker.db"
        private const val DATABASE_VERSION = 4
        private val SETTING_KEY_PATTERN = Regex("[a-z0-9_.-]{1,80}")
    }
}
