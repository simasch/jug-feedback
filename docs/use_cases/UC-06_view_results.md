# Use Case: View Results

## Overview

**Use Case ID:** UC-06
**Use Case Name:** View Results
**Primary Actor:** Form Owner, Shared User
**Goal:** View the feedback results of a form
**Status:** Implemented

## Preconditions

- User is authenticated
- User has access to the form (owner or shared)

## Main Success Scenario

1. User clicks "Results" in the dashboard
2. System checks access permissions
3. System displays form title, speaker, and number of responses
4. For each RATING question:
   - Question text with number
   - Average rating (2 decimal places) or "N/A"
5. For each TEXT question:
   - Question text with number
   - List of all non-empty text answers as a bulleted list

## Alternative Flows

### A1: No Access

**Trigger:** User does not have permission for the form
**Flow:**

1. System redirects to the dashboard

### A2: No Responses Available

**Trigger:** No feedback has been submitted yet
**Flow:**

1. System displays "No responses yet" message

## Postconditions

### Success Postconditions

- No changes to the system

### Failure Postconditions

- No changes to the system
- User is redirected to the dashboard

## Business Rules

### BR-011: Status-Independent Results

Results can be viewed regardless of form status

### BR-012: Average Calculation

Average calculation only includes non-null ratings

### BR-013: Empty Text Answers

Empty text answers are not displayed
