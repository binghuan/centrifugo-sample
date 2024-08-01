import SwiftUI
import SwiftCentrifuge
class CentrifugoViewModel: NSObject, ObservableObject, CentrifugeConnectionTokenGetter, CentrifugeClientDelegate, CentrifugeSubscriptionDelegate {
    @Published var wsAddress: String = UserDefaults.standard.string(forKey: "wsAddress") ?? "wss://your-centrifugo-server.com/connection/websocket" {
        didSet {
            saveSettings()
        }
    }
    
    @Published var token: String = UserDefaults.standard.string(forKey: "token") ?? "" {
        didSet {
            saveSettings()
        }
    }
    
    @Published var messages: [String] = []
    @Published var isConnected: Bool = false
    @Published var isSubscribed: Bool = false
    
    private var client: CentrifugeClient?
    private var subscription: CentrifugeSubscription?
    
    func initializeClient() {
        let config = CentrifugeClientConfig(
            token: token,
            tokenGetter: self
        )
        
        client = CentrifugeClient(endpoint: wsAddress, config: config, delegate: self)
    }
    
    func connect() {
        guard client == nil else {
            client?.connect()
            return
        }
        initializeClient()
        client?.connect()
    }
    
    func disconnect() {
        client?.disconnect()
    }
    
    func toggleConnection() {
        guard let client = client else {
            initializeClient()
            client?.connect()
            return
        }
        if client.state == .connected || client.state == .connecting {
            client.disconnect()
        } else {
            client.connect()
        }
    }
    
    func sendMessage() {
        guard isSubscribed else {
            prependMessage("Cannot publish: Not subscribed to the channel")
            return
        }
        
        let data = ["input": "hello, I'm user 1"]
        guard let jsonData = try? JSONSerialization.data(withJSONObject: data, options: .prettyPrinted) else { return }
        subscription?.publish(data: jsonData) { [weak self] result in
            switch result {
            case .success:
                DispatchQueue.main.async {
                    self?.prependMessage("Message sent successfully")
                }
            case .failure(let error):
                DispatchQueue.main.async {
                    self?.prependMessage("Publish error: \(error.localizedDescription)")
                }
            }
        }
    }
    
    private func prependMessage(_ message: String) {
        let timestamp = DateFormatter.localizedString(from: Date(), dateStyle: .short, timeStyle: .medium)
        messages.insert("\(timestamp): \(message)", at: 0)
    }
    
    // MARK: - CentrifugeConnectionTokenGetter
    func getConnectionToken(_ event: CentrifugeConnectionTokenEvent, completion: @escaping (Result<String, Error>) -> ()) {
        completion(.success(""))
    }
    
    // MARK: - CentrifugeClientDelegate
    func onConnected(_ client: CentrifugeClient, _ event: CentrifugeConnectedEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.isConnected = true
            self?.prependMessage("Connected with id \(event.client)")
        }
        
        // Subscribe to the channel immediately after connecting
        do {
            subscription = try client.newSubscription(channel: "public:test", delegate: self)
            subscription?.subscribe()
        } catch {
            prependMessage("Cannot create subscription: \(error.localizedDescription)")
        }
    }
    
    func onDisconnected(_ client: CentrifugeClient, _ event: CentrifugeDisconnectedEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.isConnected = false
            self?.isSubscribed = false
            self?.prependMessage("Disconnected with code \(event.code) and reason \(event.reason)")
        }
    }
    
    func onConnecting(_ client: CentrifugeClient, _ event: CentrifugeConnectingEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.prependMessage("Connecting with code \(event.code) and reason \(event.reason)")
        }
    }
    
    func onError(_ client: CentrifugeClient, _ event: CentrifugeErrorEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.prependMessage("Client error: \(event.error.localizedDescription)")
        }
    }
    
    // MARK: - CentrifugeSubscriptionDelegate
    func onSubscribed(_ subscription: CentrifugeSubscription, _ event: CentrifugeSubscribedEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.isSubscribed = true
            self?.prependMessage("Subscribed to channel \(subscription.channel), was recovering \(event.wasRecovering), recovered \(event.recovered)")
        }
    }
    
    func onSubscribing(_ subscription: CentrifugeSubscription, _ event: CentrifugeSubscribingEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.prependMessage("Subscribing to channel \(subscription.channel), code \(event.code), reason \(event.reason)")
        }
    }
    
    func onUnsubscribed(_ subscription: CentrifugeSubscription, _ event: CentrifugeUnsubscribedEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.isSubscribed = false
            self?.prependMessage("Unsubscribed from channel \(subscription.channel), code \(event.code), reason \(event.reason)")
        }
    }
    
    func onError(_ subscription: CentrifugeSubscription, _ event: CentrifugeSubscriptionErrorEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.prependMessage("Subscription error: \(event.error.localizedDescription)")
        }
    }
    
    func onPublication(_ subscription: CentrifugeSubscription, _ event: CentrifugePublicationEvent) {
        let data = String(data: event.data, encoding: .utf8) ?? ""
        DispatchQueue.main.async { [weak self] in
            self?.prependMessage("Message from channel \(subscription.channel): \(data)")
        }
    }
    
    func onJoin(_ subscription: CentrifugeSubscription, _ event: CentrifugeJoinEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.prependMessage("Client joined channel \(subscription.channel), user ID \(event.user)")
        }
    }
    
    func onLeave(_ subscription: CentrifugeSubscription, _ event: CentrifugeLeaveEvent) {
        DispatchQueue.main.async { [weak self] in
            self?.prependMessage("Client left channel \(subscription.channel), user ID \(event.user)")
        }
    }
    
    // Save and load settings
    func saveSettings() {
        UserDefaults.standard.set(wsAddress, forKey: "wsAddress")
        UserDefaults.standard.set(token, forKey: "token")
    }
    
    func loadSettings() {
        wsAddress = UserDefaults.standard.string(forKey: "wsAddress") ?? "wss://your-centrifugo-server.com/connection/websocket"
        token = UserDefaults.standard.string(forKey: "token") ?? ""
    }
}
