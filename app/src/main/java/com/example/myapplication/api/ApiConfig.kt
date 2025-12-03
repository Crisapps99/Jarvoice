package com.example.myapplication.api

import com.google.gson.annotations.SerializedName
import org.w3c.dom.Text
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST


private const val BASE_URL="https://beata-unweakening-echo.ngrok-free.dev/"

//cuerpo JSON QUE ENViamos ala api
data class ActionRequest(val texto: String)

data class ActionResponse(
    @SerializedName("action") val action: String,
    @SerializedName("response_text") val responseText: String? = null
)
//retrofit para definir el endpoint de la red neuronal
interface  ActionApiService{
    @POST("predecir")
    suspend fun predictAction(@Body request: ActionRequest): ActionResponse

}


object RetrofitClient{
    val actionApiService: ActionApiService by lazy{
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ActionApiService::class.java)
    }
}