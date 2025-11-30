package com.renan.auren.controllers;

import com.renan.auren.domain.entities.InventoryItem;
import com.renan.auren.services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/redeem")
    public ResponseEntity<?> redeemCode(@RequestBody Map<String, Object> request) 
            throws ExecutionException, InterruptedException {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String redeemCode = request.get("redeemCode").toString();
            
            InventoryItem item = inventoryService.addItemToInventory(userId, redeemCode);
            return ResponseEntity.ok(item);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InventoryItem>> getUserInventory(@PathVariable Long userId) 
            throws ExecutionException, InterruptedException {
        List<InventoryItem> inventory = inventoryService.getUserInventory(userId);
        return ResponseEntity.ok(inventory);
    }
}

