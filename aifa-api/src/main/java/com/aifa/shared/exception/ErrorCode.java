package com.aifa.shared.exception;

public final class ErrorCode {

    public static final String WALLET_REQUIRED = "WALLET_REQUIRED";
    public static final String INSUFFICIENT_WALLET_BALANCE = "INSUFFICIENT_WALLET_BALANCE";
    public static final String SMS_FORMAT_NOT_RECOGNIZED = "SMS_FORMAT_NOT_RECOGNIZED";
    public static final String AIRTEL_NOT_SUPPORTED_FOR_MTN_WALLET = "AIRTEL_NOT_SUPPORTED_FOR_MTN_WALLET";
    public static final String IMPORT_BATCH_NOT_FOUND = "IMPORT_BATCH_NOT_FOUND";
    public static final String IMPORT_BATCH_EXPIRED = "IMPORT_BATCH_EXPIRED";

    private ErrorCode() {}
}
