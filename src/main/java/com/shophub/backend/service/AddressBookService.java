package com.shophub.backend.service;

import com.shophub.backend.dto.AddressBookDto;
import com.shophub.backend.dto.AddressBookRequest;
import com.shophub.backend.entity.Address;
import com.shophub.backend.entity.User;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.AddressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AddressBookService {

    private static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;

    public AddressBookService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressBookDto> list(User user) {
        return addressRepository.findByUserId(user.getId()).stream()
                .sorted(Comparator.comparing(Address::isDefaultAddress).reversed()
                        .thenComparing(Comparator.comparing(Address::getId).reversed()))
                .map(AddressBookService::toDto)
                .toList();
    }

    @Transactional
    public List<AddressBookDto> create(User user, AddressBookRequest req) {
        if (addressRepository.countByUserId(user.getId()) >= MAX_ADDRESSES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can save up to " + MAX_ADDRESSES + " addresses");
        }
        Address a = new Address();
        a.setUser(user);
        apply(a, req);
        boolean first = addressRepository.countByUserId(user.getId()) == 0;
        Address saved = addressRepository.save(a);
        if (first || Boolean.TRUE.equals(req.makeDefault())) makeDefault(user, saved);
        return list(user);
    }

    @Transactional
    public List<AddressBookDto> update(User user, Long id, AddressBookRequest req) {
        Address a = find(user, id);
        apply(a, req);
        addressRepository.save(a);
        if (Boolean.TRUE.equals(req.makeDefault())) makeDefault(user, a);
        return list(user);
    }

    @Transactional
    public List<AddressBookDto> setDefault(User user, Long id) {
        makeDefault(user, find(user, id));
        return list(user);
    }

    @Transactional
    public List<AddressBookDto> delete(User user, Long id) {
        Address a = find(user, id);
        boolean wasDefault = a.isDefaultAddress();
        addressRepository.delete(a);
        if (wasDefault) {                                  // keep one default when any address is left
            addressRepository.findByUserId(user.getId()).stream()
                    .filter(x -> !x.getId().equals(id))
                    .max(Comparator.comparing(Address::getId))
                    .ifPresent(next -> makeDefault(user, next));
        }
        return list(user);
    }

    // ---------------------------------------------------------------- helpers

    private Address find(User user, Long id) {
        // looked up by owner too, so one customer can never touch another one's address
        return addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Address not found"));
    }

    private void makeDefault(User user, Address target) {
        for (Address a : addressRepository.findByUserId(user.getId())) {
            boolean shouldBe = a.getId().equals(target.getId());
            if (a.isDefaultAddress() != shouldBe) {
                a.setDefaultAddress(shouldBe);
                addressRepository.save(a);
            }
        }
    }

    private void apply(Address a, AddressBookRequest req) {
        a.setLabel(req.label() == null || req.label().isBlank() ? "Home" : req.label().trim());
        a.setFirstName(req.firstName().trim());
        a.setLastName(req.lastName() == null ? "" : req.lastName().trim());
        a.setPhone(req.phone() == null ? "" : req.phone().trim());
        a.setStreet(req.address().trim());
        a.setCity(req.city().trim());
        a.setZipCode(req.zipCode().trim());
        a.setCountry(req.country().trim());
    }

    private static AddressBookDto toDto(Address a) {
        return new AddressBookDto(String.valueOf(a.getId()), a.getLabel(), a.getFirstName(), a.getLastName(),
                a.getPhone(), a.getStreet(), a.getCity(), a.getZipCode(), a.getCountry(), a.isDefaultAddress());
    }
}
