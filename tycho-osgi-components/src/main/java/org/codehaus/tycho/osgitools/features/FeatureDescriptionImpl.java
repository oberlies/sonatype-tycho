package org.codehaus.tycho.osgitools.features;

import java.io.File;

import org.codehaus.tycho.model.Feature;
import org.osgi.framework.Version;

public class FeatureDescriptionImpl implements FeatureDescription {

	private Version version;

	private String name;

	private File location;

	public FeatureDescriptionImpl(String name, Version version, File location) {
		super();
		this.name = name;
		this.version = version;
		this.location = location;
	}

	public FeatureDescriptionImpl(Feature feature, File location) {
		this(feature.getId(), new Version(feature.getVersion()), location);
	}

	public File getLocation() {
		return this.location;
	}

	public String getName() {
		return this.name;
	}

	public Version getVersion() {
		return this.version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FeatureDescription))
			return false;
		FeatureDescription other = (FeatureDescription) obj;
		if (name == null) {
			if (other.getName() != null)
				return false;
		} else if (!name.equals(other.getName()))
			return false;
		if (version == null) {
			if (other.getVersion() != null)
				return false;
		} else if (!version.equals(other.getVersion()))
			return false;
		return true;
	}

}
