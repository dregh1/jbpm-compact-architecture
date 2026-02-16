package mg.orange.workflow.exception;

import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

    private static final Logger LOG = Logger.getLogger(RuntimeExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(RuntimeException exception) {
        // Log détaillé pour le debugging
        LOG.error(String.format("[%s] %s %s - Internal server error: %s",
                headers.getHeaderString("User-Agent"),
                uriInfo.getRequestUri(),
                headers.getHeaderString("X-Forwarded-For") != null ?
                    "via " + headers.getHeaderString("X-Forwarded-For") : "",
                exception.getMessage()), exception);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createErrorResponse(exception))
                .build();
    }

    private ErrorResponse createErrorResponse(RuntimeException exception) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Internal server error");
        errorResponse.setErrorCode("INTERNAL_ERROR");
        errorResponse.setTimestamp(System.currentTimeMillis());
        return errorResponse;
    }
}
