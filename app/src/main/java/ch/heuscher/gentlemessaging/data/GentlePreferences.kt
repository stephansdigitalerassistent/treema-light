package ch.heuscher.gentlemessaging.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * SharedPreferences wrapper for all the "gentle messages" settings.
 * These are local-only settings managed by the caregiver in Admin mode.
 *
 * WHY SharedPreferences? Because it's device-local, simple, and doesn't
 * need any cloud dependency. Phase 2 will layer remote settings on top.
 */
class GentlePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "gentle_messaging_prefs"
        private const val KEY_FAVORITE_IDS = "favorite_contact_ids"
        private const val KEY_ADMIN_PIN = "admin_pin"
        private const val KEY_FONT_SIZE_LEVEL = "font_size_level"
        private const val KEY_QUICK_REPLIES = "quick_replies"
        private const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"

        // Font size levels: 0=medium, 1=large (default), 2=very large
        const val FONT_SIZE_MEDIUM = 0
        const val FONT_SIZE_LARGE = 1
        const val FONT_SIZE_VERY_LARGE = 2

        private val DEFAULT_QUICK_REPLIES = listOf("Ja", "Nein", "Danke", "Mir geht es gut")

        @Volatile
        private var instance: GentlePreferences? = null

        fun getInstance(context: Context): GentlePreferences {
            return instance ?: synchronized(this) {
                instance ?: GentlePreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // =========================================================================
    // FAVORITES
    // =========================================================================

    /** Get all favorite contact/group IDs */
    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
    }

    /** Check if a specific ID is a favorite */
    fun isFavorite(id: String): Boolean = getFavoriteIds().contains(id)

    /** Toggle favorite status. Returns the new isFavorite state. */
    fun toggleFavorite(id: String): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val isNowFavorite = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, current).apply()
        return isNowFavorite
    }

    /** Set favorite for a specific ID */
    fun setFavorite(id: String, isFavorite: Boolean) {
        val current = getFavoriteIds().toMutableSet()
        if (isFavorite) current.add(id) else current.remove(id)
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, current).apply()
    }

    // =========================================================================
    // ADMIN PIN
    // =========================================================================

    /** Get the admin PIN (default: "1234") */
    fun getAdminPin(): String = prefs.getString(KEY_ADMIN_PIN, "1234") ?: "1234"

    /** Set a new admin PIN */
    fun setAdminPin(newPin: String) {
        prefs.edit().putString(KEY_ADMIN_PIN, newPin).apply()
    }

    // =========================================================================
    // FONT SIZE
    // =========================================================================

    /** 0=medium, 1=large (default), 2=very large */
    fun getFontSizeLevel(): Int = prefs.getInt(KEY_FONT_SIZE_LEVEL, FONT_SIZE_LARGE)

    fun setFontSizeLevel(level: Int) {
        prefs.edit().putInt(KEY_FONT_SIZE_LEVEL, level.coerceIn(0, 2)).apply()
    }

    // =========================================================================
    // QUICK REPLIES
    // =========================================================================

    /** Get the list of quick-reply phrases */
    fun getQuickReplies(): List<String> {
        val stored = prefs.getString(KEY_QUICK_REPLIES, null)
        return if (stored.isNullOrBlank()) {
            DEFAULT_QUICK_REPLIES
        } else {
            stored.split("|||").filter { it.isNotBlank() }
        }
    }

    /** Set a new list of quick-reply phrases */
    fun setQuickReplies(replies: List<String>) {
        prefs.edit().putString(KEY_QUICK_REPLIES, replies.joinToString("|||")).apply()
    }

    // =========================================================================
    // WELCOME SCREEN
    // =========================================================================

    fun hasSeenWelcome(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)

    fun setHasSeenWelcome(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_WELCOME, seen).apply()
    }
}
