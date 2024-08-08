package com.bh.centrifugo_android_client

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.AppCompatEditText
import com.bh.centrifugo_android_client.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var wsManager: CentrifugoWsManager? = null
    private lateinit var clearDrawable: Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clearDrawable = ContextCompat.getDrawable(
            this, android.R.drawable.ic_menu_close_clear_cancel
        )!!

        setupClearButton(binding.wsAddressInput)
        setupClearButton(binding.tokenInput)
        setupClearButton(binding.channelInput)

        val sharedPreferences =
            getSharedPreferences("WebSocketPrefs", Context.MODE_PRIVATE)
        binding.wsAddressInput.setText(
            sharedPreferences.getString(
                "wsAddress",
                "wss://your-centrifugo-server.com/connection/websocket"
            )
        )
        binding.tokenInput.setText(sharedPreferences.getString("token", ""))
        binding.channelInput.setText(sharedPreferences.getString("channel", ""))

        binding.saveButton.setOnClickListener {
            val wsAddress = binding.wsAddressInput.text.toString().trim()
            val token = binding.tokenInput.text.toString().trim()
            val channel = binding.channelInput.text.toString().trim()

            if (wsAddress.isNotEmpty() && token.isNotEmpty()) {
                val editor = sharedPreferences.edit()
                editor.putString("wsAddress", wsAddress)
                editor.putString("token", token)
                editor.putString("channel", channel)
                editor.apply()
                binding.connectionStatus.text = "Settings saved"
            } else {
                binding.connectionStatus.text =
                    "WebSocket Address and Token required"
            }
        }

        binding.connectButton.setOnClickListener {
            val wsAddress = binding.wsAddressInput.text.toString().trim()
            val token = binding.tokenInput.text.toString().trim()

            if (wsAddress.isNotEmpty() && token.isNotEmpty()) {
                binding.connectionStatus.text = "Connecting..."
                setupWebSocketManager(wsAddress, token)
                wsManager?.connect()
            } else {
                binding.connectionStatus.text =
                    "WebSocket Address and Token required"
            }
        }

        binding.subscribeButton.setOnClickListener {
            val channel = binding.channelInput.text.toString().trim()
            if (channel.isNotEmpty()) {
                wsManager?.subscribe(channel)
            }

            // dismiss IME
            val imm =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.channelInput.windowToken, 0)
        }

        binding.sendButton.setOnClickListener {
            wsManager?.publishMessage("public:test", "hello, I'm user 1")
        }
    }

    private fun setupWebSocketManager(wsAddress: String, token: String) {
        wsManager = CentrifugoWsManager(wsAddress, token)
        wsManager?.setStatusListener { status ->
            runOnUiThread {
                binding.connectionStatus.text = status
                binding.sendButton.isEnabled = status.contains("Connected")
            }
        }
        wsManager?.setMessageListener { message ->
            runOnUiThread {
                val currentMessages = binding.messagesView.text.toString()
                binding.messagesView.text = "$message\n$currentMessages"
            }
        }
    }

    private fun setupClearButton(editText: AppCompatEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                showClearButton(editText, s?.isNotEmpty() == true)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editText.right - editText.compoundPaddingEnd)) {
                    editText.text?.clear()
                    showClearButton(editText, false)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun showClearButton(
        editText: AppCompatEditText, show: Boolean
    ) {
        editText.setCompoundDrawablesWithIntrinsicBounds(
            null, null, if (show) clearDrawable else null, null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        wsManager?.disconnect()
    }
}
