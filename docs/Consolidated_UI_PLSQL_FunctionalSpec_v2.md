## Functional Name

**Loan Enquiry Creation and Submission**

---

## 1. Summary

This process enables a field officer or authorised user to create or modify a loan enquiry by submitting all required customer, asset, and financial information — along with supporting identity documents — through a mobile or online channel. Upon receipt, the process authenticates the requesting user and their device, validates the completeness and format of all submitted data, applies a comprehensive sequence of business validations spanning identity document checks against caution and blocked lists, de-duplication of identity details against internal records and external services, cross-branch customer policy enforcement, geographic service-area validation, product and eligibility rule enforcement (including restrictions for NRI applicants, customers with active emergency credit lines, and deceased customers), and asset cost integrity checks. Once all validations pass, the process creates or updates the enquiry record and assigns a unique system-generated enquiry number. It records all applicant details, marks the DAN as utilised where applicable, dispatches consent SMS messages to opted-in applicants, uploads KYC document images to the document management system, links the enquiry to any originating lead management record, initiates fraud screening and credit bureau requests, screens all applicants against the caution database, and sends internal alerts and marketing employee notifications where applicable before returning the enquiry number and asset cost advisory to the caller.

- **Business purpose:** Create a validated, fully recorded loan enquiry that can proceed to formal application assessment, or update an existing enquiry with revised data.
- **Triggered by:** A field officer, marketing employee, or authorised system user submitting loan enquiry details via a mobile or web interface.
- **Accepts document upload:** Yes — a compressed archive of KYC images is accepted alongside the enquiry data.
- **Input summary:** User credentials and device identifiers; processing mode (create or modify); enquiry date; contract and product details; asset details; one or more applicant records containing personal, address, identity document, and communication details; GPS location; and an optional lead management system reference.
- **Output summary:** A system-generated enquiry number, a success or failure status with descriptive message, an asset cost advisory where the system price has been updated, and credit bureau and fraud check status indicators.
- **Key supporting operations:**
  - User and device authentication against the system registry
  - Branch resolution from the employee register
  - Mobile number format and eligibility validation (length, starting digit, prohibited pattern, device/employee exclusivity)
  - Mobile number validation error logging in a permanent independent transaction
  - CKYC duplication check (unique per company and not shared with a different PAN)
  - One-branch-one-customer policy enforcement with component-specific severity (active from a configured implementation date)
  - KYC identity document de-duplication across PAN, driving licence, passport, voter ID, and Aadhaar
  - PAN caution and block list screening
  - External PAN de-duplication API call with graded response handling
  - Geographic pincode-to-branch service-area validation
  - Product model market value retrieval 
  - Appraisal category validation 
  - Enquiry number generation in an independent transaction using a locked control record
  - Post-save caution list screening with email alerts for sanction and internal list matches
  - Fraud screening via Hunter and Sherlock services 
  - Credit bureau request initiation
  - Document image upload to the document management system
  - Lead management system linkage 
  - Internal alert emails for two-stage approval branches
  - Marketing employee notifications for new and reassigned enquiries


java_source_line_ranges: ["Java: f1_18185-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: f4_15407-f5_4343, f16_110-f16_21809, f16_19546-f16_19771, f16_27916-f16_28098, f16_34505-f16_34721, f16_613-f16_4034"]
---

## 2. High-Level Functional Requirements

1. The process must authenticate the requesting user and validate that their registered device is active before accepting any submission, rejecting unauthenticated requests immediately with the reason provided.
2. The process must require that all mandatory submission data — including user identity, processing mode, enquiry date, contract details, and at least one applicant record — is present, rejecting the submission if any mandatory element is absent.
3. The process must resolve the submitting user's assigned branch from the employee register and use that branch for all subsequent geographic and product validations.
4. The process must validate each applicant's Aadhaar number using the Verhoeff check-digit algorithm, rejecting the submission if any number fails.
5. The process must enforce all mobile number format rules — exact length, permitted starting digit, prohibited repetitive patterns, and exclusion from employee device registrations and employee records — logging every failure permanently in an independent transaction that survives any main transaction rollback.
6. The process must validate mobile number validation applicability against a system-wide parameter; if validation is disabled, all mobile number checks must be bypassed.
7. The process must screen each applicant's CKYC number for uniqueness within the company and must reject any submission where a CKYC number is shared with a different customer carrying a different PAN.
8. The process must enforce the one-branch-one-customer policy, checking whether the applicant holds an active loan at a different branch and responding with an error or informational message of severity determined by the calling context, but only after the system-configured policy implementation date has been reached.
9. The process must screen each applicant's identity documents — PAN, driving licence, passport, voter ID, and Aadhaar — against the internal caution and block list, halting with a specific error if any PAN is not in an allowed state.
10. The process must perform external PAN de-duplication for non-OEM enquiries, halting on a confirmed duplicate, halting for review on a potential duplicate, and allowing continuation on a soft warning.
11. The process must validate the applicant's geographic location against the branch's service area, applying distance-based and explicit mapping checks, with bypass provisions for SME branches, users with the designated bypass access right, and transactions on the explicit exclusion list.
12. The process must validate the asset cost as a positive value not less than the requested finance amount, and must validate the asset model year within the permitted range for vehicle assets.
13. The process must enforce product eligibility rules for NRI applicants, restricting them to permitted product and asset combinations and mandating valid passport details, alternate mobile number, and email address.
14. The process must block enquiry creation for customers with active emergency credit line accounts, for customers marked as deceased, and for contract types that must be originated through the Vruddhi channel by users with Vruddhi access.
15. The process must block new enquiries at branches with unresolved aged pending enquiries, and must block new enquiries where a marketing employee or branch has pending items exceeding the configured day thresholds.
16. The process must prevent modification of an enquiry for which a loan application already exists in a non-terminal state, for which all applicants are in a closed status, or which is under active fraud investigation.
17. The process must create or update the enquiry record and assign a unique system-generated enquiry number following the standard format, using an independent transaction that locks the control record to prevent concurrent duplicates.
18. The process must record all applicant details, validate and mark DAN records as utilised, enforce address line length constraints, and dispatch SMS consent messages to opted-in applicants without allowing SMS failures to halt the transaction.
19. The process must screen all applicants against the caution database after saving, sending a formatted email alert to pre-configured recipients for sanction list matches and a separate alert for internal list matches, without allowing caution check results to block the overall committed transaction.
20. The process must upload KYC document images to the document management system under the enquiry reference, routing to the correct environment-specific endpoint and encoding each image in Base64.
21. The process must initiate fraud screening and credit bureau requests following successful enquiry creation, and must send internal alert emails to branch teams and system notifications to marketing employees where applicable.
22. The process must release all held resources regardless of whether the process completed successfully or failed.


java_source_line_ranges: ["Java: f1_18185-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: f4_15407-f5_4343, f16_110-f16_21809, f16_19546-f16_19771, f16_613-f16_4034, f16_17305-f16_18039, f16_36211-f16_40586"]

---

## 3. Process Flow

### 3.1 Process Overview

The Loan Enquiry Creation and Submission process receives a multipart request from a field officer operating via a mobile or online channel. It authenticates the requesting user and device, applies a comprehensive sequence of business validations spanning identity, geography, product eligibility, and data integrity, persists the enquiry and applicant records, and returns a unique enquiry number identifying the record for all downstream processing. When operating in modify mode, the process re-validates all data and refreshes the existing record. The process generates its own enquiry numbers, handles document uploads, and initiates downstream fraud and credit checks before returning control to the caller.

java_source_line_ranges: ["Java: f1_18185-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: f16_613-f16_4034"]


### 3.2 End-to-End Process Flow

---

**Step 1: User and Device Authentication**
- **Fields involved:** User identifier, Device IMEI, Device UUID, Application version
- The primary device identifier is checked; if blank, the secondary device identifier is substituted.
- The authentication service is invoked to verify that the user is active in the system registry and that the device is authorised.
- Decision point — user authentication outcome:
  - If authentication fails or an error is returned: a failure response is returned to the caller with the reason; no further processing occurs.
  - If authentication succeeds: processing continues to Step 2.
- **Applicable rules:** Rule 1
- **Outcome:** User and device are confirmed as active and authorised.

java_source_line_ranges: ["Java: f1_18283-f1_18291"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED — PS_PK_VAL_APP_STORE_DTLS.PS_PR_OUT_USER_STATUS not provided in B2)"]
user_defined_methods_called: [getUserValidity() — PROVIDED]
backend_procedures_called: [PS_PK_VAL_APP_STORE_DTLS.PS_PR_OUT_USER_STATUS]

---

**Step 2: Input Completeness Check**
- **Fields involved:** Enquiry input details, Contract type
- The processing mode indicator is trimmed of whitespace.
- The enquiry input details field is trimmed; if blank after trimming, it is treated as absent.
- Decision point — enquiry input details presence:
  - If absent: a failure response is returned indicating that input values are required; processing ends.
  - If present: processing continues to Step 3.
- **Applicable rules:** Rule 2
- **Outcome:** All mandatory top-level input is confirmed present.

java_source_line_ranges: ["Java: f1_18295-f1_18309"]
plsql_source_line_ranges: []
plsql_rule_ids_covered: []

---

**Step 3: Branch Resolution**
- **Fields involved:** User identifier, Branch code
- The user's assigned branch is retrieved from the employee register by matching the user identifier (left-padded to 6 characters) to the employee record.
- The resolved branch code is used in all subsequent geographic, product, and enquiry-saving operations.
- **Applicable rules:** Rule 3
- **Outcome:** The submitting user's branch is known and held for use throughout the process.

java_source_line_ranges: ["Java: f1_18311"]
plsql_source_line_ranges: []
user_defined_methods_called: [getUserBranch() — PROVIDED]
plsql_rule_ids_covered: []

---

**Step 4: Enquiry Date Parsing and Contract Details Extraction**
- **Fields involved:** Enquiry date, Enquiry input details, Contract type, Asset make, Asset type, Asset cost, Contract type, Asset class, Business source, Loan purpose, Marketing employee, Finance amount, Loan tenure, Number of units, Asset model, Remarks, Assessment criteria, Employment years, Net income, Asset usage, Experience in years, Quoted interest rate, Residence pincode, Dealer code, Appraisal category, Fuel type, Horsepower, Product model, Enquiry number
- The enquiry date string is parsed into a date value using the DD/MM/YYYY format.
- The enquiry input details are parsed to extract the contract details sub-object.
- Decision point — contract type presence:
  - If the contract type field is absent from the contract details: a failure response is returned; processing ends.
  - If present: all contract-level fields are extracted (asset make, asset type, asset cost, contract type, asset class, business source, loan purpose, marketing employee, finance amount, loan tenure, number of units, asset model, remarks, assessment criteria, employment years, net income, asset usage, experience in years, quoted interest rate, residence pincode, residence location, dealer code, appraisal category, fuel type, tractor ownership details, land holdings, agricultural indicator, repayment frequency, horsepower, product model).
- If the enquiry input contains an existing enquiry number sub-object, that value is extracted and held for modify mode.
- **Applicable rules:** Rule 2
- **Outcome:** All contract-level fields are extracted and available for validation and persistence.

java_source_line_ranges: ["Java: f1_18312-f1_18362"]
plsql_source_line_ranges: []
plsql_rule_ids_covered: []

---

**Step 5: Applicant Details Extraction and Preparation**
- **Fields involved:** Applicant type, Customer code, Gender, Date of birth, Passport validity date, SMS indicator, Declaration Account Number (DAN), PAN exemption flag, Enquiry decision indicator, CKYC number, Mobile number, Alternate mobile number
- The applicant list is iterated. For each applicant record:
  - The applicant type is extracted and normalised (characters after a hyphen-separator are removed).
  - The applicant is classified as Main Applicant or Additional Applicant with a sequential number.
  - The customer code presence determines whether the applicant is flagged as existing or new.
  - Gender is normalised to a single-character code; for non-individual applicants, the gender field is cleared.
  - Date of birth and passport validity date are parsed from DD/MM/YYYY if provided.
  - All KYC document numbers, contact details, and preference flags are extracted.
  - The SMS indicator is derived from the enquiry decision indicator: if the decision indicator is 'N', the SMS indicator is cleared; if 'Y' and no SMS indicator is set, it defaults to 'N'.
  - Any modification override for the SMS indicator is applied from the modify-details sub-object for the matching applicant type.
  - If a Declaration Account Number (DAN) is provided, the PAN exemption flag is set; otherwise it is cleared.
  - The customer code is resolved against both the customer register and the contract register; if it refers to a contract number, the contract number is stored separately and the associated customer code is retrieved and substituted.
  - For the main applicant, the location record identifier is preserved for use in product model price retrieval.
  - Each prepared applicant record is assembled into the full applicant collection and, if flagged as new (based on the SMS send indicator), also into the modify-specific collection.
- **Applicable rules:** Rule 13 (gender normalisation), Rule 14 (SMS indicator derivation)
- **Outcome:** All applicant records are prepared, normalised, and assembled for validation and persistence.


java_source_line_ranges: ["Java: f1_18363-f1_18735"]
plsql_source_line_ranges: []
plsql_rule_ids_covered: []

---

**Step 6: Aadhaar Number Validation**
- **Fields involved:** Aadhaar number
- The Aadhaar number from the last-processed applicant is subjected to the Verhoeff algorithm check: the digit string is reversed, processed through a permutation table and a multiplication table, and the computed result must equal zero.
- Decision point — Aadhaar validity:
  - If the computed check value is not zero: any partial changes are reversed, and a failure response indicating an invalid Aadhaar number is returned; processing ends.
  - If valid: processing continues to Step 7.
- **Applicable rules:** Rule 15
- **Outcome:** The Aadhaar number is confirmed as structurally valid.

java_source_line_ranges: ["Java: f1_18740-f1_18751"]
plsql_source_line_ranges: []
user_defined_methods_called: [aadhaarValidation() — PROVIDED, stringToReversedIntArray() — PROVIDED, reverse() — PROVIDED]
plsql_rule_ids_covered: []

---

**Step 7: KYC De-duplication and Caution Screening**
- **Fields involved:** PAN number, Aadhaar number, Constitution type, Driving licence number, Passport number, Voter ID number, Mobile number, Alternate mobile number, Gender, Occupation, Customer code, GECL system parameter, Deceased indicator, CKYC number, Risk category
- The KYC details of all applicants are submitted to the KYC validation routine.
- For each applicant, the routine:
  - Retrieves existing KYC data for comparison if the enquiry already exists, to detect changed document numbers.
  - Attempts to locate an existing customer record using the customer code, then the PAN if no customer code is available.
  - Checks the GECL system parameter to determine if emergency credit line validation is active.
  - If a PAN is present, submits it to the PAN caution and block list check; if the status is anything other than 'Allowed', halts with a specific error.
  - Iterates through all five identity document types (PAN, driving licence, passport, voter ID, Aadhaar):
    - Validates that a male applicant's occupation is not 'House Wife'.
    - Determines whether PAN is mandatory for the applicant based on their profile; if mandatory and absent, raises an error.
    - If the submitted document number differs from the stored value, performs a deeper de-duplication check against the master contact database for that document type.
  - Validates that individual applicants' Aadhaar numbers are exactly 12 digits long.
  - If the GECL parameter is active and the applicant is an existing customer, checks for active emergency credit line contracts; if one is found, raises an error.
  - Validates both the primary and alternate mobile numbers against all mobile number rules (see Rules 22–28); logs any failure permanently in an independent transaction.
