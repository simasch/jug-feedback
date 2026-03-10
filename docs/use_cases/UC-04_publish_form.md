# Use Case: Publish Form

## Overview

**Use Case ID:** UC-04
**Use Case Name:** Publish Form
**Primary Actor:** Form Owner
**Goal:** Publish a draft form so that it becomes publicly accessible
**Status:** Implemented

## Preconditions

- User is authenticated
- Form is in DRAFT status
- User is the owner of the form

## Main Success Scenario

1. User clicks "Publish" in the dashboard
2. System changes form status from DRAFT to PUBLIC
3. Dashboard is updated

## Postconditions

### Success Postconditions

- Form status is PUBLIC
- Form is accessible via the public link
- Anonymous users can submit feedback

### Failure Postconditions

- Form status remains DRAFT
