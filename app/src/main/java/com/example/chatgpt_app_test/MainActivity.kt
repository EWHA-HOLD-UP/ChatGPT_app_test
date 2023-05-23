package com.example.chatgpt_app_test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private var requestJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val question = findViewById<EditText>(R.id.etQuestion)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val txtResponse = findViewById<TextView>(R.id.txtResponse)

        btnSubmit.setOnClickListener {
            val questionText = question.text.toString().trim()
            if (questionText.isNotEmpty()) {
                Toast.makeText(this, questionText, Toast.LENGTH_SHORT).show()
                requestJob?.cancel()
                requestJob = CoroutineScope(Dispatchers.IO).launch {
                    getResponse(questionText) { response ->
                        runOnUiThread {
                            txtResponse.text = response
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a word", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getResponse(question: String, callback: (String) -> Unit) {
        val url = "https://api.openai.com/v1/completions"
        val apiKey = "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"

        val requestBody = """
    {
        "model": "text-davinci-003",
        "prompt": "Please make 10 Instagram IDs which contain the word $question. However do not create the family name and do not put it on the ID. Your answer must be the format of >>제안1 : ID and so on",
        "max_tokens": 1000,
        "temperature": 0
    }
""".trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    Log.v("data", body)
                    val jsonObject = JSONObject(body)
                    if (jsonObject.has("choices")) {
                        val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                        if (jsonArray.length() > 0) {
                            val textResult = jsonArray.getJSONObject(0).getString("text")
                            callback(textResult)
                            return
                        }
                    }
                }
                Log.v("data", "empty or invalid response")
            }
        })
    }
}