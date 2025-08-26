package com.alejandro.microservices.promptgeneratorsaas.dto;

import lombok.Data;

@Data
public class SubscriptionPlan {
    private String id;
    private String name;
    private String description;
    private String stripePriceId;
    private Double price;
    private String currency;
    private String interval; // monthly, yearly
    private Integer promptLimit;
    private Boolean customPrompts;
    private Boolean prioritySupport;
}
