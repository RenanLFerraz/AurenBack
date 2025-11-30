package com.renan.auren.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Item {
    private String id;
    private String name;
    private String description;
    private String category; 
    private String rarity; 
    private Integer value;
    private String icon; 
    private String redeemCode;
    private Boolean active;
}