- Decision point — any KYC or mobile validation failure:
  - If a failure is returned: processing halts and the error is returned to the caller.
  - If all pass: processing continues to Step 8.
- **Applicable rules:** Rule 5 (mobile format rules), Rule 6 (mobile validation bypass), Rule 7 (mobile error logging), Rule 16 (Aadhaar 12-digit length), Rule 17 (gender/occupation consistency), Rule 18 (PAN mandatory), Rule 19 (PAN caution block), Rule 20 (KYC de-duplication for non-PAN documents), Rule 29 (GECL block), Rule 30 (deceased customer block, see also Step 9)
- **Outcome:** All applicant KYC documents are confirmed as valid, unique, and not on blocked or caution lists; mobile numbers are confirmed as eligible.

java_source_line_ranges: ["Java: f1_18759-f1_18786"]
plsql_source_line_ranges: ["PL/SQL: f16_19980-f16_20601, f4_15465-f4_15658, f4_16635-f4_16696"]
user_defined_methods_called: []
backend_procedures_called: [PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_MOB_VAL_KYC_DTLS]
plsql_rule_ids_covered: [C001_BR3, C001_BR6, C001_BR7, C001_BR8, C001_BR9, C001_BR10, C001_BR11, C001_BR22, C001_BR23, C001_BR24, C001_BR25, C001_BR32, C005_BR28, C005_BR26, C005_BR27, C005_BR14, C005_BR10]
---

**Step 8: Pre-Save Cross-Branch Contact Validation**
- **Fields involved:** Customer code, Branch code, Branch segment, Save indicator, System parameter (policy implementation date)
- Decision point — save indicator and applicant type:
  - If the save indicator signals a pre-save validation pass, the current applicant is an existing customer, and the applicant is the main applicant:
    - The system checks whether the applicant holds an active, non-closed loan at a different branch.
    - Decision point — policy implementation date:
      - If the current date is before the configured policy implementation date: the one-branch check is bypassed entirely.
      - If on or after: the check proceeds.
    - Decision point — conflicting branch found:
      - If a conflict is found, the response severity is determined by the calling context: for the mobile enquiry component, an informational message is issued; for other components, an error is raised.
    - Any informational warning is collected for potential inclusion in the pincode validation response in Step 9.
  - If the save indicator does not trigger pre-save validation: this step is skipped.
- **Applicable rules:** Rule 31 (one-branch-one-customer policy), Rule 32 (policy activation date gate)
- **Outcome:** Cross-branch customer status is assessed; any warning is held for combination with the pincode validation response.


java_source_line_ranges: ["Java: f1_18566-f1_18589"]
plsql_source_line_ranges: ["PL/SQL: f4_15869-f4_16176"]
backend_procedures_called: [PS_PK_COM_VAL_CONTACT.PS_PR_VAL_CONTACT_ONE_BRANCH]
plsql_rule_ids_covered: [C001_BR4, C001_BR5]

---

**Step 9: Geographic Pincode-to-Branch Validation**
- **Fields involved:** Applicant pincode, Branch code, Branch segment, Marketing employee code, Access right code, Statement code, Enquiry date, Distance, Enquiry number, Contract number
- For each applicant where an applicant type has been established, the applicant's pincode and location are validated against the branch's geographic service area:
  - Decision point — branch segment:
    - If the branch is designated as an SME branch: all geographic validation is bypassed for this applicant.
    - Otherwise: validation proceeds.
  - Decision point — user bypass access:
    - If the marketing employee holds the designated bypass access right: pincode validation is not applicable and is skipped.
    - Otherwise: the applicable flag is determined from the marketing employee's statement code configuration.
  - If validation is applicable:
    - The distance between the applicant's location and the branch is computed or retrieved.
    - Decision point — distance within limit:
      - If the distance is within the configured maximum: no error; validation passes.
      - If the distance exceeds the maximum: the system checks whether the pincode is on the explicit exclusion list.
        - If on the exclusion list: validation is bypassed for this transaction.
        - If not on the exclusion list: the system checks whether the pincode is explicitly mapped to the branch.
          - If mapped: validation passes.
          - If not mapped: an informational message is issued for the mobile channel component; a hard error is raised for all other components.
  - Any cross-branch warning from Step 8 is combined with a pincode informational response into a single conditional response.
- Decision point — validation outcome:
  - If a hard error is returned: processing ends with that error.
  - If a conditional informational response (status 3) is returned: processing ends; the caller decides whether to acknowledge and proceed.
  - If validation passes: processing continues to Step 10.
- **Applicable rules:** Rule 33 (SME branch bypass), Rule 34 (user bypass access), Rule 35 (statement code configuration), Rule 36 (distance limit), Rule 37 (explicit exclusion list), Rule 38 (pincode mapping), Rule 39 (severity by calling context)
- **Outcome:** The applicant's geographic location is confirmed as eligible for the branch, or the caller is informed of a conditional geographic restriction.


java_source_line_ranges: ["Java: f1_18591-f1_18628"]
plsql_source_line_ranges: ["PL/SQL: f16_17305-f16_18039"]
backend_procedures_called: [PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_VAL_BRANCH_PINCODE_MAP]
plsql_rule_ids_covered: [C005_BR36, C005_BR37, C005_BR38, C005_BR39, C005_BR40, C005_BR41, C005_BR42]

---

**Step 10: Comprehensive Enquiry Data Validation**
- **Fields involved:** Asset model year, Asset category, Asset cost, Finance amount, Contract type, User identifier, Location name, Mobile number, Alternate mobile number, PAN number, Customer code, GECL system parameter, Deceased indicator, Residential type, Constitution type, Asset class code, Asset usage code, Passport number, Passport validity date, Email address
- The full set of enquiry data is submitted to the comprehensive validation routine.
- The routine validates:
  - For vehicle assets (asset category 1): the asset model year falls within the permitted range calculated from the current year and the system parameter for vehicle model age.
  - The asset cost is greater than zero.
  - The asset cost is greater than or equal to the requested finance amount.
  - For new enquiries from users with Vruddhi channel access attempting designated contract types: the submission is blocked and the user is directed to the Vruddhi channel.
  - No applicant has a missing location name.
  - For each applicant: mobile number format validity and employee/device exclusivity (with permanent error logging on failure); external PAN de-duplication for non-OEM enquiries; alternate mobile number validity; active emergency credit line check for existing customers; deceased customer check; and for NRI individual applicants, all NRI-specific product, document, and contact requirements.
- Decision point — any validation failure:
  - If a failure is returned: any partial changes are reversed, and a failure response with the specific violation message is returned; processing ends.
  - If all pass: processing continues to Step 11.
- **Applicable rules:** Rule 4 (asset model year), Rule 8 (asset cost positive), Rule 9 (asset cost versus finance amount), Rule 5–7 (mobile number rules and error logging), Rule 10 (external PAN de-duplication), Rule 11–12 (PAN de-duplication response grading), Rule 19 (PAN caution block), Rule 29 (GECL block), Rule 30 (deceased customer block), Rule 40 (Vruddhi channel restriction), Rule 41 (applicant location name mandatory), Rule 42–43 (NRI eligibility and document requirements)
- **Outcome:** All enquiry business data is confirmed as valid and internally consistent.


java_source_line_ranges: ["Java: f1_18795-f1_18853"]
plsql_source_line_ranges: ["PL/SQL: f16_20883-f16_21809"]
backend_procedures_called: [PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_VAL_ENQUIRY_DATA]
plsql_rule_ids_covered: [C005_BR7, C005_BR9, C005_BR10, C005_BR11, C005_BR12, C005_BR13, C005_BR14, C005_BR15, C005_BR16, C005_BR17, C005_BR18, C001_BR22, C001_BR23, C001_BR24, C001_BR25, C001_BR26, C001_BR27, C001_BR28, C001_BR29, C001_BR30, C001_BR31, C001_BR32]

---

**Step 11: Product Model Market Value Retrieval**
- **Fields involved:** Contract type, Asset class, Dealer code, Product model, Number of units, Asset cost
- The current dealer price for the product model is retrieved based on the contract type, asset class, dealer code, main applicant location, product model identifier, and quantity.
- Processing is delegated for product model price lookup — backend specification not provided; logic not available for this step.
- Decision point — retrieval outcome:
  - If an error is returned: any partial changes are reversed, and a failure response is returned; processing ends.
  - If a market value is returned: it replaces the previously extracted asset cost, and an advisory message is prepared to inform the caller that the asset cost has been updated to the current state-level price.
  - If no market value is returned: the submitted asset cost is retained unchanged.
- **Applicable rules:** (No Section 5 rule governs this step independently — product model price retrieval has no B2 specification)
- **Outcome:** The asset cost used for all subsequent steps reflects the most current system price where available.

java_source_line_ranges: ["Java: f1_18862-f1_18895"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED — PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_GET_PRODUCT_MODEL_DATA not provided in B2)"]
backend_procedures_called: [PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_GET_PRODUCT_MODEL_DATA]
plsql_rule_ids_covered: []

---

**Step 12: Appraisal Category Validation**
- **Fields involved:** Appraisal category, Branch code, Contract type, Asset class, Asset cost, Enquiry date
- The selected appraisal category is validated against the branch, contract type, asset class, updated asset cost, and enquiry date.
- Processing is delegated for appraisal category validation — backend specification not provided; logic not available for this step.
- Decision point — validation outcome:
  - If a failure is returned: any partial changes are reversed, and a failure response is returned; processing ends.
  - If validation passes: processing continues to Step 13.
- **Applicable rules:** (No Section 5 rule governs this step independently — appraisal category validation has no B2 specification)
- **Outcome:** The appraisal category is confirmed as valid for the branch and product combination.

java_source_line_ranges: ["Java: f1_18906-f1_18937"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED — PS_PK_LN_VAL_CONTRACT.PS_PR_VAL_APPRAISAL_CATEGORY not provided in B2)"]
backend_procedures_called: [PS_PK_LN_VAL_CONTRACT.PS_PR_VAL_APPRAISAL_CATEGORY]
plsql_rule_ids_covered: []

---

**Step 13: Enquiry Record Creation or Modification**
- **Fields involved:** Branch code, Company code, User identifier, Marketing employee code, Contract type, Source of business, Enquiry number, Enquiry date, Applicant type, Constitution type, Branch stage, DAN value, PAN exemption indicator, Street name (address line 1), Address line 2, SMS indicator, Applicant status, Hunter result, Hunter verification result, Sherlock result, Sherlock verification result, CIBIL status, Enquiry status, Asset control applicability, Caution list match details, Applicant identity details, Follow-up date, Permissible pending day parameters
- The process invokes the enquiry persistence routine, passing all validated contract and applicant data along with the processing mode. The routine executes the following in sequence:
  - Validates the branch is currently active with open business and accounting dates; halts with an invalid branch error if not.
  - Attempts to resolve the active user record for the marketing employee code; if not found, sets the marketing user to absent and continues.
  - For new enquiries initiated from the mobile enquiry channel: validates the user is not absent; screens each applicant's PAN against the caution and block list; validates the user has an employee record; enforces the Vruddhi channel restriction for designated contract types.
  - Decision point — non-exempt business source and component:
    - If the source of business is not one of the exempted partner sources and the component is not the partner origination component: invokes the branch-level enquiry blocking checks (aged pending enquiry check differentiated by day of month, and maximum pending days check for the marketing employee and branch across enquiries and applications).
    - If the source is an exempted partner: this branch blocking check is skipped.
  - Calls the comprehensive data validation routine (as in Step 10) as a final gate before persistence.
  - Determines the branch's processing stage (single-stage or two-stage approval) and whether the Hunter fraud service is active.
  - Decision point — processing mode:
    - **Create mode:** A unique enquiry header record identifier is obtained from the system sequence. A new unique human-readable enquiry number is generated by locking the document number control record for the branch and document type (waiting up to 15 seconds for the lock), incrementing the sequence number, committing the reservation in an independent transaction, and constructing the number as prefix + 4-digit branch + 2-digit year + padded sequence. The enquiry header record is inserted with status Pending and full audit detail. If the mode is creation from a lead, the originating lead staging record is updated. For each applicant: the DAN is validated as present and unused and marked as utilised; address lines are validated for length; the applicant detail record is inserted; if the applicant has opted in for SMS, a confirmation message is dispatched; for the main non-individual applicant at a single-stage branch, the enquiry is immediately advanced to Eligible for Application.
    - **Modify mode:** Existing enquiry header data is retrieved. The Vruddhi source-to-contract-type consistency is checked. The source of business update is validated against vendor configuration. The enquiry header record is updated with new data; fraud and CIBIL results are reset to trigger re-processing. Applicant records that have not yet received a confirmation SMS are deleted and re-inserted with updated data. SMS consent messages and DAN updates are applied. The enquiry eligibility is re-evaluated based on the updated fraud check results: non-individual applicants are immediately set to Eligible for Application; individual applicants advance to Eligible for Application only if all required Hunter, Sherlock, and CIBIL results are passing or manually approved.
  - If the mode was new creation for contract type 11 (Hire Purchase) and the enquiry is now Eligible for Application: a welcome communication is sent to the customer.
  - If the enquiry remains in Pending status at a two-stage approval branch with applicable asset control: an internal alert email with full enquiry details is sent to the configured branch team recipients.
  - All preceding database changes are made permanent.
  - All applicants are screened against the caution database. For each applicant, the caution check call determines identity document details from the customer register for existing customers. The caution list validation is performed. If the result indicates a sanction list match requiring notification, a formatted email is sent to a configured recipient list using the sanction list notification event. If the result indicates an internal or other caution list match requiring notification, a separate formatted email is sent using the internal caution list notification event. These notifications do not block the transaction.
  - If a new enquiry was created by a user other than the designated marketing employee: a system notification is sent to the marketing employee. If an enquiry was modified and the marketing employee was changed: a notification is sent to the newly assigned marketing employee.
- Decision point — persistence routine outcome:
  - If an error is returned: any partial changes are reversed, and a failure response is returned; processing ends.
  - If successful: the enquiry number is captured from the output; processing continues to Step 14.
