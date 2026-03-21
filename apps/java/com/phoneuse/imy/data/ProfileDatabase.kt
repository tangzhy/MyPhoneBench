// Copyright 2025 PhoneUse Privacy Protocol Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.phoneuse.imy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileItem::class,
        AppPermission::class,
        Setting::class,
        WritePermission::class,
        AccessLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: ProfileDatabase? = null
        
        private const val DB_NAME = "profile.db"
        
        fun getDatabase(context: Context): ProfileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProfileDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()  // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

