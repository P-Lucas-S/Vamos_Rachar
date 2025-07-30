package com.example.constraintlayout

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var edtConta: EditText
    private lateinit var edtPessoas: EditText
    private lateinit var tvResultado: TextView
    private lateinit var fabShare: FloatingActionButton
    private lateinit var fabSpeak: FloatingActionButton
    private var tts: TextToSpeech? = null
    private var valorPorPessoa: Double = 0.0
    private var ttsInitialized = false
    private lateinit var dataStore: AppDataStore

    private var lastSavedConta: Float = 0f
    private var lastSavedPessoas: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            Log.d("MainActivity", "Iniciando inicialização dos componentes")
            dataStore = AppDataStore(applicationContext)

            edtConta = findViewById(R.id.edtConta)
            edtPessoas = findViewById(R.id.edtPessoas)
            tvResultado = findViewById(R.id.tvResultado)
            fabShare = findViewById(R.id.fabShare)
            fabSpeak = findViewById(R.id.fabSpeak)

            Log.d("MainActivity", "Views inicializadas com sucesso")

            try {
                tts = TextToSpeech(applicationContext, this)
                Log.d("MainActivity", "TTS inicializado")
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao inicializar TTS", e)
                tts = null
            }

            lifecycleScope.launch {
                try {
                    val savedConta = dataStore.getContaValueOnce()
                    val savedPessoas = dataStore.getPessoasValueOnce()

                    if (savedConta > 0) {
                        edtConta.setText(savedConta.toString())
                        lastSavedConta = savedConta
                    }
                    if (savedPessoas > 0) {
                        edtPessoas.setText(savedPessoas.toString())
                        lastSavedPessoas = savedPessoas
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao carregar valores", e)
                }
            }

            val textWatcher = object : TextWatcher {
                private var selfChange = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (selfChange) return
                    selfChange = true
                    calcularValor()
                    saveValues()
                    selfChange = false
                }
            }

            edtConta.addTextChangedListener(textWatcher)
            edtPessoas.addTextChangedListener(textWatcher)

            fabShare.setOnClickListener { compartilharValor() }
            fabSpeak.setOnClickListener {
                if (ttsInitialized && tts != null) {
                    falarValor()
                }
            }

            Log.d("MainActivity", "Inicialização concluída com sucesso")
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro durante a inicialização", e)
        }
    }

    private fun calcularValor() {
        try {
            val valorTotalStr = edtConta.text.toString()
            val numPessoasStr = edtPessoas.text.toString()

            if (valorTotalStr.isEmpty() || numPessoasStr.isEmpty()) {
                valorPorPessoa = 0.0
                tvResultado.text = "R$ 0,00"
                return
            }

            val valorTotal = valorTotalStr.toDoubleOrNull() ?: 0.0
            val numPessoas = numPessoasStr.toIntOrNull() ?: 0

            valorPorPessoa = if (numPessoas > 0) valorTotal / numPessoas else 0.0
            tvResultado.text = String.format("R$ %.2f", valorPorPessoa)
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao calcular valor", e)
            valorPorPessoa = 0.0
            tvResultado.text = "R$ 0,00"
        }
    }

    private fun saveValues() {
        val valorTotal = edtConta.text.toString().toFloatOrNull() ?: 0f
        val numPessoas = edtPessoas.text.toString().toIntOrNull() ?: 0

        if (valorTotal != lastSavedConta || numPessoas != lastSavedPessoas) {
            lifecycleScope.launch {
                dataStore.saveContaValue(valorTotal)
                dataStore.savePessoasValue(numPessoas)
                lastSavedConta = valorTotal
                lastSavedPessoas = numPessoas
            }
        }
    }

    private fun compartilharValor() {
        try {
            val mensagem = "Valor por pessoa: R$ %.2f".format(valorPorPessoa)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensagem)
            }
            startActivity(Intent.createChooser(intent, "Compartilhar via"))
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao compartilhar valor", e)
        }
    }

    private fun falarValor() {
        try {
            val textoParaFalar = "O valor por pessoa é ${String.format("%.2f", valorPorPessoa)} reais"
            tts?.speak(textoParaFalar, TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao falar valor", e)
        }
    }

    override fun onInit(status: Int) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("pt", "BR"))
                ttsInitialized = (result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED)
                Log.d("MainActivity", "TTS inicializado com sucesso: $ttsInitialized")
            } else {
                Log.e("MainActivity", "Falha na inicialização do TTS: $status")
                ttsInitialized = false
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro no onInit do TTS", e)
            ttsInitialized = false
        }
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro no onDestroy", e)
            super.onDestroy()
        }
    }
}
