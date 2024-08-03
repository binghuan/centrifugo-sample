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
            
            HStack {
                Button(action: {
                    viewModel.saveSettings()
                }) {
                    Text("Save Settings")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.gray)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                
                Button(action: {
                    viewModel.toggleConnection()
                }) {
                    Text(viewModel.isConnected ? "Disconnect" : "Connect")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(viewModel.isConnected ? Color.red : Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
            }
            .padding(.horizontal) // Add padding around the HStack to separate it from other elements
            
            
            
            Button(action: {
                viewModel.sendMessage()
            }) {
                Text("Send Test Message")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            .disabled(!viewModel.isConnected)
            .padding(.horizontal)
            
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

// Preview 
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

