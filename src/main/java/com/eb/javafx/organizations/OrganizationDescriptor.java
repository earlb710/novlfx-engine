package com.eb.javafx.organizations;

import com.eb.javafx.gamesupport.IdentifiedDefinition;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Generic organization/faction descriptor without authored behavior. */
public record OrganizationDescriptor(
        String id,
        String title,
        List<String> memberIds,
        List<String> tags,
        Map<String, String> metadata) implements IdentifiedDefinition {
    public OrganizationDescriptor {
        id = Validation.requireNonBlank(id, "Organization id is required.");
        title = Validation.requireNonBlank(title, "Organization title is required.");
        memberIds = ImmutableCollections.copyList(memberIds);
        memberIds.forEach(memberId -> Validation.requireNonBlank(memberId, "Organization member id is required."));
        tags = ImmutableCollections.copyList(tags);
        tags.forEach(tag -> Validation.requireNonBlank(tag, "Organization tag is required."));
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
