package com.hostdesign24.jobportal.dto;

import lombok.Data;

/**
 * Proof that the person deleting the account is its owner, not just someone at
 * an unlocked screen. Password accounts confirm with their password; Google
 * accounts have no password, so they confirm by typing their email address.
 */
@Data
public class DeleteAccountRequest {
    private String password;
    private String confirmEmail;
}
