package com.phoneuse.mdoordash.data

import android.content.Context

abstract class MDoordashDatabase {
    abstract fun mdoordashDao(): MDoordashDao

    companion object {
        @Volatile
        private var INSTANCE: MDoordashDatabase? = null

        private const val DB_NAME = "mdoordash.db"

        fun getDatabase(context: Context): MDoordashDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = object : MDoordashDatabase() {
                    override fun mdoordashDao(): MDoordashDao = object : MDoordashDao {}
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
