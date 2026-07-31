package com.thefarhany.crmsales.model.dto.response;

import com.thefarhany.crmsales.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String dummyField = "Hello World";

    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private Role role;
}
