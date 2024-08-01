import React, { useEffect, useState, useRef } from 'react';
import { Container, Typography, Box, List, ListItem, ListItemText, TextField, Button } from '@mui/material';
import { Centrifuge } from 'centrifuge';

function App() {
  const [messages, setMessages] = useState([]);
  const [wsUrl, setWsUrl] = useState(localStorage.getItem('wsUrl') || 'wss://srv.test.i365724.com/centrifugo/connection/websocket');
  const [token, setToken] = useState(localStorage.getItem('token') || ''); // Ensure token is set
  const [connectionStatus, setConnectionStatus] = useState('Disconnected');
  const [error, setError] = useState('');
  const centrifugeRef = useRef(null);

  const handleConnect = () => {
    if (!wsUrl || !token) {
      setError('WebSocket URL and Token are required');
      return;
    }

    const centrifuge = new Centrifuge(wsUrl, {
      token: token,
    });

    centrifuge.on('connecting', function (ctx) {
      console.log(`connecting: ${ctx.code}, ${ctx.reason}`);
    }).on('connected', function (ctx) {
      console.log(`connected over ${ctx.transport}`);
      setConnectionStatus('Connected');
      setError('');
    }).on('disconnected', function (ctx) {
      console.log(`disconnected: ${ctx.code}, ${ctx.reason}`);
      setConnectionStatus('Disconnected');
      setError(`Disconnected from WebSocket: ${ctx.reason}`);
    }).connect();

    const sub = centrifuge.newSubscription("public:test");

    sub.on('publication', function (ctx) {
      console.log('Received message:', ctx.data);
      const timestamp = new Date().toLocaleString();
      setMessages(prevMessages => [{ data: ctx.data, timestamp }, ...prevMessages]);
    }).on('subscribing', function (ctx) {
      console.log(`subscribing: ${ctx.code}, ${ctx.reason}`);
    }).on('subscribed', function (ctx) {
      console.log('subscribed', ctx);
    }).on('unsubscribed', function (ctx) {
      console.log(`unsubscribed: ${ctx.code}, ${ctx.reason}`);
    }).subscribe();

    centrifugeRef.current = centrifuge;
  };

  useEffect(() => {
    // Clean up connection on unmount
    return () => {
      if (centrifugeRef.current) {
        console.log('Cleaning up connection');
        centrifugeRef.current.disconnect();
      }
    };
  }, []);

  const handleSaveSettings = () => {
    localStorage.setItem('wsUrl', wsUrl);
    localStorage.setItem('token', token);
    window.location.reload();
  };

  const sendMessage = () => {
    if (!centrifugeRef.current) {
      console.error('Centrifuge instance is not connected');
      return;
    }

    centrifugeRef.current.publish("public:test", { input: "hello, I'm user 1" }).then(function(res) {
      console.log('successfully published');
    }, function(err) {
      console.log('publish error', err);
    });
  };

  return (
    <Container maxWidth="sm">
      <Box sx={{ my: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Real-time News
        </Typography>
        <Box sx={{ mb: 4 }}>
          <TextField
            label="WebSocket URL"
            variant="outlined"
            fullWidth
            value={wsUrl}
            onChange={(e) => setWsUrl(e.target.value)}
            margin="normal"
          />
          <TextField
            label="Token"
            variant="outlined"
            fullWidth
            value={token}
            onChange={(e) => setToken(e.target.value)}
            margin="normal"
          />
          <Button variant="contained" color="primary" onClick={handleSaveSettings}>
            Save Settings
          </Button>
          <Button variant="contained" color="secondary" onClick={handleConnect}>
            Connect
          </Button>
          <Button variant="contained" color="secondary" onClick={sendMessage} disabled={connectionStatus !== 'Connected'}>
            Send Test Message
          </Button>
        </Box>
        <Box sx={{ mb: 4 }}>
          <Typography variant="h6">Connection Status: {connectionStatus}</Typography>
        </Box>
        {error && (
          <Box sx={{ mb: 4, color: 'red' }}>
            <Typography variant="body1">{error}</Typography>
          </Box>
        )}
        <List>
          {messages.map((message, index) => (
            <ListItem key={index}>
              <ListItemText primary={`Received message: ${message.data.input}`} secondary={message.timestamp} />
            </ListItem>
          ))}
        </List>
      </Box>
    </Container>
  );
}

export default App;
