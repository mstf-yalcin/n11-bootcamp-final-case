package com.n11.bootcamp.payment_service.gateway;

import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Cancel;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.Payment;
import com.iyzipay.model.PaymentCard;
import com.iyzipay.model.PaymentChannel;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.model.Status;
import com.iyzipay.request.CreateCancelRequest;
import com.iyzipay.request.CreatePaymentRequest;
import com.n11.bootcamp.payment_service.config.IyzicoProperties;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class IyzicoPaymentGateway implements PaymentGateway {

    private static final String DEFAULT_CITY = "Istanbul";
    private static final String DEFAULT_COUNTRY = "Turkey";
    private static final String DEFAULT_ADDRESS = "Sandbox test address";
    private static final String DEFAULT_ZIP = "34000";
    private static final String DEFAULT_IDENTITY_NUMBER = "11111111111";
    private static final String DEFAULT_IP = "127.0.0.1";

    private final Options options;
    private final IyzicoProperties.TestCard testCard;

    public IyzicoPaymentGateway(Options options, IyzicoProperties.TestCard testCard) {
        this.options = options;
        this.testCard = testCard;
    }

    @Override
    public PaymentGatewayResult charge(PaymentGatewayRequest request) {
        try {
            CreatePaymentRequest iyzicoRequest = buildRequest(request);
            Payment payment = Payment.create(iyzicoRequest, options);

            if (Status.SUCCESS.getValue().equals(payment.getStatus())) {
                log.info("Iyzico SUCCESS: orderId={}, paymentId={}, conversationId={}",
                        request.orderId(), payment.getPaymentId(), payment.getConversationId());
                return PaymentGatewayResult.ok(payment.getPaymentId());
            }

            log.warn("Iyzico FAILURE: orderId={}, errorCode={}, errorMessage={}, errorGroup={}",
                    request.orderId(), payment.getErrorCode(), payment.getErrorMessage(), payment.getErrorGroup());
            return PaymentGatewayResult.fail(
                    nullSafe(payment.getErrorCode(), "IYZICO_UNKNOWN"),
                    nullSafe(payment.getErrorMessage(), "Iyzico returned non-success status without message")
            );
        } catch (Exception e) {
            log.error("Iyzico EXCEPTION: orderId={}, error={}", request.orderId(), e.getMessage(), e);
            return PaymentGatewayResult.fail("IYZICO_EXCEPTION", e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResult refund(UUID orderId, String providerPaymentId, BigDecimal amount, String correlationId) {
        if (providerPaymentId == null || providerPaymentId.isBlank()) {
            log.warn("Refund skipped — no providerPaymentId available: orderId={}", orderId);
            return PaymentGatewayResult.fail("NO_PROVIDER_PAYMENT_ID", "No Iyzico paymentId stored");
        }
        try {
            CreateCancelRequest req = new CreateCancelRequest();
            req.setLocale(Locale.TR.getValue());
            req.setConversationId(orderId.toString());
            req.setPaymentId(providerPaymentId);
            req.setIp(DEFAULT_IP);

            Cancel cancel = Cancel.create(req, options);

            if (Status.SUCCESS.getValue().equals(cancel.getStatus())) {
                log.info("Iyzico CANCEL/refund SUCCESS: orderId={}, paymentId={}", orderId, providerPaymentId);
                return PaymentGatewayResult.ok(providerPaymentId);
            }

            log.warn("Iyzico CANCEL/refund FAILURE: orderId={}, errorCode={}, errorMessage={}",
                    orderId, cancel.getErrorCode(), cancel.getErrorMessage());
            return PaymentGatewayResult.fail(
                    nullSafe(cancel.getErrorCode(), "IYZICO_CANCEL_FAILED"),
                    nullSafe(cancel.getErrorMessage(), "Iyzico Cancel returned non-success status")
            );
        } catch (Exception e) {
            log.error("Iyzico CANCEL/refund EXCEPTION: orderId={}, error={}", orderId, e.getMessage(), e);
            return PaymentGatewayResult.fail("IYZICO_CANCEL_EXCEPTION", e.getMessage());
        }
    }

    private CreatePaymentRequest buildRequest(PaymentGatewayRequest request) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setLocale(Locale.TR.getValue());
        req.setConversationId(request.orderId().toString());
        req.setPrice(request.amount());
        req.setPaidPrice(request.amount());
        req.setCurrency(resolveCurrency(request.currency()));
        req.setInstallment(1);
        req.setBasketId(request.orderId().toString());
        req.setPaymentChannel(PaymentChannel.WEB.name());
        req.setPaymentGroup(PaymentGroup.PRODUCT.name());

        req.setPaymentCard(buildCard());
        req.setBuyer(buildBuyer(request));
        req.setShippingAddress(buildAddress(request.userId().toString()));
        req.setBillingAddress(buildAddress(request.userId().toString()));
        req.setBasketItems(buildBasket(request));
        return req;
    }

    private PaymentCard buildCard() {
        PaymentCard card = new PaymentCard();
        card.setCardHolderName(testCard.holderName());
        card.setCardNumber(testCard.number());
        card.setExpireMonth(testCard.expireMonth());
        card.setExpireYear(testCard.expireYear());
        card.setCvc(testCard.cvc());
        card.setRegisterCard(0);
        return card;
    }

    private Buyer buildBuyer(PaymentGatewayRequest request) {
        Buyer buyer = new Buyer();
        buyer.setId(request.userId().toString());
        buyer.setName("Customer");
        buyer.setSurname(request.userId().toString().substring(0, 8));
        buyer.setGsmNumber("+905350000000");
        buyer.setEmail(request.userEmail() != null ? request.userEmail() : "customer@example.com");
        buyer.setIdentityNumber(DEFAULT_IDENTITY_NUMBER);
        buyer.setRegistrationAddress(DEFAULT_ADDRESS);
        buyer.setIp(DEFAULT_IP);
        buyer.setCity(DEFAULT_CITY);
        buyer.setCountry(DEFAULT_COUNTRY);
        buyer.setZipCode(DEFAULT_ZIP);
        return buyer;
    }

    private Address buildAddress(String contactName) {
        Address address = new Address();
        address.setContactName(contactName);
        address.setCity(DEFAULT_CITY);
        address.setCountry(DEFAULT_COUNTRY);
        address.setAddress(DEFAULT_ADDRESS);
        address.setZipCode(DEFAULT_ZIP);
        return address;
    }

    private List<BasketItem> buildBasket(PaymentGatewayRequest request) {
        List<BasketItem> basket = new ArrayList<>();
        // Iyzico requires the sum of basket item prices to equal the request price.
        // We aggregate per product line as (unitPrice * quantity).
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < request.items().size(); i++) {
            PaymentGatewayRequest.Item item = request.items().get(i);
            BasketItem basketItem = new BasketItem();
            basketItem.setId(item.productId().toString());
            basketItem.setName("Product-" + item.productId().toString().substring(0, 8));
            basketItem.setCategory1("General");
            basketItem.setItemType(BasketItemType.PHYSICAL.name());

            BigDecimal lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            // Make sure last item absorbs rounding so basket sum matches request.amount() exactly.
            if (i == request.items().size() - 1) {
                lineTotal = request.amount().subtract(allocated);
            }
            basketItem.setPrice(lineTotal);
            allocated = allocated.add(lineTotal);
            basket.add(basketItem);
        }
        return basket;
    }

    private String resolveCurrency(String currency) {
        try {
            return Currency.valueOf(currency).name();
        } catch (Exception e) {
            return Currency.TRY.name();
        }
    }

    private String nullSafe(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
