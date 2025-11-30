package com.renan.auren.controllers;

import com.renan.auren.domain.entities.Item;
import com.renan.auren.services.ItemService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/seed")
public class SeedController {

    @Autowired
    private ItemService itemService;

    @PostConstruct
    public void initializeItems() {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Aguarda o Firestore estar pronto
                seedItems();
                System.out.println("Itens inicializados com sucesso!");
            } catch (Exception e) {
                // Ignora erros na inicialização
                System.err.println("Erro ao inicializar itens: " + e.getMessage());
            }
        }).start();
    }

    @GetMapping("/items")
    public ResponseEntity<String> seedItemsGet() throws ExecutionException, InterruptedException {
        return seedItems();
    }

    @PostMapping("/items")
    public ResponseEntity<String> seedItems() throws ExecutionException, InterruptedException {
        List<Item> items = new ArrayList<>();

        int created = 0;
        int skipped = 0;
        for (Item item : items) {
            try {
                Item existing = itemService.getItemByRedeemCode(item.getRedeemCode());
                if (existing == null) {
                    itemService.createItem(item);
                    created++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                System.err.println("Erro ao criar item " + item.getRedeemCode() + ": " + e.getMessage());
            }
        }
        
        String message = String.format("Itens processados: %d criados, %d já existiam. Use os códigos: POCAO01, ESPADA01, MANA01, ESCUDO01, ARCO01, DRAGAO01, CAJADO01, INVIS01", 
                created, skipped);
        return ResponseEntity.ok(message);
    }
}

