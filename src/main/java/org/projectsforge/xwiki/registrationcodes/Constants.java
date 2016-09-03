package org.projectsforge.xwiki.registrationcodes;

import java.util.Arrays;
import java.util.List;

/**
 * The Class Constants.
 */
public abstract class Constants {

  /** The Constant EXTENSION_SPACE_NAME. */
  public static final String EXTENSION_SPACE_NAME = "RegistrationCodes";
  /** The Constant SPACE_NAME. */
  public static final List<String> CODE_SPACE_NAME_AS_LIST = Arrays.asList(EXTENSION_SPACE_NAME, "Code");

  /** The Constant CODE_SPACE_NAME_AS_STRING. */
  public static final String CODE_SPACE_NAME_AS_STRING = EXTENSION_SPACE_NAME + "." + "Code";

  /** The Constant DATA_SPACE_NAME_AS_LIST. */
  public static final List<String> DATA_SPACE_NAME_AS_LIST = Arrays.asList(EXTENSION_SPACE_NAME, "Data");

  /** The Constant DATA_SPACE_NAME_AS_STRING. */
  public static final String DATA_SPACE_NAME_AS_STRING = EXTENSION_SPACE_NAME + "." + "Data";

  /**
   * Instantiates a new bibliography constants.
   */
  private Constants() {
  }
}
