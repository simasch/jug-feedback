# Use Case: Generate QR Code

## Overview

**Use Case ID:** UC-08
**Use Case Name:** Generate QR Code
**Primary Actor:** Form Owner, Shared User
**Goal:** Generate a QR code for the public form link
**Status:** Implemented

## Preconditions

- User is authenticated
- User has access to the form

## Main Success Scenario

1. User clicks "QR Code" in the dashboard
2. System generates a QR code as a PNG image for the form URL
3. System displays a dialog with the QR code image and URL. The URL must be a link.
4. User can copy the URL to the clipboard

## Postconditions

### Success Postconditions

- No changes to the system

### Failure Postconditions

- No changes to the system

## Business Rules

### BR-018: URL Format

URL format: `{scheme}://{server}:{port}/form/{publicToken}`

### BR-019: QR Code Format

QR code is generated as a PNG image (ZXing library)

### BR-020: Status-Independent Availability

QR code is available for all form statuses (DRAFT, PUBLIC, CLOSED)
