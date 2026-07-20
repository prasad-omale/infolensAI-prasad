package com.example.infolensai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiClient(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    fun generateResponse(prompt: String, base64Image: String? = null, callback: (String?, String?) -> Unit) {
        val url = "$baseUrl?key=$apiKey"
        
        val parts = JsonArray()
        
        // Add the prompt text
        val textPart = JsonObject()
        textPart.addProperty("text", prompt)
        parts.add(textPart)
        
        // Add image if available
        if (base64Image != null) {
            val imagePart = JsonObject()
            val inlineData = JsonObject()
            inlineData.addProperty("mime_type", "image/jpeg")
            inlineData.addProperty("data", base64Image)
            imagePart.add("inline_data", inlineData)
            parts.add(imagePart)
        }

        val content = JsonObject()
        content.add("parts", parts)
        
        val contentsArray = JsonArray()
        contentsArray.add(content)
        
        val jsonBody = JsonObject()
        jsonBody.add("contents", contentsArray)

        // Add generation config for clean output
        val generationConfig = JsonObject()
        generationConfig.addProperty("temperature", 0.7)
        generationConfig.addProperty("topK", 40)
        generationConfig.addProperty("topP", 0.95)
        generationConfig.addProperty("maxOutputTokens", 2048)
        jsonBody.add("generationConfig", generationConfig)

        val body = gson.toJson(jsonBody).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GeminiClient", "Request failed", e)
                callback(null, "Network Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("GeminiClient", "Code: ${response.code}, Body: $responseBody")
                
                if (!response.isSuccessful) {
                    if (response.code == 429) {
                        callback(null, "QUOTA_EXCEEDED")
                        return
                    }
                    val errorDetail = try {
                        val errorJson = gson.fromJson(responseBody, JsonObject::class.java)
                        val error = errorJson.getAsJsonObject("error")
                        error.get("message").asString
                    } catch (e: Exception) {
                        "Status ${response.code}"
                    }
                    callback(null, "Error: $errorDetail")
                    return
                }

                try {
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    val candidates = jsonResponse.getAsJsonArray("candidates")
                    if (candidates != null && candidates.size() > 0) {
                        val firstCandidate = candidates[0].asJsonObject
                        val contentObj = firstCandidate.getAsJsonObject("content")
                        val partsArray = contentObj.getAsJsonArray("parts")
                        if (partsArray != null && partsArray.size() > 0) {
                            val responseText = partsArray[0].asJsonObject.get("text").asString
                            callback(responseText, null)
                        } else {
                            callback(null, "No response content")
                        }
                    } else {
                        callback(null, "No candidates returned")
                    }
                } catch (e: Exception) {
                    Log.e("GeminiClient", "Parse error", e)
                    callback(null, "Parsing Error")
                }
            }
        })
    }
}
