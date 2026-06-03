module com.novlfx.engine {
    requires java.prefs;
    requires javafx.controls;
    requires javafx.media;
    requires javafx.swing;
    requires java.desktop;
    requires batik.all;
    requires org.girod.javafx.svgimage;
    requires org.fxyz3d.core;
    requires xml.apis.ext;
    requires de.javagl.jgltf.model;

    exports com.eb.javafx.achievements;
    exports com.eb.javafx.audio;
    exports com.eb.javafx.accessibility;
    exports com.eb.javafx.assets;
    exports com.eb.javafx.bootstrap;
    exports com.eb.javafx.characters;
    exports com.eb.javafx.content;
    exports com.eb.javafx.debug;
    exports com.eb.javafx.diagnostics;
    exports com.eb.javafx.display;
    exports com.eb.javafx.events;
    exports com.eb.javafx.gamesupport;
    exports com.eb.javafx.gltf;
    exports com.eb.javafx.input;
    exports com.eb.javafx.inventory;
    exports com.eb.javafx.journal;
    exports com.eb.javafx.localization;
    exports com.eb.javafx.messages;
    exports com.eb.javafx.organizations;
    exports com.eb.javafx.prefs;
    exports com.eb.javafx.progress;
    exports com.eb.javafx.random;
    exports com.eb.javafx.resources;
    exports com.eb.javafx.globalApi;
    exports com.eb.javafx.routing;
    exports com.eb.javafx.save;
    exports com.eb.javafx.scene;
    exports com.eb.javafx.settings;
    exports com.eb.javafx.state;
    exports com.eb.javafx.text;
    exports com.eb.javafx.timeline;
    exports com.eb.javafx.ui;
    exports com.eb.javafx.util;
    exports com.eb.javafx.transitions;
    exports com.eb.javafx.gallery;
    exports com.eb.javafx.storyline;
    // com.eb.javafx.testscreen holds internal developer tooling / editor applications (screen
    // designer, conversation editor, code-table management, etc.), not a general consumer-facing
    // API, so it is NOT exported to the world. The Gradle run tasks launch them from the test
    // runtime CLASSPATH (not the module path), so module exports don't affect those.
    //
    // It IS qualified-exported to the reference admin app (com.altlife.javafx), whose admin screen
    // reflectively launches ManagementApplication.launchEmbedded() as a dev-tooling feature. A
    // qualified export keeps the package out of the published API surface for arbitrary consumers
    // while letting that one trusted module reach the embedded management tool.
    exports com.eb.javafx.testscreen to com.altlife.javafx;

    // -------------------------------------------------------------------------------------
    // `opens` for packages that ship BOTH .class files and resources.
    //
    // JPMS rules: a package containing .class files is part of the module's package set,
    // and resources inside such packages are encapsulated unless the package is `opens`d.
    // `exports` alone makes the package's public types accessible but doesn't unlock
    // resources — Class.getResourceAsStream("/com/eb/javafx/<pkg>/file") would return null
    // even when called from a class in the same module.
    //
    // Packages with .class files but resource-loading callsites:
    //   - gamesupport: system-code-tables.en.json (loaded by SystemCodeTables.defaultDefinition)
    //   - ui:          default.css, display-defaults.json, layout-contract.json, mesh files
    //
    // (Packages with resources but no .class files — e.g. com.eb.javafx.ui.screens — are
    //  NOT module packages from JPMS's perspective and don't need opens.)
    opens com.eb.javafx.gamesupport;
    opens com.eb.javafx.ui;

    // Extension-discovery SPI: a consumer declares `provides EngineModuleProvider with ...` (or a
    // classpath META-INF/services file) and the engine discovers them via ServiceLoader during
    // bootstrap assembly (BootstrapOptions.discovering).
    uses com.eb.javafx.bootstrap.EngineModuleProvider;
}
