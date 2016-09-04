package org.projectsforge.xwiki.registrationcodes.service;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.projectsforge.xwiki.registrationcodes.Utils;
import org.projectsforge.xwiki.registrationcodes.mapping.RegistrationCode;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
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
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * The Class DefaultRegistrationCodesService.
 */
@Component
public class DefaultRegistrationCodesService implements RegistrationCodesService {

  /** The Constant GROUPCLASS_REFERENCE. */
  public static final EntityReference GROUPCLASS_REFERENCE = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
      new EntityReference("XWiki", EntityType.SPACE));

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

  /** The wiki user manager. */
  @Inject
  private WikiUserManager wikiUserManager;

  /** The wiki descriptor manager. */
  @Inject
  private WikiDescriptorManager wikiDescriptorManager;

  /** The xwiki group service. */
  @Inject
  private XWikiGroupService xwikiGroupService;

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

      DocumentReference userRef = documentReferenceResolver.resolve(user, context.getWikiReference());

      DocumentReference regCodeRef = documentReferenceResolver.resolve(results.get(0), context.getWikiReference());
      XWikiDocument regCodeDoc = xwiki.getDocument(regCodeRef, context);
      RegistrationCode regCode = new RegistrationCode(this, regCodeDoc);
      if (regCode.accept(cleanedCode, user)) {
        logger.debug("Registration code accepted for {}", user);

        {
          List<String> addToWikis = regCode.getAddToWikis();
          logger.debug("Adding {} to wikis {}", userRef, addToWikis);
          for (String wikiname : addToWikis) {
            addToWiki(userRef, wikiname);
          }
        }

        {
          List<String> addToGroups = regCode.getAddToGroups();
          logger.debug("Adding {} to groups {}", userRef, addToGroups);
          for (String group : addToGroups) {
            addToGroup(userRef, group);
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

  /**
   * Adds the to group.
   *
   * @param userRef
   *          the user ref
   * @param group
   *          the group
   * @throws XWikiException
   *           the x wiki exception
   */
  private void addToGroup(DocumentReference userRef, String group) throws XWikiException {
    try {
      XWikiContext context = getContext();
      XWiki xwiki = context.getWiki();

      DocumentReference groupRef = documentReferenceResolver.resolve(group);
      XWikiDocument groupDoc = xwiki.getDocument(groupRef, context);
      List<BaseObject> xobjects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);

      String user;
      if (groupRef.getWikiReference().equals(userRef.getWikiReference())) {
        // group and user on same wiki
        user = Utils.LOCAL_REFERENCE_SERIALIZER.serialize(userRef);
      } else {
        user = userRef.toString();
      }

      boolean alreadyAMember = false;
      if (xobjects == null) {
        // if xobjects == null then it is not a group
        logger.warn("Group {} skipped since it is not a group", group);
        return;
      }

      for (BaseObject xobject : xobjects) {
        if (user.equals(xobject.getStringValue("member"))) {
          alreadyAMember = true;
        }
      }

      if (!alreadyAMember) {
        BaseObject xobject = groupDoc.newXObject(GROUPCLASS_REFERENCE, context);
        xobject.setStringValue("member", user);
        groupDoc.setContentAuthorReference(groupDoc.getAuthorReference());
        xwiki.saveDocument(groupDoc, context);
        if (logger.isDebugEnabled()) {
          logger.debug("{} added to group {}. Group members are {}", userRef, group,
              xwikiGroupService.getAllMembersNamesForGroup(group, Integer.MAX_VALUE, 0, context));
        }
      }
    } catch (XWikiException ex) {
      logger.warn("An error occurred while adding user " + userRef + " to group " + group, ex);
      throw ex;
    }
  }

  /**
   * Adds the to wiki.
   *
   * @param userRef
   *          the user ref
   * @param wikiname
   *          the wikiname
   * @throws WikiManagerException
   *           the wiki manager exception
   * @throws WikiUserManagerException
   *           the wiki user manager exception
   */
  private void addToWiki(DocumentReference userRef, String wikiname)
      throws WikiManagerException, WikiUserManagerException {
    try {
      // resolve alias into wikiid
      String realWikiname;
      if (wikiDescriptorManager.exists(wikiname)) {
        realWikiname = wikiname;
      } else {
        // try by alias
        WikiDescriptor wikiDescriptor = wikiDescriptorManager.getByAlias(wikiname);
        if (wikiDescriptor != null) {
          realWikiname = wikiDescriptor.getId();
        } else {
          logger.warn("Wiki {} skipped since unknown", wikiname);
          return;
        }
      }
      XWikiContext context = getContext();
      if (!context.getWikiId().equals(realWikiname)) {
        // add to wiki only if we are not already on the wiki
        wikiUserManager.addMember(userRef.toString(), realWikiname);
        if (logger.isDebugEnabled()) {
          logger.debug("{} added to wiki {}. Wiki members are {}", userRef, realWikiname,
              wikiUserManager.getMembers(realWikiname));
        }
      }
    } catch (WikiManagerException | WikiUserManagerException ex) {
      logger.warn("An error occurred while adding user " + userRef + " to wiki " + wikiname, ex);
      throw ex;
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
