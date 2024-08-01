import SwiftUI
import SwiftCentrifuge

struct ContentView: View {
    @StateObject private var viewModel = CentrifugoViewModel()
    
    var body: some View {
        VStack {
            Text("Centrifugo SwiftUI Client")
                .font(.title)
                .padding()
            
            TextField("WebSocket URL", text: $viewModel.wsAddress)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            
            TextField("Token", text: $viewModel.token)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            
            Button(action: {
                viewModel.saveSettings()
            }) {
                Text("Save Settings")
                    .padding()
                    .background(Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            
            Button(action: {
                viewModel.toggleConnection()
            }) {
                Text(viewModel.isConnected ? "Disconnect" : "Connect")
                    .padding()
                    .background(viewModel.isConnected ? Color.red : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            
            Button(action: {
                viewModel.sendMessage()
            }) {
                Text("Send Test Message")
                    .padding()
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            .disabled(!viewModel.isConnected)
            
            ScrollView {
                VStack(alignment: .leading) {
                    ForEach(viewModel.messages, id: \.self) { message in
                        Text(message)
                            .padding(.horizontal)
                    }
                }
            }
        }
        .padding()
        .onAppear {
            viewModel.loadSettings()
        }
    }
}
