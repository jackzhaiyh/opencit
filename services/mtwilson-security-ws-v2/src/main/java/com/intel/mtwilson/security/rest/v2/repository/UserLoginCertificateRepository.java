/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.security.rest.v2.repository;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mountwilson.as.common.ASException;
import com.intel.mtwilson.security.rest.v2.model.UserLoginCertificate;
import com.intel.mtwilson.security.rest.v2.model.UserLoginCertificateCollection;
import com.intel.mtwilson.security.rest.v2.model.UserLoginCertificateFilterCriteria;
import com.intel.mtwilson.security.rest.v2.model.UserLoginCertificateLocator;
import com.intel.mtwilson.datatypes.ErrorCode;
import com.intel.mtwilson.jersey.resource.SimpleRepository;
import com.intel.mtwilson.security.rest.v2.model.Role;
import com.intel.mtwilson.security.rest.v2.model.RoleLocator;
import com.intel.mtwilson.security.rest.v2.model.Status;
import com.intel.mtwilson.security.rest.v2.model.UserLoginCertificateRole;
import com.intel.mtwilson.security.rest.v2.model.UserLoginCertificateRoleCollection;
import com.intel.mtwilson.security.rest.v2.model.UserLoginCertificateRoleFilterCriteria;
import com.intel.mtwilson.shiro.jdbi.LoginDAO;
import com.intel.mtwilson.shiro.jdbi.MyJdbi;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresPermissions;


/**
 *
 * @author ssbangal
 */
