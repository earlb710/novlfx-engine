package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Deterministic registry extensions for reusable generic descriptors. */
public final class GenericDescriptorRegistry {
    private final DefinitionRegistry<GenericDescriptor> registry = new DefinitionRegistry<>("Generic descriptor");

    public void register(GenericDescriptor descriptor) {
        registry.register(descriptor);
    }

    public Optional<GenericDescriptor> descriptor(String id) {
        return registry.definition(id);
    }

    public List<GenericDescriptor> descriptors() {
        return registry.definitions();
    }

    public List<GenericDescriptor> descriptorsByKind(String kind) {
        String checkedKind = Validation.requireNonBlank(kind, "Descriptor kind is required.");
        return descriptors().stream()
                .filter(descriptor -> descriptor.kind().equals(checkedKind))
                .toList();
    }

    public List<GenericDescriptor> descriptorsWithTag(String tag) {
        String checkedTag = Validation.requireNonBlank(tag, "Descriptor tag is required.");
        return descriptors().stream()
                .filter(descriptor -> descriptor.tags().contains(checkedTag))
                .toList();
    }

    public void requireKnownIds(Collection<String> ids) {
        Validation.requireNonNull(ids, "Descriptor ids are required.");
        for (String id : ids) {
            String checkedId = Validation.requireNonBlank(id, "Descriptor id is required.");
            if (registry.definition(checkedId).isEmpty()) {
                throw new IllegalArgumentException("Unknown generic descriptor: " + checkedId);
            }
        }
    }
}
