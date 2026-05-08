package com.sf.leasing.lead.api.exception;

import com.sf.leasing.lead.api.dto.response.ApiError;
import com.sf.leasing.lead.domain.exception.BusinessException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    @Override
    public Response toResponse(BusinessException ex) {
        ApiError error = new ApiError(ex.getErrorCode(), ex.getMessage(), ex.isWarningOnly());
        Response.Status status = ex.isWarningOnly()
            ? Response.Status.OK          // 200 for advisory warnings (e.g. geographic warning on mobile)
            : Response.Status.BAD_REQUEST; // 400 for hard business errors
        return Response.status(status).entity(error).build();
    }
}
