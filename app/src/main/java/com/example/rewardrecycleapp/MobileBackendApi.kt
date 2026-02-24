package com.example.rewardrecycleapp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object MobileBackendApi {
    private const val HOUSEHOLD_BASE_URL = "http://10.0.2.2:7777/api/households"
    private const val PICKUP_BASE_URL = "http://10.0.2.2:7777/api/pickups"
    private const val COLLECTOR_BASE_URL = "http://10.0.2.2:7777/api/collectors"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val imageMediaType = "image/*".toMediaType()
    private val client = OkHttpClient()

    fun createHouseholdProfile(
        idToken: String,
        name: String,
        email: String,
        phone: String,
        address: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("address", address)
        }

        val request = Request.Builder()
            .url("$HOUSEHOLD_BASE_URL/signup")
            .addHeader("Authorization", "Bearer $idToken")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, null)
                    else onResult(false, extractMessage(body, "Signup profile failed"))
                }
            }
        })
    }

    fun getMyHouseholdProfile(idToken: String, onResult: (Boolean, JSONObject?, String?) -> Unit) {
        val request = Request.Builder()
            .url("$HOUSEHOLD_BASE_URL/me")
            .addHeader("Authorization", "Bearer $idToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, JSONObject(body ?: "{}").optJSONObject("data"), null)
                    else onResult(false, null, extractMessage(body, "Profile fetch failed"))
                }
            }
        })
    }

    fun updateMyHouseholdProfile(
        idToken: String,
        name: String,
        phone: String,
        address: String,
        zone: String,
        onResult: (Boolean, JSONObject?, String?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("name", name)
            put("phone", phone)
            put("address", address)
            put("zone", zone)
        }

        val request = Request.Builder()
            .url("$HOUSEHOLD_BASE_URL/me")
            .addHeader("Authorization", "Bearer $idToken")
            .put(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, JSONObject(body ?: "{}").optJSONObject("data"), null)
                    else onResult(false, null, extractMessage(body, "Profile update failed"))
                }
            }
        })
    }



    fun getMyCollectorProfile(idToken: String, onResult: (Boolean, JSONObject?, String?) -> Unit) {
        val request = Request.Builder()
            .url("$COLLECTOR_BASE_URL/me")
            .addHeader("Authorization", "Bearer $idToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, JSONObject(body ?: "{}").optJSONObject("data"), null)
                    else onResult(false, null, extractMessage(body, "Collector profile fetch failed"))
                }
            }
        })
    }

    fun updateMyCollectorProfile(
        idToken: String,
        name: String,
        phone: String,
        zone: String,
        onResult: (Boolean, JSONObject?, String?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("name", name)
            put("phone", phone)
            put("zone", zone)
        }

        val request = Request.Builder()
            .url("$COLLECTOR_BASE_URL/me")
            .addHeader("Authorization", "Bearer $idToken")
            .put(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, JSONObject(body ?: "{}").optJSONObject("data"), null)
                    else onResult(false, null, extractMessage(body, "Collector profile update failed"))
                }
            }
        })
    }

    fun submitPickupRequest(
        householdId: String,
        wasteType: String,
        address: String,
        imageFile: File,
        onResult: (Boolean, String?) -> Unit
    ) {
        val dataPayload = JSONObject().apply {
            put("household", householdId)
            put("wasteType", wasteType)
            put("address", address)
        }

        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("data", dataPayload.toString())
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody(imageMediaType))
            .build()

        val request = Request.Builder()
            .url("$PICKUP_BASE_URL/upload")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, null)
                    else onResult(false, extractMessage(body, "Pickup submission failed"))
                }
            }
        })
    }

    fun getHouseholdPickupHistory(
        householdId: String,
        onResult: (Boolean, JSONArray?, String?) -> Unit
    ) {
        val request = Request.Builder()
            .url("$PICKUP_BASE_URL/household/$householdId")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) {
                        val root = JSONObject(body ?: "{}")
                        onResult(true, root.optJSONArray("data"), null)
                    } else {
                        onResult(false, null, extractMessage(body, "Pickup history fetch failed"))
                    }
                }
            }
        })
    }



    fun getPickupById(
        pickupId: String,
        onResult: (Boolean, JSONObject?, String?) -> Unit
    ) {
        val request = Request.Builder()
            .url("$PICKUP_BASE_URL/$pickupId")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) {
                        val root = JSONObject(body ?: "{}")
                        onResult(true, root.optJSONObject("data"), null)
                    } else {
                        onResult(false, null, extractMessage(body, "Pickup details fetch failed"))
                    }
                }
            }
        })
    }







    fun getCollectorJobHistory(
        collectorId: String,
        onResult: (Boolean, JSONArray?, String?) -> Unit
    ) {
        val request = Request.Builder()
            .url("$COLLECTOR_BASE_URL/$collectorId/history")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) {
                        val root = JSONObject(body ?: "{}")
                        onResult(true, root.optJSONArray("data"), null)
                    } else {
                        onResult(false, null, extractMessage(body, "Collector history fetch failed"))
                    }
                }
            }
        })
    }

    fun submitCollectorPickupEvidence(
        pickupId: String,
        imageFile: File,
        weight: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("weight", weight)
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody(imageMediaType))
            .build()

        val request = Request.Builder()
            .url("$PICKUP_BASE_URL/collector/pick/$pickupId")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, null)
                    else onResult(false, extractMessage(body, "Evidence upload failed"))
                }
            }
        })
    }

    fun householdConfirmCollection(
        pickupId: String,
        onResult: (Boolean, JSONObject?, String?) -> Unit
    ) {
        val request = Request.Builder()
            .url("$PICKUP_BASE_URL/household/confirm/$pickupId")
            .put("".toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) {
                        val root = JSONObject(body ?: "{}")
                        onResult(true, root.optJSONObject("data"), null)
                    } else {
                        onResult(false, null, extractMessage(body, "Household confirmation failed"))
                    }
                }
            }
        })
    }

    fun startCollectorRoute(
        pickupId: String,
        liveLocation: String,
        onResult: (Boolean, JSONObject?, String?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("liveLocation", liveLocation)
            put("latitude", 9.6615)
            put("longitude", 80.0255)
        }

        val request = Request.Builder()
            .url("$PICKUP_BASE_URL/collector/start-route/$pickupId")
            .put(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) onResult(true, JSONObject(body ?: "{}").optJSONObject("data"), null)
                    else onResult(false, null, extractMessage(body, "Start route failed"))
                }
            }
        })
    }

    fun getCollectorIncomingRequests(
        collectorEmail: String,
        onResult: (Boolean, JSONArray?, String?) -> Unit
    ) {
        val request = Request.Builder()
            .url("$PICKUP_BASE_URL/collector/incoming?email=$collectorEmail")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onResult(false, null, e.message)
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) {
                        val root = JSONObject(body ?: "{}")
                        onResult(true, root.optJSONArray("data"), null)
                    } else {
                        onResult(false, null, extractMessage(body, "Incoming request fetch failed"))
                    }
                }
            }
        })
    }

    private fun extractMessage(rawBody: String?, fallback: String): String {
        return try {
            JSONObject(rawBody ?: "{}").optString("message", fallback)
        } catch (_: Exception) {
            fallback
        }
    }
}
