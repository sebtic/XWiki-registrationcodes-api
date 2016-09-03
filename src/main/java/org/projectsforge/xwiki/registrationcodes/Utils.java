package org.projectsforge.xwiki.registrationcodes;

import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;

/**
 * The Class Utils.
 */
public class Utils {

  /** Serialize document reference without the wikiname. */
  public static final LocalStringEntityReferenceSerializer LOCAL_REFERENCE_SERIALIZER = new LocalStringEntityReferenceSerializer(
      new DefaultSymbolScheme());

  /**
   * Instantiates a new utils.
   */
  private Utils() {
  }
}
