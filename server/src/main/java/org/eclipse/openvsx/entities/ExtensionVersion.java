/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.ExtensionReferenceJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.util.CollectionUtil;
import org.eclipse.openvsx.util.SemanticVersion;
import org.eclipse.openvsx.util.TimeUtil;

@Entity
public class ExtensionVersion {

    public static final Comparator<ExtensionVersion> SORT_COMPARATOR =
        Comparator.<ExtensionVersion, SemanticVersion>comparing(ev -> ev.getSemanticVersion())
                .thenComparing(Comparator.comparing(ev -> ev.getTimestamp()))
                .reversed();

    @Id
    @GeneratedValue
    long id;

    @ManyToOne
    Extension extension;

    @OneToOne(mappedBy = "latest", fetch = FetchType.LAZY)
    Extension latestInverse;

    String version;

    @Transient
    SemanticVersion semver;

    boolean preview;

    LocalDateTime timestamp;

    @ManyToOne
    PersonalAccessToken publishedWith;

    String displayName;

    @Column(length = 2048)
    String description;

    @ElementCollection
    List<String> engines;

    @ElementCollection
    List<String> categories;

    @ElementCollection
    List<String> tags;

    String license;

    String homepage;

    String repository;

    String bugs;

    @Column(length = 16)
    String markdown;

    @Column(length = 16)
    String galleryColor;

    @Column(length = 16)
    String galleryTheme;

    String qna;

    @ManyToMany
    List<Extension> dependencies;

    @ManyToMany
    List<Extension> bundledExtensions;


    /**
     * Convert to a JSON object without URLs.
     */
    public ExtensionJson toExtensionJson() {
        var json = new ExtensionJson();
        var extension = this.getExtension();
        json.namespace = extension.getNamespace().getName();
        json.name = extension.getName();
        json.averageRating = extension.getAverageRating();
        json.downloadCount = extension.getDownloadCount();
        json.version = this.getVersion();
        json.preview = this.isPreview();
        if (this.getTimestamp() != null) {
            json.timestamp = TimeUtil.toUTCString(this.getTimestamp());
        }
        json.displayName = this.getDisplayName();
        json.description = this.getDescription();
        json.engines = this.getEnginesMap();
        json.categories = this.getCategories();
        json.tags = this.getTags();
        json.license = this.getLicense();
        json.homepage = this.getHomepage();
        json.repository = this.getRepository();
        json.bugs = this.getBugs();
        json.markdown = this.getMarkdown();
        json.galleryColor = this.getGalleryColor();
        json.galleryTheme = this.getGalleryTheme();
        json.qna = this.getQna();
        if (this.getPublishedWith() != null) {
            json.publishedBy = this.getPublishedWith().getUser().toUserJson();
        }
        if (this.getDependencies() != null) {
            json.dependencies = CollectionUtil.map(this.getDependencies(), depExtension -> {
                var ref = new ExtensionReferenceJson();
                ref.namespace = depExtension.getNamespace().getName();
                ref.extension = depExtension.getName();
                return ref;
            });
        }
        if (this.getBundledExtensions() != null) {
            json.bundledExtensions = CollectionUtil.map(this.getBundledExtensions(), bndExtension -> {
                var ref = new ExtensionReferenceJson();
                ref.namespace = bndExtension.getNamespace().getName();
                ref.extension = bndExtension.getName();
                return ref;
            });
        }
        return json;
    }

    /**
     * Convert to a search entry JSON object without URLs.
     */
    public SearchEntryJson toSearchEntryJson() {
        var entry = new SearchEntryJson();
        var extension = this.getExtension();
        entry.name = extension.getName();
        entry.namespace = extension.getNamespace().getName();
        entry.averageRating = extension.getAverageRating();
        entry.downloadCount = extension.getDownloadCount();
        entry.version = this.getVersion();
        entry.timestamp = TimeUtil.toUTCString(this.getTimestamp());
        entry.displayName = this.getDisplayName();
        entry.description = this.getDescription();
        return entry;
    }

    public Map<String, String> getEnginesMap() {
        var engines = this.getEngines();
        if (engines == null)
            return null;
        var result = Maps.<String, String>newLinkedHashMapWithExpectedSize(engines.size());
        for (var engine : engines) {
            var split = engine.split("@");
            if (split.length == 2) {
                result.put(split[0], split[1]);
            }
        }
        return result;
    }

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Extension getExtension() {
		return extension;
	}

	public void setExtension(Extension extension) {
		this.extension = extension;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
    }
    
    public SemanticVersion getSemanticVersion() {
        if (semver == null) {
            var version = getVersion();
            if (version != null)
                semver = new SemanticVersion(version);
        }
        return semver;
    }

	public boolean isPreview() {
		return preview;
	}

	public void setPreview(boolean preview) {
		this.preview = preview;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

    public PersonalAccessToken getPublishedWith() {
        return publishedWith;
    }

    public void setPublishedWith(PersonalAccessToken publishedWith) {
        this.publishedWith = publishedWith;
    }

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public List<String> getEngines() {
		return engines;
	}

	public void setEngines(List<String> engines) {
		this.engines = engines;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String getBugs() {
		return bugs;
	}

	public void setBugs(String bugs) {
		this.bugs = bugs;
	}

	public String getMarkdown() {
		return markdown;
	}

	public void setMarkdown(String markdown) {
		this.markdown = markdown;
	}

	public String getGalleryColor() {
		return galleryColor;
	}

	public void setGalleryColor(String galleryColor) {
		this.galleryColor = galleryColor;
	}

	public String getGalleryTheme() {
		return galleryTheme;
	}

	public void setGalleryTheme(String galleryTheme) {
		this.galleryTheme = galleryTheme;
	}

	public String getQna() {
		return qna;
	}

	public void setQna(String qna) {
		this.qna = qna;
	}

	public List<Extension> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<Extension> dependencies) {
		this.dependencies = dependencies;
	}

	public List<Extension> getBundledExtensions() {
		return bundledExtensions;
	}

	public void setBundledExtensions(List<Extension> bundledExtensions) {
		this.bundledExtensions = bundledExtensions;
	}

}