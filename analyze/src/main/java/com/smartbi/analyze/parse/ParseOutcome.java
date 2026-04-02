package com.smartbi.analyze.parse;

public class ParseOutcome {

    private final ParseOutcomeStatus status;
    private final QuerySignature signature;
    private final String errorMessage;

    private ParseOutcome(ParseOutcomeStatus status, QuerySignature signature, String errorMessage) {
        this.status = status;
        this.signature = signature;
        this.errorMessage = errorMessage;
    }

    public static ParseOutcome ok(QuerySignature signature) {
        return new ParseOutcome(ParseOutcomeStatus.OK, signature, null);
    }

    public static ParseOutcome error(String message) {
        return new ParseOutcome(ParseOutcomeStatus.ERROR, null, message);
    }

    public static ParseOutcome skipped(String reason) {
        return new ParseOutcome(ParseOutcomeStatus.SKIPPED, null, reason);
    }

    public ParseOutcomeStatus getStatus() {
        return status;
    }

    public QuerySignature getSignature() {
        return signature;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