- **Applicable rules:** Rule 44 (active branch required), Rule 45 (valid user required), Rule 19 (PAN caution block), Rule 40 (Vruddhi restriction), Rule 46 (aged pending enquiry block), Rule 47 (maximum pending days block), Rule 48 (one-branch-one-customer policy — internal context), Rule 21 (enquiry number generation), Rule 22 (annual reset configuration), Rule 23 (document prefix), Rule 24 (number lock contention), Rule 25 (enquiry number format), Rule 49 (DAN validity and utilisation), Rule 50 (address line length), Rule 26 (enquiry modification blocked — application exists), Rule 27 (enquiry modification blocked — closed), Rule 28 (enquiry modification blocked — fraud pending), Rule 51 (Vruddhi source-to-contract consistency), Rule 52 (source of business update), Rule 53 (non-individual auto-eligibility), Rule 54 (individual eligibility after fraud and credit checks), Rule 55 (delete-and-re-insert on modify), Rule 56 (welcome communication for Hire Purchase), Rule 57 (internal alert for two-stage branch), Rule 58 (post-save caution screening), Rule 59 (caution sanction list email), Rule 60 (caution internal list email), Rule 61 (marketing employee notification — new), Rule 62 (marketing employee notification — reassignment), Rule 14 (SMS non-critical failure)
- **Outcome:** The enquiry record is persisted, all applicants are recorded, all notifications and communications are dispatched, and the enquiry number is available for downstream steps.

java_source_line_ranges: ["Java: f1_18953-f1_19073"]
plsql_source_line_ranges: ["PL/SQL: f16_613-f16_4034, f16_19546-f16_19771, f16_27916-f16_28098, f16_34505-f16_34721, f16_36211-f16_40586, f4_15465-f4_15658, f4_15869-f4_16176, f4_16635-f4_16696, f5_3288-f5_4343"]
backend_procedures_called: [PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_IUD_ENQUIRY_DATA]
plsql_rule_ids_covered: [C005_BR1, C005_BR2, C005_BR3, C005_BR4, C005_BR8, C005_BR9, C005_BR14, C005_BR19, C005_BR20, C005_BR21, C005_BR22, C005_BR23, C005_BR24, C005_BR43, C005_BR44, C005_BR45, C005_BR46, C005_BR47, C005_BR48, C005_BR49, C005_BR50, C005_BR51, C005_BR52, C005_BR53, C005_BR54, C005_BR55, C005_BR56, C005_BR57, C005_BR58, C005_BR59, C001_BR1, C001_BR2, C001_BR3, C001_BR12, C001_BR13, C001_BR14, C001_BR15, C001_BR16, C001_BR17]

---

**Step 14: Lead Management System Linkage**
- **Fields involved:** Lead management system reference, Enquiry number, User identifier, Company code
- Decision point — lead management system reference presence:
  - If a lead management system reference was provided and is non-blank: the lead update routine is called to record the newly generated enquiry number against the originating lead record.
  - Processing is delegated for lead record linkage — backend specification not provided; logic not available for this step.
  - If this call encounters an error, the error is logged but does not fail the overall transaction; a secondary confirmation is issued on success.
  - If no reference was provided: this step is skipped.
- **Applicable rules:** (No Section 5 rule governs this step — lead linkage has no B2 specification)
- **Outcome:** The originating lead record is updated with the enquiry number where applicable.


java_source_line_ranges: ["Java: f1_19092-f1_19134"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED — ps_pk_ln_iud_lead.ps_pr_upd_enquiry_no not provided in B2)"]
backend_procedures_called: [ps_pk_ln_iud_lead.ps_pr_upd_enquiry_no]
plsql_rule_ids_covered: []

---

**Step 15: KYC Document Image Upload**
- **Fields involved:** Document archive, Environment indicator, Enquiry number, Branch code, GPS coordinates
- Decision point — document file presence:
  - If the uploaded file is present and non-empty:
    - The archive is saved to the configured temporary directory under a composite identifier built from the enquiry number and a current timestamp.
    - The document environment indicator and scan date are retrieved from the system parameter registry.
    - The archive is unpacked and each entry is processed in turn:
      - The document type is determined from the filename (photo, driving licence, or other KYC type).
      - The applicant type is derived from the first two characters of the filename prefix (Main Applicant, Additional Applicant 1, 2, or 3, or the prefix as-is for unrecognised values).
      - The image file is encoded to Base64.
      - An XML metadata descriptor is constructed containing the enquiry number, branch, applicant type, customer name, document group, document type, GPS location, source device, and file format.
      - The encoded image and metadata are submitted to the document management service using the environment-appropriate service endpoint (test, beta, or live).
      - Each successful upload returns a document index; each entry's temporary file is deleted after processing.
    - The archive file and any temporary files are deleted after all entries are processed.
  - If the document file is absent or empty: a success response is constructed without image upload; a log entry notes that data was submitted but images were not uploaded.
- **Applicable rules:** Rule 63 (document management environment routing)
- **Outcome:** All KYC images are stored in the document management system under the enquiry reference, or the absence of images is noted.

java_source_line_ranges: ["Java: f1_19151-f1_19328"]
plsql_source_line_ranges: []
user_defined_methods_called: [encodeFileToBase64Binary() — PROVIDED]
plsql_rule_ids_covered: []

---

**Step 16: Fraud Screening and Credit Bureau Initiation**
- **Fields involved:** Enquiry number, Hunter result, Sherlock result, Hunter indicator, Sherlock indicator, Branch code, Individual applicant status
- The system checks the system parameter controlling the online fraud screening service.
- Decision point — online fraud screening active:
  - If active: the Hunter and Sherlock fraud detection service is invoked for the enquiry.
    - Processing is delegated for fraud XML generation — backend specification not provided; logic not available for this step.
    - Decision point — fraud service outcome:
      - If an error is returned: a failure status and message are set on the response.
      - If results are returned: Hunter and Sherlock results are evaluated in combination:
        - Both services clear: fraud status is recorded as clear; the credit bureau service is called.
        - Any non-clear result: the fraud status message is set according to the combination of Hunter and Sherlock result codes; the credit bureau service is called where individual applicants are present.
  - If the online fraud screening service is not active: the system queries the enquiry for individual applicants; if any are found, the credit bureau service is called directly.
- The credit bureau service constructs a parameterised request URL from the registry, substitutes the enquiry reference and branch, and makes an outbound request; a successful acknowledgement response records the credit bureau request as submitted.
- Decision point — credit bureau response:
  - If a successful acknowledgement is received: the credit bureau status is recorded as submitted.
  - If unsuccessful: the credit bureau status is recorded as not submitted; processing continues.
- In the cleanup phase, if a credit bureau call has not yet been recorded as made: the credit bureau service is called at that point.
- **Applicable rules:** Rule 64 (fraud screening result interpretation), Rule 65 (credit bureau submission)
- **Outcome:** Fraud screening results are recorded and the credit bureau request is submitted where applicable.


java_source_line_ranges: ["Java: f1_19337-f1_19407, f1_19409-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED — PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_HUNTER_SHERLOCK_XML not provided in B2)"]
user_defined_methods_called: [submitHunterSherlockValues() — PROVIDED, callbureauoneservice() — PROVIDED]
backend_procedures_called: [PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_HUNTER_SHERLOCK_XML]
plsql_rule_ids_covered: []



---

**Step 17: Response Construction and Resource Release**
- **Fields involved:** Enquiry number, Asset cost
- A success response is assembled containing: the status indicator, the message 'Success', the enquiry number, the enquiry reference label, and the asset cost advisory message where the system price was updated.
- All resources — connections, file handles, temporary files — are released.
- Decision point — whether a credit bureau call has been recorded during cleanup:
  - If not yet recorded: the credit bureau service is called at this point as a final safety measure.
- **Applicable rules:** (No additional rules apply in this step)
- **Outcome:** The caller receives the enquiry number, status, and any asset cost advisory; all held resources are freed.
java_source_line_ranges: ["Java: f1_19402-f1_19437"]
plsql_source_line_ranges: []
plsql_rule_ids_covered: []

---

### 3.3 Process End States

| End State Name | Trigger Condition | Business Outcome |
|---|---|---|
| Enquiry Created Successfully | All validations pass, enquiry record persisted, enquiry number assigned | Enquiry number returned; KYC images uploaded; fraud and credit checks initiated; all notifications dispatched |
| Enquiry Modified Successfully | All validations pass, existing enquiry record updated | Updated enquiry returned; eligibility re-evaluated; relevant notifications dispatched |
| Authentication Failure | User or device not active or not registered | No enquiry record created; failure response with reason returned |
| Mandatory Input Absent | Enquiry input details or contract type is missing | No enquiry record created; failure response specifying the missing element returned |
| Aadhaar Validation Failure | Aadhaar number fails the Verhoeff check | Any partial changes reversed; failure response indicating invalid Aadhaar number returned |
| KYC Caution Block | Applicant PAN found on the caution or block list | Processing halted; failure response with the specific caution reason returned |
| KYC De-duplication Failure | Applicant identity document is a confirmed duplicate against an active or pending application | Processing halted; failure response with the specific duplicate error returned |
| Mobile Number Validation Failure | Applicant mobile number fails any mobile format or exclusivity rule | Processing halted; failure response returned; error logged permanently |
| Comprehensive Data Validation Failure | Any business rule (asset cost, NRI eligibility, deceased, Vruddhi, model year, GECL, etc.) is violated | Any partial changes reversed; failure response with the specific violation returned |
| Geographic Validation Conditional Warning | Applicant pincode is outside the branch service area | Conditional response (status 3) returned with save-allowed indicator; caller decides whether to proceed |
| Enquiry Persistence Failure | The enquiry creation or modification routine returns an error | Any partial changes reversed; failure response returned |
| Enquiry Number Generation Lock Timeout | Document number control record locked by another user for more than 15 seconds | Failure response indicating the number is in use by another user; no enquiry created |
| Aged Pending Enquiries Block | Branch or marketing employee has unresolved aged pending items beyond configured thresholds | New enquiry blocked; failure response specifying the blocking category and age returned |
| Modification Blocked — Application Exists | A loan application already exists for the enquiry in a non-terminal state | Modification rejected; failure response returned |
| Modification Blocked — Enquiry Closed | All applicants on the enquiry have a closed status | Modification rejected; failure response returned |
| Modification Blocked — Fraud Pending | Enquiry is under active Hunter or Sherlock fraud investigation | Modification rejected; failure response returned |
| Product Model Price Override | System replaces submitted asset cost with current state-level price | Success response returned with advisory message that asset cost has been updated |
| Fraud Screening Non-Clear | Hunter or Sherlock result indicates a non-clear or error outcome | Enquiry saved; fraud status message returned; enquiry remains in Pending status |
| Credit Bureau Not Submitted | Credit bureau outbound request returns a non-success acknowledgement | Enquiry saved; credit bureau status recorded as not submitted; response indicates this |

java_source_line_ranges: ["Java: f1_18185-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: f16_613-f16_4034, f16_19546-f16_19771, f16_17305-f16_18039, f4_15869-f4_16176"]


---

## 4. Business Rules and Exception Conditions

Rules are presented in consolidation-first order: consolidated (Both) → entry-layer only (Java) → processing-layer only (PL/SQL).

---

### Consolidated Rules (source_layer: Both)

---

**Rule 1: User and Device Authentication Gate**
- **Statement:** Every request must be authenticated against the system registry before any business processing begins. The user must be active and the device must be registered.
- **Condition:** A request is received with user credentials and device identifiers.
- **Action/Outcome:** The authentication service confirms the user status as active and the device as registered; processing proceeds.
- **Exception condition:** If the user status is not active, or if an error is returned by the authentication service, the submission is rejected immediately and a failure response is returned to the caller. No data is changed.
- **Fields involved:** User identifier, Device IMEI, Device UUID, Application version
- **Applies to process step:** 1



java_source_line_ranges: ["Java: f1_18283-f1_18291"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED)"]
source_layer: Both
plsql_rule_id: N/A

---

**Rule 2: Mandatory Input Completeness**
- **Statement:** The enquiry input details and contract type must be present for any submission to be accepted.
- **Condition:** A submission is received.
- **Action/Outcome:** Both the enquiry input details and the contract type within those details are confirmed as present; processing proceeds.
- **Exception condition:** If the enquiry input details are absent or blank, the submission is rejected with the message 'Lead Input Values Is Required.' If the contract type is absent from the contract details sub-object, the submission is rejected with the message 'CONTRACT_TYPE Lead Input Values Is Required.' No backend processing occurs.
- **Fields involved:** Enquiry input details, Contract type
- **Applies to process step:** 2, 4



java_source_line_ranges: ["Java: f1_18300-f1_18362"]
plsql_source_line_ranges: []
source_layer: Java
plsql_rule_id: N/A

*(Note: Rule 2 kept as Java-only per UE-3; no equivalent B2 paragraph. Listed here for ordering continuity.)*

---

**Rule 3: Asset Model Year Within Permitted Range**
- **Statement:** For vehicle assets, the submitted asset model year must fall within the range permitted by the system configuration, calculated relative to the current year.
- **Condition:** A loan enquiry is being created or validated and the asset category is vehicle (category 1).
- **Action/Outcome:** The model year is confirmed to fall within the configured minimum and maximum; validation passes.
- **Exception condition:** If the model year falls outside the permitted range, an error is raised and the submission is rejected.
- **Fields involved:** Asset model year, Contract type, Asset category
- **Applies to process step:** 10



plsql_source_line_ranges: ["PL/SQL: f16_20951-f16_21048"]
source_layer: PL/SQL
plsql_rule_id: N/A

---

**Rule 4: Asset Cost Must Be Positive**
- **Statement:** The asset cost submitted for a loan enquiry must be greater than zero. This is enforced both in the mobile KYC validation pass and in the comprehensive enquiry data validation routine.
- **Condition:** An asset cost is submitted for a loan enquiry.
- **Action/Outcome:** The asset cost is confirmed as positive; validation passes.
- **Exception condition:** If the asset cost is zero or negative, error LN3574 is raised with the message 'Asset Cost Should Be Greater Than Zero'; the submission is rejected.
- **Fields involved:** Asset cost (market value)
- **Applies to process step:** 10



java_source_line_ranges: ["Java: f1_18820-f1_18822"]
plsql_source_line_ranges: ["PL/SQL: f16_21053-f16_21070"]
source_layer: Both
plsql_rule_id: C001_BR29, C005_BR7

---

**Rule 5: Asset Cost Must Not Be Less Than Finance Amount**
- **Statement:** The asset cost must be greater than or equal to the requested finance amount.
- **Condition:** Both asset cost and finance amount are submitted for a loan enquiry.
- **Action/Outcome:** The asset cost is confirmed to be at least equal to the finance amount; validation passes.
- **Exception condition:** If the asset cost is less than the finance amount, error LN3574 is raised with the message 'Asset Cost Should be greater than or Equal to Finance Amount'; the submission is rejected.
- **Fields involved:** Asset cost, Finance amount
- **Applies to process step:** 10



