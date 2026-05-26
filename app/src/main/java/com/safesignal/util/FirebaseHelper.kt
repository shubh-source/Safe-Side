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

    /** Listens for both active/inactive SOS status and initial location details */
    fun listenForSosStatus(pairCode: String, onStatusChanged: (active: Boolean, partnerPhone: String, lat: Double, lng: Double) -> Unit): Any {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                val partnerPhone = snapshot.child("partnerPhone").getValue(String::class.java) ?: ""
                
                if (active) {
                    locationRef(pairCode).get().addOnSuccessListener { locSnap ->
                        val lat = locSnap.child("lat").getValue(Double::class.java) ?: 0.0
                        val lng = locSnap.child("lng").getValue(Double::class.java) ?: 0.0
                        onStatusChanged(true, partnerPhone, lat, lng)
                    }.addOnFailureListener {
                        onStatusChanged(true, partnerPhone, 0.0, 0.0)
                    }
                } else {
                    onStatusChanged(false, partnerPhone, 0.0, 0.0)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        sosRef(pairCode).addValueEventListener(listener)
        return listener
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

    private fun pairRef(pairCode: String) = db.child("pairs").child(pairCode)

    /** GF registers herself under the pair code */
    fun registerAsProtected(pairCode: String, myNumber: String) {
        pairRef(pairCode).child("protected").setValue(myNumber)
    }

    /** Guardian registers himself under the pair code → triggers GF's waiting screen */
    fun registerAsGuardian(pairCode: String, myNumber: String) {
        pairRef(pairCode).child("guardian").setValue(myNumber)
    }

    /** Legacy - keep for backward compat */
    fun registerPairCode(pairCode: String, partnerNumber: String) {
        pairRef(pairCode).child("partnerNumber").setValue(partnerNumber)
    }

    /** GF's WaitingActivity listens for guardian to connect */
    fun listenForGuardianConnect(pairCode: String, onConnected: (guardianNumber: String) -> Unit): Any {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val guardianNumber = snapshot.getValue(String::class.java)
                if (!guardianNumber.isNullOrEmpty()) {
                    onConnected(guardianNumber)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        pairRef(pairCode).child("guardian").addValueEventListener(listener)
        return listener
    }

    /** Home screens use this to show "Successfully Linked" */
    fun listenForPairingSuccess(pairCode: String, onLinked: (partnerPhone: String) -> Unit): Any {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                // Check both nodes - if both exist, pairing is complete
                val guardian = snapshot.child("guardian").getValue(String::class.java)
                val protected = snapshot.child("protected").getValue(String::class.java)
                val partner = guardian ?: protected
                if (!partner.isNullOrEmpty()) {
                    onLinked(partner)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        pairRef(pairCode).addValueEventListener(listener)
        return listener
    }

    fun removePairingListener(pairCode: String, listener: Any) {
        if (listener is com.google.firebase.database.ValueEventListener) {
            pairRef(pairCode).removeEventListener(listener)
        }
    }

    fun removeGuardianListener(pairCode: String, listener: Any) {
        if (listener is com.google.firebase.database.ValueEventListener) {
            pairRef(pairCode).child("guardian").removeEventListener(listener)
        }
    }
}
