package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.AuthenticationRequest;
import com.hostdesign24.jobportal.dto.AuthenticationResponse;
import com.hostdesign24.jobportal.dto.PasswordUpdateDto;
import com.hostdesign24.jobportal.dto.ResetPasswordRequest;
import com.hostdesign24.jobportal.dto.userDevice.VerifyDeviceDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

public interface AuthService {

  /**
   * Authenticates a user with the provided email and password.
   *
   * @param authenticationRequest the authentication request containing the email and password
   * @param response              the HTTP servlet response object
   * @param clientTypeHeader      the client type header
   * @return a message indicating the result of the authentication
   */
  AuthenticationResponse authenticateUser(
      AuthenticationRequest authenticationRequest,
      HttpServletResponse response,
      String clientTypeHeader,
      HttpServletRequest httpRequest);

  /**
   * Change password
   *
   * @param passwordUpdateDto contains the current password, new password and confirm password
   * @return returns a message either success of failure
   */
  String changePassword(PasswordUpdateDto passwordUpdateDto);

  /**
   * return a refresh token
   *
   * @param refreshToken the refresh token
   * @return returns a new refresh and access token
   */
  AuthenticationResponse refreshToken(String refreshToken, HttpServletResponse response,
      String clientTypeHeader);

  /**
   * initiates the password reset process for a user Generates a unique token, stores it to the
   * user's record, and sends a reset email
   *
   * @param email the email of the user
   */
  void initiatePasswordReset(String email);

  /**
   * Completes the password reset process for a user
   *
   * @param resetPasswordRequest contains the new password and the token
   */
  void completePasswordReset(ResetPasswordRequest resetPasswordRequest);

  void logout(HttpServletRequest request, HttpServletResponse response);

  @Transactional
  AuthenticationResponse verifyDeviceAndAuthenticate(
          VerifyDeviceDto request,
          HttpServletResponse response,
          String clientTypeHeader);

  AuthenticationResponse requestDeviceVerification(@Valid AuthenticationRequest request, HttpServletResponse response, String clientType, HttpServletRequest httpRequest);
}
