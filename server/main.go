package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/golang-jwt/jwt/v4"
)

// Replace this value with your Centrifugo secret key
var centrifugoSecret = "token_hmac_secret_key"

// TokenRequest struct to receive user ID information
type TokenRequest struct {
	UserID string `json:"user_id"`
}

// TokenResponse struct to return the generated token
type TokenResponse struct {
	Token string `json:"token"`
}

// generateToken creates a JWT token for use with Centrifugo
func generateToken(userID string) (string, error) {
	// Set token claims
	claims := jwt.MapClaims{
		"sub": userID,                           // User ID
		"exp": time.Now().Add(time.Hour).Unix(), // Token expiration time
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(centrifugoSecret))
}

// tokenHandler handles token requests
func tokenHandler(w http.ResponseWriter, r *http.Request) {
	userID := r.URL.Query().Get("user_id")
	if userID == "" {
		http.Error(w, "user_id is required", http.StatusBadRequest)
		return
	}

	token, err := generateToken(userID)
	if err != nil {
		http.Error(w, "Failed to generate token", http.StatusInternalServerError)
		return
	}

	response := TokenResponse{Token: token}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func main() {
	// Register HTTP route and handler
	http.HandleFunc("/token", tokenHandler)

	fmt.Println("Server running at http://localhost:8080")
	http.ListenAndServe(":8080", nil)
}
