package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.model.dto.request.AiEmailRequest;
import com.thefarhany.crmsales.model.dto.response.AiEmailResponse;
import com.thefarhany.crmsales.model.entity.Contract;
import com.thefarhany.crmsales.model.entity.GeneratedEmail;
import com.thefarhany.crmsales.repository.ContractRepository;
import com.thefarhany.crmsales.repository.GeneratedEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiEmailService {

    private final ContractRepository contractRepository;
    private final GeneratedEmailRepository generatedEmailRepository;

    private final RestTemplate restClient = new RestTemplate();

    @Transactional
    public AiEmailResponse generateRenewalEmail(AiEmailRequest request) {

        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract Not Found!"));

        Map<String, Object> pythonPayload = new HashMap<>();
        pythonPayload.put("client_name", contract.getClient().getPicName() != null ? contract.getClient().getPicName() : "-");
        pythonPayload.put("company_name", contract.getClient().getCompanyName() != null ? contract.getClient().getCompanyName() : "-");
        pythonPayload.put("end_date", contract.getEndDate() != null ? contract.getEndDate().toString() : "-");
        pythonPayload.put("tone", request.getTone() != null ? request.getTone().name() : "PROFESSIONAL");
        pythonPayload.put("additional_note", request.getAdditionalNote() != null ? request.getAdditionalNote() : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pythonPayload, headers);

        ResponseEntity<AiEmailResponse> responseEntity = restClient.postForEntity(
                "http://localhost:8000/api/ai/generate",
                entity,
                AiEmailResponse.class
        );

        AiEmailResponse pythonResponse = responseEntity.getBody();

        if (pythonResponse == null || pythonResponse.getContent() == null) {
            throw new RuntimeException("Waduh, server Python gagal ngasih balasan nih!");
        }

        String generatedSubject = "Renewal Notice - " + contract.getClient().getCompanyName();

        GeneratedEmail history = new GeneratedEmail();
        history.setContract(contract);
        history.setSubject(generatedSubject);
        history.setGeneratedContent(pythonResponse.getContent());
        generatedEmailRepository.save(history);

        pythonResponse.setSubject(generatedSubject);

        return pythonResponse;
    }
}