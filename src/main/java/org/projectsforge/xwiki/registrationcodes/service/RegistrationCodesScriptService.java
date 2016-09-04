package org.projectsforge.xwiki.registrationcodes.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

/**
 * The Class RegistrationCodesScriptService.
 */
@Component
@Singleton
@Named("registrationcodes")
public class RegistrationCodesScriptService implements ScriptService {

  /** The service. */
  @Inject
  private RegistrationCodesService service;

  /**
   * Activate regitration code.
   *
   * @param code
   *          the code
   * @param userRef
   *          the user ref
   * @return success, noresult, multipleresults, error
   */
  public String activateRegitrationCode(String code, String userRef) {
    return service.activateRegitrationCode(code, userRef);
  }

  /**
   * Gets the new registration code reference.
   *
   * @return the new registration code reference
   */
  public DocumentReference getNewRegistrationCodeReference() {
    return service.getNewRegistrationCodeReference();
  }

  /**
   * Gets the random registration code.
   *
   * @return the random registration code
   */
  public String getRandomRegistrationCode() {
    return service.getRandomRegistrationCode();
  }

  /**
   * Checks if is registration code existent.
   *
   * @param code
   *          the code
   * @return true, if is registration code existent
   */
  public boolean isRegistrationCodeExistent(String code) {
    return service.isRegistrationCodeExistent(code);
  }

}
