package org.projectsforge.xwiki.registrationcodes.service;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * The Interface RegistrationCodesService.
 */
@Role
public interface RegistrationCodesService {

  /**
   * Activate regitration code.
   *
   * @param code
   *          the code
   * @param userRef
   *          the user ref
   * @return success, noresult, multipleresults, error
   */
  String activateRegitrationCode(String code, String userRef);

  /**
   * Gets the context.
   *
   * @return the context
   */
  XWikiContext getContext();

  /**
   * Gets the new registration code reference.
   *
   * @return the new registration code reference
   */
  DocumentReference getNewRegistrationCodeReference();

  /**
   * Gets the random registration code.
   *
   * @return the random registration code
   */
  String getRandomRegistrationCode();

  /**
   * Checks if is registration code existent.
   *
   * @param code
   *          the code
   * @return true, if is registration code existent
   */
  boolean isRegistrationCodeExistent(String code);

}
