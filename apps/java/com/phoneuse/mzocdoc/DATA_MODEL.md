# mZocdoc Data Model Design

## JSON Data Format (`/sdcard/mzocdoc_data.json`)

```json
{
  "doctors": [
    {
      "id": 1,
      "name": "Dr. Sarah Smith",
      "specialty": "Dermatology",
      "hospital": "NYC Medical Center",
      "city": "New York",
      "rating": 4.8,
      "available_slots": [
        "2026-02-11 09:00",
        "2026-02-11 10:00",
        "2026-02-11 14:00",
        "2026-02-12 09:00",
        "2026-02-12 11:00"
      ]
    }
  ],
  "appointments": []
}
```

## SQLite Schema (Room Entities)

### Doctor Entity
```kotlin
@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey
    val id: Int,
    val name: String,
    val specialty: String,
    val hospital: String,
    val city: String,
    val rating: Double = 4.5,
    val availableSlots: String  // JSON array string
)
```

### Appointment Entity
```kotlin
@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val doctorId: Int,
    val patientName: String,
    val patientPhone: String? = null,
    val patientDob: String? = null,
    val patientEmail: String? = null,
    val patientGender: String? = null,
    val insuranceProvider: String? = null,
    val insuranceId: String? = null,
    val visitReason: String,
    val appointmentTime: String,
    val status: String = "confirmed",
    val createdAt: String = ""
)
```

## Data Flow

1. **App Startup**: Load doctors from `/sdcard/mzocdoc_data.json` via `DataLoader`
2. **UI Rendering**: Display doctors in HomeScreen and DoctorListScreen
3. **User Interaction**: Agent fills booking form and clicks "Confirm Booking"
4. **Database Write**: Appointment saved to SQLite `appointments` table
5. **Task Eval**: Python framework reads `appointments` table to verify task completion

