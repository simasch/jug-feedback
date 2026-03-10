# Use Case: Submit Feedback

## Overview

**Use Case ID:** UC-05
**Use Case Name:** Submit Feedback
**Primary Actor:** Anonymous User
**Goal:** Submit feedback via a public form
**Status:** Implemented

## Preconditions

- User has the form link or QR code
- Form exists and is in PUBLIC status

## Main Success Scenario

1. User opens `/form/{publicToken}` (via link or QR code)
2. System loads the form using the token
3. System displays the form title, speaker, date, and location
4. For each question:
   - RATING: RadioButtonGroup with options 1-5 (localized labels)
   - TEXT: TextArea with placeholder
5. User fills out the form
6. User clicks "Submit"
7. System creates FeedbackResponse with FeedbackAnswers
8. System displays a thank-you page

## Alternative Flows

### A1: Form Not Found

**Trigger:** publicToken does not exist in the database
**Flow:**

1. System displays "Form not found" message

### A2: Form Not Public

**Trigger:** Form is not in PUBLIC status
**Flow:**

1. System displays "Form not available" message

## Postconditions

### Success Postconditions

- New FeedbackResponse with FeedbackAnswers saved
- Thank-you page is displayed

### Failure Postconditions

- No FeedbackResponse is created
- Error message is displayed

## Business Rules

### BR-007: Rating Scale

Ratings are 1-5 (Very poor to Very good)

### BR-008: Optional Text Answers

Text answers are optional (only non-empty ones are saved)

### BR-009: New Response Per Submission

Each submission creates a new FeedbackResponse

### BR-010: No Authentication

No authentication required for submitting feedback
