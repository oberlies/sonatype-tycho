/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.PublisherService;

/**
 * This goal invokes the category publisher and publishes category information.
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 * @goal publish-categories
 */
public final class PublishCategoriesMojo extends AbstractPublishMojo {

    @Override
    protected Collection<?> publishContent(PublisherService publisherService) throws MojoExecutionException,
            MojoFailureException {
        try {
            List<Object> categoryIUs = new ArrayList<Object>();
            for (Category category : getCategories()) {
                final File buildCategoryFile = prepareBuildCategory(category, getBuildDirectory());

                Collection<?> ius = publisherService.publishCategories(buildCategoryFile);
                categoryIUs.addAll(ius);
            }
            return categoryIUs;
        } catch (FacadeException e) {
            throw new MojoExecutionException("Exception while publishing categories: " + e.getMessage(), e);
        }
    }

    /**
     * Writes the Tycho-internal representation of categories back to a category.xml.
     * 
     * @param category
     *            a category, with "qualifier" literals already replaced by the build qualifier.
     */
    private File prepareBuildCategory(Category category, File buildFolder) throws MojoExecutionException {
        try {
            File ret = new File(buildFolder, "category.xml");
            buildFolder.mkdirs();
            Category.write(category, ret);
            return ret;
        } catch (IOException e) {
            throw new MojoExecutionException("I/O exception while writing category definition to disk", e);
        }
    }

    private List<Category> getCategories() {
        return getEclipseRepositoryProject().loadCategories(getProject());
    }
}
