package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.ClientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {
    private Long id;
    private String clientName;
    private String companyName;
    private String email;
    private String phone;
    private String industry;
    private String address;
    private String city;
    private String country;

    private String picName;
    private String picEmail;
    private String picPhone;
    private String picPosition;

    private ClientStatus status;
    private String notes;

    private Long totalContracts;
    private Long activeContracts;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
