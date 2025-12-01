package com.renan.auren.infrastructure.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Object lock = new Object();
    private static volatile boolean initialized = false;

    private static InputStream createServiceAccountStream() throws IOException {
        String firebaseConfig = System.getenv("FIREBASE_CONFIG");

        if (firebaseConfig != null && !firebaseConfig.isBlank()) {
            // Credencial vinda da variável de ambiente (Railway)
            return new ByteArrayInputStream(
                    firebaseConfig.getBytes(StandardCharsets.UTF_8)
            );
        }

        // Fallback para desenvolvimento local
        ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Nenhuma credencial do Firebase encontrada. " +
                    "Defina a variável de ambiente FIREBASE_CONFIG " +
                    "ou coloque serviceAccountKey.json em src/main/resources."
            );
        }
        return resource.getInputStream();
    }

    private static FirebaseOptions buildFirebaseOptions() throws IOException {
        try (InputStream serviceAccount = createServiceAccountStream()) {
            return FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
        }
    }

    @PostConstruct
    public void initializeFirebase() throws IOException {
        synchronized (lock) {
            if (!initialized) {
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseOptions options = buildFirebaseOptions();
                    FirebaseApp.initializeApp(options);
                    System.out.println("Firebase inicializado com sucesso");
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

            return FirestoreClient.getFirestore(app);
        } catch (Exception e) {
            System.err.println("Erro ao obter Firestore válido: " + e.getMessage());
            return createNewFirestoreInstance();
        }
    }

    public static Firestore getNewFirestoreInstance() {
        return createNewFirestoreInstance();
    }

    private static Firestore createNewFirestoreInstance() {
        synchronized (lock) {
            try {
                FirebaseOptions options = buildFirebaseOptions();

                String newAppName = "auren-app-" + System.currentTimeMillis();

                FirebaseApp newApp;
                try {
                    newApp = FirebaseApp.getInstance(newAppName);
                } catch (IllegalStateException e) {
                    newApp = FirebaseApp.initializeApp(options, newAppName);
                    System.out.println("Nova instância do FirebaseApp criada: " + newAppName);
                }

                return FirestoreClient.getFirestore(newApp);
            } catch (Exception e) {
                System.err.println("Erro ao criar nova instância do Firestore: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Não foi possível criar uma instância válida do Firestore", e);
            }
        }
    }
}
