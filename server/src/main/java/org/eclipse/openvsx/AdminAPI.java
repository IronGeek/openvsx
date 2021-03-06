/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import java.net.URI;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.PersistedLog;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.NamespaceMembershipListJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.StatsJson;
import org.eclipse.openvsx.json.UserPublishInfoJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AdminAPI {

    @Autowired
    RepositoryService repositories;

    @Autowired
    AdminService admins;

    @Autowired
    LocalRegistryService local;

    @Autowired
    SearchService search;

    @GetMapping(
        path = "/admin/stats",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StatsJson> getStats(@RequestParam("token") String tokenValue) {
        try {
            admins.checkAdminUser();

            var json = new StatsJson();
            json.userCount = repositories.countUsers();
            json.extensionCount = repositories.countExtensions();
            json.namespaceCount = repositories.countNamespaces();
            return ResponseEntity.ok(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(StatsJson.class);
        }
    }

    @GetMapping(
        path = "/admin/log",
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String getLog(@RequestParam(name = "period", required = false) String periodString) {
        try {
            admins.checkAdminUser();

            Streamable<PersistedLog> logs;
            if (Strings.isNullOrEmpty(periodString)) {
                logs = repositories.findAllPersistedLogs();
            } else {
                try {
                    var period = Period.parse(periodString);
                    var now = TimeUtil.getCurrentUTC();
                    logs = repositories.findPersistedLogsAfter(now.minus(period));
                } catch (DateTimeParseException exc) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid period");
                }
            }
            return logs.stream()
                    .map(this::toString)
                    .collect(Collectors.joining("\n")) + "\n";
        } catch (ErrorResultException exc) {
            var status = exc.getStatus() != null ? exc.getStatus() : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(status);
        }
    }

    private String toString(PersistedLog log) {
        var timestamp = log.getTimestamp().minusNanos(log.getTimestamp().getNano());
        return timestamp + "\t" + log.getUser().getLoginName() + "\t" + log.getMessage();
    }

    @PostMapping(
        path = "/admin/update-search-index",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> updateSearchIndex() {
        try {
            var adminUser = admins.checkAdminUser();

            search.updateSearchIndex(true);

            var result = ResultJson.success("Updated search index");
            admins.logAdminAction(adminUser, result);
            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @PostMapping(
        path = "/admin/{namespaceName}/change-member",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> editNamespaceMember(@PathVariable String namespaceName,
                                                          @RequestParam("user") String userName,
                                                          @RequestParam(required = false) String provider,
                                                          @RequestParam String role) {
        try {
            var adminUser = admins.checkAdminUser();
            var result = admins.editNamespaceMember(namespaceName, userName, provider, role, adminUser);
            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @PostMapping(
        path = "/admin/{namespaceName}/delete-extension",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> deleteExtension(@PathVariable String namespaceName,
                                                      @RequestParam("extension") String extensionName,
                                                      @RequestParam(required = false) String version) {
        try {
            var adminUser = admins.checkAdminUser();
            var result = admins.deleteExtension(namespaceName, extensionName, version, adminUser);
            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @GetMapping(path = "/admin/{namespaceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NamespaceJson> getNamespace(@PathVariable String namespaceName) {
        try {
            admins.checkAdminUser();

            var namespace = local.getNamespace(namespaceName);
            var serverUrl = UrlUtil.getBaseUrl();
            namespace.membersUrl = UrlUtil.createApiUrl(serverUrl, "admin", namespace.name, "members");
            namespace.roleUrl = UrlUtil.createApiUrl(serverUrl, "admin", namespace.name, "change-member");
            return ResponseEntity.ok(namespace);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(NamespaceJson.class);
        }
    }

    @GetMapping(path = "/admin/{namespaceName}/members", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NamespaceMembershipListJson> getNamespaceMembers(@PathVariable String namespaceName) {
        try{
            admins.checkAdminUser();
            var namespace = repositories.findNamespace(namespaceName);
            var memberships = repositories.findMemberships(namespace);
            var membershipList = new NamespaceMembershipListJson();
            membershipList.namespaceMemberships = memberships.map(membership -> membership.toJson()).toList();
            return ResponseEntity.ok(membershipList);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(NamespaceMembershipListJson.class);
        }
    }

    @PostMapping(
        path = "/admin/-/create-namespace",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> createNamespace(@RequestBody NamespaceJson namespace) {
        try {
            admins.checkAdminUser();
            var json = admins.createNamespace(namespace);
            var serverUrl = UrlUtil.getBaseUrl();
            var url = UrlUtil.createApiUrl(serverUrl, "admin", namespace.name);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create(url))
                    .body(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @GetMapping(path = "/admin/-/{provider}/{loginName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserPublishInfoJson> getUserPublishInfo(@PathVariable String provider, @PathVariable String loginName) {
        try {
            admins.checkAdminUser();
            var userPublishInfo = admins.getUserPublishInfo(provider, loginName);
            return ResponseEntity.ok(userPublishInfo);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(UserPublishInfoJson.class);
        }
    }

    @PostMapping(
        path = "/admin/-/{provider}/{loginName}/revoke",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> revokePublisherAgreement(@PathVariable String loginName, @PathVariable String provider) {
        try {
            var adminUser = admins.checkAdminUser();
            var result = admins.revokePublisherAgreement(provider, loginName, adminUser);
            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }
    
}