package com.alejandro.microservices.promptgeneratorsaas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {
    private String status;
    private String message;

    public static HealthResponse ok() {
        return HealthResponse.builder()
                .status("ok")
                .message("API funcionando correctamente")
                .build();
    }
}
