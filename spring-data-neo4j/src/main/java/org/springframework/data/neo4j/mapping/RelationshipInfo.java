/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.mapping;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.Field;

public class RelationshipInfo {

    private boolean isMultiple;
    private final Direction direction;
    private final String type;
    private final TypeInformation<?> targetType;
    private final boolean targetsNodes;
    private boolean readonly;
    private Neo4jPersistentEntity targetEntity;

    public Direction getDirection() {
        return direction;
    }

    public String getType() {
        return type;
    }
    public RelationshipType getRelationshipType() {
        return DynamicRelationshipType.withName(type);
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public RelationshipInfo(String type, Direction direction, TypeInformation<?> typeInformation, TypeInformation<?> concreteActualType, Neo4jMappingContext ctx) {
        this.type = type;
        this.direction = direction;
        isMultiple = typeInformation.isCollectionLike();
        targetType = concreteActualType!=null ? concreteActualType : typeInformation.getActualType();
        this.targetEntity = ctx.getPersistentEntity(targetType);
        targetsNodes = targetEntity.isNodeEntity();
        this.readonly = isMultiple() && typeInformation.getType().equals(Iterable.class);
    }

    public static RelationshipInfo fromField(Field field, TypeInformation<?> typeInformation, Neo4jMappingContext ctx) {
        return new RelationshipInfo(field.getName(), Direction.OUTGOING, typeInformation,null, ctx);
    }

    public static RelationshipInfo fromField(Field field, RelatedTo annotation, TypeInformation<?> typeInformation, Neo4jMappingContext ctx) {
        return new RelationshipInfo(
                annotation.type().isEmpty() ? field.getName() : annotation.type(),
                annotation.direction(),
                typeInformation,
                annotation.elementClass() != Object.class ? ClassTypeInformation.from(annotation.elementClass()) : null,
                ctx
        );
    }

    public static RelationshipInfo fromField(Field field, RelatedToVia annotation, TypeInformation<?> typeInformation, Neo4jMappingContext ctx) {
        final TypeInformation<?> elementClass = elementClass(annotation, typeInformation);
        return new RelationshipInfo(
                relationshipType(field,annotation,typeInformation),
                annotation.direction(),
                typeInformation,
                elementClass,
                ctx
        );
    }

    private static String relationshipType(Field field, RelatedToVia annotation, TypeInformation<?> typeInformation) {
        if (!annotation.type().isEmpty()) return annotation.type();
        final TypeInformation<?> relationshipEntityType = elementClass(annotation, typeInformation);
        final RelationshipEntity relationshipEntity = relationshipEntityType.getType().getAnnotation(RelationshipEntity.class);
        if (!relationshipEntity.type().isEmpty()) return relationshipEntity.type();
        return field.getName();
    }

    private static TypeInformation<?> elementClass(RelatedToVia annotation, TypeInformation<?> typeInformation) {
        return annotation.elementClass() != Object.class ? ClassTypeInformation.from(annotation.elementClass()) : typeInformation.getActualType();
    }

    public TypeInformation<?> getTargetType() {
        return targetType;
    }

    public boolean targetsNodes() {
        return targetsNodes;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public Neo4jPersistentEntity getTargetEntity() {
        return targetEntity;
    }
}
