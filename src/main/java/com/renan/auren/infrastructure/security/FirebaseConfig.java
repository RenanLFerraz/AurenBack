package com.renan.auren.infrastructure.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    private static final Object lock = new Object();
    private static volatile boolean initialized = false;

    private InputStream createServiceAccountStream() throws IOException {
        // 1) tenta ler da variável de ambiente FIREBASE_CONFIG
        String firebaseConfig = System.getenv("FIREBASE_CONFIG");

        if (firebaseConfig != null && !firebaseConfig.isBlank()) {
            // Credencial vinda da variável de ambiente (Railway)
            return new ByteArrayInputStream(
                    firebaseConfig.getBytes(StandardCharsets.UTF_8)
            );
        }

        // 2) fallback: tenta carregar serviceAccountKey.json do resources (modo local)
        ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
        return resource.getInputStream();
    }

    @PostConstruct
    public void initializeFirebase() throws IOException {
        synchronized (lock) {
            if (!initialized) {
                if (FirebaseApp.getApps().isEmpty()) {
                    InputStream serviceAccount = createServiceAccountStream();
                    try {
                        FirebaseOptions options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();

                        FirebaseApp.initializeApp(options);
                        System.out.println("Firebase inicializado com sucesso");
                    } finally {
                        serviceAccount.close();
                    }
                } else {
                    System.out.println("Firebase já estava inicializado");
                }
                initialized = true;
            }
        }
    }

    @Bean
    public Firestore firestore() {
        return getFirestoreInstance();
    }

    private Firestore getFirestoreInstance() {
        // Garante que o Firebase está inicializado
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    try {
                        initializeFirebase();
                    } catch (IOException e) {
                        throw new RuntimeException("Erro ao inicializar Firebase", e);
                    }
                }
            }
        }

        // Obtém a instância do FirebaseApp
        FirebaseApp app;
        try {
            app = FirebaseApp.getInstance();
        } catch (IllegalStateException e) {
            // Se não encontrar, tenta obter a instância padrão
            if (FirebaseApp.getApps().isEmpty()) {
                try {
                    initializeFirebase();
                    app = FirebaseApp.getInstance();
                } catch (IOException ioException) {
                    throw new RuntimeException("Erro ao inicializar Firebase", ioException);
                }
            } else {
                app = FirebaseApp.getApps().get(0);
            }
        }

        if (app == null) {
            throw new IllegalStateException("FirebaseApp não foi inicializado corretamente");
        }

        return FirestoreClient.getFirestore(app);
    }

    public static Firestore getValidFirestore() {
        try {
            FirebaseApp app;
            if (FirebaseApp.getApps().isEmpty()) {
                throw new IllegalStateException("Firebase não foi inicializado");
            }
            app = FirebaseApp.getApps().get(0);

            // Tenta obter o Firestore
            return FirestoreClient.getFirestore(app);
        } catch (Exception e) {
            System.err.println("Erro ao obter Firestore válido: " + e.getMessage());
            // Tenta criar uma nova instância
            return createNewFirestoreInstance();
        }
    }

    // Método para obter uma nova instância do Firestore quando a anterior foi fechada
    public static Firestore getNewFirestoreInstance() {
        return createNewFirestoreInstance();
    }

    // Cria uma nova instância do FirebaseApp e retorna um Firestore válido
    private static Firestore createNewFirestoreInstance() {
        synchronized (lock) {
            try {
                // Tenta obter uma instância existente primeiro
                if (!FirebaseApp.getApps().isEmpty()) {
                    // Tenta criar uma nova instância com nome único
                    String newAppName = "auren-app-" + System.currentTimeMillis();

                    FirebaseApp newApp;
                    try {
                        newApp = FirebaseApp.getInstance(newAppName);
                    } catch (IllegalStateException e) {
                        // Não existe, cria nova instância usando o arquivo local
                        ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                        InputStream serviceAccount = resource.getInputStream();
                        try {
                            FirebaseOptions options = FirebaseOptions.builder()
                                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                    .build();

                            newApp = FirebaseApp.initializeApp(options, newAppName);
                            System.out.println("Nova instância do FirebaseApp criada: " + newAppName);
                        } finally {
                            serviceAccount.close();
                        }
                    }

                    return FirestoreClient.getFirestore(newApp);
                } else {
                    // Se não há instâncias, inicializa normalmente usando o arquivo local
                    ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                    InputStream serviceAccount = resource.getInputStream();
                    try {
                        FirebaseOptions options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();

                        FirebaseApp app = FirebaseApp.initializeApp(options);
                        System.out.println("FirebaseApp reinicializado");
                        return FirestoreClient.getFirestore(app);
                    } finally {
                        serviceAccount.close();
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao criar nova instância do Firestore: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Não foi possível criar uma instância válida do Firestore", e);
            }
        }
    }
}
