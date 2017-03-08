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

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;

import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;

@ServerSide
public class GoogleSettings {

  public static final String CLIENT_ID = "sonar.auth.google.clientId";
  public static final String CLIENT_SECRET = "sonar.auth.google.secret";
  public static final String HOSTED_DOMAIN = "sonar.auth.google.hd";
  public static final String ENABLED = "sonar.auth.google.enabled";
  public static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.google.allowUsersToSignUp";

  public static final String CATEGORY = "google";
  public static final String SUBCATEGORY = "authentication";

  private final Settings settings;

  public GoogleSettings(Settings settings) {
    this.settings = settings;
  }

  private static String toShellCompatible(String name) {
    return name.replace('.', '_');
  }

  private static String fromEnv(String key, String value) {
    key = toShellCompatible(key);
    return System.getenv(key) != null ? System.getenv(key) : value;
  }

  public static List<PropertyDefinition> definitions() {
    return Arrays.asList(
        PropertyDefinition.builder(ENABLED)
            .name("Enabled")
            .description("Enable Google users to login. Value is ignored if client ID and secret are not defined.")
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .type(BOOLEAN)
            .defaultValue(fromEnv(ENABLED, valueOf(false)))
            .index(1)
            .build(),
        PropertyDefinition.builder(CLIENT_ID)
            .name("Client ID")
            .description("Client ID provided by Google when registering the application.")
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .defaultValue(fromEnv(CLIENT_ID, ""))
            .index(2)
            .build(),
        PropertyDefinition.builder(CLIENT_SECRET)
            .name("Client Secret")
            .description("Client password provided by Google when registering the application.")
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .defaultValue(fromEnv(CLIENT_SECRET, ""))
            .index(3)
            .build(),
        PropertyDefinition.builder(HOSTED_DOMAIN)
            .name("Hosted domain")
            .description("Optional Google Apps hosted domain.")
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .defaultValue(fromEnv(HOSTED_DOMAIN, ""))
            .index(4)
            .build(),
        PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
            .name("Allow users to sign-up")
            .description("Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server.")
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .type(BOOLEAN)
            .defaultValue(fromEnv(ALLOW_USERS_TO_SIGN_UP, valueOf(false)))
            .index(5)
            .build()
    );
  }

  @CheckForNull
  public String clientId() {
    return settings.getString(CLIENT_ID);
  }

  @CheckForNull
  public String clientSecret() {
    return settings.getString(CLIENT_SECRET);
  }

  public String hostedDomain() {
    return settings.getString(HOSTED_DOMAIN);
  }

  public boolean isEnabled() {
    return settings.getBoolean(ENABLED) && clientId() != null && clientSecret() != null;
  }

  public boolean allowUsersToSignUp() {
    return settings.getBoolean(ALLOW_USERS_TO_SIGN_UP);
  }
}
