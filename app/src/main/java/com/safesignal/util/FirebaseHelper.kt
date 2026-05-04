package com.safesignal.util

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FirebaseHelper {

    // Real Firebase Realtime Database URL
    private val db by lazy {
        Firebase.database("https://safeside-2cfff-default-rtdb.firebaseio.com").reference
    }

    // ── Paths ────────────────────────────────────────────────────────────────
    private fun sosRef(pairCode: String)      = db.child("sos").child(pairCode)
    private fun locationRef(pairCode: String) = db.child("location").child(pairCode)
    private fun partnerNumRef(pairCode: String) = db.child("pairs").child(pairCode).child("partnerNumber")

    // ── SOS ──────────────────────────────────────────────────────────────────

    /** Her phone calls this to trigger SOS */
    fun triggerSOS(pairCode: String, partnerPhone: String) {
        val expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // 24 hours
        sosRef(pairCode).setValue(
            mapOf(
                "active"       to true,
                "timestamp"    to System.currentTimeMillis(),
                "expiresAt"    to expiresAt,
                "partnerPhone" to partnerPhone
            )
        )
    }

    /** His phone listens for SOS on this pairCode */
    fun listenForSOS(pairCode: String, onSOS: (partnerPhone: String, lat: Double, lng: Double) -> Unit): Any {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                if (active) {
                    val partnerPhone = snapshot.child("partnerPhone").getValue(String::class.java) ?: ""
                    // Start listening to location as well
                    locationRef(pairCode).get().addOnSuccessListener { locSnap ->
                        val lat = locSnap.child("lat").getValue(Double::class.java) ?: 0.0
                        val lng = locSnap.child("lng").getValue(Double::class.java) ?: 0.0
                        onSOS(partnerPhone, lat, lng)
                    }
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        sosRef(pairCode).addValueEventListener(listener)
        return listener
    }

    fun removeSOSListener(pairCode: String, listener: Any) {
        if (listener is com.google.firebase.database.ValueEventListener) {
            sosRef(pairCode).removeEventListener(listener)
        }
    }

    /** Deactivate SOS (optional, after 24h or manual dismiss) */
    fun deactivateSOS(pairCode: String) {
        sosRef(pairCode).child("active").setValue(false)
    }

    // ── Location ─────────────────────────────────────────────────────────────

    /** Her phone pushes location updates */
    fun pushLocation(pairCode: String, lat: Double, lng: Double) {
        locationRef(pairCode).setValue(
            mapOf(
                "lat"       to lat,
                "lng"       to lng,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    /** His phone listens for live location changes */
    fun listenForLocation(pairCode: String, onLocation: (lat: Double, lng: Double) -> Unit): Any {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java) ?: return
                val lng = snapshot.child("lng").getValue(Double::class.java) ?: return
                onLocation(lat, lng)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        locationRef(pairCode).addValueEventListener(listener)
        return listener
    }

    fun removeLocationListener(pairCode: String, listener: Any) {
        if (listener is com.google.firebase.database.ValueEventListener) {
            locationRef(pairCode).removeEventListener(listener)
        }
    }

    // ── Pairing ──────────────────────────────────────────────────────────────

    /** Store partner phone number under this pair code */
    fun registerPairCode(pairCode: String, partnerNumber: String) {
        partnerNumRef(pairCode).setValue(partnerNumber)
    }
}
