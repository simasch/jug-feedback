# Use Case: Reopen Form

## Overview

**Use Case ID:** UC-10
**Use Case Name:** Reopen Form
**Primary Actor:** Form Owner
**Goal:** Reopen a closed form so that feedback can be submitted again
**Status:** Implemented

## Preconditions

- User is authenticated
- Form is in CLOSED status
- User is the owner of the form

## Main Success Scenario

1. User clicks "Reopen" in the dashboard
2. System changes form status from CLOSED to PUBLIC
3. Dashboard is updated

## Postconditions

### Success Postconditions

- Form status is PUBLIC
- Anonymous users can submit feedback again

### Failure Postconditions

- Form status remains CLOSED
