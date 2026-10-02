package org.code.api.organizations.domain;

/**
 * Conceals missing, inactive and inaccessible organization/member identities behind one result.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public class OrganizationAccessDenied extends RuntimeException {
  /** Creates the same safe message for nonexistent and inaccessible scope. */
  public OrganizationAccessDenied() {
    super("Organization or active membership not found");
  }
}
