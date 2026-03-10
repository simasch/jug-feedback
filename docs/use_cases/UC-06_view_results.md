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
   - Rating distribution chart: horizontal bars showing count and percentage per rating value (5 to 1, highest first)
   - Average rating (2 decimal places) and total number of ratings, or "N/A" if no ratings
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

### BR-014: Rating Distribution Chart

For each rating question, the system displays a horizontal bar chart with one bar per rating value (1–5), ordered from 5 (highest) to 1 (lowest). Each bar shows the count and percentage of responses. Bar width is proportional to the count relative to the maximum count across all rating values for that question. The chart is rendered using pure CSS/HTML (no external charting library).
