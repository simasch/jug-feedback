# Use Case: Login

## Overview

**Use Case ID:** UC-01
**Use Case Name:** Login
**Primary Actor:** Anonymous User
**Goal:** Log in using an email-based login code
**Status:** Implemented

## Preconditions

- User is not authenticated

## Main Success Scenario

1. User navigates to `/login`
2. User enters email address
3. System deletes existing tokens for this email
4. System generates an 8-digit login code
5. System stores AccessToken with a 10-minute expiration time
6. System sends the code via email
7. User enters the received code
8. System validates the code (not used, not expired)
9. System creates an authenticated session
10. User is redirected to the dashboard

## Alternative Flows

### A1: Invalid Code

**Trigger:** User enters an incorrect code
**Flow:**

1. System displays an error message
2. User can re-enter the code

### A2: Expired Code

**Trigger:** Code is older than 10 minutes
**Flow:**

1. System displays an error message
2. User can request a new code

## Postconditions

### Success Postconditions

- User is authenticated with ROLE_USER
- Session is created

### Failure Postconditions

- User remains unauthenticated
- No new token is consumed

## Business Rules

### BR-001: Code Format

Code is 8 digits, numeric

### BR-002: Code Expiration Time

Code expires after 10 minutes

### BR-003: Single Use

Code can only be used once

### BR-004: Token Cleanup

Previous tokens are deleted when requesting a new code
