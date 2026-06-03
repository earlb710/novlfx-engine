module com.example.starter {
    requires com.novlfx.engine;
    requires javafx.controls;
    requires javafx.graphics;

    // Lets the engine discover the provider via ServiceLoader on the module path.
    // (On the class path, the equivalent is the file
    //  src/main/resources/META-INF/services/com.eb.javafx.bootstrap.EngineModuleProvider.)
    provides com.eb.javafx.bootstrap.EngineModuleProvider
            with com.example.starter.StarterModuleProvider;
}
