package org.projectsforge.xwiki.registrationcodes.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.projectsforge.xwiki.registrationcodes.mapping.RegistrationCode;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The Class DefaultRegistrationCodesService.
 */
@Component
public class DefaultRegistrationCodesService implements RegistrationCodesService {

  /** The logger. */
  @Inject
  private Logger logger;

  /** The document reference resolver. */
  @Inject
  private DocumentReferenceResolver<String> documentReferenceResolver;

  /** The query manager. */
  @Inject
  private QueryManager queryManager;

  /** The context provider. */
  @Inject
  private Provider<XWikiContext> contextProvider;

  /*
   * (non-Javadoc)
   *
   * @see
   * org.projectsforge.xwiki.registrationcodes.service.RegistrationCodesService#
   * activateRegitrationCode(java.lang.String, java.lang.String)
   */
  @Override
  public boolean activateRegitrationCode(String code, String userRef) {
    XWikiContext context = getContext();
    XWiki xwiki = context.getWiki();

    try {
      List<String> results = queryManager
          .createQuery(String.format(
              "from doc.object(%s) as regcode where doc.space like :space and regcode.active = 1 and regcode.code = :code",
              RegistrationCode.getClassReferenceAsString()), Query.XWQL)
          .bindValue("code", StringUtils.trimToEmpty(code)).bindValue("space", "RegistrationCodes.Data.%").execute();

      if (results.isEmpty()) {
        logger.warn("No result for registration code {} : {}. Rejecting activation.", code, results);
        return false;
      }
      if (results.size() > 1) {
        logger.warn("Multiple results for registration code {} : {}. Rejecting activation.", code, results);
        return false;
      }

      DocumentReference regCodeRef = documentReferenceResolver.resolve(results.get(0),
          new SpaceReference("XWiki", context.getWikiReference()));
      XWikiDocument regCodeDoc = xwiki.getDocument(regCodeRef, context);
      RegistrationCode regCode = new RegistrationCode(this, regCodeDoc);
      if (regCode.accept(code, userRef)) {
        logger.debug("Registration code accepted for {}", userRef);
        DocumentReference xwikiGroupsRef = new DocumentReference(context.getWikiId(), "XWiki", "XWikiGroups");
        boolean found = false;
        for (String group : regCode.getAddToGroups()) {
          if (StringUtils.isBlank(group)) {
            continue;
          }
          DocumentReference groupRef = documentReferenceResolver.resolve(group, context.getWikiReference());
          XWikiDocument groupDoc = xwiki.getDocument(groupRef, context);
          // search for already existing member in the group
          List<BaseObject> xobjects = groupDoc.getXObjects(xwikiGroupsRef);
          if (xobjects != null) {
            for (BaseObject xobject : xobjects) {
              found = found || (Objects.equals(xobject.getStringValue("member"), userRef));
            }
          }
          if (!found) {
            logger.debug("Adding {} to group {}", userRef, groupRef);
            groupDoc.newXObject(xwikiGroupsRef, context).setStringValue("member", userRef);
            xwiki.saveDocument(groupDoc, context);
          }
        }

        regCode.addUser(userRef);
        xwiki.saveDocument(regCodeDoc, context);
        return true;
      }
    } catch (XWikiException | QueryException ex) {
      logger.warn("An error occurred", ex);
    }

    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.projectsforge.xwiki.registrationcodes.service.RegistrationCodesService#
   * getContext()
   */
  @Override
  public XWikiContext getContext() {
    return contextProvider.get();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.projectsforge.xwiki.registrationcodes.service.RegistrationCodesService#
   * getNewRegistrationCodeReference()
   */
  @Override
  public DocumentReference getNewRegistrationCodeReference() {
    List<String> results = null;
    try {
      results = queryManager
          .createQuery(String.format("from doc.object(%s) as regcode", RegistrationCode.getClassReferenceAsString()),
              Query.XWQL)
          .execute();
    } catch (QueryException ex) {
      logger.warn("An error occurred while executing query ", ex);
    }

    int counter = 0;
    if (results != null) {
      for (String id : results) {
        if (id.startsWith(RegistrationCode.NAME_PREFIX) && id.endsWith(RegistrationCode.NAME_SUFFIX)) {
          String number = id.substring(0, id.length() - RegistrationCode.NAME_SUFFIX.length())
              .substring(RegistrationCode.NAME_PREFIX.length());
          try {
            counter = Math.max(counter, Integer.parseInt(number));
          } catch (NumberFormatException ex) {
            logger.warn("Can not extract number", ex);
          }
        }
      }
    }
    counter++;
    return documentReferenceResolver.resolve(RegistrationCode.NAME_PREFIX + counter + RegistrationCode.NAME_SUFFIX);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.projectsforge.xwiki.registrationcodes.service.RegistrationCodesService#
   * getRandomRegistrationCode()
   */
  @Override
  public String getRandomRegistrationCode() {
    String code;
    do {
      code = UUID.randomUUID().toString();
    } while (isRegistrationCodeExistent(code));
    return code;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.projectsforge.xwiki.registrationcodes.service.RegistrationCodesService#
   * isRegistrationCodeExistent(java.lang.String)
   */
  @Override
  public boolean isRegistrationCodeExistent(String code) {
    try {
      return !queryManager
          .createQuery(String.format("from doc.object(%s) as regcode where regcode.code = :code",
              RegistrationCode.getClassReferenceAsString()), Query.XWQL)
          .bindValue("code", code).setLimit(1).execute().isEmpty();
    } catch (QueryException ex) {
      logger.warn("An error occurred", ex);
      return false;
    }
  }

}