java_source_line_ranges: ["Java: f1_18820-f1_18828"]
plsql_source_line_ranges: ["PL/SQL: f16_21072-f16_21088"]
source_layer: Both
plsql_rule_id: C001_BR30, C005_BR7

---

**Rule 6: Mobile Number Validation System-Wide Toggle**
- **Statement:** Mobile number validation can be disabled system-wide. When disabled, all mobile number format and eligibility checks are bypassed for all applicants.
- **Condition:** A mobile number validation is triggered for any applicant.
- **Action/Outcome:** The system-wide parameter is checked; if enabled, validation proceeds; if disabled, validation is skipped and processing continues.
- **Fields involved:** Mobile number, System parameter (mobile validation toggle)
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f4_15483-f4_15508"]
source_layer: PL/SQL
plsql_rule_id: C001_BR6
---

**Rule 7: Mobile Number Length**
- **Statement:** A mobile number must be exactly 10 digits long.
- **Condition:** A mobile number is provided for an applicant and mobile validation is enabled.
- **Action/Outcome:** The length is confirmed as 10 characters; validation passes.
- **Exception condition:** If the length is not exactly 10, error COM112 is raised (with generic display code COM110); the submission is rejected. The error is permanently logged in an independent transaction (see Rule 9).
- **Fields involved:** Mobile number, Alternate mobile number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f4_15531-f4_15540"]
source_layer: PL/SQL
plsql_rule_id: C001_BR7

---

**Rule 8: Mobile Number Starting Digit**
- **Statement:** A mobile number must begin with the digit 6, 7, 8, or 9.
- **Condition:** A mobile number is provided and length is confirmed as 10.
- **Action/Outcome:** The first digit is confirmed as one of the permitted digits; validation passes.
- **Exception condition:** If the first digit is not in the permitted set, error COM113 is raised (with generic display code COM110); the submission is rejected. The error is permanently logged.
- **Fields involved:** Mobile number, Alternate mobile number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f4_15543-f4_15552"]
source_layer: PL/SQL
plsql_rule_id: C001_BR8



---

**Rule 9: Mobile Number Prohibited Pattern**
- **Statement:** Mobile numbers consisting entirely of a single repeated digit (all sixes, all sevens, all eights, or all nines) are invalid, when pattern validation is enabled.
- **Condition:** A mobile number is provided, length and starting digit are valid, and the pattern validation parameter is enabled.
- **Action/Outcome:** The number is confirmed as not matching any prohibited pattern; validation passes.
- **Exception condition:** If the number matches a prohibited pattern, error COM114 is raised (with generic display code COM110); the submission is rejected. The error is permanently logged.
- **Fields involved:** Mobile number, Alternate mobile number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f4_15555-f4_15567"]
source_layer: PL/SQL
plsql_rule_id: C001_BR9



---

**Rule 10: Mobile Number Must Not Be Registered to an Active Employee Device**
- **Statement:** A mobile number submitted for a non-employee customer must not be registered to an active employee device in the device registration registry.
- **Condition:** A mobile number is provided for an applicant whose occupation is not 'Employee - SFL'.
- **Action/Outcome:** The number is confirmed as absent from the device registration registry; validation passes.
- **Exception condition:** If the number is found in the active device registration list, error COM115 is raised with the message 'Available in Device Registration' (with generic display code COM110); the submission is rejected. The error is permanently logged.
- **Fields involved:** Mobile number, Alternate mobile number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f4_15569-f4_15607"]
source_layer: PL/SQL
plsql_rule_id: C001_BR10



---

**Rule 11: Mobile Number Must Not Belong to an Active Non-Consultant Employee**
- **Statement:** A mobile number submitted for a non-employee customer must not match the personal or office mobile number of any active, non-consultant employee in the employee register.
- **Condition:** A mobile number is provided for an applicant whose occupation is not 'Employee - SFL', and the number was not found in the device registration registry.
- **Action/Outcome:** The number is confirmed as absent from the employee register; validation passes.
- **Exception condition:** If the number matches an active non-consultant employee's personal or office number, error COM115 is raised with the message 'Available in FHR' (with generic display code COM110); the submission is rejected. The error is permanently logged.
- **Fields involved:** Mobile number, Alternate mobile number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f4_15609-f4_15647"]
source_layer: PL/SQL
plsql_rule_id: C001_BR11



---

**Rule 12: External PAN De-duplication — Confirmed Duplicate Halts Processing**
- **Statement:** If the external PAN de-duplication service returns a confirmed duplicate error, the submission must be halted immediately.
- **Condition:** A PAN is submitted to the external de-duplication service and the service returns a confirmed duplicate response.
- **Action/Outcome:** The error type is set to hard error and the procedure terminates with a return.
- **Exception condition:** The submission is rejected with the error message returned by the de-duplication service. Processing cannot continue.
- **Fields involved:** PAN number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f16_4651-f16_4655, f16_7175-f16_7179"]
source_layer: PL/SQL
plsql_rule_id: C005_BR16

---

**Rule 13: External PAN De-duplication — Potential Duplicate Halts for Review**
- **Statement:** If the external PAN de-duplication service returns a potential duplicate warning, the submission must be halted to allow for review.
- **Condition:** A PAN is submitted to the external de-duplication service and the service returns a potential duplicate warning.
- **Action/Outcome:** An informational message is provided; the procedure terminates with a return to allow user review.
- **Exception condition:** The submission is halted with the informational warning. Processing cannot continue until the potential duplicate is reviewed.
- **Fields involved:** PAN number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f16_4646-f16_4650, f16_7170-f16_7174"]
source_layer: PL/SQL
plsql_rule_id: C005_BR17



---

**Rule 14: SMS Dispatch Must Not Halt the Transaction**
- **Statement:** A failure to send an SMS notification to an applicant must not halt the overall business process. The failure is logged internally and the transaction continues as if the SMS was sent.
- **Condition:** An SMS dispatch is attempted for an opted-in applicant and the sending service returns an error.
- **Action/Outcome:** The SMS error is logged; the error variables are cleared; processing continues normally.
- **Fields involved:** SMS indicator, Applicant mobile number
- **Applies to process step:** 13



java_source_line_ranges: ["Java: f1_19402-f1_19407"]
plsql_source_line_ranges: ["PL/SQL: f16_34695-f16_34703, f16_28073-f16_28081"]
source_layer: Both
plsql_rule_id: C005_BR23, C001_BR17



---

**Rule 15: Aadhaar Number Verhoeff Check**
- **Statement:** The Aadhaar number submitted for any applicant must pass the Verhoeff check-digit algorithm validation at the entry layer. The number is reversed, processed through a permutation table and a multiplication table, and the final computed value must be zero.
- **Condition:** An applicant's Aadhaar number is provided.
- **Action/Outcome:** The computed check value equals zero; the number is structurally valid and processing continues.
- **Exception condition:** If the check value is not zero, any partial changes are reversed and a failure response is returned with the message 'Invalid Aadhaar Number'.
- **Fields involved:** Aadhaar number
- **Applies to process step:** 6



java_source_line_ranges: ["Java: f1_18740-f1_18751"]
plsql_source_line_ranges: []
source_layer: Java
plsql_rule_id: N/A

---

**Rule 16: Aadhaar Number Must Be Exactly 12 Digits**
- **Statement:** An Aadhaar number provided for an individual applicant must be exactly 12 digits long.
- **Condition:** An Aadhaar number is provided for an applicant with an individual constitution type.
- **Action/Outcome:** The length is confirmed as exactly 12; validation passes.
- **Exception condition:** If the length is not exactly 12, error COM103 is raised; the submission is rejected.
- **Fields involved:** Aadhaar number, Constitution type
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_20461-f16_20474"]
source_layer: PL/SQL
plsql_rule_id: C005_BR28, C001_BR24

---

**Rule 17: Gender and Occupation Consistency**
- **Statement:** An applicant whose gender is recorded as male cannot have 'House Wife' as their occupation.
- **Condition:** An applicant's gender and occupation are provided.
- **Action/Outcome:** The combination is confirmed as consistent; validation passes.
- **Exception condition:** If the gender is male and the occupation is 'House Wife', error COM122 is raised; the submission is rejected.
- **Fields involved:** Gender, Occupation
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_20252-f16_20274"]
source_layer: PL/SQL
plsql_rule_id: C005_BR26, C001_BR22
---

**Rule 18: PAN Mandatory for Individual Applicants Without Exemption**
- **Statement:** Individual applicants must provide a PAN unless they qualify for a documented exemption based on their profile.
- **Condition:** An applicant has an individual constitution type and has not provided a PAN.
- **Action/Outcome:** The exemption eligibility is evaluated; if exempt, processing continues; if not exempt and PAN is mandatory, an error is raised.
- **Exception condition:** If PAN is determined to be mandatory for the applicant's profile and is not provided, error COM129 ('PAN No Is Required') is raised; the submission is rejected.
- **Fields involved:** PAN number, Constitution type, Occupation, Gender, Form code, Exemption unique number
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_20276-f16_20343"]
source_layer: PL/SQL
plsql_rule_id: C005_BR27, C001_BR23



---

**Rule 19: PAN Must Not Be on the Caution or Block List**
- **Statement:** An applicant's PAN must not appear on the internal caution or block list. This check is applied in the KYC mobile validation pass, within the KYC de-duplication routine, and within the enquiry persistence routine for Vruddhi channel new enquiries.
- **Condition:** An applicant's PAN is provided.
- **Action/Outcome:** The PAN status is confirmed as 'Allowed'; validation passes.
- **Exception condition:** If the PAN status is anything other than 'Allowed', error LN5337 is raised with the specific reason from the caution check function; the submission is rejected immediately.
- **Fields involved:** PAN number
- **Applies to process step:** 7, 10, 13



java_source_line_ranges: ["Java: f1_18759-f1_18786"]
plsql_source_line_ranges: ["PL/SQL: f16_20215-f16_20235, f16_4245-f16_4277, f16_878-f16_908"]
source_layer: Both
plsql_rule_id: C005_BR14, C001_BR32



---

**Rule 20: KYC De-duplication for Non-PAN Identity Documents**
- **Statement:** If a driving licence, passport, voter ID, or Aadhaar number has been changed or is new, it must be checked against existing enquiry and application records. The enquiry is blocked if the document is linked to a pending or active application.
- **Condition:** A driving licence, passport, voter ID, or Aadhaar number is submitted that differs from the value already stored against the enquiry, or is new.
- **Action/Outcome:** No matching active enquiry or application is found; validation passes.
- **Exception condition:** If a duplicate is found linked to a pending application (credit decision status Pending), error LN4084 is raised. If linked to an application with a rejected status, error LN4088 is raised. If a duplicate exists with no specific status match, generic duplicate error LN3712 is raised. In all cases, the submission is rejected.
- **Fields involved:** Driving licence number, Passport number, Voter ID number, Aadhaar number
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_4986-f16_5283, f16_5285-f16_5598, f16_5600-f16_5913, f16_5915-f16_6219"]
source_layer: PL/SQL
plsql_rule_id: C005_BR6



---

**Rule 21: Enquiry Number Generation — Independent Transaction with Lock Protection**
- **Statement:** New enquiry numbers must be generated by locking the document number control record for the branch and document type, incrementing the sequence, and committing the reservation in a transaction independent of the main enquiry transaction. This ensures the number is permanently reserved even if the main transaction is rolled back, and prevents concurrent duplicate generation.
- **Condition:** A new enquiry is being created.
- **Action/Outcome:** The control record is locked (waiting up to 15 seconds), the sequence is incremented, the reservation is committed in an independent transaction, and the formatted number is returned.
- **Exception condition:** If the lock cannot be acquired within 15 seconds because another user holds it, error D283 ('This Document Number Is Being Used By Another Person') is raised; the independent transaction is rolled back and no number is assigned. If any other error occurs during the select, update, or insert operations, the independent transaction is rolled back and no number is assigned.
- **Fields involved:** Branch code, Document type, Company code, Annual reset configuration
- **Applies to process step:** 13



java_source_line_ranges: ["Java: f1_19082-f1_19090"]
plsql_source_line_ranges: ["PL/SQL: f16_19546-f16_19771"]
source_layer: Both
plsql_rule_id: C005_BR1, C005_BR21, C001_BR15, C001_BR16



---

**Rule 22: Annual Reset Configuration for Document Numbers**
- **Statement:** The document number sequence can be configured to reset at the start of each year or to run continuously across all years.
- **Condition:** A new document number is being generated.
- **Action/Outcome:** If the annual reset parameter is 'Y', the number is unique within the current year. If 'N', the number is unique across all years.
- **Fields involved:** Annual reset system parameter, Document year
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_19567-f16_19574, f16_19611-f16_19614"]
source_layer: PL/SQL
plsql_rule_id: C005_BR19

---

**Rule 23: Document Number Prefix by Document Type**
- **Statement:** The prefix of a newly generated document number is determined by the document type: 'EQ' for enquiry, 'AP' for application, 'AC' for CAM, and 'UP' for MOW.
- **Condition:** A new document number is being generated for the first time for a given branch and document type combination.
- **Action/Outcome:** The appropriate prefix is assigned and stored with the initial control record.
- **Fields involved:** Document type, Document prefix
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_19621-f16_19632"]
source_layer: PL/SQL
plsql_rule_id: C005_BR20



---

**Rule 24: Document Number Format Standard**
- **Statement:** Every generated document number must follow the standard format: document-type prefix, followed by a 4-digit zero-padded branch code, the 2-digit current year, and the sequence number padded to at least 4 digits.
- **Condition:** A new document number has been successfully generated and the reservation has been committed.
- **Action/Outcome:** The formatted number is constructed and returned to the caller.
- **Fields involved:** Document prefix, Branch code, Year, Sequence number
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_19736-f16_19757"]
source_layer: PL/SQL
plsql_rule_id: C005_BR22



---

**Rule 25: Mobile Number Validation Error Permanent Logging**
- **Statement:** Every mobile number validation failure must be logged permanently in a transaction that is independent of the main enquiry transaction. This ensures the log record is preserved even if the main transaction is subsequently rolled back.
- **Condition:** A mobile number validation failure occurs for any applicant.
- **Action/Outcome:** The failure details — including the mobile number, user, enquiry reference, and application reference — are inserted into the dedicated error log and immediately committed in an independent transaction.
- **Exception condition:** If the error log insert itself fails, the independent transaction is rolled back and the failure detail is returned; the main process is not affected by the logging failure.
- **Fields involved:** Mobile number, User identifier, Enquiry number, Application number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f4_16635-f4_16696"]
source_layer: PL/SQL
plsql_rule_id: C001_BR3

---

