/*
 * Google Authentication for SonarQube
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.auth.google;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;

import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.Oauth2Scopes;
import com.google.api.services.oauth2.model.Userinfoplus;

@ServerSide
public class GoogleIdentityProvider implements OAuth2IdentityProvider {

  private final String appId = "sonarqube";

  private final GoogleSettings settings;

  private final GoogleAuthorizationCodeFlow authorizationFlow;

  public GoogleIdentityProvider(GoogleSettings settings) {
    this.settings = settings;
    authorizationFlow = new GoogleAuthorizationCodeFlow.Builder(
        new NetHttpTransport(), JacksonFactory.getDefaultInstance(),
        settings.clientId(), settings.clientSecret(),
        Arrays.asList(
            Oauth2Scopes.USERINFO_EMAIL, Oauth2Scopes.USERINFO_PROFILE)
    ).build();
  }

  private static String getFullUrl(HttpServletRequest request) {
    StringBuffer fullUrlBuf = request.getRequestURL();
    if (request.getQueryString() != null) {
      fullUrlBuf.append('?').append(request.getQueryString());
    }
    return fullUrlBuf.toString();
  }

  private static String getAuthorizationCode(String fullUrl) {
    AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(fullUrl);
    if (authResponse.getError() != null) {
      String errorDescription = String.format("Failed to authenticate the user. %s", authResponse.getErrorDescription());
      throw new IllegalStateException(errorDescription);
    }
    return authResponse.getCode();
  }

  @Override
  public String getKey() {
    return "google";
  }

  @Override
  public String getName() {
    return "Google";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
        .setIconPath("https://google-developers.appspot.com/identity/sign-in/g-normal.png")
        .setBackgroundColor("#4285F4")
        .build();
  }

  @Override
  public boolean isEnabled() {
    return settings.isEnabled();
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return settings.allowUsersToSignUp();
  }

  @Override
  public void init(InitContext context) {
    GoogleAuthorizationCodeRequestUrl url = authorizationFlow
        .newAuthorizationUrl();
    if (this.settings.hostedDomain() != null) {
      url.set("hd", this.settings.hostedDomain());
    }
    String authorizationUrl = url
        .setState(context.generateCsrfState())
        .setRedirectUri(context.getCallbackUrl())
        .build();

    context.redirectTo(authorizationUrl);
  }

  @Override
  public void callback(CallbackContext context) {
    context.verifyCsrfState();

    String requestUrl = getFullUrl(context.getRequest());
    String authorizationCode = getAuthorizationCode(requestUrl);
    try {
      GoogleTokenResponse response = authorizationFlow
          .newTokenRequest(authorizationCode)
          .setRedirectUri(context.getCallbackUrl())
          .execute();

      Credential credential = authorizationFlow.createAndStoreCredential(response, null);

      Oauth2 googleUserDetailService = new Oauth2.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
          .setApplicationName(appId)
          .build();

      Userinfoplus userinfo = googleUserDetailService
          .userinfo()
          .get()
          .execute();

      if (settings.hostedDomain() != null && !settings.hostedDomain().equals(userinfo.getHd())) {
        throw new IllegalStateException("Not allowed Google Apps Hosted Domain");
      }

      if (!userinfo.isVerifiedEmail()) {
        throw new IllegalStateException("Access with not verified email");
      }

      UserIdentity userIdentity = UserIdentity
          .builder()
          .setProviderLogin(userinfo.getId())
          .setLogin(userinfo.getId())
          .setName(userinfo.getName())
          .setEmail(userinfo.getEmail())
          .build();

      context.authenticate(userIdentity);
      context.redirectToRequestedPage();
    }
    catch (IOException e) {
      throw new IllegalStateException("User verification failed", e);
    }
  }
}
