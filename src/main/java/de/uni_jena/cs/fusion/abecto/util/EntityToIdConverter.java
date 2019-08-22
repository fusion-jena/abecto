package de.uni_jena.cs.fusion.abecto.util;

import java.util.UUID;

import com.fasterxml.jackson.databind.util.StdConverter;

public class EntityToIdConverter extends StdConverter<AbstractEntityWithUUID, UUID> {

	@Override
	public UUID convert(AbstractEntityWithUUID entity) {
		return entity.getId();
	}

}