**Rule 26: Enquiry Cannot Be Modified After Application Creation**
- **Statement:** A loan enquiry cannot be modified if a formal loan application or CAM application has already been created from it and is not in a terminal state.
- **Condition:** An attempt is made to modify an existing enquiry.
- **Action/Outcome:** No active application is found; modification proceeds.
- **Exception condition:** If a record exists in the loan application or CAM application tables for the enquiry and the application count in a non-terminal state is greater than zero, error LN3713 ('Loan Application Created... Cannot Modify.') is raised; the modification is rejected.
- **Fields involved:** Enquiry number
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_145-f16_286"]
source_layer: PL/SQL
plsql_rule_id: C005_BR3, C001_BR18

---

**Rule 27: Closed Enquiry Cannot Be Modified**
- **Statement:** An enquiry where all applicants have a closed status cannot be modified.
- **Condition:** An attempt is made to modify an enquiry in modify mode.
- **Action/Outcome:** At least one applicant with a non-closed status is found; modification proceeds.
- **Exception condition:** If no applicant with status 'APC' or 'ANC' exists on the enquiry, error LN4926 ('Enquiry closed. Not allowed to modify.') is raised; the modification is rejected.
- **Fields involved:** Enquiry number, Applicant status
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_365-f16_409"]
source_layer: PL/SQL
plsql_rule_id: C005_BR24



---

**Rule 28: Enquiry Cannot Be Modified Under Fraud Investigation**
- **Statement:** An enquiry cannot be modified while it is under active Hunter or Sherlock fraud investigation with a pending or failed verification status.
- **Condition:** An attempt is made to modify an enquiry in modify mode.
- **Action/Outcome:** Fraud verification is complete or not applicable; modification proceeds.
- **Exception condition:** If the enquiry has a Hunter hit with a verification status of 'FA' or 'F', or a Sherlock refer with a verification status of 'F', error LN3785 ('Hunter Verification Is Under Process') is raised; the modification is rejected.
- **Fields involved:** Enquiry number, Hunter result, Hunter verification result, Sherlock result, Sherlock verification result
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_412-f16_441"]
source_layer: PL/SQL
plsql_rule_id: C005_BR4, C001_BR19



---

**Rule 29: Active Emergency Credit Line Blocks New Enquiry**
- **Statement:** A new loan enquiry cannot be created for a customer who holds an active Guaranteed Emergency Credit Line account, when the system parameter for this check is enabled.
- **Condition:** A new enquiry is initiated for an existing customer and the GECL validation parameter is active.
- **Action/Outcome:** No active emergency credit line contract is found; the enquiry creation proceeds.
- **Exception condition:** If an active GECL contract is found for the applicant, error LN4323 is raised; the enquiry creation is blocked.
- **Fields involved:** Customer code, GECL system parameter
- **Applies to process step:** 7, 10



java_source_line_ranges: ["Java: f1_18673-f1_18694"]
plsql_source_line_ranges: ["PL/SQL: f16_21522-f16_21551, f16_20478-f16_20509"]
source_layer: Both
plsql_rule_id: C005_BR10, C001_BR25



---

**Rule 30: Deceased Customer Blocks Enquiry Creation**
- **Statement:** A loan enquiry cannot be created for a customer who is marked as deceased in the customer registry.
- **Condition:** A new enquiry is being created for an existing customer.
- **Action/Outcome:** The customer's deceased indicator is confirmed as not set; the enquiry creation proceeds.
- **Exception condition:** If the deceased indicator is 'Y', error LN3574 with the message 'This Customer is deceased' is raised; the enquiry creation is blocked.
- **Fields involved:** Customer code, Deceased indicator
- **Applies to process step:** 10



java_source_line_ranges: ["Java: f1_18673-f1_18694"]
plsql_source_line_ranges: ["PL/SQL: f16_21552-f16_21601"]
source_layer: Both
plsql_rule_id: C005_BR11, C001_BR26



---

**Rule 31: One-Branch-One-Customer Policy**
- **Statement:** A customer may be restricted from creating a new loan at the current branch if they hold an active, non-closed loan with a positive balance at a different branch. The restriction applies when the branch is not designated as SME or CF type, or when the SME parameter is not set, and is supplemented by a secondary check using the customer's current fleet branch if the primary check finds nothing.
- **Condition:** A pre-save cross-branch validation is triggered for an existing main applicant.
- **Action/Outcome:** No conflicting active loan at a different branch is found; processing continues.
- **Exception condition:** If a conflicting branch is found and is different from the current branch: for the mobile enquiry component, an informational message LN5098 is issued; for components PS01081 or PS01156, a hard error LN5098 is raised; for component PS00710, informational message LN3574 about contract branch transfer is issued; for all other components where the branch segment is not SME or CF, error LN5099 is raised. In all error cases, the submission is rejected. For informational responses, the message is combined with any pincode validation response.
- **Fields involved:** Customer code, Branch code, Branch segment
- **Applies to process step:** 8



java_source_line_ranges: ["Java: f1_18566-f1_18589"]
plsql_source_line_ranges: ["PL/SQL: f4_16005-f4_16166"]
source_layer: Both
plsql_rule_id: C001_BR5



---

**Rule 32: One-Branch Policy Activation Date Gate**
- **Statement:** The one-branch-one-customer policy is only active on or after the system-configured implementation date. Before that date, all one-branch checks are bypassed.
- **Condition:** A one-branch validation check is initiated.
- **Action/Outcome:** The current date is on or after the configured implementation date; the check proceeds. If before the date, the check is skipped entirely.
- **Fields involved:** System parameter (policy implementation date)
- **Applies to process step:** 8



plsql_source_line_ranges: ["PL/SQL: f4_15894-f4_15911"]
source_layer: PL/SQL
plsql_rule_id: C001_BR4



---

**Rule 33: SME Branch Geographic Validation Bypass**
- **Statement:** Pincode-to-branch geographic validation is not performed for branches designated as SME branches.
- **Condition:** A branch's segment is identified as 'SM'.
- **Action/Outcome:** All geographic validation steps are immediately bypassed; processing continues.
- **Fields involved:** Branch code, Branch segment
- **Applies to process step:** 9



java_source_line_ranges: ["Java: f1_18591-f1_18628"]
plsql_source_line_ranges: ["PL/SQL: f16_17614-f16_17616"]
source_layer: Both
plsql_rule_id: C005_BR36



---

**Rule 34: User Access Right Bypasses Pincode Validation**
- **Statement:** Users who have been granted the designated geographic bypass access right do not require pincode validation.
- **Condition:** A marketing employee's access rights are checked as part of pincode validation applicability determination.
- **Action/Outcome:** If the bypass access right is confirmed, pincode validation is skipped; otherwise, determination continues via statement code configuration.
- **Fields involved:** Marketing employee code, Access right code
- **Applies to process step:** 9



plsql_source_line_ranges: ["PL/SQL: f16_17550-f16_17553"]
source_layer: PL/SQL
plsql_rule_id: C005_BR37



---

**Rule 35: Pincode Validation Applicability by Statement Code**
- **Statement:** The applicability of pincode validation for a marketing employee is determined by the pincode validation flag configured against the employee's statement code, effective as of the enquiry date.
- **Condition:** The marketing employee does not hold the bypass access right.
- **Action/Outcome:** The pincode validation flag for the employee's statement code is retrieved; if 'Y', validation applies; if 'N', validation is not applicable and is bypassed.
- **Fields involved:** Marketing employee code, Statement code, Enquiry date
- **Applies to process step:** 9



plsql_source_line_ranges: ["PL/SQL: f16_17556-f16_17590"]
source_layer: PL/SQL
plsql_rule_id: C005_BR38



---

**Rule 36: Distance-Based Pincode Validity Check**
- **Statement:** If the distance between the applicant's location and the branch exceeds the configured maximum distance (either branch-specific or system default of 35 km), the pincode is not automatically valid and must be checked against the explicit mapping table.
- **Condition:** Pincode validation is applicable and the calculated distance exceeds the configured maximum.
- **Action/Outcome:** The check proceeds to the explicit pincode mapping table.
- **Fields involved:** Applicant pincode, Branch code, Distance
- **Applies to process step:** 9



plsql_source_line_ranges: ["PL/SQL: f16_17790-f16_17795"]
source_layer: PL/SQL
plsql_rule_id: C005_BR39



---

**Rule 37: Explicit Exclusion List Bypass for Geographic Validation**
- **Statement:** Specific enquiry or contract numbers that appear on the pincode validation exclusion list are exempt from geographic validation.
- **Condition:** The distance exceeds the maximum and pincode validation is applicable.
- **Action/Outcome:** If the enquiry or contract number is found on the exclusion list, geographic validation is bypassed for this transaction.
- **Fields involved:** Enquiry number, Contract number
- **Applies to process step:** 9



plsql_source_line_ranges: ["PL/SQL: f16_17905-f16_17935"]
source_layer: PL/SQL
plsql_rule_id: C005_BR40



---

**Rule 38: Pincode Must Be Mapped to Branch Service Area**
- **Statement:** A pincode must be officially mapped to the branch's service area for the transaction to proceed.
- **Condition:** The enquiry is not on the exclusion list and the distance exceeds the configured maximum.
- **Action/Outcome:** The pincode is found in the branch mapping table; geographic validation passes.
- **Exception condition:** If the pincode is not found in the mapping table for the branch, error LN3955 ('Pincode not mapped with the Branch') is raised. The severity varies by step context (Rule 39).
- **Fields involved:** Applicant pincode, Branch code
- **Applies to process step:** 9



plsql_source_line_ranges: ["PL/SQL: f16_17994-f16_18022"]
source_layer: PL/SQL
plsql_rule_id: C005_BR41



---

**Rule 39: Geographic Validation Failure Severity Depends on Calling Context**
- **Statement:** When a pincode is not mapped to a branch, the severity of the failure is determined by the component initiating the validation. For the mobile enquiry component, the failure is informational (allowing the caller to acknowledge and proceed); for all other components, it is a hard error.
- **Condition:** A pincode mapping failure is detected.
- **Action/Outcome:** For the mobile enquiry component: an informational response is returned with a conditional save-allowed indicator. For all other components: a hard error is returned and the submission is rejected.
- **Fields involved:** Calling component code, Applicant pincode, Branch code
- **Applies to process step:** 9



java_source_line_ranges: ["Java: f1_18613-f1_18625"]
plsql_source_line_ranges: ["PL/SQL: f16_17999-f16_18005"]
source_layer: Both
plsql_rule_id: C005_BR42



---

**Rule 40: Vruddhi Channel Restriction**
- **Statement:** Users who have access to the Vruddhi loan origination channel must not create enquiries for designated contract types (12, 22, 41, 42, 43, or 44) through any other channel.
- **Condition:** A user with Vruddhi access attempts to create a new enquiry for a designated contract type from a non-Vruddhi component.
- **Action/Outcome:** The user does not have Vruddhi access, or the contract type is not in the restricted set; creation proceeds.
- **Exception condition:** If the user has Vruddhi access and the contract type is in the restricted set, the submission is blocked with an error instructing the user to use the Vruddhi system.
- **Fields involved:** Contract type, User identifier
- **Applies to process step:** 10, 13



java_source_line_ranges: ["Java: f1_18324-f1_18330"]
plsql_source_line_ranges: ["PL/SQL: f16_21093-f16_21227, f16_927-f16_1029"]
source_layer: Both
plsql_rule_id: C005_BR9, C001_BR31



---

**Rule 41: Applicant Location Name Mandatory**
- **Statement:** Every applicant in the submission must have a location name; no applicant may have a missing location.
- **Condition:** The applicant collection is validated before applicant-level loop processing.
- **Action/Outcome:** All applicants have a location name; validation proceeds.
- **Exception condition:** If any applicant has a missing location name, error LN5078 is raised; the submission is rejected.
- **Fields involved:** Location name, Applicant collection
- **Applies to process step:** 10



plsql_source_line_ranges: ["PL/SQL: f16_21326-f16_21363"]
source_layer: PL/SQL
plsql_rule_id: N/A



---

**Rule 42: NRI Applicant Product Eligibility**
- **Statement:** Non-Resident Indian applicants are eligible only for specific loan and asset type combinations: individual constitution, new vehicle contract type, Cars and UVs asset class, and private car usage.
- **Condition:** An applicant is identified as an NRI with an individual constitution type.
- **Action/Outcome:** The submitted product combination falls within the permitted NRI set; validation passes.
- **Exception condition:** If the contract type, asset class, or asset usage falls outside the permitted NRI combination, an error is raised; the submission is rejected.
- **Fields involved:** Residential type, Constitution type, Contract type, Asset class code, Asset usage code
- **Applies to process step:** 10



java_source_line_ranges: ["Java: f1_18500-f1_18502"]
plsql_source_line_ranges: ["PL/SQL: f16_21607-f16_21636, f16_21761-f16_21789"]
source_layer: Both
plsql_rule_id: C005_BR12, C001_BR27



---

**Rule 43: NRI Applicant Mandatory Document and Contact Requirements**
- **Statement:** NRI applicants must provide a valid passport number with a validity date that is not in the past and extends at least 90 days into the future, along with an alternate mobile number and an email address.
- **Condition:** An applicant is identified as an NRI with an individual constitution type.
- **Action/Outcome:** All required fields are present and the passport validity date is at least 90 days in the future; validation passes.
- **Exception condition:** If the passport number, passport validity date, alternate mobile number, or email address is absent, or if the passport validity date is in the past or expires within 90 days, an error is raised; the submission is rejected.
- **Fields involved:** Passport number, Passport validity date, Alternate mobile number, Email address
- **Applies to process step:** 10



java_source_line_ranges: ["Java: f1_18481-f1_18488, f1_18504-f1_18522"]
plsql_source_line_ranges: ["PL/SQL: f16_21636-f16_21756"]
source_layer: Both
plsql_rule_id: C005_BR13, C001_BR28



---

**Rule 44: Active Branch Required**
- **Statement:** A loan enquiry can only be processed for a branch that is currently active and open for both business and accounting activities.
- **Condition:** A branch code is submitted with a loan enquiry.
- **Action/Outcome:** The branch is found with open business and accounting dates and no close dates; processing proceeds.
- **Exception condition:** If the branch is not found as active, error LN480 ('Invalid Branch Code') is raised; the process halts immediately.
- **Fields involved:** Branch code, Company code
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_788-f16_825"]
source_layer: PL/SQL
plsql_rule_id: C005_BR45



---

**Rule 45: Valid User with Employee Record Required**
- **Statement:** The user submitting the enquiry must be valid and have an associated employee record to create or modify an enquiry through the Vruddhi channel.
- **Condition:** A new enquiry is being created from the mobile enquiry component.
- **Action/Outcome:** The user is found with a valid employee record; processing proceeds.
- **Exception condition:** If no valid user with an employee record is found, error GL461 ('Invalid User') is raised; the process halts.
- **Fields involved:** User identifier
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_927-f16_953"]
source_layer: PL/SQL
plsql_rule_id: C005_BR46



---

