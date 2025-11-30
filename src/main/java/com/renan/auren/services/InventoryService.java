package com.renan.auren.services;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.renan.auren.domain.entities.InventoryItem;
import com.renan.auren.domain.entities.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class InventoryService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private ItemService itemService;

    private static final String COLLECTION_NAME = "inventory";

    public InventoryItem addItemToInventory(Long userId, String redeemCode) 
            throws ExecutionException, InterruptedException {
        
        Item item = itemService.getItemByRedeemCode(redeemCode);
        if (item == null || item.getId() == null) {
            throw new RuntimeException("Código de resgate inválido ou item não encontrado");
        }

        // Verifica se o usuário já possui o item
        InventoryItem existingItem = getInventoryItemByUserIdAndItemId(userId, item.getId());
        
        if (existingItem != null) {
            String existingItemId = existingItem.getId();
            if (existingItemId != null) {
                // Incrementa a quantidade
                existingItem.setQuantity(existingItem.getQuantity() + 1);
                firestore.collection(COLLECTION_NAME)
                        .document(existingItemId)
                        .update("quantity", existingItem.getQuantity())
                        .get();
                return existingItem;
            }
        }
        
        // Cria novo item no inventário se não existir ou se o ID for null
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setUserId(userId);
        inventoryItem.setItemId(item.getId());
        inventoryItem.setItemName(item.getName());
        inventoryItem.setItemDescription(item.getDescription());
        inventoryItem.setItemCategory(item.getCategory());
        inventoryItem.setItemRarity(item.getRarity());
        inventoryItem.setItemIcon(item.getIcon());
        inventoryItem.setQuantity(1L);
        inventoryItem.setAcquiredAt(System.currentTimeMillis());

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("userId", inventoryItem.getUserId());
        itemData.put("itemId", inventoryItem.getItemId());
        itemData.put("itemName", inventoryItem.getItemName());
        itemData.put("itemDescription", inventoryItem.getItemDescription());
        itemData.put("itemCategory", inventoryItem.getItemCategory());
        itemData.put("itemRarity", inventoryItem.getItemRarity());
        itemData.put("itemIcon", inventoryItem.getItemIcon());
        itemData.put("quantity", inventoryItem.getQuantity());
        itemData.put("acquiredAt", inventoryItem.getAcquiredAt());

        var docRef = firestore.collection(COLLECTION_NAME).document();
        docRef.set(itemData).get();
        inventoryItem.setId(docRef.getId());
        return inventoryItem;
    }

    public List<InventoryItem> getUserInventory(Long userId) throws ExecutionException, InterruptedException {
        var future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get();

        List<InventoryItem> items = new ArrayList<>();
        for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
            InventoryItem item = doc.toObject(InventoryItem.class);
            if (item != null) {
                item.setId(doc.getId());
                items.add(item);
            }
        }
        return items;
    }

    private InventoryItem getInventoryItemByUserIdAndItemId(Long userId, String itemId) 
            throws ExecutionException, InterruptedException {
        var future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("itemId", itemId)
                .get();

        var docs = future.get().getDocuments();
        if (docs.isEmpty()) {
            return null;
        }
        InventoryItem item = docs.get(0).toObject(InventoryItem.class);
        String docId = docs.get(0).getId();
        if (item != null && docId != null) {
            item.setId(docId);
        }
        return item;
    }
}

