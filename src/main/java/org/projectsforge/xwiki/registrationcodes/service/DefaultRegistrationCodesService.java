package org.projectsforge.xwiki.registrationcodes.service;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.projectsforge.xwiki.registrationcodes.mapping.RegistrationCode;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

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

  @Inject
  private WikiUserManager wikiUserManager;

  @Inject
  private WikiDescriptorManager wikiDescriptorManager;

  /*
   * (non-Javadoc)
   *
   * @see
   * org.projectsforge.xwiki.registrationcodes.service.RegistrationCodesService#
   * activateRegitrationCode(java.lang.String, java.lang.String)
   */
  @Override
  public String activateRegitrationCode(String code, String user) {
    XWikiContext context = getContext();
    XWiki xwiki = context.getWiki();

    String cleanedCode = StringUtils.trimToEmpty(code);

    try {
      List<String> results = queryManager
          .createQuery(String.format(
              "from doc.object(%s) as regcode where doc.space like :space and regcode.active = 1 and regcode.code = :code",
              RegistrationCode.getClassReferenceAsString()), Query.XWQL)
          .bindValue("code", StringUtils.trimToEmpty(cleanedCode)).bindValue("space", "RegistrationCodes.Data.%")
          .execute();

      if (results.isEmpty()) {
        logger.warn("No result for registration code {} : {}. Rejecting activation.", cleanedCode, results);
        return "noresult";
      }
      if (results.size() > 1) {
        logger.warn("Multiple results for registration code {} : {}. Rejecting activation.", cleanedCode, results);
        return "multipleresults";
      }

      DocumentReference regCodeRef = documentReferenceResolver.resolve(results.get(0), context.getWikiReference());
      XWikiDocument regCodeDoc = xwiki.getDocument(regCodeRef, context);
      RegistrationCode regCode = new RegistrationCode(this, regCodeDoc);
      if (regCode.accept(cleanedCode, user)) {
        logger.debug("Registration code accepted for {}", user);
        List<String> addToWikis = regCode.getAddToWikis();
        List<String> addToGroups = regCode.getAddToGroups();
        logger.debug("Adding {} to wikis {} and groups {}", user, addToWikis, addToGroups);

        if (addToWikis.isEmpty()) {
          addToGroup(user, context.getWikiId(), addToGroups);
        } else {
          for (String addToWiki : addToWikis) {
            // resolve alias into wikiid
            String wikiName;
            if (wikiDescriptorManager.exists(addToWiki)) {
              wikiName = addToWiki;
            } else {
              // try by alias
              WikiDescriptor wikiDescriptor = wikiDescriptorManager.getByAlias(addToWiki);
              if (wikiDescriptor != null) {
                wikiName = wikiDescriptor.getId();
              } else {
                logger.warn("Wiki {} skipped since unknown", addToWiki);
                continue;
              }
            }
            addToGroup(user, wikiName, addToGroups);
            wikiUserManager.addMember(user, wikiName);
            logger.debug("Wiki {} members : {}", wikiName, wikiUserManager.getMembers(wikiName));
          }
        }

        regCode.addUser(user);
        xwiki.saveDocument(regCodeDoc, context);
        return "success";
      } else {
        return "noresult";
      }
    } catch (XWikiException | QueryException | WikiManagerException | WikiUserManagerException ex) {
      logger.warn("An error occurred", ex);
    }

    return "error";
  }

  private void addToGroup(String user, String wikiName, List<String> groups) throws XWikiException {
    XWikiContext context = getContext();
    XWiki xwiki = context.getWiki();
    DocumentReference xwikiGroupsRef = new DocumentReference(wikiName, "XWiki", "XWikiGroups");

    for (String group : groups) {
      DocumentReference groupRef = documentReferenceResolver.resolve(group, new WikiReference(wikiName));
      XWikiDocument groupDoc = xwiki.getDocument(groupRef, context);
      List<BaseObject> xobjects = groupDoc.getXObjects(xwikiGroupsRef);

      String userRef;
      if (context.getWikiId().equals(wikiName)) {
        // we are on the same wiki, keep short reference
        userRef = user;
      } else {
        // we are on a different wiki, we need fully qualified reference
        // (including wiki)
        userRef = documentReferenceResolver.resolve(user, context.getWikiReference()).toString();
      }

      boolean alreadyAMember = false;
      if (xobjects != null) {
        for (BaseObject xobject : xobjects) {
          if (userRef.equals(xobject.getStringValue("member"))) {
            alreadyAMember = true;
          }
        }
      }
      if (!alreadyAMember) {
        BaseObject xobject = groupDoc.newXObject(xwikiGroupsRef, context);
        xobject.setStringValue("member", userRef);
        groupDoc.setContentAuthorReference(groupDoc.getAuthorReference());
        xwiki.saveDocument(groupDoc, context);
        if (logger.isDebugEnabled()) {
          logger.debug("Group {} on {} : {}", groupRef, wikiName, groupDoc);
        }
      }
    }
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
