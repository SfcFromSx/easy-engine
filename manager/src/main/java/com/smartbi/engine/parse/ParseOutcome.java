package com.smartbi.engine.parse;

import com.smartbi.engine.domain.ParseStatus;

public class ParseOutcome {

    private final ParseStatus status;
    private final QuerySignature signature;
    private final String errorMessage;

    private ParseOutcome(ParseStatus status, QuerySignature signature, String errorMessage) {
        this.status = status;
        this.signature = signature;
        this.errorMessage = errorMessage;
    }

    public static ParseOutcome ok(QuerySignature signature) {
        return new ParseOutcome(ParseStatus.OK, signature, null);
    }

    public static ParseOutcome error(String message) {
        return new ParseOutcome(ParseStatus.ERROR, null, message);
    }

    public static ParseOutcome skipped(String reason) {
        return new ParseOutcome(ParseStatus.SKIPPED, null, reason);
    }

    public ParseStatus getStatus() {
        return status;
    }

    public QuerySignature getSignature() {
        return signature;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
