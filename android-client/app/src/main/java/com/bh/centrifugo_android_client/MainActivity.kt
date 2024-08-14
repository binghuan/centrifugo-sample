package com.bh.centrifugo_android_client

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bh.centrifugo_android_client.databinding.ActivityMainBinding
import com.bh.centrifugo_android_client.vo.RtmChannelMsg
import com.google.gson.Gson

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

        val sharedPreferences =
            getSharedPreferences("WebSocketPrefs", Context.MODE_PRIVATE)
        binding.wsAddressInput.setText(
            sharedPreferences.getString(
                "wsAddress",
                "wss://your-centrifugo-server.com/connection/websocket"
            )
        )
        binding.tokenInput.setText(sharedPreferences.getString("token", ""))
        binding.subTokenInput.setText(
            sharedPreferences.getString(
                "subToken", ""
            )
        )
        binding.channelInput.setText(sharedPreferences.getString("channel", ""))

        binding.saveButton.setOnClickListener {
            val wsAddress = binding.wsAddressInput.text.toString().trim()
            val token = binding.tokenInput.text.toString().trim()
            val channel = binding.channelInput.text.toString().trim()
            val subToken = binding.subTokenInput.text.toString().trim()

            if (wsAddress.isNotEmpty() && token.isNotEmpty()) {
                val editor = sharedPreferences.edit()
                editor.putString("wsAddress", wsAddress)
                editor.putString("token", token)
                editor.putString("channel", channel)
                editor.putString("subToken", subToken)
                editor.apply()
                binding.connectionStatus.text =
                    getString(R.string.settings_saved)
            } else {
                binding.connectionStatus.text =
                    getString(R.string.websocket_address_and_token_required)
            }
        }

        binding.connectButton.setOnClickListener {
            val wsAddress = binding.wsAddressInput.text.toString().trim()
            val token = binding.tokenInput.text.toString().trim()

            if (wsAddress.isNotEmpty() && token.isNotEmpty()) {
                binding.connectionStatus.text = getString(R.string.connecting)
                setupWebSocketManager(wsAddress, token)
                wsManager?.connect()
            } else {
                binding.connectionStatus.text =
                    getString(R.string.websocket_address_and_token_required)
            }

            dismissIME()
        }

        binding.subscribeButton.setOnClickListener {
            val channel = binding.channelInput.text.toString().trim()
            val subToken = binding.subTokenInput.text.toString().trim()
            if (channel.isNotEmpty()) {
                wsManager?.subscribe(channel, subToken)
            }

            dismissIME()
        }

        binding.unsubscribeButton.setOnClickListener {
            val channel = binding.channelInput.text.toString().trim()
            if (channel.isNotEmpty()) {
                wsManager?.unsubscribe(channel)
            }
            dismissIME()
        }

        binding.sendButton.setOnClickListener {
            val msg = composeMessage()
            wsManager?.publishMessage("public:test", msg)
        }
    }

    private fun composeMessage(): String {
        val rtmChannelMsg = RtmChannelMsg(
            account = "+12345678901",
            id = "+12345678901",
            name = "User 1",
            timestamp = System.currentTimeMillis(),
            text = "hello, I am user 1",
            type = MeetingConstants.MEETING_ROOM_COMMAND_TEXT,
        )

        val gson = Gson()
        val jsonString = gson.toJson(rtmChannelMsg)

        return jsonString
    }

    private fun dismissIME() {
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.channelInput.windowToken, 0)
    }

    private fun setupWebSocketManager(wsAddress: String, token: String) {
        wsManager?.disconnect()
        wsManager = CentrifugoWsManager.getInstance(wsAddress, token)
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

    override fun onDestroy() {
        super.onDestroy()
        wsManager?.disconnect()
    }
}
