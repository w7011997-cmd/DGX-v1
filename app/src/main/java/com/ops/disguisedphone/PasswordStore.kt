package com.ops.disguisedphone

import android.content.Context
import java.security.MessageDigest

object PasswordStore {
    private const val PREFS = "password_prefs"
    private const val KEY_HASH = "password_hash"

    fun isSet(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_HASH)
    }

    fun setPassword(context: Context, plaintext: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HASH, hash(plaintext))
            .apply()
    }

    fun verify(context: Context, attempt: String): Boolean {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HASH, null) ?: return false
        return stored == hash(attempt)
    }

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
