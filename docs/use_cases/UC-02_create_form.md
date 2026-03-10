# Use Case: Create Feedback Form

## Overview

**Use Case ID:** UC-02
**Use Case Name:** Create Feedback Form
**Primary Actor:** Authenticated User
**Goal:** Create a new feedback form based on a template with predefined questions
**Status:** Implemented

## Preconditions

- User is authenticated

## Main Success Scenario

1. User clicks "Create New" in the dashboard
2. System displays a dialog with input fields
3. User enters the title (required field)
4. User optionally enters speaker, date, and location
5. User confirms
6. System creates the form in DRAFT status with a unique publicToken (UUID)
7. System adds 13 template questions (9 rating, 4 text)
8. Dashboard is updated and a success message is displayed

## Alternative Flows

### A1: Title Not Filled In

**Trigger:** User leaves the required title field empty
**Flow:**

1. Validation prevents creation
2. User must enter a title

## Postconditions

### Success Postconditions

- New form created in DRAFT status
- 13 template questions (9 rating, 4 text) assigned
- User is the owner of the form

### Failure Postconditions

- No form is created
- Dashboard remains unchanged

## Business Rules

### BR-005: Template Questions

Each new form receives 9 rating questions (RATING, scale 1-5) and 4 text questions (TEXT, free text) as predefined German-language questions about presentations

### BR-006: Unique Public Token

Each form receives a unique publicToken (UUID) for public access
