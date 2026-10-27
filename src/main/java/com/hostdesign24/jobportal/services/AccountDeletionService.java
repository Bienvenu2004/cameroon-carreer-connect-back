package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.DeleteAccountRequest;

public interface AccountDeletionService {

    /**
     * Deletes the signed-in user's own account after checking the confirmation.
     * The email address is released, so it can be registered again, possibly
     * with a different role.
     */
    void deleteCurrentAccount(DeleteAccountRequest request);
}
