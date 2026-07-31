package com.thefarhany.crmsales.controller;

import com.thefarhany.crmsales.model.dto.ApiResponse;
import com.thefarhany.crmsales.model.dto.PageResponse;
import com.thefarhany.crmsales.model.dto.request.CreateClientRequest;
import com.thefarhany.crmsales.model.dto.request.UpdateClientRequest;
import com.thefarhany.crmsales.model.dto.response.ClientResponse;
import com.thefarhany.crmsales.model.enums.ClientStatus;
import com.thefarhany.crmsales.model.dto.response.ContractResponse;
import com.thefarhany.crmsales.service.ContractService;
import com.thefarhany.crmsales.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ContractService contractService;

    /**
     * GET /api/clients
     * Get all clients dengan filter dan pagination
     * Access: ADMIN, MARKETING_MANAGER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM', 'BOD')")
    public ResponseEntity<ApiResponse<PageResponse<ClientResponse>>> getAllClients(
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ClientResponse> clients = clientService.getAllClients(status, keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Clients retrieved successfully", clients)
        );
    }

    /**
     * GET /api/clients/{id}/contracts
     * Get all contracts dari client
     * Access: All authenticated users
     */
    @GetMapping("/{id}/contracts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ContractResponse>>> getClientContracts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ContractResponse> contracts = contractService.getContractsByClient(id, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Client contracts retrieved successfully", contracts)
        );
    }

    /**
     * POST /api/clients
     * Create client baru
     * Access: ADMIN, MARKETING_MANAGER, MARKETING_TEAM
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @Valid @RequestBody CreateClientRequest request) {

        ClientResponse client = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Client created successfully", client)
        );
    }

    /**
     * GET /api/clients/{id}
     * Get client by ID
     * Access: All authenticated users
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientById(@PathVariable Long id) {
        ClientResponse client = clientService.getClientById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Client retrieved successfully", client)
        );
    }

    /**
     * PUT /api/clients/{id}
     * Update client
     * Access: ADMIN, MARKETING_MANAGER, MARKETING_TEAM
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest request) {

        ClientResponse client = clientService.updateClient(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Client updated successfully", client)
        );
    }

    /**
     * DELETE /api/clients/{id}
     * Delete client (soft delete)
     * Access: ADMIN, MARKETING_MANAGER
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.ok(
                ApiResponse.success("Client deleted successfully", null)
        );
    }

    /**
     * POST /api/clients/import
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM')")
    public ResponseEntity<ApiResponse<ClientService.ImportResult>> importClients(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("File is empty")
            );
        }

        ClientService.ImportResult result = clientService.importClients(file);
        return ResponseEntity.ok(
                ApiResponse.success("Import completed: " + result.getImported() + " imported, " + result.getFailed() + " failed", result)
        );
    }

    /**
     * GET /api/clients/export
     * Export clients ke CSV
     * Access: ADMIN, MARKETING_MANAGER
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_TEAM', 'MARKETING_MANAGER')")
    public ResponseEntity<byte[]> exportClients(
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) String keyword) {

        byte[] csvData = clientService.exportClients(status, keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "clients_export.csv");

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

    /**
     * GET /api/clients/active
     * Get active clients (untuk dropdown selector)
     * Access: All authenticated users
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getActiveClients() {
        List<ClientResponse> clients = clientService.getActiveClients();
        return ResponseEntity.ok(
                ApiResponse.success("Active clients retrieved successfully", clients)
        );
    }

    /**
     * GET /api/clients/import-template
     */
    @GetMapping("/import-template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_TEAM')")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        String template =
                "client_name,company_name,email,phone,industry,address,city,country,pic_name,pic_email,pic_phone,pic_position,notes\n" +
                        "John Doe,PT Maju Jaya Teknologi,john@majujaya.com,08123456789,Technology,\"Jl. Sudirman No. 10, Gedung A Lt. 5\",Jakarta,Indonesia,Jane Doe,jane@majujaya.com,08198765432,IT Manager,Client besar dari Jakarta\n" +
                        "Ahmad Reza,CV Berkah Sentosa,ahmad@berkah.com,08111222333,Manufacturing,\"Jl. Rungkut Industri No. 25\",Surabaya,Indonesia,Siti Nurhaliza,siti@berkah.com,08133445566,Procurement Head,Client manufacturing Surabaya\n" +
                        "Maria Garcia,PT Global Solusi Digital,maria@globalsolusi.com,02155667788,Consulting,\"Jl. Gatot Subroto Kav. 12\",Jakarta,Indonesia,Budi Santoso,budi@globalsolusi.com,08177889900,Finance Director,\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "client_import_template.csv");

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = template.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);

        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }
}
