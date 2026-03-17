// SubscriptionRequiredException.java
package com.fortalcloud.api.exception;

public class SubscriptionRequiredException extends RuntimeException {
    public SubscriptionRequiredException() {
        super("Assinatura ativa necessária para acessar este recurso.");
    }
}