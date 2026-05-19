package com.n11.bootcamp.product_service.util;

import java.text.Normalizer;

public final class SlugUtil {

    private SlugUtil() {
    }

    public static String slugifyTurkish(String input) {
        String tr = input
                .replace('ı', 'i').replace('İ', 'I')
                .replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ş', 's').replace('Ş', 'S')
                .replace('ö', 'o').replace('Ö', 'O')
                .replace('ü', 'u').replace('Ü', 'U')
                .replace('ç', 'c').replace('Ç', 'C');
        return Normalizer.normalize(tr, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
