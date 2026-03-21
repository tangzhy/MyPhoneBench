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

package com.phoneuse.mzocdoc.data

import android.content.Context
// Room temporarily disabled
// import androidx.room.Database
// import androidx.room.Room
// import androidx.room.RoomDatabase

// @Database(
//     entities = [
//         Doctor::class,
//         Appointment::class
//     ],
//     version = 1,
//     exportSchema = false
// )
abstract class MZocdocDatabase { // : RoomDatabase() {
    abstract fun mzocdocDao(): MZocdocDao
    
    companion object {
        @Volatile
        private var INSTANCE: MZocdocDatabase? = null
        
        private const val DB_NAME = "mzocdoc.db"
        
        fun getDatabase(context: Context): MZocdocDatabase {
            return INSTANCE ?: synchronized(this) {
                // Room temporarily disabled - return a simple instance
                val instance = object : MZocdocDatabase() {
                    override fun mzocdocDao(): MZocdocDao = object : MZocdocDao {}
                }
                INSTANCE = instance
                instance
            }
        }
    }
}

