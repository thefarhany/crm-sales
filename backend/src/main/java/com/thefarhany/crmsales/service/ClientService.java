package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.exception.BadRequestException;
import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.CreateClientRequest;
import com.thefarhany.crmsales.model.dto.request.UpdateClientRequest;
import com.thefarhany.crmsales.model.dto.response.ClientResponse;
import com.thefarhany.crmsales.model.entity.Client;
import com.thefarhany.crmsales.model.enums.ClientStatus;
import com.thefarhany.crmsales.repository.ClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {
    @Autowired
    private ClientRepository clientRepository;

    /**
     * Normalize phone number (08... -> +62...)
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String cleaned = phone.trim().replaceAll("[^\\d+]", "");
        // Convert leading 0 to +62
        if (cleaned.startsWith("0") && !cleaned.startsWith("00")) {
            return "+62" + cleaned.substring(1);
        }
        return cleaned;
    }

    /**
     * Create client baru
     */
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        // Cek email unik
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Client with email '" + request.getEmail() + "' already exists");
        }

        Client client = new Client();
        client.setClientName(request.getClientName());
        client.setCompanyName(request.getCompanyName());
        client.setEmail(request.getEmail());
        client.setPhone(normalizePhoneNumber(request.getPhone()));
        client.setIndustry(request.getIndustry());
        client.setAddress(request.getAddress());
        client.setCity(request.getCity());
        client.setCountry(request.getCountry() != null ? request.getCountry() : "Indonesia");
        client.setPicName(request.getPicName());
        client.setPicEmail(request.getPicEmail());
        client.setPicPhone(normalizePhoneNumber(request.getPicPhone()));
        client.setPicPosition(request.getPicPosition());
        client.setNotes(request.getNotes());
        client.setStatus(ClientStatus.ACTIVE);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());

        Client savedClient = clientRepository.save(client);
        return mapToClientResponse(savedClient);
    }

    /**
     * Get all clients dengan filter dan pagination
     */
    public PageResponse<ClientResponse> getAllClients(ClientStatus status, String keyword,
                                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Client> clientPage = clientRepository.findByFilters(status, keyword, pageable);

        List<ClientResponse> clientResponses = clientPage.getContent().stream()
                .map(this::mapToClientResponse)
                .collect(Collectors.toList());

        return PageResponse.<ClientResponse>builder()
                .content(clientResponses)
                .pageNumber(clientPage.getNumber())
                .pageSize(clientPage.getSize())
                .totalElements(clientPage.getTotalElements())
                .totalPages(clientPage.getTotalPages())
                .last(clientPage.isLast())
                .first(clientPage.isFirst())
                .build();
    }

    /**
     * Get client by ID
     */
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));
        return mapToClientResponse(client);
    }

    /**
     * Update client
     */
    @Transactional
    public ClientResponse updateClient(Long id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));

        // Cek email unik jika berubah
        if (request.getEmail() != null && !request.getEmail().equals(client.getEmail())) {
            if (clientRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            client.setEmail(request.getEmail());
        }

        if (request.getClientName() != null) {
            client.setClientName(request.getClientName());
        }
        if (request.getCompanyName() != null) {
            client.setCompanyName(request.getCompanyName());
        }
        if (request.getPhone() != null) {
            client.setPhone(normalizePhoneNumber(request.getPhone()));
        }
        if (request.getIndustry() != null) {
            client.setIndustry(request.getIndustry());
        }
        if (request.getAddress() != null) {
            client.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            client.setCity(request.getCity());
        }
        if (request.getCountry() != null) {
            client.setCountry(request.getCountry());
        }
        if (request.getPicName() != null) {
            client.setPicName(request.getPicName());
        }
        if (request.getPicEmail() != null) {
            client.setPicEmail(request.getPicEmail());
        }
        if (request.getPicPhone() != null) {
            client.setPicPhone(normalizePhoneNumber(request.getPicPhone()));
        }
        if (request.getPicPosition() != null) {
            client.setPicPosition(request.getPicPosition());
        }
        if (request.getNotes() != null) {
            client.setNotes(request.getNotes());
        }
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }

        client.setUpdatedAt(LocalDateTime.now());
        Client updatedClient = clientRepository.save(client);
        return mapToClientResponse(updatedClient);
    }

    /**
     * Delete client (soft delete - set status INACTIVE)
     */
    @Transactional
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));
        client.setStatus(ClientStatus.INACTIVE);
        client.setUpdatedAt(LocalDateTime.now());
        clientRepository.save(client);
    }

    /**
     * Import clients dari CSV
     */
    @Transactional
    public ImportResult importClients(MultipartFile file) {
        int imported = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = parseCsvLine(line);

                    if (fields.length < 3 || fields[0].trim().isEmpty()
                            || fields[1].trim().isEmpty() || fields[2].trim().isEmpty()) {
                        failed++;
                        errors.add("Line " + lineNumber + ": Missing required fields (client_name, company_name, email)");
                        continue;
                    }

                    String email = fields[2].trim();

                    if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        failed++;
                        errors.add("Line " + lineNumber + ": Invalid email format '" + email + "'");
                        continue;
                    }

                    if (clientRepository.existsByEmail(email)) {
                        failed++;
                        errors.add("Line " + lineNumber + ": Email already exists '" + email + "'");
                        continue;
                    }

                    Client client = new Client();
                    client.setClientName(fields[0].trim());
                    client.setCompanyName(fields[1].trim());
                    client.setEmail(email);
                    client.setPhone(getField(fields, 3));
                    client.setIndustry(getField(fields, 4));
                    client.setAddress(getField(fields, 5));
                    client.setCity(getField(fields, 6));
                    client.setCountry(getFieldOrDefault(fields, 7, "Indonesia"));
                    client.setPicName(getFieldOrDefault(fields, 8, fields[0].trim()));
                    client.setPicEmail(getFieldOrDefault(fields, 9, email));
                    client.setPicPhone(getField(fields, 10));
                    client.setPicPosition(getField(fields, 11));
                    client.setNotes(getField(fields, 12));
                    client.setStatus(ClientStatus.ACTIVE);
                    client.setCreatedAt(LocalDateTime.now());
                    client.setUpdatedAt(LocalDateTime.now());

                    clientRepository.save(client);
                    imported++;

                } catch (Exception e) {
                    failed++;
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read CSV file: " + e.getMessage());
        }

        return new ImportResult(imported, failed, errors);
    }

    /**
     * Parse CSV line
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        return fields.toArray(new String[0]);
    }

    /**
     * Get field dari array, return null jika index out of bounds atau empty
     */
    private String getField(String[] fields, int index) {
        if (index >= fields.length) return null;
        String value = fields[index].trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Get field dari array, return default value jika kosong
     */
    private String getFieldOrDefault(String[] fields, int index, String defaultValue) {
        String value = getField(fields, index);
        return value != null ? value : defaultValue;
    }

    /**
     * Export clients ke CSV
     */
    public byte[] exportClients(ClientStatus status, String keyword) {
        List<Client> clients;
        if (status != null || (keyword != null && !keyword.isEmpty())) {
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id").ascending());
            Page<Client> page = clientRepository.findByFilters(status, keyword, pageable);
            clients = page.getContent();
        } else {
            clients = clientRepository.findAll(Sort.by("id").ascending());
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Client Name,Company Name,Email,Phone,Industry,City,Country,PIC Name,PIC Email,PIC Phone,PIC Position,Status,Created At\n");

        for (Client client : clients) {
            csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    client.getId(),
                    nullSafe(client.getClientName()),
                    nullSafe(client.getCompanyName()),
                    nullSafe(client.getEmail()),
                    nullSafe(client.getPhone()),
                    nullSafe(client.getIndustry()),
                    nullSafe(client.getCity()),
                    nullSafe(client.getCountry()),
                    nullSafe(client.getPicName()),
                    nullSafe(client.getPicEmail()),
                    nullSafe(client.getPicPhone()),
                    nullSafe(client.getPicPosition()),
                    client.getStatus(),
                    client.getCreatedAt()
            ));
        }

        return csv.toString().getBytes();
    }

    /**
     * Get active clients (untuk dropdown)
     */
    public List<ClientResponse> getActiveClients() {
        List<Client> clients = clientRepository.findByStatus(ClientStatus.ACTIVE);
        return clients.stream()
                .map(this::mapToClientResponse)
                .collect(Collectors.toList());
    }

    // ========== Helper Methods ==========

    private ClientResponse mapToClientResponse(Client client) {
        // TODO: Uncomment saat ContractRepository sudah dibuat
        // Long totalContracts = contractRepository.countByClientId(client.getId());
        // Long activeContracts = contractRepository.countByClientIdAndStatus(client.getId(), ContractStatus.ACTIVE);

        return ClientResponse.builder()
                .id(client.getId())
                .clientName(client.getClientName())
                .companyName(client.getCompanyName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .industry(client.getIndustry())
                .address(client.getAddress())
                .city(client.getCity())
                .country(client.getCountry())
                .picName(client.getPicName())
                .picEmail(client.getPicEmail())
                .picPhone(client.getPicPhone())
                .picPosition(client.getPicPosition())
                .status(client.getStatus())
                .notes(client.getNotes())
                .totalContracts(0L)      // TODO: ganti dengan contractRepository count
                .activeContracts(0L)     // TODO: ganti dengan contractRepository count
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    // Inner class untuk import result
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ImportResult {
        private int imported;
        private int failed;
        private List<String> errors;
    }

}
