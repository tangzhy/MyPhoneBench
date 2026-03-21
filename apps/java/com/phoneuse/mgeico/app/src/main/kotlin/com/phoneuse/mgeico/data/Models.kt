package com.phoneuse.mgeico.data

data class InsurancePlan(
    val id: Int,
    val planName: String,
    val coverageType: String,
    val deductible: Int,
    val monthlyPremium: Double,
    val annualPremium: Double,
    val coverageLimit: String,
    val includesCollision: Boolean,
    val includesComprehensive: Boolean,
    val provider: String,
    val rating: Double = 4.5
)

data class Quote(
    val id: Int = 0,
    val planId: Int,
    val applicantName: String,
    val applicantPhone: String? = null,
    val applicantDob: String? = null,
    val applicantEmail: String? = null,
    val applicantGender: String? = null,
    val applicantAddress: String? = null,
    val applicantOccupation: String? = null,
    val vehicleYear: String,
    val vehicleMakeModel: String,
    val currentInsurance: String? = null,
    val currentPolicyNumber: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val roadsidePhone: String? = null,
    val claimsEmail: String? = null,
    val coverageNotes: String,
    val status: String = "submitted",
    val createdAt: String = ""
)
