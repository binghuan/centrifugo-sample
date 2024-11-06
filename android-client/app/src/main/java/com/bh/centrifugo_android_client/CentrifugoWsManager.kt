package com.bh.centrifugo_android_client

import android.util.Log
import com.bh.centrifugo_android_client.api.RetrofitClient
import com.bh.centrifugo_android_client.api.TokenResponse
import com.bh.centrifugo_android_client.vo.RtmChannelMsg
import io.github.centrifugal.centrifuge.Client
import io.github.centrifugal.centrifuge.ConnectedEvent
import io.github.centrifugal.centrifuge.ConnectingEvent
import io.github.centrifugal.centrifuge.ConnectionTokenEvent
import io.github.centrifugal.centrifuge.ConnectionTokenGetter
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
import io.github.centrifugal.centrifuge.SubscriptionOptions
import io.github.centrifugal.centrifuge.SubscriptionTokenEvent
import io.github.centrifugal.centrifuge.SubscriptionTokenGetter
import io.github.centrifugal.centrifuge.TokenCallback
import io.github.centrifugal.centrifuge.UnsubscribedEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CentrifugoWsManager private constructor(
    private val wsAddress: String, private val token: String
) {
    private lateinit var client: Client
    private val subscriptions = mutableMapOf<String, Subscription>()

    private var statusListener: ((String) -> Unit)? = null
    private var messageListener: ((RtmChannelMsg) -> Unit)? = null

    init {
        initClient()
    }

    companion object {

        const val TAG = "CentrifugoWsManager"

        @Volatile
        private var INSTANCE: CentrifugoWsManager? = null

        fun getInstance(wsAddress: String, token: String): CentrifugoWsManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CentrifugoWsManager(
                    wsAddress, token
                ).also { INSTANCE = it }
            }
    }

    private fun fetchToken(
        userId: String,
        onSuccess: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        Log.d(TAG, "Fetching token for user: $userId")

        val call = RetrofitClient.instance.getToken(userId)
        call.enqueue(object : Callback<TokenResponse> {
            override fun onResponse(
                call: Call<TokenResponse>, response: Response<TokenResponse>
            ) {
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        Log.d(TAG, "fetchToken Token: $token")
                        onSuccess?.invoke(token) // Call onSuccess if it's not null
                    } else {
                        Log.e(TAG, "fetchToken Token is null")
                        onError?.invoke("Token is null") // Call onError if it's not null
                    }
                } else {
                    Log.e(TAG, "fetchToken Failed to retrieve token")
                    onError?.invoke("Failed to retrieve token") // Call onError if it's not null
                }
            }

            override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                Log.e(TAG, "fetchToken Error: ${t.message}")
                onError?.invoke(
                    t.message ?: "Unknown error"
                ) // Call onError if it's not null
            }
        })
    }


    private fun initClient() {

        val options = Options()
        options.token = token
        options.tokenGetter = object : ConnectionTokenGetter() {
            override fun getConnectionToken(
                event: ConnectionTokenEvent?, cb: TokenCallback?
            ) {
                fetchToken("testuser", onSuccess = { newToken ->
                    cb?.Done(null, newToken)
                }, onError = { error ->
                    cb?.Done(Exception(error), "")
                })
            }
        }

        Log.d(
            TAG, "Creating client with address: $wsAddress, token=$token"
        )

        client = Client(wsAddress, options, object : EventListener() {
            override fun onConnecting(
                client: Client?, event: ConnectingEvent?
            ) {
                val message = "Connecting: ${event?.reason}"
                Log.d(TAG, message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }

            override fun onConnected(client: Client?, event: ConnectedEvent?) {
                val message = "Connected"
                Log.d(TAG, message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }

            override fun onDisconnected(
                client: Client?, event: DisconnectedEvent?
            ) {
                val message = "Disconnected: ${event?.reason}"
                Log.d(TAG, message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }

            override fun onError(client: Client?, event: ErrorEvent?) {
                val message = "Error: ${event?.error}"
                Log.e(TAG, message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }
        })
    }

    fun connect() {
        client.connect()
    }

    fun disconnect() {
        client.disconnect()
    }

    fun setStatusListener(listener: (String) -> Unit) {
        statusListener = listener
    }

    fun setMessageListener(listener: (RtmChannelMsg) -> Unit) {
        messageListener = listener
    }

    private fun checkSubscription(channel: String): Boolean {
        if (subscriptions.containsKey(channel)) {
            val message = "Already subscribed to $channel"
            showMessage("[${getCurrentTimestamp()}] $message")
            return true
        }
        return false
    }

    fun subscribe(channel: String, subToken: String) {
        if (checkSubscription(channel)) return

        val subscription =
            client.newSubscription(channel, SubscriptionOptions().apply {
                token = subToken
                tokenGetter = object : SubscriptionTokenGetter() {
                    override fun getSubscriptionToken(
                        event: SubscriptionTokenEvent?,
                        cb: TokenCallback?
                    ) {
                        super.getSubscriptionToken(event, cb)

                    }
                }
            }, object : SubscriptionEventListener() {
                override fun onSubscribing(
                    sub: Subscription?, event: SubscribingEvent?
                ) {
                    val message = "Subscribing to channel: ${sub?.channel}"
                    Log.d(TAG, message)
                    showMessage("[${getCurrentTimestamp()}] $message")
                }

                override fun onSubscribed(
                    sub: Subscription?, event: SubscribedEvent?
                ) {
                    val message = "Subscribed to channel: ${sub?.channel}"
                    Log.d(TAG, message)
                    showMessage("[${getCurrentTimestamp()}] $message")
                }

                override fun onUnsubscribed(
                    sub: Subscription?, event: UnsubscribedEvent?
                ) {
                    val message = "Unsubscribed from channel: ${sub?.channel}"
                    Log.d(TAG, message)
                    showMessage("[${getCurrentTimestamp()}] $message")
                }

                override fun onError(
                    sub: Subscription?, event: SubscriptionErrorEvent?
                ) {
                    val message = "Subscription error: ${event?.message}"
                    Log.e(TAG, message)

                }

                override fun onPublication(
                    sub: Subscription?, event: PublicationEvent?
                ) {
                    event?.data?.let { data ->
                        val jsonString = String(data, StandardCharsets.UTF_8)
                        val message =
                            "Received message from ${sub?.channel}: $jsonString"
                        Log.d(TAG, message)
                        showMessage("[${getCurrentTimestamp()}] $message")
                    }
                }
            })
        subscription.subscribe()
        subscriptions[channel] = subscription
    }

    private fun showMessage(message: String) {
        RtmChannelMsg(
            timestamp = System.currentTimeMillis(),
            text = message,
            type = "text"
        ).let { messageListener?.invoke(it) }
    }

    fun unsubscribe(channel: String) {
        val subscription = subscriptions[channel]
        if (subscription != null) {
            subscription.unsubscribe()
            subscriptions.remove(channel)
            client.removeSubscription(subscription)
        } else {
            showMessage("[${getCurrentTimestamp()}] Not subscribed to channel $channel")
        }
    }

    fun publishMessage(channel: String, message: String) {
        val subscription = subscriptions[channel]
        if (subscription == null) {
            showMessage("[${getCurrentTimestamp()}] Cannot publish: Not subscribed to the channel $channel")
            return
        }

        val jsonMessage = "{\"input\": \"$message\"}"
        subscription.publish(jsonMessage.toByteArray()) { e, _ ->
            if (e != null) {
                val errorMessage = "Error publishing message: $e"
                Log.e(TAG, errorMessage)
                showMessage("[${getCurrentTimestamp()}] $errorMessage")
            } else {
                val successMessage = "Message published to channel $channel"
                Log.d(TAG, successMessage)
                showMessage("[${getCurrentTimestamp()}] $successMessage")
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
