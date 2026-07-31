package com.thefarhany.crmsales.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReminderSettingRequest {

    @NotBlank(message = "Setting key wajib diisi")
    private String settingKey;

    @NotBlank(message = "Setting value wajib diisi")
    private String settingValue;
}
