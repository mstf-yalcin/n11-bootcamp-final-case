package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.user_service.dto.request.CreateAddressRequest;
import com.n11.bootcamp.user_service.dto.request.UpdateAddressRequest;
import com.n11.bootcamp.user_service.dto.response.AddressResponse;
import com.n11.bootcamp.user_service.dto.response.CheckoutContextResponse;
import com.n11.bootcamp.user_service.dto.response.UserInfoResponse;
import com.n11.bootcamp.user_service.entity.Address;
import com.n11.bootcamp.user_service.entity.User;
import com.n11.bootcamp.user_service.exception.AddressNotFoundException;
import com.n11.bootcamp.user_service.exception.UserNotFoundException;
import com.n11.bootcamp.user_service.repository.AddressRepository;
import com.n11.bootcamp.user_service.repository.UserRepository;
import com.n11.bootcamp.user_service.util.PhoneNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AddressService {

    private static final String DEFAULT_COUNTRY = "Turkey";

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressResponse> listForUser(UUID userId) {
        return addressRepository.findAllByUserIdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, CreateAddressRequest request) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId.toString()));

        boolean shouldBeDefault = Boolean.TRUE.equals(request.isDefault())
                || !addressRepository.existsByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);

        Address address = Address.builder()
                .userId(userId)
                .title(request.title())
                .contactName(request.contactName())
                .fullAddress(request.fullAddress())
                .city(request.city())
                .district(request.district())
                .country(request.country() != null && !request.country().isBlank()
                        ? request.country() : DEFAULT_COUNTRY)
                .zipCode(request.zipCode())
                .phone(PhoneNormalizer.ensureCountryCode(request.phone()))
                .isDefault(shouldBeDefault)
                .build();

        Address saved = addressRepository.save(address);

        if (shouldBeDefault) {
            addressRepository.unsetDefaultExcept(userId, saved.getId());
        }

        log.info("Address created: addressId={}, userId={}, isDefault={}",
                saved.getId(), userId, shouldBeDefault);
        return toResponse(saved);
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, UpdateAddressRequest request) {
        Address address = addressRepository.findByIdAndUserIdAndIsActiveTrue(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        if (request.title() != null)
            address.setTitle(request.title());
        if (request.contactName() != null)
            address.setContactName(request.contactName());
        if (request.fullAddress() != null)
            address.setFullAddress(request.fullAddress());
        if (request.city() != null)
            address.setCity(request.city());
        if (request.district() != null)
            address.setDistrict(request.district());
        if (request.country() != null)
            address.setCountry(request.country());
        if (request.zipCode() != null)
            address.setZipCode(request.zipCode());
        if (request.phone() != null)
            address.setPhone(PhoneNormalizer.ensureCountryCode(request.phone()));

        boolean becomesDefault = Boolean.TRUE.equals(request.isDefault());
        if (becomesDefault && !address.isDefault()) {
            address.setDefault(true);
            addressRepository.save(address);
            addressRepository.unsetDefaultExcept(userId, addressId);
        } else {
            addressRepository.save(address);
        }

        log.info("Address updated: addressId={}, userId={}", addressId, userId);
        return toResponse(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserIdAndIsActiveTrue(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
        address.setActive(false);
        addressRepository.save(address);
        log.info("Address soft-deleted: addressId={}, userId={}", addressId, userId);
    }

    public CheckoutContextResponse getCheckoutContext(UUID userId, UUID addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        Address address = addressRepository.findByIdAndUserIdAndIsActiveTrue(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        UserInfoResponse userInfo = new UserInfoResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhone()
        );

        String effectivePhone = (address.getPhone() != null && !address.getPhone().isBlank())
                ? address.getPhone() : user.getPhone();

        AddressResponse addressResponse = new AddressResponse(
                address.getId(), address.getTitle(), address.getContactName(),
                address.getFullAddress(), address.getCity(), address.getDistrict(),
                address.getCountry(), address.getZipCode(),
                effectivePhone, address.isDefault()
        );

        return new CheckoutContextResponse(userInfo, addressResponse);
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(
                a.getId(), a.getTitle(), a.getContactName(),
                a.getFullAddress(), a.getCity(), a.getDistrict(),
                a.getCountry(), a.getZipCode(), a.getPhone(), a.isDefault()
        );
    }

}
