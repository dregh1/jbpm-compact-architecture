package mg.orange.workflow.exception;

import org.jboss.logging.Logger;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private static final Logger LOG = Logger.getLogger(NotFoundExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(NotFoundException exception) {
        // Log détaillé pour le debugging
        LOG.warn(String.format("[%s] %s %s - Endpoint not found",
                headers.getHeaderString("User-Agent"),
                uriInfo.getRequestUri(),
                headers.getHeaderString("X-Forwarded-For") != null ?
                    "via " + headers.getHeaderString("X-Forwarded-For") : ""));

        return Response.status(Response.Status.NOT_FOUND)
                .entity(createErrorResponse())
                .build();
    }

    private ErrorResponse createErrorResponse() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Endpoint not found");
        errorResponse.setErrorCode("ENDPOINT_NOT_FOUND");
        errorResponse.setTimestamp(System.currentTimeMillis());
        return errorResponse;
    }
}