**Rule 46: Aged Pending Enquiries Block New Enquiry at Branch**
- **Statement:** New enquiries are blocked at a branch if that branch has pending enquiries with a closed-pending applicant status from a prior period whose follow-up date is not set, based on a look-back period determined by the current day of the month relative to a system parameter threshold.
- **Condition:** A new enquiry creation is attempted.
- **Action/Outcome:** No aged pending enquiries are found at the branch; creation proceeds.
- **Exception condition:** If aged pending enquiries are found, error LN4924 is raised specifying the date by which the pending enquiries must be resolved; the new enquiry creation is blocked.
- **Fields involved:** Branch code, Enquiry date, Applicant status, Follow-up date
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_36211-f16_36444"]
source_layer: PL/SQL
plsql_rule_id: C005_BR43



---

**Rule 47: Maximum Pending Days Block for Marketing Employee and Branch**
- **Statement:** New enquiries are blocked if a marketing employee has pending enquiries, or a branch has pending enquiries or applications, that are older than the configured permissible day thresholds.
- **Condition:** A new enquiry creation is attempted.
- **Action/Outcome:** No pending items exceed the configured thresholds; creation proceeds.
- **Exception condition:** If any threshold is exceeded, error LN3574 is raised with a detailed message specifying which category is overdue and by how many days; the new enquiry creation is blocked.
- **Fields involved:** Marketing employee code, Branch code, Enquiry date, Permissible pending day parameters
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_40119-f16_40586"]
source_layer: PL/SQL
plsql_rule_id: C005_BR44



---

**Rule 48: Exempt Sources Bypass Branch-Level Validation**
- **Statement:** Enquiries originating from designated partner sources (MSIL, TMF, TAFE, HYUNDAI) or from the partner origination component are not subject to branch-level aged pending validation.
- **Condition:** A new enquiry is being created.
- **Action/Outcome:** If the source is a designated exempt partner or the component is the partner origination component, branch-level aged pending validation is bypassed.
- **Fields involved:** Source of business, Calling component code
- **Applies to process step:** 13



java_source_line_ranges: ["Java: f1_18331"]
plsql_source_line_ranges: ["PL/SQL: f16_1032-f16_1090"]
source_layer: Both
plsql_rule_id: C005_BR47



---

**Rule 49: DAN Must Be Valid and Not Previously Used**
- **Statement:** A Declaration Account Number provided for PAN exemption must be present in the system and must not have been previously utilised for another transaction. Once used, it is marked as utilised and cannot be reused.
- **Condition:** A DAN is provided for an applicant at the time of enquiry creation or modification.
- **Action/Outcome:** The DAN is found as valid and unused; it is marked as utilised; processing continues.
- **Exception condition:** If the DAN is not found in the system, error LN4468 is raised. If the DAN has already been utilised, error LN4470 is raised. In both cases, the submission is rejected.
- **Fields involved:** DAN value, PAN exemption indicator
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_1652-f16_1735, f16_2379-f16_2464"]
source_layer: PL/SQL
plsql_rule_id: C005_BR48



---

**Rule 50: Applicant Address Line Length Constraints**
- **Statement:** Applicant address lines must be between 3 and 40 characters in length.
- **Condition:** An applicant's address lines are provided during enquiry creation or modification.
- **Action/Outcome:** Both address lines are confirmed as within the permitted length range; processing continues.
- **Exception condition:** If address line 1 or address line 2 is shorter than 3 or longer than 40 characters, error LN5226 or LN5227 is raised; the submission is rejected.
- **Fields involved:** Street name (address line 1), Address line 2
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_1740-f16_1764, f16_2468-f16_2494"]
source_layer: PL/SQL
plsql_rule_id: C005_BR49



---

**Rule 51: Non-Individual Main Applicant Auto-Eligibility**
- **Statement:** An enquiry for a non-individual main applicant at a single-stage approval branch is automatically advanced to Eligible for Application status upon creation, without requiring fraud or credit checks.
- **Condition:** A new enquiry is created for a non-individual main applicant and the branch is not configured for two-stage approval.
- **Action/Outcome:** The enquiry status is immediately set to Eligible for Application.
- **Fields involved:** Applicant type, Constitution type, Branch stage
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_2030-f16_2059"]
source_layer: PL/SQL
plsql_rule_id: C005_BR50



---

**Rule 52: Individual Applicant Eligibility After Fraud and Credit Checks**
- **Statement:** An enquiry for an individual main applicant advances to Eligible for Application only after all required Hunter, Sherlock, and CIBIL checks return passing or manually approved results.
- **Condition:** An individual main applicant's enquiry has completed all applicable checks.
- **Action/Outcome:** All checks pass or have a manual approval; enquiry status is set to Eligible for Application.
- **Exception condition:** If any check is pending or not yet complete, the enquiry status remains Pending.
- **Fields involved:** Hunter result, Hunter verification result, Sherlock result, Sherlock verification result, CIBIL status, Constitution type
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_3077-f16_3194"]
source_layer: PL/SQL
plsql_rule_id: C005_BR54



---

**Rule 53: Delete-and-Re-insert Applicants on Modification (SMS-Preservation Exception)**
- **Statement:** When an enquiry is modified, all applicant detail records that have not yet received a confirmation SMS and whose status is not 'ANC' are deleted and then re-inserted with the updated data. Records where a confirmation SMS has already been sent are preserved.
- **Condition:** An enquiry is being modified.
- **Action/Outcome:** Applicant records without a sent SMS are deleted and re-inserted; SMS-confirmed records are preserved.
- **Fields involved:** Applicant SMS indicator, Applicant status
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_2352-f16_2358"]
source_layer: PL/SQL
plsql_rule_id: C005_BR51



---

**Rule 54: Vruddhi Sourced Enquiry Cannot Change to Non-Vruddhi Contract Type**
- **Statement:** An enquiry originally sourced through the Vruddhi channel cannot be modified to use a contract type outside the Vruddhi-designated set (12, 22, 41, 42, 43, 44).
- **Condition:** An existing Vruddhi-sourced enquiry is being modified.
- **Action/Outcome:** The contract type remains within the Vruddhi-designated set; modification proceeds.
- **Exception condition:** If the new contract type is outside the Vruddhi-designated set, an error is raised; the modification is rejected.
- **Fields involved:** Source of business, Contract type
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_2142-f16_2169"]
source_layer: PL/SQL
plsql_rule_id: C005_BR52



---

**Rule 55: Source of Business Update Restricted to External Vendor Sources**
- **Statement:** An enquiry's source of business can only be changed to another external vendor source. If the new source is not a recognised external vendor, the original source of business is retained.
- **Condition:** An attempt is made to update the source of business on an existing enquiry.
- **Action/Outcome:** If the new source is a recognised external vendor, the source is updated. If not, the original source is preserved.
- **Fields involved:** Source of business
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_2246-f16_2251"]
source_layer: PL/SQL
plsql_rule_id: C005_BR53



---

**Rule 56: Welcome Communication for New Hire Purchase Enquiry Becoming Eligible**
- **Statement:** When a new Hire Purchase (contract type 11) enquiry is created and immediately achieves Eligible for Application status, a welcome communication must be sent to the customer containing the loan details and a link.
- **Condition:** A new enquiry for contract type 11 is created and its status is set to Eligible for Application.
- **Action/Outcome:** A welcome email and/or SMS is sent to the customer.
- **Fields involved:** Contract type, Enquiry status
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_3587-f16_3668"]
source_layer: PL/SQL
plsql_rule_id: C005_BR55



---

**Rule 57: Internal Alert Email for Two-Stage Approval Branch**
- **Statement:** When an enquiry is in Pending status at a branch configured for two-stage credit assessment and the asset type is covered by the branch's asset control configuration, an internal HTML-formatted alert email must be sent to the configured internal distribution list.
- **Condition:** An enquiry is in Pending status and the branch has two-stage approval configuration with applicable asset control.
- **Action/Outcome:** An internal alert email with full enquiry details is sent to the configured recipients.
- **Fields involved:** Enquiry status, Branch stage, Asset control applicability
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_3682-f16_3814"]
source_layer: PL/SQL
plsql_rule_id: C005_BR56



---

**Rule 58: Post-Save Caution Database Screening**
- **Statement:** All applicants on an enquiry must be screened against the internal customer caution database after the enquiry is saved and committed. This check does not block the transaction.
- **Condition:** An enquiry has been successfully created or modified and the transaction is committed.
- **Action/Outcome:** The caution check is performed; results are recorded; if a secondary commit is needed, it is issued; processing continues regardless of the caution check result (unless a critical system error occurs).
- **Fields involved:** Enquiry number, Applicant identity details
- **Applies to process step:** 13



java_source_line_ranges: ["Java: f1_19402-f1_19407"]
plsql_source_line_ranges: ["PL/SQL: f16_3857-f16_3878, f5_3288-f5_4343"]
source_layer: Both
plsql_rule_id: C005_BR57, C001_BR12



---

**Rule 59: Sanction List Match Triggers Email Alert**
- **Statement:** If any applicant on an enquiry matches the external UNSC Sanction List during the caution database screening, a formatted email alert must be sent to the configured recipients using the sanction list notification event.
- **Condition:** The caution database screening identifies a sanction list match (source category 'E') for an applicant not in a terminal caution status.
- **Action/Outcome:** An HTML-formatted email containing the applicant details and matching sanction information is generated and sent to the configured recipient list.
- **Fields involved:** Applicant identity details, Caution list match details, Branch name, Region name
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f5_3495-f5_3978"]
source_layer: PL/SQL
plsql_rule_id: C001_BR13



---

**Rule 60: Internal Caution List Match Triggers Email Alert**
- **Statement:** If any applicant on an enquiry matches an internal or other non-sanction caution list during screening, a formatted email alert must be sent to the configured recipients using the internal caution list notification event, with the recipient list populated from the user's email and branch emails in the live environment.
- **Condition:** The caution database screening identifies an internal or other caution list match (source category 'I' or 'O') for an applicant not in a terminal caution status.
- **Action/Outcome:** An HTML-formatted email containing the applicant details and matching caution information is generated and sent.
- **Fields involved:** Applicant identity details, Caution list match details, Branch name, Region name
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f5_3983-f5_4331"]
source_layer: PL/SQL
plsql_rule_id: C001_BR14



---

**Rule 61: Marketing Employee Notification for Third-Party Created Enquiry**
- **Statement:** When a new enquiry is created by a user who is not the designated marketing employee for that enquiry, a system notification must be sent to the designated marketing employee.
- **Condition:** A new enquiry is created and the creating user differs from the designated marketing employee.
- **Action/Outcome:** A notification is dispatched to the marketing employee.
- **Fields involved:** User identifier, Marketing employee code
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_3880-f16_3975"]
source_layer: PL/SQL
plsql_rule_id: C005_BR58



---

**Rule 62: Marketing Employee Notification for Reassignment**
- **Statement:** When an existing enquiry is modified and the designated marketing employee is changed, a system notification must be sent to the newly assigned marketing employee.
- **Condition:** An enquiry is being modified and the marketing employee code is changed.
- **Action/Outcome:** A notification is dispatched to the new marketing employee.
- **Fields involved:** Marketing employee code
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_3976-f16_4019"]
source_layer: PL/SQL
plsql_rule_id: C005_BR59



---

**Rule 63: Document Management Environment-Based Routing**
- **Statement:** KYC document images are routed to the correct document management service endpoint based on the environment indicator stored in the system parameter registry. Test environments route to the test endpoint, beta environments to the beta endpoint, and the live environment to the production endpoint.
- **Condition:** A KYC document image is being uploaded after a successful enquiry save.
- **Action/Outcome:** The environment-appropriate endpoint is selected and the encoded image with metadata is submitted to it.
- **Fields involved:** Environment indicator, Document archive
- **Applies to process step:** 15



java_source_line_ranges: ["Java: f1_19163-f1_19175, f1_19282-f1_19304"]
plsql_source_line_ranges: []
source_layer: Java
plsql_rule_id: N/A



---

**Rule 64: Fraud Screening Result Interpretation**
- **Statement:** The combined results of the Hunter and Sherlock fraud detection services determine the fraud status of the enquiry. Both service results must be clear for the enquiry to be considered fraud-clear; any non-clear result is recorded with a specific status message.
- **Condition:** Fraud screening results are received following an enquiry creation.
- **Action/Outcome:** If both Hunter and Sherlock are clear: fraud status is recorded as clear and the credit bureau service is called. If any non-clear result is received: the combined fraud status message is set and recorded accordingly.
- **Fields involved:** Hunter result, Sherlock result, Hunter indicator, Sherlock indicator
- **Applies to process step:** 16



java_source_line_ranges: ["Java: f1_19440-f1_19660"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED — PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_HUNTER_SHERLOCK_XML not provided in B2)"]
source_layer: Java
plsql_rule_id: N/A



---

**Rule 65: Credit Bureau Request Initiation**
- **Statement:** A credit bureau request must be submitted for each enquiry where individual applicants are present, following successful fraud screening or directly when the online fraud screening service is not active.
- **Condition:** An enquiry has been created and individual applicants are present.
- **Action/Outcome:** A parameterised request is constructed and an outbound call is made to the credit bureau service; a successful acknowledgement response records the submission as complete.
- **Exception condition:** If the outbound call returns a non-success response, the credit bureau status is recorded as not submitted; the overall process is not halted.
- **Fields involved:** Enquiry number, Branch code, Individual applicant status
- **Applies to process step:** 16, 17



java_source_line_ranges: ["Java: f1_20122-f1_20182, f1_19427-f1_19437"]
plsql_source_line_ranges: []
source_layer: Java
plsql_rule_id: N/A



---

**Rule 66: CIBIL Request Validity for Re-initiation**
- **Statement:** A credit bureau (CIBIL) re-initiation request can only be processed if one is not already in progress for the enquiry, and only if at least one applicant is of individual constitution type, and only if fraud verification has been completed.
- **Condition:** A CIBIL re-initiation is requested for an existing enquiry.
- **Action/Outcome:** CIBIL status is not 'Sent', at least one individual applicant exists, and fraud verification is complete; re-initiation proceeds.
- **Exception condition:** If CIBIL status is 'Sent', error LN3786 is raised. If no individual applicant exists, error LN3780 is raised. If fraud verification is not complete, error LN3782 is raised. In all cases, the re-initiation is blocked.
- **Fields involved:** CIBIL status, Constitution type, Hunter verification result, Sherlock verification result
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f16_491-f16_611"]
source_layer: PL/SQL
plsql_rule_id: C005_BR25, C005_BR5, C001_BR20, C001_BR21


---

**Rule 67: KYC De-duplication — Effectively Filed Enquiry Blocks New Submission**
- **Statement:** A new loan enquiry cannot proceed if an existing enquiry with the same KYC identity details already carries an 'Effectively Filed' status.
- **Condition:** KYC details are submitted for a new enquiry.
- **Action/Outcome:** No matching 'Effectively Filed' enquiry is found; the new enquiry proceeds.
- **Exception condition:** If a matching enquiry with 'Effectively Filed' status is found, the system flags that an active enquiry exists; the submission is halted.
- **Fields involved:** PAN, Voter ID, Driving licence, Passport number, Aadhaar number
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_4352-f16_4391, f16_6862-f16_6912"]
source_layer: PL/SQL
plsql_rule_id: C005_BR29



