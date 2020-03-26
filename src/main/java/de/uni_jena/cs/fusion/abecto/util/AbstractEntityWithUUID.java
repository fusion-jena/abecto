/**
 * Copyright © 2019 Heinz Nixdorf Chair for Distributed Information Systems, Friedrich Schiller University Jena (http://fusion.cs.uni-jena.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_jena.cs.fusion.abecto.util;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.Transient;

import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract base class for entities providing an UUID based ID and
 * implementations of {@link #equals(Object)} and {@link #hashCode()}, as well
 * as some performance improvements. It is based on <a href=
 * "https://jivimberg.io/blog/2018/11/05/using-uuid-on-spring-data-jpa-entities/">
 * this article</a>.
 */
@MappedSuperclass
public abstract class AbstractEntityWithUUID implements Persistable<UUID> {

	@Id
	@Column(length = 16, unique = true, nullable = false)
	protected UUID id = UUID.randomUUID();

	@Transient
	private Boolean persisted = false;

	@Override
	public boolean equals(Object obj) {
		return this == obj
				|| (obj instanceof AbstractEntityWithUUID && this.id.equals(((AbstractEntityWithUUID) obj).id));
	}

	@Override
	public UUID getId() {
		return this.id;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	@JsonIgnore
	public boolean isNew() {
		return !this.persisted;
	}

	@PostLoad
	@PostPersist
	protected void setPersisted() {
		this.persisted = true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[id=" + this.getId().toString() + "]";
	}

}
