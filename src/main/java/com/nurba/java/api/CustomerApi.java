package com.nurba.java.api;

import com.nurba.java.dto.request.CustomerRequest;
import com.nurba.java.dto.responce.CustomerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Customer", description = "Управление данными клиентов")
@RequestMapping("/api/v1/customer")
public interface CustomerApi {

    @Operation(summary = "Получить список всех клиентов")
    @GetMapping
    List<CustomerResponse> getAll();

    @Operation(summary = "Получить клиента по ID")
    @GetMapping("/{id}")
    CustomerResponse getById(@PathVariable Long id);

    @Operation(summary = "Создать нового клиента")
    @PostMapping
    CustomerResponse create(CustomerRequest customerRequest);

    @Operation(summary = "Обновлять клиента")
    @PutMapping
    CustomerResponse update(CustomerRequest customerRequest);

    @Operation(summary = "Удалить клиента")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
