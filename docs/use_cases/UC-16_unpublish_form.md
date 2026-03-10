# Use Case: Unpublish Form

## Overview

**Use Case ID:** UC-16
**Use Case Name:** Unpublish Form
**Primary Actor:** Form Owner
**Goal:** Move a published form back to draft status so it can be edited, as long as no feedback has been submitted
**Status:** Planned

## Preconditions

- User is authenticated
- Form is in PUBLIC status
- User is the owner of the form
- No feedback responses have been submitted for this form

## Main Success Scenario

1. User clicks "Unpublish" in the dashboard
2. System verifies that the form has no feedback responses
3. System changes form status from PUBLIC to DRAFT
4. Dashboard is updated

## Alternative Flows

### A1: Form has existing responses

- **Trigger:** The form already has one or more feedback responses
- The "Unpublish" action is not available (button is hidden or disabled)

## Postconditions

### Success Postconditions

- Form status is DRAFT
- Form is no longer accessible via the public link
- Form can be edited again

### Failure Postconditions

- Form status remains PUBLIC

## Business Rules

### BR-035: Unpublish only without responses

A published form can only be moved back to DRAFT if it has zero feedback responses. Once responses exist, the form cannot be unpublished.
