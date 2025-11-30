package com.renan.auren.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class InventoryItem {
    private String id;
    private Long userId;
    private String itemId;
    private String itemName;
    private String itemDescription;
    private String itemCategory;
    private String itemRarity;
    private String itemIcon;
    private Long quantity;
    private Long acquiredAt;
}

