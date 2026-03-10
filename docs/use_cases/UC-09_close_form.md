# Use Case: Close Form

## Overview

**Use Case ID:** UC-09
**Use Case Name:** Close Form
**Primary Actor:** Form Owner
**Goal:** Close a public form so that no further feedback can be submitted
**Status:** Implemented

## Preconditions

- User is authenticated
- Form is in PUBLIC status
- User is the owner of the form

## Main Success Scenario

1. User clicks "Close" in the dashboard
2. System changes form status from PUBLIC to CLOSED
3. Dashboard is updated

## Postconditions

### Success Postconditions

- Form status is CLOSED
- No new feedback submissions possible
- Existing results are preserved

### Failure Postconditions

- Form status remains PUBLIC
