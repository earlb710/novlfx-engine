Reserved classpath directory for engine bootstrap resources.

The `bootstrap` resource category in the engine `config.json` points here. Add bootstrap-only resources (engine startup data files, packaged defaults that should not appear in other categories) under this directory; `ResourceRegistry` walks it recursively and indexes every regular file.
