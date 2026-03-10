# Use Case: Delete Form

## Overview

**Use Case ID:** UC-11
**Use Case Name:** Delete Form
**Primary Actor:** Form Owner
**Goal:** Permanently delete a closed form
**Status:** Implemented

## Preconditions

- User is authenticated
- Form is in CLOSED status
- User is the owner of the form

## Main Success Scenario

1. User clicks "Delete" in the dashboard
2. System deletes the form with all dependent data (cascade)
3. Dashboard is updated

## Postconditions

### Success Postconditions

- Form and all associated data deleted (questions, answers, feedback responses, shares)

### Failure Postconditions

- Form remains unchanged

## Business Rules

### BR-021: Only Closed Forms

Deletion is only possible for closed forms

### BR-022: Irreversible

Deletion cannot be undone

### BR-023: Cascading Delete

Cascading deletion of all dependent data: questions, answers, feedback responses, shares
