package org.projectsforge.xwiki.registrationcodes.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.projectsforge.xwiki.registrationcodes.Constants;
import org.projectsforge.xwiki.registrationcodes.service.RegistrationCodesService;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The Class RegistrationCode.
 */
public class RegistrationCode {

  /** The Constant NAME_PREFIX. */
  public static final String NAME_PREFIX = Constants.DATA_SPACE_NAME_AS_STRING + ".RegistrationCode-";

  /** The Constant NAME_SUFFIX. */
  public static final String NAME_SUFFIX = ".WebHome";

  /** The Constant FIELD_ACTIVE. */
  private static final String FIELD_ACTIVE = "active";

  /** The Constant FIELD_CODE. */
  private static final String FIELD_CODE = "code";

  /** The Constant FIELD_MAX_USE. */
  private static final String FIELD_MAX_USE = "maxUse";

  /** The Constant FIELD_START_DATE. */
  private static final String FIELD_START_DATE = "startDate";

  /** The Constant FIELD_END_DATE. */
  private static final String FIELD_END_DATE = "endDate";

  /** The Constant FIELD_ADD_TO_GROUPS. */
  private static final String FIELD_ADD_TO_GROUPS = "addToGroups";

  /** The Constant FIELD_ADD_TO_WIKIS. */
  private static final String FIELD_ADD_TO_WIKIS = "addToWikis";

  /** The Constant FIELD_USERS. */
  private static final String FIELD_USERS = "users";

  /** The Constant FIELD_LAST_USED. */
  private static final String FIELD_LAST_USED = "lastUsed";

  /**
   * Gets the class reference.
   *
   * @param entityReference
   *          the entity reference
   * @return the class reference
   */
  public static DocumentReference getClassReference(EntityReference entityReference) {
    return new DocumentReference(entityReference.extractReference(EntityType.WIKI).getName(),
        Constants.CODE_SPACE_NAME_AS_LIST, "RegistrationCodeClass");
  }

  /**
   * Gets the class reference.
   *
   * @param document
   *          the document
   * @return the class reference
   */
  public static DocumentReference getClassReference(XWikiDocument document) {
    return getClassReference(document.getDocumentReference());
  }

  /**
   * Gets the class reference as string.
   *
   * @return the class reference as string
   */
  public static Object getClassReferenceAsString() {
    return Constants.CODE_SPACE_NAME_AS_STRING + ".RegistrationCodeClass";
  }

  /** The xobject. */
  private BaseObject xobject;

  /**
   * Instantiates a new person.
   *
   * @param service
   *          the service
   * @param document
   *          the document
   */
  public RegistrationCode(RegistrationCodesService service, XWikiDocument document) {
    this.xobject = document.getXObject(getClassReference(document), true, service.getContext());
    if (xobject == null) {
      throw new IllegalStateException(
          String.format("%s is not a RegistrationCodeClass", document.getDocumentReference()));
    }
  }

  /**
   * Accept.
   *
   * @param code
   *          the code
   * @param userRef
   *          the user ref
   * @return true, if successful
   */
  public boolean accept(String code, String userRef) {
    if (!StringUtils.equals(StringUtils.trim(code), StringUtils.trim(getCode()))) {
      return false;
    }

    if (!isActive()) {
      return false;
    }

    if (getUsers().size() >= getMaxUse()) {
      return false;
    }

    Date date = new Date();
    if (getStartDate().after(date) || getEndDate().before(date)) {
      return false;
    }

    if (getUsers().contains(userRef)) {
      return false;
    }

    return true;
  }

  /**
   * Adds the user.
   *
   * @param user
   *          the user
   */
  public void addUser(String user) {
    List<String> users = new ArrayList<>(getUsers());
    if (!users.contains(user)) {
      users.add(user);
    }
    xobject.setStringListValue(FIELD_USERS, users);
    xobject.setDateValue(FIELD_LAST_USED, new Date());
  }

  /**
   * Gets the adds the to groups.
   *
   * @return the adds the to groups
   */
  @SuppressWarnings("unchecked")
  public List<String> getAddToGroups() {
    List<String> result = xobject.getListValue(FIELD_ADD_TO_GROUPS);
    if (result.isEmpty()) {
      result = Collections.emptyList();
    }
    return result;
  }

  /**
   * Gets the adds the to wikis.
   *
   * @return the adds the to wikis
   */
  @SuppressWarnings("unchecked")
  public List<String> getAddToWikis() {
    List<String> result = xobject.getListValue(FIELD_ADD_TO_WIKIS);
    if (result.isEmpty()) {
      result = Collections.emptyList();
    }
    return result;
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  public String getCode() {
    return xobject.getStringValue(FIELD_CODE);
  }

  /**
   * Gets the end date.
   *
   * @return the end date
   */
  public Date getEndDate() {
    return xobject.getDateValue(FIELD_END_DATE);
  }

  /**
   * Gets the max use.
   *
   * @return the max use
   */
  public int getMaxUse() {
    return xobject.getIntValue(FIELD_MAX_USE);
  }

  /**
   * Gets the start date.
   *
   * @return the start date
   */
  public Date getStartDate() {
    return xobject.getDateValue(FIELD_START_DATE);
  }

  /**
   * Gets the users.
   *
   * @return the users
   */
  @SuppressWarnings("unchecked")
  public List<String> getUsers() {
    return xobject.getListValue(FIELD_USERS);
  }

  /**
   * Checks if is active.
   *
   * @return true, if is active
   */
  public boolean isActive() {
    return xobject.getIntValue(FIELD_ACTIVE) == 1;
  }

}
