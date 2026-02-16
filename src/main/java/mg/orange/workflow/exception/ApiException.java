package mg.orange.workflow.exception;

import jakarta.ws.rs.core.Response;

public class ApiException extends RuntimeException {
    private final Response.Status status;
    private final String errorCode;

    public ApiException(Response.Status status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ApiException(String message, int statusCode) {
        super(message);
        this.status = Response.Status.fromStatusCode(statusCode);
        this.errorCode = null;
    }

    public ApiException(Response.Status status, String message) {
        this(status, message, null);
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
