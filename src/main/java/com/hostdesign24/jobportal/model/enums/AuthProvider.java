package com.hostdesign24.jobportal.model.enums;

/**
 * How a user account authenticates.
 *
 * <ul>
 *   <li>{@link #LOCAL}  — email + password (the classic registration flow).</li>
 *   <li>{@link #GOOGLE} — created / linked via Google Sign-In (ID-token flow).</li>
 * </ul>
 *
 * An account created locally can still be signed into with Google as long as
 * the verified Google email matches — the provider recorded here reflects how
 * the account was <em>first</em> created.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