public class UserLoginCertificateRepository implements SimpleRepository<UserLoginCertificate, UserLoginCertificateCollection, UserLoginCertificateFilterCriteria, UserLoginCertificateLocator> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserLoginCertificateRepository.class);
    
    @Override
    @RequiresPermissions("user_login_certificates:search")        
    public UserLoginCertificateCollection search(UserLoginCertificateFilterCriteria criteria) {
        log.debug("UserLoginCertificate:Search - Got request to search for the users login certificates.");        
        UserLoginCertificateCollection objCollection = new UserLoginCertificateCollection();
        try (LoginDAO loginDAO = MyJdbi.authz()) {
            if (criteria.id != null) {
                UserLoginCertificate obj = loginDAO.findUserLoginCertificateById(criteria.id);
                if (obj != null) {
                    obj.setRoles(getAssociateRolesForLoginCertificateId(obj.getId()));
                    objCollection.getUserLoginCertificates().add(obj);
                }
            } else if (criteria.userIdEqualTo != null) {
                UserLoginCertificate obj = loginDAO.findUserLoginCertificateByUserId(criteria.userIdEqualTo);
                if (obj != null) {
                    obj.setRoles(getAssociateRolesForLoginCertificateId(obj.getId()));
                    objCollection.getUserLoginCertificates().add(obj);
                }
            } else if (criteria.userNameEqualTo != null && !criteria.userNameEqualTo.isEmpty()) {
                UserLoginCertificate obj = loginDAO.findUserLoginCertificateByUsername(criteria.userNameEqualTo);
                if (obj != null) {
                    obj.setRoles(getAssociateRolesForLoginCertificateId(obj.getId()));
                    objCollection.getUserLoginCertificates().add(obj);
                }                
            } else if (criteria.sha1 != null) {
                UserLoginCertificate obj = loginDAO.findUserLoginCertificateBySha1(criteria.sha1);
                if (obj != null) {
                    obj.setRoles(getAssociateRolesForLoginCertificateId(obj.getId()));
                    objCollection.getUserLoginCertificates().add(obj);
                }                
            } else if (criteria.sha256 != null) {
                UserLoginCertificate obj = loginDAO.findUserLoginCertificateBySha256(criteria.sha256);
                if (obj != null) {
                    obj.setRoles(getAssociateRolesForLoginCertificateId(obj.getId()));
                    objCollection.getUserLoginCertificates().add(obj);
                }                
            } 
        } catch (Exception ex) {
            log.error("Error during user keystore search.", ex);
            throw new ASException(ErrorCode.MS_API_USER_SEARCH_ERROR, ex.getClass().getSimpleName());
        }
        log.debug("UserLoginCertificate:Search - Returning back {} of results.", objCollection.getUserLoginCertificates().size());                
        return objCollection;
    }

    @Override
    @RequiresPermissions("user_login_certificates:retrieve")        
    public UserLoginCertificate retrieve(UserLoginCertificateLocator locator) {
        if( locator == null || locator.id == null ) { return null; }
        log.debug("UserLoginCertificate:Retrieve - Got request to retrieve user login certificate with id {}.", locator.id);                
         try (LoginDAO loginDAO = MyJdbi.authz()) {
            UserLoginCertificate obj = loginDAO.findUserLoginCertificateById(locator.id);
            if (obj != null) {
                obj.setRoles(getAssociateRolesForLoginCertificateId(obj.getId()));
                return obj;
            }
        } catch (Exception ex) {
            log.error("Error during user login certificate search.", ex);
            throw new ASException(ErrorCode.MS_API_USER_SEARCH_ERROR, ex.getClass().getSimpleName());
        }
        return null;
    }

    @Override
    @RequiresPermissions("user_login_certificates:store")        
    public void store(UserLoginCertificate item) {
        log.debug("UserLoginCertificate:Store - Got request to update user login certificate with id {}.", item.getId().toString());        
         try (LoginDAO loginDAO = MyJdbi.authz()) {
            UserLoginCertificate obj = loginDAO.findUserLoginCertificateById(item.getId());
            if (obj != null) {
                if (item.getComment() != null)
                    obj.setComment(item.getComment());
                
                obj.setEnabled(item.isEnabled());
                                
                if (item.getStatus() != null)
                    obj.setStatus(item.getStatus());
                
                loginDAO.updateUserLoginCertificateById(obj.getId(), obj.isEnabled(), obj.getStatus(), obj.getComment());
                log.debug("UserLoginCertificate:Store - Updated the user login certificate with id {} successfully.", obj.getId());

                // Before we add the roles we need to delete the existing ones
                UserLoginCertificateRoleRepository repo = new UserLoginCertificateRoleRepository();
                
                UserLoginCertificateRoleFilterCriteria criteria = new UserLoginCertificateRoleFilterCriteria();
                criteria.loginCertificateIdEqualTo = item.getId();
                repo.delete(criteria);
                
                // Now we need to add the roles requested by the user
                Set<String> roles = item.getRoles();
                for (String role : roles) {
                    // Let us verify if the role exists, if it does, then we will map the role to the user login password entry
                    Role roleInSystem = loginDAO.findRoleByName(role);
                    if (roleInSystem != null) {
                        UserLoginCertificateRole userLoginCertificateRole = new UserLoginCertificateRole();
                        userLoginCertificateRole.setLoginCertificateId(item.getId());
                        userLoginCertificateRole.setRoleId(roleInSystem.getId());
                        repo.create(userLoginCertificateRole);
                    }
                }
                
            } else {
                log.error("UserLoginCertificate:Store - User login certificate will not be updated since it does not exist.");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }            
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            log.error("Error during user update.", ex);
            throw new ASException(ErrorCode.MS_API_USER_UPDATE_ERROR, ex.getClass().getSimpleName());
        }
        
    }

    /**
     * Creates a new user
     * @param item 
     */
    @Override
    @RequiresPermissions("user_login_certificates:create")        
    public void create(UserLoginCertificate item) {
        log.debug("UserLoginCertificate:Create - Got request to create a new user keystore.");
         try (LoginDAO loginDAO = MyJdbi.authz()) {
            UserLoginCertificate obj = loginDAO.findUserLoginCertificateByUserId(item.getUserId());
            if (obj == null) {
                obj = new UserLoginCertificate();
                obj.setId(item.getId());
                obj.setUserId(item.getUserId());
                obj.setCertificate(item.getCertificate());
                obj.setComment(item.getComment());
                obj.setEnabled(false);
                obj.setExpires(item.getExpires());
                obj.setSha1Hash(item.getSha1Hash());
                obj.setSha256Hash(item.getSha256Hash());
                obj.setStatus(Status.PENDING);
                loginDAO.insertUserLoginCertificate(obj.getId(), obj.getUserId(), obj.getCertificate(), obj.getSha1Hash(), obj.getSha256Hash(),
                        obj.getExpires(), obj.isEnabled(), obj.getStatus(), obj.getComment());
                log.debug("UserLoginCertificate:Create - Created the user login certificate for user with id {} successfully.", obj.getUserId());
            } else {
                log.error("UserLoginCertificate:Create - User login certificate for user with Id {} will not be created since a duplicate already exists.", obj.getUserId());
                throw new WebApplicationException(Response.Status.CONFLICT);
            }            
        } catch (WebApplicationException wex) {
            throw wex;
        } catch (Exception ex) {
            log.error("Error during user creation.", ex);
            throw new ASException(ErrorCode.MS_API_USER_REGISTRATION_ERROR, ex.getClass().getSimpleName());
        }
    }

    @Override
    @RequiresPermissions("user_login_certificates:delete")        
    public void delete(UserLoginCertificateLocator locator) {
        if( locator == null || locator.id == null ) { return; }
        log.debug("UserLoginCertificate:Delete - Got request to delete user login certificate with id {}.", locator.id.toString());        
        try (LoginDAO loginDAO = MyJdbi.authz()) {
            UserLoginCertificate obj = loginDAO.findUserLoginCertificateById(locator.id);
            if (obj != null ) {

                // First delete all the role mappings from the UserLoginCertificateRole table
                UserLoginCertificateRoleRepository repo = new UserLoginCertificateRoleRepository();
                UserLoginCertificateRoleFilterCriteria criteria = new UserLoginCertificateRoleFilterCriteria();
                criteria.loginCertificateIdEqualTo = locator.id;
                repo.delete(criteria);
                
                loginDAO.deleteUserLoginCertificateById(locator.id);
                log.debug("UserLoginCertificate:Delete - Deleted the user login certificate with id {} successfully.", locator.id);
            } else {
                log.info("UserLoginCertificate:Delete - User login certificate does not exist in the system.");
            }
        } catch (Exception ex) {
            log.error("Error during user deletion.", ex);
            throw new ASException(ErrorCode.MS_API_USER_DELETION_ERROR, ex.getClass().getSimpleName());
        }
    }
    
    @Override
    @RequiresPermissions("user_login_certificates:delete,search")        
    public void delete(UserLoginCertificateFilterCriteria criteria) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Helper function to retrieve the roles associated with user login password. This would be called by
     * retrieve and search methods.
     * @param id
     * @return 
     */
    private Set<String> getAssociateRolesForLoginCertificateId(UUID id) {
        Set<String> associatedRoles = new HashSet<>();

        UserLoginCertificateRoleRepository repo = new UserLoginCertificateRoleRepository();
        RoleRepository roleRepo = new RoleRepository();

        UserLoginCertificateRoleFilterCriteria criteria = new UserLoginCertificateRoleFilterCriteria();
        criteria.loginCertificateIdEqualTo = id;
        UserLoginCertificateRoleCollection roles = repo.search(criteria);
        if (roles != null && roles.getUserLoginCertificateRoles().size() > 0) {
            for (UserLoginCertificateRole role : roles.getUserLoginCertificateRoles()) {
                RoleLocator roleLocator = new RoleLocator();
                roleLocator.id = role.getRoleId();
                Role retrieve = roleRepo.retrieve(roleLocator);
                associatedRoles.add(retrieve.getRoleName());
            }            
        }                 
        return associatedRoles;
    }
    
}