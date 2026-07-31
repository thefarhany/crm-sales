package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.request.AiEmailRequest;
import com.thefarhany.crmsales.model.dto.response.AiEmailResponse;
import com.thefarhany.crmsales.service.AiEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiEmailController {

    private final AiEmailService aiEmailService;

    @PostMapping("/generate-email")
    public ResponseEntity<AiEmailResponse> generateEmail(@RequestBody AiEmailRequest request) {

        AiEmailResponse response = aiEmailService.generateRenewalEmail(request);

        return ResponseEntity.ok(response);
    }
}