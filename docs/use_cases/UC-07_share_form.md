# Use Case: Share Form

## Overview

**Use Case ID:** UC-07
**Use Case Name:** Share Form
**Primary Actor:** Form Owner
**Goal:** Share a form with other users
**Status:** Implemented

## Preconditions

- User is authenticated
- User is the owner of the form

## Main Success Scenario

1. User clicks "Share" in the dashboard
2. System displays a dialog with current shares
3. User enters an email address
4. User clicks "Add"
5. System validates the email and creates a FormShare
6. Share list is updated

## Alternative Flows

### A1: Invalid Email

**Trigger:** Email format is invalid
**Flow:**

1. System displays an error message

### A2: Email Is Form Owner

**Trigger:** User enters their own email address
**Flow:**

1. System displays an error message (cannot share with yourself)

### A3: Email Already Shared

**Trigger:** A share for this email already exists
**Flow:**

1. System prevents duplicate

### A4: Remove Share

**Trigger:** User wants to revoke an existing share
**Flow:**

1. User clicks "Remove" next to a share
2. System deletes the FormShare
3. Share list is updated

## Postconditions

### Success Postconditions

- Shared user sees the form in their dashboard
- Shared user can view results and QR code

### Failure Postconditions

- No share is created
- Existing shares remain unchanged

## Business Rules

### BR-014: Only Owner Can Share

Only the owner can share a form

### BR-015: No Re-Sharing

Shared users cannot share further

### BR-016: Email Validation

Email format is validated

### BR-017: No Duplicate Shares

Unique constraint on (form_id, shared_with_email)
