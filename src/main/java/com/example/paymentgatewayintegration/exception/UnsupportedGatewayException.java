package com.example.paymentgatewayintegration.exception;

public class UnsupportedGatewayException extends RuntimeException {

    public UnsupportedGatewayException(String message) {
        super(message);
    }
}