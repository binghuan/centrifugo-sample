package com.bh.centrifugo_android_client

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bh.centrifugo_android_client.databinding.ActivityMainBinding
import com.google.gson.Gson
import io.github.centrifugal.centrifuge.Client
import io.github.centrifugal.centrifuge.ConnectedEvent
import io.github.centrifugal.centrifuge.ConnectingEvent
import io.github.centrifugal.centrifuge.DisconnectedEvent
import io.github.centrifugal.centrifuge.ErrorEvent
import io.github.centrifugal.centrifuge.EventListener
import io.github.centrifugal.centrifuge.Options
import io.github.centrifugal.centrifuge.PublicationEvent
import io.github.centrifugal.centrifuge.SubscribedEvent
import io.github.centrifugal.centrifuge.SubscribingEvent
import io.github.centrifugal.centrifuge.Subscription
import io.github.centrifugal.centrifuge.SubscriptionErrorEvent
import io.github.centrifugal.centrifuge.SubscriptionEventListener
import io.github.centrifugal.centrifuge.UnsubscribedEvent
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val gson = Gson()
    private lateinit var client: Client
    private var subscription: Subscription? = null
    private lateinit var clearDrawable: Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clearDrawable = ContextCompat.getDrawable(
            this, android.R.drawable.ic_menu_close_clear_cancel
        )!!

        // Initialize clear buttons for EditTexts
        setupClearButton(binding.wsAddressInput)
        setupClearButton(binding.tokenInput)

        // Load saved preferences
        val sharedPreferences =
            getSharedPreferences("WebSocketPrefs", Context.MODE_PRIVATE)
        binding.wsAddressInput.setText(
            sharedPreferences.getString(
                "wsAddress",
                "wss://your-centrifugo-server.com/connection/websocket"
            )
        )
        binding.tokenInput.setText(sharedPreferences.getString("token", ""))

        binding.saveButton.setOnClickListener {
            val wsAddress = binding.wsAddressInput.text.toString().trim()
            val token = binding.tokenInput.text.toString().trim()

            if (wsAddress.isNotEmpty() && token.isNotEmpty()) {
                val editor = sharedPreferences.edit()
                editor.putString("wsAddress", wsAddress)
                editor.putString("token", token)
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
                connectWebSocket(wsAddress, token)
            } else {
                binding.connectionStatus.text =
                    "WebSocket Address and Token required"
            }
        }

        binding.sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupClearButton(editText: androidx.appcompat.widget.AppCompatEditText) {
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
        editText: androidx.appcompat.widget.AppCompatEditText, show: Boolean
    ) {
        editText.setCompoundDrawablesWithIntrinsicBounds(
            null, null, if (show) clearDrawable else null, null
        )
    }

    private fun connectWebSocket(wsAddress: String, token: String) {
        val options = Options()
        options.token = token

        client = Client(wsAddress, options, object : EventListener() {
            override fun onConnecting(
                client: Client?, event: ConnectingEvent?
            ) {
                Log.d("Centrifuge", "Connecting: ${event?.reason}")
                runOnUiThread {
                    binding.connectionStatus.text =
                        "Connecting: ${event?.reason}"
                }
            }

            override fun onConnected(client: Client?, event: ConnectedEvent?) {
                Log.d("Centrifuge", "Connected")
                runOnUiThread {
                    binding.connectionStatus.text = "Connected"
                    binding.sendButton.isEnabled = true
                }
            }

            override fun onDisconnected(
                client: Client?, event: DisconnectedEvent?
            ) {
                Log.d("Centrifuge", "Disconnected: ${event?.reason}")
                runOnUiThread {
                    binding.connectionStatus.text =
                        "Disconnected: ${event?.reason}"
                    binding.sendButton.isEnabled = false
                }
            }

            override fun onError(client: Client?, event: ErrorEvent?) {
                Log.e("Centrifuge", "Error: ${event?.error}")
                runOnUiThread {
                    binding.connectionStatus.text = "Error: ${event?.error}"
                }
            }
        })

        client.connect()

        // Subscribe to a channel
        subscription = client.newSubscription(
            "public:test",
            object : SubscriptionEventListener() {
                override fun onSubscribing(
                    sub: Subscription?, event: SubscribingEvent?
                ) {
                    Log.d(
                        "Centrifuge", "Subscribing to channel: ${sub?.channel}"
                    )
                    runOnUiThread { binding.messagesView.append("Subscribing to channel\n") }
                }

                override fun onSubscribed(
                    sub: Subscription?, event: SubscribedEvent?
                ) {
                    Log.d(
                        "Centrifuge", "Subscribed to channel: ${sub?.channel}"
                    )
                    runOnUiThread { binding.messagesView.append("Subscribed to channel\n") }
                }

                override fun onUnsubscribed(
                    sub: Subscription?, event: UnsubscribedEvent?
                ) {
                    Log.d(
                        "Centrifuge",
                        "Unsubscribed from channel: ${sub?.channel}"
                    )
                    runOnUiThread { binding.messagesView.append("Unsubscribed from channel\n") }
                }

                override fun onError(
                    sub: Subscription?, event: SubscriptionErrorEvent?
                ) {
                    super.onError(sub, event)
                    Log.e("Centrifuge", "Subscription error: ${event?.message}")
                    runOnUiThread { binding.messagesView.append("Subscription error: ${event?.message}\n") }
                }

                override fun onPublication(
                    sub: Subscription?, event: PublicationEvent?
                ) {
                    // Ensure event data is not null
                    event?.data?.let { data ->
                        // Decode the byte buffer to a JSON string
                        val jsonString = String(data, StandardCharsets.UTF_8)
                        // Parse the JSON string to a Message object
                        val message =
                            gson.fromJson(jsonString, Message::class.java)

                        // Access the channel from the subscription
                        val channel = sub?.channel ?: "unknown"

                        runOnUiThread {
                            // Append the received message and channel info to the messages view
                            binding.messagesView.append("Received message from $channel: ${message.input}\n")
                        }
                    }
                }
            })

        subscription?.subscribe()
    }

    private fun sendMessage() {
        val message = mapOf("input" to "hello, I'm user 1")
        val jsonMessage = gson.toJson(message)

        subscription?.publish(jsonMessage.toByteArray()) { e, result ->
            if (e != null) {
                Log.e("Centrifuge", "Error publishing message: $e")
                runOnUiThread { binding.messagesView.append("Error publishing message: $e\n") }
            } else {
                Log.d("Centrifuge", "Message published: $result")
                runOnUiThread { binding.messagesView.append("Message published: $result\n") }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
    }

    // Message data class for Gson
    data class Message(val input: String)
}
