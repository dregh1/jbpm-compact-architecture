package mg.orange.workflow.exception;

import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(ApiException exception) {
        String errorCode = exception.getErrorCode() != null ? exception.getErrorCode() : "UNKNOWN_ERROR";

        // Log détaillé pour le debugging
        LOG.error(String.format("[%s] %s %s - Error %s (%s): %s",
                headers.getHeaderString("User-Agent"),
                uriInfo.getRequestUri(),
                headers.getHeaderString("X-Forwarded-For") != null ?
                    "via " + headers.getHeaderString("X-Forwarded-For") : "",
                exception.getStatus().getStatusCode(),
                errorCode,
                exception.getMessage()));

        return Response.status(exception.getStatus())
                .entity(createErrorResponse(exception.getMessage(), errorCode))
                .build();
    }

    private ErrorResponse createErrorResponse(String message, String errorCode) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(message);
        errorResponse.setErrorCode(errorCode);
        errorResponse.setTimestamp(System.currentTimeMillis());
        return errorResponse;
    }
}
