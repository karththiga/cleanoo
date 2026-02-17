package com.example.rewardrecycleapp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object MobileBackendApi {
    private const val BASE_URL = "http://10.0.2.2:7777/api/households"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
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
            .url("$BASE_URL/signup")
            .addHeader("Authorization", "Bearer $idToken")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) {
                        onResult(true, null)
                    } else {
                        val message = try {
                            JSONObject(body ?: "{}").optString("message", "Signup profile failed")
                        } catch (_: Exception) {
                            "Signup profile failed"
                        }
                        onResult(false, message)
                    }
                }
            }
        })
    }

    fun getMyHouseholdProfile(idToken: String, onResult: (Boolean, JSONObject?, String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/me")
            .addHeader("Authorization", "Bearer $idToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, null, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful) {
                        val root = JSONObject(body ?: "{}")
                        onResult(true, root.optJSONObject("data"), null)
                    } else {
                        val message = try {
                            JSONObject(body ?: "{}").optString("message", "Profile fetch failed")
                        } catch (_: Exception) {
                            "Profile fetch failed"
                        }
                        onResult(false, null, message)
                    }
                }
            }
        })
    }
}
