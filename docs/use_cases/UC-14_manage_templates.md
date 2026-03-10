# Use Case: Manage Templates

## Overview

**Use Case ID:** UC-14
**Use Case Name:** Manage Templates
**Primary Actor:** Template Owner
**Goal:** View, rename, and delete owned templates
**Status:** Planned

## Preconditions

- User is authenticated

## Main Success Scenario

1. User navigates to the templates view at `/templates`
2. System displays a list of templates owned by the current user
3. User performs management actions (rename or delete)

## Alternative Flows

### 3a. User renames a template

1. User clicks "Rename" on a template
2. System displays a dialog with the current name
3. User enters a new name and confirms
4. System updates the template name

### 3a-3a. User clears the name

1. System shows validation error: template name is required
2. User enters a valid name
3. Flow continues at step 3a.4

### 3b. User deletes a template

1. User clicks "Delete" on a template
2. System deletes the template and its questions
3. Template list is updated

### 3c. User has no templates

1. System displays an empty state message

## Postconditions

### Success Postconditions

- Template list reflects the performed changes (rename or delete)

### Failure Postconditions

- Templates remain unchanged

## Business Rules

### BR-029: Name Required

Template name is required and cannot be empty

### BR-030: Delete Does Not Affect Forms

Deleting a template does not affect any forms that were previously created from it

### BR-031: Cascading Delete

Template deletion cascades to all associated template questions
