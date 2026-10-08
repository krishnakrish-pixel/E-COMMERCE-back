package com.shophub.backend.controller;

import com.shophub.backend.dto.AddressBookDto;
import com.shophub.backend.dto.AddressBookRequest;
import com.shophub.backend.entity.User;
import com.shophub.backend.service.AddressBookService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The logged-in customer's saved addresses. Every endpoint returns the full, updated list. */
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressBookService service;

    public AddressController(AddressBookService service) {
        this.service = service;
    }

    @GetMapping
    public List<AddressBookDto> list(@AuthenticationPrincipal User user) {
        return service.list(user);
    }

    @PostMapping
    public List<AddressBookDto> create(@AuthenticationPrincipal User user, @Valid @RequestBody AddressBookRequest request) {
        return service.create(user, request);
    }

    @PutMapping("/{id}")
    public List<AddressBookDto> update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                       @Valid @RequestBody AddressBookRequest request) {
        return service.update(user, id, request);
    }

    @PutMapping("/{id}/default")
    public List<AddressBookDto> setDefault(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.setDefault(user, id);
    }

    @DeleteMapping("/{id}")
    public List<AddressBookDto> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.delete(user, id);
    }
}