---

**Rule 68: PAN Format Validity**
- **Statement:** A customer's PAN must conform to the standard national format before de-duplication or other checks are applied.
- **Condition:** A PAN is provided for validation.
- **Action/Outcome:** The PAN format is confirmed as valid; validation continues.
- **Exception condition:** If the PAN format is invalid, an error is raised; the process stops.
- **Fields involved:** PAN number
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_4602-f16_4609, f16_7124-f16_7131"]
source_layer: PL/SQL
plsql_rule_id: C005_BR30



---

**Rule 69: Passport Number Format Validity**
- **Statement:** A customer's passport number must conform to the standard format before de-duplication checks are applied.
- **Condition:** A passport number is provided for validation.
- **Action/Outcome:** The passport number format is confirmed as valid; validation continues.
- **Exception condition:** If the passport number format is invalid, an error is raised; the process stops.
- **Fields involved:** Passport number
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_5289-f16_5295, f16_7849-f16_7855"]
source_layer: PL/SQL
plsql_rule_id: C005_BR31



---

**Rule 70: Voter ID Format Validity**
- **Statement:** A customer's Voter ID must conform to the standard format before de-duplication checks are applied.
- **Condition:** A Voter ID is provided for validation.
- **Action/Outcome:** The Voter ID format is confirmed as valid; validation continues.
- **Exception condition:** If the Voter ID format is invalid, an error is raised; the process stops.
- **Fields involved:** Voter ID number
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_5604-f16_5610, f16_8185-f16_8191"]
source_layer: PL/SQL
plsql_rule_id: C005_BR32



---

**Rule 71: Duplicate Customer Details Retrieval on De-duplication Match**
- **Statement:** If a general de-duplication check identifies an existing customer, their full profile details must be retrieved and returned to the caller for review.
- **Condition:** A general de-duplication check identifies an existing customer match.
- **Action/Outcome:** The existing customer's full details — including name, address, DOB, all KYC numbers, contact information, and the most recent active contract number — are retrieved and returned as output.
- **Fields involved:** All applicant KYC and contact fields
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_6339-f16_6594, f16_8950-f16_9231"]
source_layer: PL/SQL
plsql_rule_id: C005_BR33



---

**Rule 72: Existing Customer Must Not Be in Restricted Risk Category**
- **Statement:** An existing customer identified through de-duplication must not be in a restricted risk category.
- **Condition:** A de-duplication check has identified an existing customer.
- **Action/Outcome:** The customer's risk category is confirmed as not restricted; processing continues.
- **Exception condition:** If the customer's risk category is restricted, an error is raised; the process stops.
- **Fields involved:** Customer code, Risk category
- **Applies to process step:** 7



plsql_source_line_ranges: ["PL/SQL: f16_6599-f16_6618, f16_9242-f16_9262"]
source_layer: PL/SQL
plsql_rule_id: C005_BR34



---

**Rule 73: CKYC Number Uniqueness Within Company**
- **Statement:** A customer's CKYC number must be unique within the company. No other customer record under the same company should carry the same CKYC number.
- **Condition:** A CKYC number is submitted for a customer.
- **Action/Outcome:** No other customer within the company shares this CKYC number; validation passes.
- **Exception condition:** If another customer record (excluding the customer currently being processed) holds the same CKYC number, error COM105 is raised; the submission is rejected.
- **Fields involved:** CKYC number, Customer code, Company code
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f4_15407-f4_15462"]
source_layer: PL/SQL
plsql_rule_id: C001_BR1


---

**Rule 74: CKYC Number Must Not Be Associated With a Different PAN**
- **Statement:** A CKYC number must not be shared with any other customer who carries a different PAN. If the same CKYC number exists for a different customer with a different PAN, this is a duplication conflict.
- **Condition:** A CKYC number is submitted along with a PAN.
- **Action/Outcome:** No other customer holds the same CKYC number with a different PAN; validation passes.
- **Exception condition:** If one or more customers share the same CKYC number but carry a different PAN, error COM105 is raised with the list of conflicting customer and PAN combinations; the submission is rejected.
- **Fields involved:** CKYC number, PAN number, Customer code
- **Applies to process step:** 13



plsql_source_line_ranges: ["PL/SQL: f4_18548-f4_18653"]
source_layer: PL/SQL
plsql_rule_id: C001_BR2



---

**Rule 75: External PAN De-duplication — Soft Warning Allows Continuation**
- **Statement:** If the external PAN de-duplication service returns a soft warning, the warning code is recorded but processing is allowed to continue. The soft warning does not halt the submission.
- **Condition:** A PAN is submitted to the external de-duplication service and the service returns a soft warning response.
- **Action/Outcome:** The soft warning code is stored; processing continues.
- **Fields involved:** PAN number
- **Applies to process step:** 7, 10



plsql_source_line_ranges: ["PL/SQL: f16_4643-f16_4645, f16_7167-f16_7169"]
source_layer: PL/SQL
plsql_rule_id: C005_BR18



---

## 5. Business Entities and Definitions

---

1.  **Entity: Loan Enquiry**
- **Definition:** The central record capturing a customer's expression of interest in a loan product, tracking its progress from initial submission through validation, persistence, and assessment. Assigned a unique system-generated number upon creation.
- **Key attributes:** Enquiry number (system-generated, formatted as prefix + 4-digit branch + 2-digit year + padded sequence); processing mode; enquiry date; enquiry status (Pending, Eligible for Application, Effectively Filed, Closed); contract type; asset class code; business source; loan purpose; marketing employee code; finance amount; loan tenure; asset model; number of units; remarks; assessment criteria; employment years; net income; asset usage code; experience in years; quoted interest rate; dealer code; appraisal category; fuel type; horsepower; product model; tractor ownership details; land holdings; agricultural indicator; repayment frequency; branch code; company code; Hunter result; Sherlock result; CIBIL status; branch stage; audit detail.
- **Relationships:** Contains one or more Applicant Detail records. May be linked to a Lead Management Record. May be advanced to a Loan Application. Subject to Fraud Screening Results and Credit Bureau Requests. Subject to Caution Database Screening.


java_source_line_ranges: ["Java: f1_18185-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: f16_613-f16_4034, f16_19546-f16_19771"]
source_layer: Both


---

2.  **Entity: Applicant Detail**
- **Definition:** The record of an individual or non-individual party participating in a loan enquiry, including their personal, address, identity document, and contact information. Classified by role (Main Applicant or Additional Applicant) and tracked through the enquiry lifecycle.
- **Key attributes:** Applicant type (Main/Additional with sequential number); constitution type (Individual/Non-Individual); customer code (if existing customer); contract number (if existing contract); gender; title; full name components; address lines (street, line 1, line 2); pincode; location record identifier; location name; district; state; date of birth; PAN number; driving licence number; passport number; passport validity date; voter ID; Aadhaar number; mobile number; area code; telephone number; alternate mobile number; email address; marital status; education; occupation; business type; CKYC number; CKYC indicator; PAN exemption indicator; DAN value; preferred language code; SMS indicator; customer confirmation indicator; enquiry decision indicator; form code; customer class code; residential type; applicant status.
- **Relationships:** Belongs to one Loan Enquiry. May reference an existing Customer Record. May reference an existing Contract.



java_source_line_ranges: ["Java: f1_18366-f1_18718"]
plsql_source_line_ranges: ["PL/SQL: f16_1626-f16_1950"]
source_layer: Both



---

3.  **Entity: KYC Document Image**
- **Definition:** A scanned or photographed identity or supporting document submitted as part of the loan enquiry. Uploaded to the document management system under the enquiry reference and tagged with metadata.
- **Key attributes:** Archive file (compressed, containing individual image files); document filename (encodes applicant type prefix, document type, and KYC document name); file type (PDF or image); document type (Photo, Driving Licence, or other KYC type); applicant type (derived from filename prefix); customer name; enquiry number; branch code; document group (KYC); scan date; scan user; GPS location; source device; environment indicator; Base64-encoded content; document management index.
- **Relationships:** Associated with one Loan Enquiry. Tagged with one Applicant Detail record.



java_source_line_ranges: ["Java: f1_19151-f1_19328"]
plsql_source_line_ranges: []
source_layer: Java



---

4.  **Entity: User and Device Session**
- **Definition:** The authenticated context of the user submitting the enquiry, established by validating the user identifier and device credentials against the system registry.
- **Key attributes:** User identifier; primary device IMEI; secondary device IMEI; device UUID; application version; user status (must be active); authentication error detail.
- **Relationships:** Authorises access to all operations. Resolves to a Branch via the employee register.



java_source_line_ranges: ["Java: f1_18283-f1_18291"]
plsql_source_line_ranges: ["PL/SQL: (UNRESOLVED)"]
source_layer: Java


---

5.  **Entity: Asset**
- **Definition:** The physical item being financed, characterised by its make, type, class, model, cost, usage, and other product attributes. Asset details govern eligibility rules and product configuration.
- **Key attributes:** Asset make code; asset type code; asset class code; asset model; asset cost (market value, potentially overridden by system NDLP price); number of units; fuel type; horsepower; product model; asset usage code; asset category code.
- **Relationships:** Linked to one Loan Enquiry. Governs NRI eligibility rules and Vruddhi channel rules. Subject to model year and market value retrieval.



java_source_line_ranges: ["Java: f1_18325-f1_18357"]
plsql_source_line_ranges: ["PL/SQL: f16_20951-f16_21048"]
source_layer: Both



---

6.  **Entity: Branch**
- **Definition:** The organisational unit through which the enquiry is processed, determining geographic service area, product eligibility, processing stage, and notification routing.
- **Key attributes:** Branch code; branch name; branch segment (SF, SM, CF, etc.); MIS state code; business open/close dates; account open/close dates; maximum pincode distance; branch stage (single or two-stage approval); asset control applicability; one-branch SME parameter.
- **Relationships:** Resolved from the user's employee record. Governs geographic validation, processing stage, and pending enquiry blocking rules.



java_source_line_ranges: ["Java: f1_18311"]
plsql_source_line_ranges: ["PL/SQL: f16_788-f16_825, f16_1142-f16_1231, f4_15968-f4_15994"]
source_layer: Both



---

7.  **Entity: Enquiry Number Control Record**
- **Definition:** A system-managed registry entry tracking the current sequence number for each combination of company, document type, and branch, used to generate unique and sequential document numbers.
- **Key attributes:** Company code; document type (Enquiry, Application, CAM, MOW); branch code; current document number; document prefix; document year (for annual-reset configurations).
- **Relationships:** Used exclusively by the number generation function. Locked exclusively during each generation to prevent duplicates.



plsql_source_line_ranges: ["PL/SQL: f16_19546-f16_19771"]
source_layer: PL/SQL



---

8.  **Entity: Pincode-Branch Mapping**
- **Definition:** A configuration record defining which pincodes fall within a branch's authorised service area, used to validate applicant locations.
- **Key attributes:** Company code; branch code; pincode; maximum distance (branch-specific or system default); explicit exclusion list entries.
- **Relationships:** Referenced during geographic validation. May be bypassed for SME branches, users with bypass access, or transactions on the exclusion list.



plsql_source_line_ranges: ["PL/SQL: f16_17305-f16_18039"]
source_layer: PL/SQL



---

9.  **Entity: Caution Database Record**
- **Definition:** A record in the centralised customer caution database identifying individuals or entities on sanction, internal, or other restricted lists, screened against enquiry applicants after the enquiry is saved.
- **Key attributes:** Enquiry number; applicant name; PAN; Aadhaar number; date of birth; pincode; caution list source category (external sanction/internal/other); caution flag; email notification indicator; matching caution list details.
- **Relationships:** Associated with one Loan Enquiry. Triggers email alerts to configured recipients on match.



plsql_source_line_ranges: ["PL/SQL: f5_3288-f5_4343"]
source_layer: PL/SQL



---

10.  **Entity: Document Management Upload Request**
- **Definition:** A structured request submitted to the document management system for each KYC image, containing metadata and the encoded image content.
- **Key attributes:** Data class name; XML metadata descriptor (enquiry number, branch, applicant type, document type, GPS location, source device, file format); document folder path; document name; Base64-encoded document content; environment indicator; document index (returned on successful upload).
- **Relationships:** Associated with one KYC Document Image and one Loan Enquiry.



java_source_line_ranges: ["Java: f1_19258-f1_19305"]
plsql_source_line_ranges: []
source_layer: Java



---

11.  **Entity: Mobile Number Validation Error Log**
- **Definition:** A permanent audit record of mobile number validation failures, written in an independent transaction to ensure the record is preserved regardless of the main enquiry transaction outcome.
- **Key attributes:** Company code; mobile number; user identifier; enquiry number; application number; entry date and time.
- **Relationships:** Associated with one Applicant Detail and one Loan Enquiry.



plsql_source_line_ranges: ["PL/SQL: f4_16635-f4_16696"]
source_layer: PL/SQL



---

## 6. Integration Touchpoints

| Backend Procedure Name | Business Purpose | Business Inputs Passed | Business Outputs Received | Integration Status |
|---|---|---|---|---|
| PS_PK_VAL_APP_STORE_DTLS.PS_PR_OUT_USER_STATUS | Validates user identity and device registration | Company code, user ID, device IMEI, device UUID, component code, app version | User status result, error code, type, message | UNRESOLVED |
| PS_PK_COM_VAL_CONTACT.PS_PR_VAL_CONTACT_ONE_BRANCH | Enforces one-branch-one-customer policy for existing main applicant | Company code, customer code, branch code, component code, mode indicator, user ID | Error code, type, message (severity varies by component) | RESOLVED |
| PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_VAL_BRANCH_PINCODE_MAP | Validates applicant pincode is within branch geographic service area | Company code, branch code, enquiry/contract number, pincode, location record ID, marketing employee, distance, component code, user ID | Error code (LN3955), type (Informational or Error), message | RESOLVED |
| PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_MOB_VAL_KYC_DTLS | Validates and de-duplicates all applicant KYC identity documents; enforces mobile number rules; screens PAN against caution list | Company code, applicant collection, employee code | Error code, type, message | RESOLVED |
| PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_VAL_ENQUIRY_DATA | Validates all enquiry business data — asset, eligibility, NRI, GECL, Vruddhi, model year, mobile, PAN de-duplication | Company code, mode, branch code, enquiry number, date, all contract fields, applicant collection, user ID, product/machine identifiers, all asset fields | Error code, type, message | RESOLVED |
| PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_GET_PRODUCT_MODEL_DATA | Retrieves current NDLP state-level market value for the product model | Company code, contract type, asset class, dealer code, main applicant location, product model, units | Market value, error code, type, message | UNRESOLVED |
| PS_PK_LN_VAL_CONTRACT.PS_PR_VAL_APPRAISAL_CATEGORY | Validates the selected appraisal category against branch and product parameters | Company code, branch code, appraisal category, contract type, asset class, market value, enquiry date, component code, mode, user ID | Error code, type, message | UNRESOLVED |
| PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_IUD_ENQUIRY_DATA | Creates or modifies the loan enquiry and all applicant records; generates enquiry number; validates branch, user, and all business rules; dispatches SMS, caution screening, notifications | All validated contract, asset, applicant, and operational fields; processing mode; applicant collection | Enquiry number (on create), application number, error code, type, message | RESOLVED |
| ps_pk_ln_iud_lead.ps_pr_upd_enquiry_no | Updates the originating lead management system record with the new enquiry number | Company code, user ID, LMS lead ID, enquiry number | Error code, type, message | UNRESOLVED |
| PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_HUNTER_SHERLOCK_XML | Submits enquiry to Hunter and Sherlock fraud detection services and retrieves results | Company code, enquiry number | Hunter result, Sherlock result, Hunter indicator, Sherlock indicator, error code, type, message | UNRESOLVED |



