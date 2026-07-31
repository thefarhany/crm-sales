package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.response.GlobalSearchResponse;
import com.thefarhany.crmsales.model.entity.Client;
import com.thefarhany.crmsales.model.entity.Contract;
import com.thefarhany.crmsales.repository.ClientRepository;
import com.thefarhany.crmsales.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<List<GlobalSearchResponse>>> search(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(ApiResponse.success("Query too short", new ArrayList<>()));
        }

        String trimmedQuery = query.trim();
        List<GlobalSearchResponse> results = new ArrayList<>();

        // Search Contracts
        List<Contract> contracts = contractRepository.findTop5ByQuery(trimmedQuery);
        results.addAll(contracts.stream().map(c -> GlobalSearchResponse.builder()
                .id(c.getId().toString())
                .title(c.getContractNumber())
                .subtitle(c.getContractTitle())
                .type("CONTRACT")
                .link("/contracts/" + c.getId())
                .build()).collect(Collectors.toList()));

        // Search Clients
        List<Client> clients = clientRepository.findTop5ByQuery(trimmedQuery);
        results.addAll(clients.stream().map(c -> GlobalSearchResponse.builder()
                .id(c.getId().toString())
                .title(c.getCompanyName())
                .subtitle(c.getClientName())
                .type("CLIENT")
                .link("/clients/" + c.getId())
                .build()).collect(Collectors.toList()));

        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", results));
    }
}
