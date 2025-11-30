package com.renan.auren.controllers;

import com.renan.auren.domain.entities.Item;
import com.renan.auren.services.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() throws ExecutionException, InterruptedException {
        List<Item> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) 
            throws ExecutionException, InterruptedException {
        Item created = itemService.createItem(item);
        return ResponseEntity.ok(created);
    }
}