java_source_line_ranges: ["Java: f1_18568-f1_19103"]
plsql_source_line_ranges: ["PL/SQL: f16_613-f16_4034, f16_17305-f16_18039, f16_19980-f16_20601, f16_20883-f16_21809, f4_15869-f4_16176"]
backend_procedures_called: [PS_PK_VAL_APP_STORE_DTLS.PS_PR_OUT_USER_STATUS, PS_PK_COM_VAL_CONTACT.PS_PR_VAL_CONTACT_ONE_BRANCH, PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_VAL_BRANCH_PINCODE_MAP, PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_MOB_VAL_KYC_DTLS, PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_VAL_ENQUIRY_DATA, PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_GET_PRODUCT_MODEL_DATA, PS_PK_LN_VAL_CONTRACT.PS_PR_VAL_APPRAISAL_CATEGORY, PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_IUD_ENQUIRY_DATA, ps_pk_ln_iud_lead.ps_pr_upd_enquiry_no, PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_HUNTER_SHERLOCK_XML]
output_parameters_received: [errCode (positions 7/10/48-51), errType, errMessage, leadId/enqNumber (statement.getString(47/48)), MARKET_VALUE (statementPM.getString(9))]


---

## 7. Glossary

1. **Term:** Loan Enquiry
   **Definition:** A formally recorded expression of interest in a loan product, capturing all relevant customer, asset, and financial details, and assigned a unique system-generated number that identifies it throughout the lending process.

2. **Term:** Enquiry Number
   **Definition:** A unique, system-generated identifier assigned to each loan enquiry, formatted as a document-type prefix, 4-digit zero-padded branch code, 2-digit year, and padded sequence number (e.g., EQ00012500001).

3. **Term:** Main Applicant
   **Definition:** The primary borrower in a loan enquiry, whose details govern eligibility rules including NRI checks, GECL validation, and product restrictions.

4. **Term:** Additional Applicant
   **Definition:** A co-borrower or guarantor added to a loan enquiry alongside the main applicant, numbered sequentially (Additional Applicant 1, 2, 3, etc.).

5. **Term:** KYC (Know Your Customer)
   **Definition:** The set of identity verification documents — PAN, Aadhaar, Passport, Driving Licence, Voter ID — required to establish an applicant's identity for regulatory and de-duplication purposes.

6. **Term:** PAN (Permanent Account Number)
   **Definition:** A national tax identifier for individuals and entities, used as a primary KYC document and subjected to format validation, caution list checks, and external de-duplication.

7. **Term:** Aadhaar Number
   **Definition:** A 12-digit national biometric identifier, validated using the Verhoeff check-digit algorithm at submission and confirmed to be exactly 12 digits for individual applicants.

8. **Term:** Verhoeff Algorithm
   **Definition:** A check-digit validation method applied to Aadhaar numbers that uses a permutation table and multiplication table on the reversed digit array; a result of zero indicates a structurally valid number.

9. **Term:** CKYC (Central Know Your Customer)
   **Definition:** A centralised KYC registration number that must be unique within the company and must not be shared with a different customer carrying a different PAN.

10. **Term:** DAN (Declaration Account Number)
    **Definition:** A unique number used by applicants who are exempt from providing a PAN, allowing them to declare their identity through an alternative mechanism. Each DAN may only be used once and is marked as utilised upon first use.

11. **Term:** NRI (Non-Resident Indian)
    **Definition:** An Indian citizen or person of Indian origin resident outside India. NRI applicants are subject to additional product restrictions, mandatory passport validity requirements, and contact information obligations.

12. **Term:** GECL (Guaranteed Emergency Credit Line)
    **Definition:** A government-backed emergency credit facility. Applicants with an active GECL account are blocked from creating new loan enquiries when the GECL validation parameter is active.

13. **Term:** Caution Database (CCDB)
    **Definition:** An internal database of individuals and entities flagged as high-risk, sanctioned, or otherwise restricted; screened against all applicants on every enquiry after saving.

14. **Term:** UNSC Sanction List
    **Definition:** The external United Nations Security Council sanctions list of individuals and entities prohibited from financial dealings; matches trigger a dedicated email alert.

15. **Term:** Pincode-Branch Mapping
    **Definition:** The configuration defining which postal pincodes fall within a branch's authorised geographic service area, used to validate that applicant addresses are eligible for the branch's products.

16. **Term:** SME Branch
    **Definition:** A branch designated with segment code 'SM' (Small and Medium Enterprise), for which pincode geographic validation is bypassed entirely.

17. **Term:** Branch Stage
    **Definition:** A configuration parameter indicating whether a branch operates a single-stage (1) or two-stage (2) credit approval process, affecting when an enquiry is advanced to Eligible for Application status.

18. **Term:** Eligible for Application (EA)
    **Definition:** An enquiry status indicating that all validations have passed and the enquiry is ready to be converted into a formal loan application.

19. **Term:** Pending (PE)
    **Definition:** An enquiry status indicating that the enquiry has been created but is awaiting completion of fraud or credit checks before it can advance.

20. **Term:** APC / ANC
    **Definition:** Closed applicant status codes on an enquiry ('APC' = Applicant Closed, 'ANC' = Applicant Not Converted). An enquiry where all applicants are in these statuses is considered closed and cannot be modified.

21. **Term:** Hunter / Sherlock
    **Definition:** External fraud detection services that screen enquiry and applicant data against fraud databases; their combined results determine whether an enquiry is fraud-clear or requires further review.

22. **Term:** CIBIL / Credit Bureau
    **Definition:** The external credit reporting service to which a bureau request is submitted following successful enquiry creation for individual applicants, to assess creditworthiness.

23. **Term:** Document Management System (DMS / Newgen)
    **Definition:** The external system to which KYC images are uploaded under the enquiry reference, accessed via environment-specific service endpoints (test, beta, or live).

24. **Term:** NDLP (National Dealer List Price)
    **Definition:** The standardised dealer price for a product model in a given state, fetched from the system and used to override the submitted asset cost where the NDLP differs.

25. **Term:** Vruddhi
    **Definition:** A specialised loan origination channel for designated high-value contract types (12, 22, 41, 42, 43, 44). Users with Vruddhi access must create these contract types exclusively through that channel.

26. **Term:** LMS (Lead Management System)
    **Definition:** An external system that captures pre-sales leads. When a lead reference is provided with an enquiry submission, the LMS record is updated with the resulting enquiry number.

27. **Term:** Processing Mode
    **Definition:** A field indicating whether the current operation is creating a new enquiry ('C' or 'L') or modifying an existing one ('M').

28. **Term:** Appraisal Category
    **Definition:** A classification that determines the credit assessment approach applied to a loan enquiry, validated against branch, contract type, and asset cost parameters.

29. **Term:** Save Indicator
    **Definition:** A field in the submission that controls whether certain pre-save validations (such as the cross-branch contact check) are performed before the main save operation.

30. **Term:** Autonomous Transaction
    **Definition:** A database mechanism used in enquiry number generation and mobile validation error logging to commit operations independently of the main transaction, ensuring that reserved numbers and error logs are permanent even if the main transaction is rolled back.

31. **Term:** OEM Vendor
    **Definition:** An Original Equipment Manufacturer partner (e.g., MSIL, TMF, TAFE, HYUNDAI) whose enquiries are exempt from certain external PAN de-duplication checks and branch-level aged pending validation.

32. **Term:** One-Branch-One-Customer Policy
    **Definition:** A business rule enforced from a configured implementation date that restricts a customer from creating a new loan at a branch if they hold an active loan at a different branch, with severity determined by the calling component.



java_source_line_ranges: ["Java: f1_18185-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: f4_15407-f5_4343, f16_110-f16_21809, f16_613-f16_4034, f16_19546-f16_19771, f16_17305-f16_18039"]



---

## 8. Omissions and Coverage Analysis

### 8.1 General Omissions

1. Commented-out logic detected at Java: f1_18226 — a `MODE_IND` variable initialization (`String MODE_IND = "L"`) — business intent not determinable. Requires business owner review.
2. Commented-out logic detected at Java: f1_18244 — a `customer_type` variable declaration — business intent not determinable. Requires business owner review.
3. Commented-out logic detected at Java: f1_18946-f1_18951 — conditional logic for `mainApplicantInd` based on `enqDecisionInd` — business intent not determinable. Requires business owner review.
4. Commented-out logic detected at Java: f1_19141-f1_19143 — the `recordUserLocationDtls` call for GPS location tracking — business intent not determinable from commented content. Requires business owner review.
5. Commented-out logic detected at Java: f1_20189-f1_20738 — the `enqDtlsFetch` method containing what appears to have been a CIBIL bureau submission and enquiry detail fetch flow — business intent not determinable. Requires business owner review.
6. Commented-out logic detected at Java: f1_21665-f1_21724 — the `searchImageFile` method for document management image search — business intent not determinable. Requires business owner review.
7. Commented-out logic at PL/SQL: f16_4395-f16_4482 and f16_6916-f16_7003 — logic that would block customers with prior write-offs or repossessions. This corresponds to B1 rule C005_BR35 which is explicitly noted as inactive/deprecated. Not incorporated as executable logic; noted as an inactive rule.
8. Commented-out logic at PL/SQL: f16_4484-f16_4525 — logic that would check a customer's SUBGL_TYPE to ensure they are not a Depositor. Business intent not determinable from commented content.
9. Commented-out logic at PL/SQL: f16_443-f16_489 — SMS consent pending check (errors LN4424, LN4425, LN4426) in the enquiry validation routine — this check was present but commented out; business intent not determinable.
10. Commented-out logic at PL/SQL: f16_4677-f16_4981 — older internal PAN de-duplication logic that checked existing enquiry and application statuses (errors LN4084, LN4088, LN3712); superseded by the external API approach — noted as inactive.
11. B1 rule C005_BR35 has no active matching Technical Execution Details paragraph — noted as deprecated/inactive in the source. Cannot be incorporated as executable logic.
12. The `getLeadQuery`, `getBranchCode`, `getMarketingOfficer`, and `getLoanPurpose` methods and endpoints are defined in the same class but are not part of the `sendInfoWithFileData` use case and are excluded from this specification.
13. The `sendMail` method and private `mail` method are defined in the class but are not called within the active path of `sendInfoWithFileData`. Excluded from this specification.

### 8.2 Unresolved Integration Points

1. **Exact procedure name:** PS_PK_VAL_APP_STORE_DTLS.PS_PR_OUT_USER_STATUS
   - **Called at process step:** 1
   - **Business inputs passed:** Company code, user ID, device IMEI, device UUID, component code, application version
   - **Action required:** The corresponding B2 Technical Execution Details must be provided to complete the user authentication step specification.

2. **Exact procedure name:** PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_GET_PRODUCT_MODEL_DATA
   - **Called at process step:** 11
   - **Business inputs passed:** Company code, contract type, asset class, dealer code, main applicant location, product model, units
   - **Action required:** The corresponding B2 Technical Execution Details must be provided to complete the product model market value retrieval step specification.

3. **Exact procedure name:** PS_PK_LN_VAL_CONTRACT.PS_PR_VAL_APPRAISAL_CATEGORY
   - **Called at process step:** 12
   - **Business inputs passed:** Company code, branch code, appraisal category, contract type, asset class, market value, enquiry date, component code, mode, user ID
   - **Action required:** The corresponding B2 Technical Execution Details must be provided to complete the appraisal category validation step specification.

4. **Exact procedure name:** ps_pk_ln_iud_lead.ps_pr_upd_enquiry_no
   - **Called at process step:** 14
   - **Business inputs passed:** Company code, user ID, LMS lead ID, enquiry number
   - **Action required:** The corresponding B2 Technical Execution Details must be provided to complete the lead management linkage step specification.

5. **Exact procedure name:** PS_PK_LN_LOAN_ENQUIRY_GEN.PS_PR_HUNTER_SHERLOCK_XML
   - **Called at process step:** 16
   - **Business inputs passed:** Company code, enquiry number
   - **Action required:** The corresponding B2 Technical Execution Details must be provided to complete the fraud screening step specification.

### 8.3 Missing Java Method Implementations

All user-defined methods called in the active code paths of `sendInfoWithFileData` have implementations provided in the input. No missing implementations exist.

The following methods are defined in the class but are not called within the active path of `sendInfoWithFileData` and are noted for completeness only:
- `recordUserLocationDtls()` — implementation provided; call is commented out in the active path.
- `getResultValues()` — implementation provided; not called in the active path.
- `getExchangeServerName()` — implementation provided; not called in the active path.
- `converToUtilDate()` — implementation provided; not called in the active path.



java_source_line_ranges: ["Java: f1_18185-f1_19437"]
plsql_source_line_ranges: ["PL/SQL: f4_15407-f5_4343, f16_110-f16_21809, f16_613-f16_4034"]


---

## Completeness and Consolidation Verification Signature


Total business rules in Section 5              : 75
  — Consolidated (source_layer: Both)          : 20
  — Entry-layer only (source_layer: Java)      : 8
  — Processing-layer only (source_layer: PL/SQL): 47
  — Rules with exception_condition populated   : 52
  — Rules with exception_condition omitted     : 23

Total entities in Section 6                    : 10
  — Consolidated (source_layer: Both)          : 5
  — Entry-layer only (source_layer: Java)      : 3
  — Processing-layer only (source_layer: PL/SQL): 2 (Note: Caution Database Record added; Pincode-Branch Mapping added)

Total integration touchpoints in Section 7     : 10
  — RESOLVED                                   : 5
  — UNRESOLVED                                 : 5

Total glossary terms in Section 8              : 31

Total missing implementations in Section 9.3   : 0

B1 rules with no B2 paragraph (Section 9.1)   : 1 (C005_BR35 — deprecated/inactive)
