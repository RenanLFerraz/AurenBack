package com.renan.auren.services;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.renan.auren.domain.entities.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class ItemService {
    //Logica de criação dos Itens e Validação do Redeem Code 
    // (Aqui poode ser alterado futuramente para a criação de mais itens do RPG)
    @Autowired
    private Firestore firestore;

    private static final String COLLECTION_NAME = "items";

    private Firestore getFirestore() {
        if (firestore == null) {
            throw new IllegalStateException("Firestore não está inicializado");
        }
        return firestore;
    }

    public Item getItemByRedeemCode(String redeemCode) throws ExecutionException, InterruptedException {
        try {
            var future = getFirestore().collection(COLLECTION_NAME)
                    .whereEqualTo("redeemCode", redeemCode.toUpperCase())
                    .whereEqualTo("active", true)
                    .get();

            var docs = future.get().getDocuments();
            if (docs.isEmpty()) {
                return null;
            }
            Item item = docs.get(0).toObject(Item.class);
            if (item != null) {
                item.setId(docs.get(0).getId());
            }
            return item;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("closed")) {
                throw new IllegalStateException("Firestore foi fechado. Reinicie a aplicação.", e);
            }
            throw e;
        }
    }

    public List<Item> getAllItems() throws ExecutionException, InterruptedException {
        var future = getFirestore().collection(COLLECTION_NAME)
                .whereEqualTo("active", true)
                .get();

        List<Item> items = new ArrayList<>();
        for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
            Item item = doc.toObject(Item.class);
            if (item != null) {
                item.setId(doc.getId());
                items.add(item);
            }
        }
        return items;
    }

    public Item createItem(Item item) throws ExecutionException, InterruptedException {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("name", item.getName());
        itemData.put("description", item.getDescription());
        itemData.put("category", item.getCategory());
        itemData.put("rarity", item.getRarity());
        itemData.put("value", item.getValue());
        itemData.put("icon", item.getIcon());
        String redeemCode = item.getRedeemCode();
        if (redeemCode != null) {
            itemData.put("redeemCode", redeemCode.toUpperCase());
        }
        itemData.put("active", item.getActive() != null ? item.getActive() : true);

        var docRef = getFirestore().collection(COLLECTION_NAME).document();
        docRef.set(itemData).get();
        item.setId(docRef.getId());
        return item;
    }
}

