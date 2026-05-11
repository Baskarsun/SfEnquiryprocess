package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.Channel;
import com.sf.leasing.lead.domain.enums.DedupLabel;
import com.sf.leasing.lead.domain.enums.LeadType;
import com.sf.leasing.lead.domain.enums.SourceCategory;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.model.Applicant;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.infrastructure.adapter.CautionListAdapter;
import com.sf.leasing.lead.infrastructure.adapter.PanDedupAdapter;
import com.sf.leasing.lead.infrastructure.adapter.UcicMappingAdapter;
import com.sf.leasing.lead.infrastructure.persistence.LeadDedupResultRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    CautionListAdapter cautionListAdapter;

    @Mock
    UcicMappingAdapter ucicMappingAdapter;

    @Mock
    PanDedupAdapter panDedupAdapter;

    @Mock
    ExceptionQueueService exceptionQueueService;

    @Mock
    LeadDedupResultRepository dedupResultRepository;

    @Mock
    EntityManager em;

    DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new DeduplicationService(
            exceptionQueueService,
            cautionListAdapter,
            ucicMappingAdapter,
            panDedupAdapter,
            dedupResultRepository
        );
        ReflectionTestUtils.setField(deduplicationService, "em", em);

        when(cautionListAdapter.checkPan(anyString()))
            .thenReturn(CautionListAdapter.CautionStatus.ALLOWED);
        when(ucicMappingAdapter.lookupByPan(anyString()))
            .thenReturn(UcicMappingAdapter.UcicResult.notFound());
        when(ucicMappingAdapter.lookupByGstin(anyString()))
            .thenReturn(UcicMappingAdapter.UcicResult.notFound());
        when(panDedupAdapter.check(anyString()))
            .thenReturn(new PanDedupAdapter.PanDedupResult(PanDedupAdapter.DedupCode.CLEAR, null));
    }

    // -------------------------------------------------------
    // Gender / occupation check (COM122)
    // -------------------------------------------------------

    @Test
    void shouldThrowCom122WhenMaleApplicantHasHouseWifeOccupation() {
        Lead lead = buildLead(LeadType.INDIVIDUAL);
        Applicant a = buildApplicant("ABCDE1234F", null, null);
        a.gender     = "M";
        a.occupation = "HOUSE WIFE";
        a.panExemptionFlag = "N";
        lead.applicants = List.of(a);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> deduplicationService.runDedupChecks(lead, false));
        assertEquals("COM122", ex.getErrorCode());
    }

    @Test
    void shouldPassWhenFemaleApplicantHasHouseWifeOccupation() {
        Lead lead = buildLead(LeadType.INDIVIDUAL);
        Applicant a = buildApplicant("ABCDE1234F", null, null);
        a.gender     = "F";
        a.occupation = "HOUSE WIFE";
        a.panExemptionFlag = "N";
        lead.applicants = List.of(a);

        // Should not throw COM122
        assertDoesNotThrow(() -> deduplicationService.runDedupChecks(lead, false));
    }

    // -------------------------------------------------------
    // PAN mandatory check (COM129)
    // -------------------------------------------------------

    @Test
    void shouldThrowCom129WhenIndividualHasNoPanAndNoDanExemption() {
        Lead lead = buildLead(LeadType.INDIVIDUAL);
        Applicant a = buildApplicant(null, "9876543210", null);  // no PAN
        a.panExemptionFlag     = "N";
        a.constitutionType     = "INDIVIDUAL";
        lead.applicants = List.of(a);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> deduplicationService.runDedupChecks(lead, false));
        assertEquals("COM129", ex.getErrorCode());
    }

    @Test
    void shouldPassWhenIndividualHasDanExemption() {
        Lead lead = buildLead(LeadType.INDIVIDUAL);
        Applicant a = buildApplicant(null, "9876543210", null);  // no PAN
        a.panExemptionFlag = "Y";   // has DAN
        a.constitutionType = "INDIVIDUAL";
        lead.applicants = List.of(a);

        assertDoesNotThrow(() -> deduplicationService.runDedupChecks(lead, false));
    }

    // -------------------------------------------------------
    // Caution list check (LN5337)
    // -------------------------------------------------------

    @Test
    void shouldThrowLn5337WhenPanIsOnCautionList() {
        when(cautionListAdapter.checkPan("ABCDE1234F"))
            .thenReturn(CautionListAdapter.CautionStatus.BLOCKED);

        Lead lead = buildLead(LeadType.INDIVIDUAL);
        Applicant a = buildApplicant("ABCDE1234F", null, null);
        a.panExemptionFlag = "N";
        lead.applicants = List.of(a);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> deduplicationService.runDedupChecks(lead, true));
        assertEquals("LN5337", ex.getErrorCode());
    }

    // -------------------------------------------------------
    // External PAN dedup (COM110 hard reject)
    // -------------------------------------------------------

    @Test
    void shouldThrowCom110WhenExternalDedupConfirmsDuplicate() {
        when(panDedupAdapter.check("XYZPQ9999Z"))
            .thenReturn(new PanDedupAdapter.PanDedupResult(PanDedupAdapter.DedupCode.COM110, "EXT-MATCH-001"));

        Lead lead = buildLead(LeadType.INDIVIDUAL);
        Applicant a = buildApplicant("XYZPQ9999Z", null, null);
        a.panExemptionFlag = "N";
        lead.applicants = List.of(a);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> deduplicationService.runDedupChecks(lead, true));
        assertEquals("COM110", ex.getErrorCode());
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Lead buildLead(LeadType type) {
        Lead lead = new Lead();
        lead.lrn            = "LS-202601-000001";
        lead.leadType       = type;
        lead.sourceCategory = SourceCategory.INTERNAL;
        lead.sourceName     = "Test";
        lead.channel        = Channel.DESKTOP;
        lead.dedupLabel     = DedupLabel.UNKNOWN;
        return lead;
    }

    private Applicant buildApplicant(String pan, String mobile, String gstin) {
        Applicant a = new Applicant();
        a.pan    = pan;
        a.mobile = mobile;
        a.gstin  = gstin;
        a.gender = "M";
        return a;
    }
}
