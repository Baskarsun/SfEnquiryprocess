package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.ApplicantRequest;
import com.sf.leasing.lead.api.dto.request.CreateLeadRequest;
import com.sf.leasing.lead.api.dto.response.CreateLeadResponse;
import com.sf.leasing.lead.domain.enums.Channel;
import com.sf.leasing.lead.domain.enums.LeadType;
import com.sf.leasing.lead.domain.enums.SourceCategory;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import com.sf.leasing.lead.infrastructure.persistence.ApplicantRepository;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadCreationServiceTest {

    @Mock RedisSequenceGenerator sequenceGenerator;
    @Mock LeadEventProducer eventProducer;
    @Mock ExceptionQueueService exceptionQueueService;
    @Mock DeduplicationService deduplicationService;
    @Mock GeographicValidationService geographicValidationService;
    @Mock LeadRepository leadRepository;
    @Mock ApplicantRepository applicantRepository;

    LeadCreationService service;

    @BeforeEach
    void setUp() {
        service = new LeadCreationService(
            sequenceGenerator, exceptionQueueService, deduplicationService,
            geographicValidationService, eventProducer, leadRepository, applicantRepository
        );
        when(sequenceGenerator.generateLrn(any())).thenReturn("LS-202604-000001");
        when(sequenceGenerator.generateTempCustomerNumber()).thenReturn("TMP-2026-000001");
        Mockito.doNothing().when(eventProducer).publishLeadCreated(any(), any(), any(), any());
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(applicantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(geographicValidationService.validatePincodeBranch(any(), any(), any())).thenReturn(true);
    }

    @Test
    void createLead_nullRequest_throwsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.createLead(null, "EMP003", Channel.DESKTOP, null, null));
        assertEquals(ErrorCodes.LEAD_INPUT_REQUIRED, ex.getErrorCode());
    }

    @Test
    void createLead_nullLeadType_throwsBusinessException() {
        CreateLeadRequest req = new CreateLeadRequest();
        req.sourceCategory = SourceCategory.INTERNAL;
        req.sourceName     = "Test Source";
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.createLead(req, "EMP003", Channel.DESKTOP, null, null));
        assertEquals(ErrorCodes.LEAD_INPUT_REQUIRED, ex.getErrorCode());
    }

    @Test
    void createLead_commercialWithoutCompanyName_throwsBusinessException() {
        CreateLeadRequest req = buildCommercialRequest();
        req.companyKnownAs = null;
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.createLead(req, "EMP003", Channel.DESKTOP, null, null));
        assertEquals(ErrorCodes.LEAD_INPUT_REQUIRED, ex.getErrorCode());
    }

    @Test
    void createLead_invalidPanFormat_throwsBusinessException() {
        CreateLeadRequest req = buildIndividualRequest();
        req.applicants.get(0).pan = "INVALID123";
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.createLead(req, "EMP003", Channel.DESKTOP, null, null));
        assertEquals(ErrorCodes.LEAD_INPUT_REQUIRED, ex.getErrorCode());
    }

    @Test
    void createLead_noMinimumIdentifier_routesToExceptionQueue() {
        CreateLeadRequest req = buildIndividualRequest();
        req.applicants.get(0).mobile = null;
        req.applicants.get(0).email  = null;
        req.applicants.get(0).pan    = null;
        req.applicants.get(0).gstin  = null;
        req.applicants.get(0).addressLine1 = null;

        when(exceptionQueueService.routeToExceptionQueue(any(), any(), any(), any(), any(), any()))
            .thenReturn(null);

        CreateLeadResponse response = service.createLead(req, "EMP003", Channel.DESKTOP, null, null);
        assertTrue(response.routedToExceptionQueue);
    }

    @Test
    void validateUserAndDevice_blankUserId_throwsGl461() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.validateUserAndDevice("", "IMEI123", null));
        assertEquals(ErrorCodes.INVALID_USER, ex.getErrorCode());
    }

    private CreateLeadRequest buildIndividualRequest() {
        CreateLeadRequest req = new CreateLeadRequest();
        req.leadType       = LeadType.INDIVIDUAL;
        req.sourceCategory = SourceCategory.INTERNAL;
        req.sourceName     = "Walk-In-2026";

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.applicantName    = "John Doe";
        applicant.mobile           = "9876543210";
        applicant.constitutionType = "INDIVIDUAL";
        req.applicants = List.of(applicant);
        return req;
    }

    private CreateLeadRequest buildCommercialRequest() {
        CreateLeadRequest req = new CreateLeadRequest();
        req.leadType       = LeadType.COMMERCIAL;
        req.sourceCategory = SourceCategory.DEALER;
        req.sourceName     = "DealerXYZ-2026";
        req.companyKnownAs = "Acme Corp";
        req.contactPerson  = "Jane Smith";

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.mobile           = "9876543211";
        applicant.constitutionType = "NON_INDIVIDUAL";
        req.applicants = List.of(applicant);
        return req;
    }
}
