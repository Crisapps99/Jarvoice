package com.example.myapplication.activity

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.speech.tts.TextToSpeech
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityJarBinding
import com.example.myapplication.activity.ActionExecutor
import com.example.myapplication.handlers.IntentHandler
import java.util.*
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.ActionApiService
import com.example.myapplication.api.ActionRequest
import com.google.ai.client.generativeai.GenerativeModel
import retrofit2.converter.gson.GsonConverterFactory

class JarActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var binding: ActivityJarBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private var isListener = false
    private lateinit var generativeModel: GenerativeModel // Comentamos Gemini
    private lateinit var textToSpeech: TextToSpeech
    private val actionApiService: ActionApiService = RetrofitClient.actionApiService

    private val RECORD_AUDIO_PERMISSION_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "AIzaSyCZp4qdhQkid8b_0UfteuvM2erkpUPJxTs"
        )

        binding = ActivityJarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //permiso del microfono
        requestAudioPermission()

        configurarReconocedor()
        textToSpeech = TextToSpeech(this){ status ->
            if (status == TextToSpeech.SUCCESS){
                val result = textToSpeech.setLanguage(Locale("es", "ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    Toast.makeText(this,"idioma español no disponible", Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(this,"eror al inicial el motor tts", Toast.LENGTH_LONG).show()
            }
        }
        binding.micButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            ) {
                if (isListener){
                    detenerRreconocimientovoz()
                }else{
                    iniciarReconocimientoVoz()
                }

            } else {
                Toast.makeText(this, "Permiso de micrófono denegado.", Toast.LENGTH_SHORT).show()
                requestAudioPermission()
            }
        }
        // [Fin del onCreate]
    }


//... [Resto de las funciones (permisos, reconocimiento) permanecen iguales] ...

    //CONCEDER PERMISOS
    private fun requestAudioPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),RECORD_AUDIO_PERMISSION_CODE)
        }
    }
    //MANEJO DE RESPUESTA DE LA SOLICTUD
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== RECORD_AUDIO_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "permiso consedido", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "permiso denegado", Toast.LENGTH_LONG).show()
        }
    }
    //configuracion del permiso
    private fun configurarReconocedor() {
        // Obtiene la instancia del motor de reconocimiento
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this) // La Activity es el listener

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Usa el idioma por defecto del sistema
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            // Para ver el texto en tiempo real
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }
    private fun detenerRreconocimientovoz(){
        if (isListener){
            speechRecognizer.stopListening()
            isListener = false
            binding.transcriptionTextView.text = "reconocimientodetenido"
        }
    }
    //iniciamos el reconocimiento devoz
    private fun iniciarReconocimientoVoz() {
        speechRecognizer.stopListening()
        speechRecognizer.startListening(recognizerIntent)
        isListener = true
    }

    //metodos para el recognitionlistener
    override fun onReadyForSpeech(params: Bundle?){
        Toast.makeText(this, "Escuchando...", Toast.LENGTH_SHORT).show()
        binding.transcriptionTextView.text= "di algo"
    }
    //par aimpiar el texto antes de pempesar hablar
    override fun onBeginningOfSpeech() {
        binding.transcriptionTextView.text=""
    }
    //mostramos la trasncripcion en tiempo real
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()){
            binding.transcriptionTextView.text = matches[0]
        }
    }
    //mostramos el resultado  en la caja de texto
    override fun onResults(results: Bundle?) {
        isListener=false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()){
            val userQuery = matches[0]
            binding.transcriptionTextView.text = userQuery

            //llamamoas ala ia con un texto trascrito
            IniciarInteraccionNeurona(userQuery)
            Toast.makeText(this, "transcripcion finalizada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun IniciarInteraccionNeurona(prompt: String){
        binding.transcriptionTextView.text="Pensando en tu comando..."

        //ejecutamos la llamada a la api en un hilo de coroutine
        lifecycleScope.launch {
            try {

                val apiResponse = actionApiService.predictAction(ActionRequest(prompt))
                val predictedAction = apiResponse.action ?: "UNKNOWN_ACTION"




                val geminiPrompt = when (predictedAction) {
                    "abrir_camara" -> "El usuario quiere 'abrir_camara'. Da una respuesta natural confirmándolo."
                    "abrir_mapa" -> "El usuario quiere 'abrir_mapa'. Da una respuesta natural confirmándolo."
                    "UNKNOWN_ACTION" -> "El comando del usuario ('$prompt') no se reconoció. Pídele que lo repita de forma amable."
                    else -> "El usuario ha dado el comando '$predictedAction'. Primero Saluda Cordialmente y Da una respuesta corta y natural confirmando la acción. y  pregutna que quieres hacer "
                }
                val geminiResponse = try {
                    val response = generativeModel.generateContent(geminiPrompt)
                    response.text ?: "No pude generar una respuesta conversacional."
                } catch (e: Exception) {
                    "Error en Gemini: No pude generar la respuesta. ${e.message}"
                }
                if (geminiResponse.isNotEmpty()){
                    textToSpeech.speak(geminiResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                }
                val executionResult = IntentHandler.handleIntent(this@JarActivity, predictedAction)

                binding.transcriptionTextView.text =
                    "$geminiResponse\n\n" +
                            "[Acción ML: $predictedAction, Ejecución Local: $executionResult]"

            }catch (e: Exception){
                // Error de red, JSON o conexión con tu servidor ngrok0
                val errorMessage = "Error API (ngrok): ${e.message ?: "Verifica que el servidor esté activo y la URL correcta."}"
                binding.transcriptionTextView.text = errorMessage
                Toast.makeText(this@JarActivity, errorMessage, Toast.LENGTH_LONG ).show()
            }
        }
    }


    //... [Resto de las funciones permanecen iguales] ...
    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No se pudo reconocer la voz."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Error de red. ¿Hay conexión?"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes."
            else -> "Error: $error"
        }
        binding.transcriptionTextView.text = errorMessage
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
    // Métodos restantes de la interfaz (implementación vacía si no se usan)
    override fun onRmsChanged(rmsdB: Float) { /* Lógica de indicador de volumen */ }
    override fun onEndOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::speechRecognizer.isInitialized){
            speechRecognizer.destroy()
        }
        super.onDestroy()
    }
}