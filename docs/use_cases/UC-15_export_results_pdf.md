# Use Case: Export Results as PDF

## Overview

**Use Case ID:** UC-15
**Use Case Name:** Export Results as PDF
**Primary Actor:** Form Owner, Shared User
**Goal:** Export the feedback results of a form as a downloadable PDF document
**Status:** Implemented

## Preconditions

- User is authenticated
- User has access to the form (owner or shared)
- User is viewing the results page (UC-06)

## Main Success Scenario

1. User clicks "Export PDF" button on the results page.
2. System generates a PDF document containing:
   - Form title and speaker name
   - Export date
   - Total number of responses
   - For each RATING question: question text, rating distribution chart, average rating, and number of ratings
   - For each TEXT question: question text and all non-empty text answers
3. System downloads the PDF file to the user's device.
4. System displays a success notification.

## Alternative Flows

### A1: No Responses Available

**Trigger:** No feedback has been submitted yet (step 2)
**Flow:**

1. System generates a PDF with form details and a "No responses yet" message.
2. Use case continues at step 3.

### A2: PDF Generation Fails

**Trigger:** Error during PDF generation (step 2)
**Flow:**

1. System displays an error notification.
2. Use case ends.

## Postconditions

### Success Postconditions

- PDF file is downloaded to the user's device
- No changes to the system data

### Failure Postconditions

- No file is downloaded
- Error notification is displayed to the user

## Business Rules

### BR-032: PDF File Name

The PDF file name follows the pattern: `{form-title}_results.pdf` with special characters replaced by underscores.

### BR-033: PDF Content Matches View

The PDF content must match the data displayed in the results view (UC-06), including rating distribution charts.

### BR-034: Access Control

Only users with access to view results (form owner or shared user) can export the PDF.
