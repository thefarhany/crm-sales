package com.thefarhany.crmsales.model.dto.request;

import com.thefarhany.crmsales.model.enums.ClientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClientRequest {
    @Size(min = 2, max = 100, message = "Client name must be between 2 and 100 characters")
    private String clientName;

    @Size(min = 2, max = 200, message = "Company name must be between 2 and 200 characters")
    private String companyName;

    @Email(message = "Email must be valid")
    @Size(min = 5, max = 100, message = "Email must be between 5 and 100 characters")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9\\s-]{10,20}$", message = "Phone must be 10-20 characters and start with country code (e.g., +62)")
    @Size(min = 10, max = 20, message = "Phone must be between 10 and 20 characters")
    private String phone;

    @Size(min = 2, max = 100, message = "Industry must be between 2 and 100 characters")
    private String industry;

    @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    private String address;

    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @Size(min = 2, max = 100, message = "Country must be between 2 and 100 characters")
    private String country;

    @Size(min = 2, max = 100, message = "PIC name must be between 2 and 100 characters")
    private String picName;

    @Email(message = "PIC email must be valid")
    @Size(min = 5, max = 100, message = "PIC email must be between 5 and 100 characters")
    private String picEmail;

    @Pattern(regexp = "^$|^\\+?[0-9\\s-]{10,20}$", message = "PIC phone must be 10-20 characters and start with country code (e.g., +62)")
    @Size(min = 10, max = 20, message = "PIC phone must be between 10 and 20 characters")
    private String picPhone;

    @Size(min = 2, max = 100, message = "PIC position must be between 2 and 100 characters")
    private String picPosition;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private ClientStatus status;
}
