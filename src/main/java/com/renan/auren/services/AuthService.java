package com.renan.auren.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.renan.auren.domain.entities.User;
import com.renan.auren.dtos.FirebaseLoginRequest;
import com.renan.auren.dtos.LoginRequest;
import com.renan.auren.dtos.LoginResponse;
import com.renan.auren.infrastructure.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;


    // LOGIN NORMAL (email + senha)
    public ResponseEntity<?> login(LoginRequest request)
            throws ExecutionException, InterruptedException {

        String email = request.email();
        String password = request.password();

        User user = userService.getUserByEmail(email);

        if (user == null) {
            return ResponseEntity.status(401).body("Usuário não encontrado");
        }

        if (!user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body("Senha incorreta");
        }

        String jwt = tokenService.generateToken(user.getEmail(), user.getId());

        return ResponseEntity.ok(new LoginResponse(user, jwt));

    }

    // LOGIN VIA FIREBASE/GOOGLE OAUTH
    public ResponseEntity<?> firebaseLogin(FirebaseLoginRequest request)
            throws Exception {

        try {
            String token = request.token();

            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(400).body("Token não fornecido");
            }

            String email = null;

            // Tenta primeiro como Firebase ID token
            try {
                FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
                email = decoded.getEmail();
                System.out.println("Token validado como Firebase ID token. Email: " + email);
            } catch (FirebaseAuthException e) {
                System.out.println("Token não é Firebase ID token, tentando validar como Google OAuth token...");
                // Se falhar, tenta validar como Google OAuth token
                email = validateGoogleOAuthToken(token);
            }

            if (email == null || email.isEmpty()) {
                return ResponseEntity.status(400).body("Email não encontrado no token");
            }

            User user = userService.getUserByEmail(email);

            if (user == null) {
                user = userService.createUserFromFirebase(email);
            }

            String jwt = tokenService.generateToken(user.getEmail(), user.getId());

            return ResponseEntity.ok(new LoginResponse(user, jwt));

        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("Token inválido: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao processar login: " + e.getMessage());
        }

    }

    // Valida token do Google OAuth diretamente
    private String validateGoogleOAuthToken(String token) throws Exception {
        try {
            // Primeiro tenta como ID token
            String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
            System.out.println("Tentando validar como ID token...");
            
            URL url = new URL(tokenInfoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            
            String responseBody;
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                responseBody = response.toString();
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);
                
                // Verifica se o token é válido
                if (jsonNode.has("error")) {
                    String errorDescription = jsonNode.has("error_description") 
                        ? jsonNode.get("error_description").asText() 
                        : jsonNode.get("error").asText();
                    System.out.println("Erro na validação do ID token: " + errorDescription);
                    // Se falhar como ID token, tenta como access token
                    return validateAsAccessToken(token);
                }

                // Retorna o email do token
                if (jsonNode.has("email")) {
                    String email = jsonNode.get("email").asText();
                    System.out.println("Email extraído do ID token: " + email);
                    return email;
                }
            } else {
                // Lê a resposta de erro
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String inputLine;
                StringBuilder errorResponse = new StringBuilder();
                while ((inputLine = errorReader.readLine()) != null) {
                    errorResponse.append(inputLine);
                }
                errorReader.close();
                responseBody = errorResponse.toString();
                
                System.out.println("Erro ao validar ID token - HTTP " + responseCode);
                System.out.println("Resposta: " + responseBody);
                
                // Se falhar como ID token, tenta como access token
                return validateAsAccessToken(token);
            }
            
            throw new Exception("Email não encontrado no token do Google");
        } catch (Exception e) {
            System.err.println("Erro ao validar token do Google: " + e.getMessage());
            // Tenta como access token como último recurso
            try {
                return validateAsAccessToken(token);
            } catch (Exception e2) {
                e.printStackTrace();
                throw new Exception("Erro ao validar token do Google: " + e.getMessage());
            }
        }
    }
    
    // Valida token como access token e busca informações do usuário
    private String validateAsAccessToken(String accessToken) throws Exception {
        try {
            System.out.println("Tentando validar como access token...");
            // Usa o endpoint userinfo para obter informações do usuário com access token
            URL url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response.toString());
                
                if (jsonNode.has("email")) {
                    String email = jsonNode.get("email").asText();
                    System.out.println("Email extraído do access token: " + email);
                    return email;
                }
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String inputLine;
                StringBuilder errorResponse = new StringBuilder();
                while ((inputLine = errorReader.readLine()) != null) {
                    errorResponse.append(inputLine);
                }
                errorReader.close();
                
                System.out.println("Erro ao validar access token - HTTP " + responseCode);
                System.out.println("Resposta: " + errorResponse.toString());
            }
            
            throw new Exception("Não foi possível validar o token como ID token nem como access token");
        } catch (Exception e) {
            System.err.println("Erro ao validar access token: " + e.getMessage());
            throw e;
        }
    }


}
