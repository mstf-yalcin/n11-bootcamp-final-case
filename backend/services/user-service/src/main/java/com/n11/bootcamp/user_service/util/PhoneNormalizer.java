package com.n11.bootcamp.user_service.util;

public final class PhoneNormalizer {

    private static final String COUNTRY_CODE = "+90";

    private PhoneNormalizer() {}

    public static String ensureCountryCode(String phone) {
        if (phone == null || phone.isBlank()) return null;
        return phone.startsWith(COUNTRY_CODE) ? phone : COUNTRY_CODE + phone;
    }
}
