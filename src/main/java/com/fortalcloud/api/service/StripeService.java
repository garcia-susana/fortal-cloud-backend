package com.fortalcloud.api.service;

import com.fortalcloud.api.exception.BusinessException;
import com.fortalcloud.api.exception.ResourceNotFoundException;
import com.fortalcloud.api.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.price-id}")
    private String priceId;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    private final UserRepository userRepository;

    public StripeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String createCheckoutSession(String userEmail){
        Stripe.apiKey = secretKey;

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(userEmail)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();

        } catch (StripeException ex) {
            throw new BusinessException("Erro ao criar sessão de pagamento: " + ex.getMessage());
        }

    }

    public void cancelSubscription(String userEmail) {
        Stripe.apiKey = secretKey;

        //Busca o srtipeCustomerId do usuário
        String customerId = userRepository.findByEmail(userEmail)
                .map(u -> u.getStripeCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if(customerId == null) {
            throw new BusinessException("Usuário não possui assinatura ativa no Stripe.");
        }

        try{
            //Lista assinaturas ativas do cliente
            SubscriptionListParams listParams = SubscriptionListParams.builder()
                    .setCustomer(customerId)
                    .setStatus(SubscriptionListParams.Status.ACTIVE)
                    .build();

            SubscriptionCollection subscriptions = Subscription.list(listParams);

            if (subscriptions.getData().isEmpty()){
                throw new BusinessException("Nenhuma assinatura ativa encontrada.");
            }

            //Cancela a primeira assinatura ativa
            Subscription subscription = subscriptions.getData().get(0);
            subscription.cancel();

        } catch (StripeException e) {
            throw new BusinessException("Erro ao cancelar assinatura: " +  e.getMessage());
        }
    }
}