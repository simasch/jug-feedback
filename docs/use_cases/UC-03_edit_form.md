# Use Case: Edit Form

## Overview

**Use Case ID:** UC-03
**Use Case Name:** Edit Form
**Primary Actor:** Form Owner
**Goal:** Edit the details and questions of a draft form
**Status:** Implemented

## Preconditions

- User is authenticated
- Form is in DRAFT status
- User is the owner of the form

## Main Success Scenario

1. User clicks "Edit" in the dashboard
2. System checks access permissions
3. System displays FormEditorView with form details and question list
4. User edits title, speaker, date, and/or location
5. User adds questions (see A2)
6. User clicks "Save"
7. System saves changes and displays a success message

## Alternative Flows

### A1: No Access

**Trigger:** User is not the owner of the form
**Flow:**

1. System redirects to the dashboard

### A2: Add Question

**Trigger:** User wants to add a new question to the form
**Flow:**

1. User enters question text
2. User selects question type:
   - **RATING** — scale 1-5
   - **TEXT** — free text (textarea)
3. User clicks "Add"
4. System assigns the next orderIndex
5. Question appears in the question list

## Postconditions

### Success Postconditions

- Form details and/or questions updated

### Failure Postconditions

- Form remains unchanged
- User is redirected to the dashboard
