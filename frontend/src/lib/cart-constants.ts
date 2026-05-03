/**
 * Sepetteki bir ürünün maksimum adet limiti — backend kuralı, frontend de aynı sayıya
 * göre +/- butonlarını cap'ler ki kullanıcı 409 hatası almasın.
 *
 * Backend: `app.cart.max-quantity-per-item` (default 10)
 * Backend exception: `CART_ITEM_QUANTITY_LIMIT_EXCEEDED`
 */
export const MAX_QUANTITY_PER_CART_ITEM = 10;
