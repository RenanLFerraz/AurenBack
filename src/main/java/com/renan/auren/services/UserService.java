package com.renan.auren.services;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.renan.auren.domain.entities.User;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    
    // Método helper para obter uma instância válida do Firestore
    private Firestore getFirestore() {
        // Sempre obtém diretamente do FirebaseConfig, que gerencia a criação de novas instâncias se necessário
        return com.renan.auren.infrastructure.security.FirebaseConfig.getValidFirestore();
    }
    
    // Método helper que tenta a operação e, se falhar com "closed", tenta novamente
    private <T> T executeWithRetry(java.util.function.Function<Firestore, T> operation) throws ExecutionException, InterruptedException {
        try {
            return operation.apply(getFirestore());
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("closed")) {
                System.err.println("Firestore estava fechado, criando nova instância do FirebaseApp...");
                // Tenta novamente com uma nova instância do FirebaseApp
                try {
                    Thread.sleep(100); // Pequeno delay
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                // Cria uma nova instância do FirebaseApp quando o Firestore está fechado
                return operation.apply(com.renan.auren.infrastructure.security.FirebaseConfig.getNewFirestoreInstance());
            }
            throw e;
        }
    }

    private static final String COLLECTION_NAME = "users";
    private static final String COUNTER_COLLECTION = "counters";
    private static final String USER_COUNTER_DOC = "users";


    //Função para garantir que o ID do usuário seja único
    private Long getNextUserId() throws ExecutionException, InterruptedException {
        return executeWithRetry(fs -> {
            try {
                DocumentReference counterRef = fs
                        .collection(COUNTER_COLLECTION)
                        .document(USER_COUNTER_DOC);

                return fs.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(counterRef).get();

                    Long lastId;

                    if (!snapshot.exists()) {
                        lastId = 0L;
                        transaction.set(counterRef,
                                java.util.Collections.singletonMap("lastId", lastId)
                        );
                    } else {
                        lastId = snapshot.getLong("lastId");
                    }

                    Long nextId = lastId + 1;
                    transaction.update(counterRef, "lastId", nextId);

                    return nextId;
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public User createUser(User user) throws ExecutionException, InterruptedException {
        Long newId = getNextUserId();
        user.setId(newId);

        executeWithRetry(fs -> {
            try {
                fs.collection(COLLECTION_NAME)
                        .document(String.valueOf(newId))
                        .set(user)
                        .get();
                return null;
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return user; // retorna o usuário criado
    }

    public User getUserByEmail(String email) throws ExecutionException, InterruptedException {
        return executeWithRetry(fs -> {
            try {
                var future = fs.collection(COLLECTION_NAME)
                        .whereEqualTo("email", email)
                        .get();

                var docs = future.get().getDocuments();

                return docs.isEmpty() ? null : docs.get(0).toObject(User.class);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public User createUserFromFirebase(String email) throws ExecutionException, InterruptedException {
        User user = new User();
        user.setNickname(email.split("@")[0]);
        user.setEmail(email);
        user.setPassword("firebase");

        return createUser(user);
    }

    public User getUserById(Long id) throws ExecutionException, InterruptedException {
        return executeWithRetry(fs -> {
            try {
                DocumentReference ref = fs.collection(COLLECTION_NAME).document(String.valueOf(id));
                DocumentSnapshot snap = ref.get().get();
                return snap.exists() ? snap.toObject(User.class) : null;
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
