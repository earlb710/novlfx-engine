# Screen Designer Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix four concrete issues in `ScreenDesignerApplication`: remove dead code, harden error display, replace the non-interactive validation text area with a clickable list that navigates to the problem node, and add a filter field above the navigation tree.

**Architecture:** All changes are self-contained edits to `ScreenDesignerApplication.java` and its companion test class `ScreenDesignerApplicationTest.java`. No new files are needed. The validation list reuses the existing `navigationNodeForValidationPath()` logic. The tree filter adds an overload of `buildNavigationTree()` that prunes non-matching nodes.

**Tech Stack:** Java 17, Swing (`JList`, `JTextField`, `DefaultListCellRenderer`, `DocumentListener`), JUnit 5. Build: `./gradlew --no-daemon`.

---

## File Map

| File | Change |
|------|--------|
| `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java` | All four changes below |
| `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java` | Tests for all four changes |

---

## Task 1: Remove dead `promoteTemporary()` method

The method at line 961 is defined but never called. The "Promote Temporary" context-menu action (line 1862) is wired inline and does not invoke this method.

**Files:**
- Modify: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java:961-967`
- Test: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java`

- [ ] **Step 1: Write a failing test that verifies the method does not exist**

Add to `ScreenDesignerApplicationTest`:

```java
@Test
void promoteTemporaryMethodIsRemoved() throws Exception {
    boolean found = false;
    for (java.lang.reflect.Method m : ScreenDesignerApplication.class.getDeclaredMethods()) {
        if ("promoteTemporary".equals(m.getName())) {
            found = true;
            break;
        }
    }
    assertFalse(found, "Dead promoteTemporary() method should be removed");
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.promoteTemporaryMethodIsRemoved"
```

Expected: FAIL — `found` is `true` because the method exists.

- [ ] **Step 3: Remove the dead method**

In `ScreenDesignerApplication.java`, delete lines 961–967:

```java
    private void promoteTemporary() {
        String itemId = JOptionPane.showInputDialog("Temporary item id to promote");
        if (itemId != null && !itemId.isBlank()) {
            design = ScreenDesignService.promoteTemporaryItem(design, itemId);
            refreshAll();
        }
    }
```

- [ ] **Step 4: Run the test to confirm it passes, then run the full test class**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest"
```

Expected: all tests PASS.

- [ ] **Step 5: Compile-check**

```
./gradlew --no-daemon compileJava testClasses
```

- [ ] **Step 6: Commit**

```
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java
git commit -m "Remove dead promoteTemporary() method in ScreenDesignerApplication"
```

---

## Task 2: Fix `runSafely()` swallowing null exception messages

`exception.getMessage()` returns `null` for `NullPointerException` and some other exceptions, causing the dialog to show the word "null". Extract a static helper so it can be tested independently.

**Files:**
- Modify: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java:1375-1385`
- Test: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java`

- [ ] **Step 1: Write a failing test for the error message helper**

Add to `ScreenDesignerApplicationTest`:

```java
@Test
void runSafelyErrorMessageFallsBackToClassNameWhenMessageIsNull() {
    NullPointerException nullMessage = new NullPointerException();
    IllegalArgumentException withMessage = new IllegalArgumentException("bad input");

    assertEquals("NullPointerException", ScreenDesignerApplication.errorDisplayMessage(nullMessage));
    assertEquals("bad input", ScreenDesignerApplication.errorDisplayMessage(withMessage));
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.runSafelyErrorMessageFallsBackToClassNameWhenMessageIsNull"
```

Expected: FAIL — `errorDisplayMessage` does not exist yet.

- [ ] **Step 3: Add the static helper and update `runSafely()`**

In `ScreenDesignerApplication.java`, replace `runSafely()` and add the helper immediately after it:

```java
private void runSafely(String actionName, Runnable action) {
    try {
        action.run();
    } catch (RuntimeException exception) {
        String message = errorDisplayMessage(exception);
        statusLabel.setText(actionName + " failed: " + message);
        JOptionPane.showMessageDialog(null,
                message,
                actionName + " Error",
                JOptionPane.ERROR_MESSAGE);
    }
}

static String errorDisplayMessage(RuntimeException exception) {
    String message = exception.getMessage();
    return message != null ? message : exception.getClass().getSimpleName();
}
```

- [ ] **Step 4: Run the test, then run the full test class**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest"
```

Expected: all tests PASS.

- [ ] **Step 5: Compile-check**

```
./gradlew --no-daemon compileJava testClasses
```

- [ ] **Step 6: Commit**

```
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java
git commit -m "Fix runSafely() showing null when exception message is absent"
```

---

## Task 3: Replace validation `JTextArea` with clickable `JList`

Currently `validationArea` is a read-only `JTextArea`. Clicking a problem should select the corresponding node in the navigation tree. The `navigationNodeForValidationPath()` method already does the path → node lookup; this task wires it up.

**Files:**
- Modify: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java`
  - Field declaration (~line 244)
  - `editor()` method (~line 425)
  - `refreshAll()` method (~line 1126)
- Test: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java`

- [ ] **Step 1: Write a failing test for the validation list field type**

Add to `ScreenDesignerApplicationTest`:

```java
@Test
void validationPanelUsesClickableListNotTextArea() throws Exception {
    ScreenDesignerApplication application = new ScreenDesignerApplication();

    Object field = fieldValue(application, "validationList");

    assertInstanceOf(JList.class, field);
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.validationPanelUsesClickableListNotTextArea"
```

Expected: FAIL — field `validationList` does not exist.

- [ ] **Step 3: Replace the `validationArea` field declaration**

Find:
```java
private final JTextArea validationArea = new JTextArea(3, 20);
```

Replace with:
```java
private final JList<ScreenDesignValidationProblem> validationList = new JList<>();
```

- [ ] **Step 4: Update the `editor()` method configuration block**

Find the three lines that configure `validationArea` in `editor()`:
```java
validationArea.setEditable(false);
validationArea.setLineWrap(true);
validationArea.setWrapStyleWord(true);
```

Replace with:
```java
validationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
validationList.setCellRenderer(new ValidationProblemCellRenderer());
validationList.addListSelectionListener(event -> {
    if (!event.getValueIsAdjusting() && validationList.getSelectedValue() != null) {
        selectNavigationNode(navigationNodeForValidationPath(validationList.getSelectedValue().path()));
    }
});
```

- [ ] **Step 5: Swap the `validationArea` scroll pane in the lower split**

Find in `editor()`:
```java
JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(validationArea), new JScrollPane(jsonArea));
```

Replace with:
```java
JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(validationList), new JScrollPane(jsonArea));
```

- [ ] **Step 6: Update `refreshAll()` to populate the list**

Find in `refreshAll()`:
```java
validationArea.setText(validationSummary(problems));
```

Replace with:
```java
validationList.setListData(problems.toArray(ScreenDesignValidationProblem[]::new));
```

- [ ] **Step 7: Add the `ValidationProblemCellRenderer` static inner class**

Add this class inside `ScreenDesignerApplication`, near the existing `ValidationTreeCellRenderer` class:

```java
private static final class ValidationProblemCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof ScreenDesignValidationProblem problem) {
            setText(problem.path() + ": " + problem.message());
        }
        return this;
    }
}
```

- [ ] **Step 8: Run the test, then run the full test class**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest"
```

Expected: all tests PASS. (The `editorPinsPropertyButtonsBelowScrollablePropertiesPanel` test does not inspect the lower split contents, so it remains green.)

- [ ] **Step 9: Compile-check**

```
./gradlew --no-daemon compileJava testClasses
```

- [ ] **Step 10: Commit**

```
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java
git commit -m "Replace validation text area with clickable JList that navigates to problem node"
```

---

## Task 4: Add filter field above the navigation tree

A `JTextField` above the tree lets the user type a string. On each keystroke the tree rebuilds to show only nodes whose ID contains the filter text (case-insensitive). Block nodes are retained whenever they have at least one matching descendant.

**Files:**
- Modify: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java`
  - New instance field `treeFilterField`
  - `navigation()` method
  - New static `buildNavigationTree(ScreenDesignModel, String)` overload
- Test: `src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java`

- [ ] **Step 1: Write a failing test for the filtered tree overload**

Add to `ScreenDesignerApplicationTest`:

```java
@Test
void navigationTreeFilterExcludesNonMatchingNodesButKeepsParentsWithMatchingChildren() {
    ScreenDesignModel design = new ScreenDesignModel(
            "sample.screen",
            "Sample Screen",
            com.eb.javafx.ui.ScreenLayoutType.FORM,
            Map.of(),
            List.of(
                    new ScreenDesignBlock("main", "Main"),
                    new ScreenDesignBlock("secondary", "Secondary")),
            List.of(
                    new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                            "Title", "Saved", null, null, null, Map.of()),
                    new ScreenDesignItem("subtitle.text", "secondary", ScreenDesignItemType.TEXT,
                            "Subtitle", "Saved", null, null, null, Map.of())),
            List.of());

    DefaultMutableTreeNode filtered = ScreenDesignerApplication.buildNavigationTree(design, "subtitle");

    // Screen root always included
    assertEquals("screen: sample.screen", filtered.getUserObject().toString());
    // "main" block has no matching children — excluded
    assertEquals(1, filtered.getChildCount());
    DefaultMutableTreeNode secondaryBlock = (DefaultMutableTreeNode) filtered.getChildAt(0);
    assertEquals("block: secondary", secondaryBlock.getUserObject().toString());
    assertEquals(1, secondaryBlock.getChildCount());
    assertEquals("item: subtitle.text",
            ((DefaultMutableTreeNode) secondaryBlock.getChildAt(0)).getUserObject().toString());
}

@Test
void navigationTreeFilterIsBlankReturnsSameTreeAsNoFilter() {
    ScreenDesignModel design = new ScreenDesignModel(
            "sample.screen",
            "Sample Screen",
            com.eb.javafx.ui.ScreenLayoutType.FORM,
            Map.of(),
            List.of(new ScreenDesignBlock("main", "Main")),
            List.of(new ScreenDesignItem("title.text", "main", ScreenDesignItemType.TEXT,
                    "Title", "Saved", null, null, null, Map.of())),
            List.of());

    DefaultMutableTreeNode unfiltered = ScreenDesignerApplication.buildNavigationTree(design);
    DefaultMutableTreeNode blankFilter = ScreenDesignerApplication.buildNavigationTree(design, "");
    DefaultMutableTreeNode nullFilter  = ScreenDesignerApplication.buildNavigationTree(design, null);

    // Same structure regardless of blank/null filter
    assertEquals(unfiltered.getChildCount(), blankFilter.getChildCount());
    assertEquals(unfiltered.getChildCount(), nullFilter.getChildCount());
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.navigationTreeFilterExcludesNonMatchingNodesButKeepsParentsWithMatchingChildren" --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.navigationTreeFilterIsBlankReturnsSameTreeAsNoFilter"
```

Expected: FAIL — overload `buildNavigationTree(ScreenDesignModel, String)` does not exist.

- [ ] **Step 3: Add the `treeFilterField` instance field**

Near the other instance fields (after `objectTree`), add:

```java
private final JTextField treeFilterField = new JTextField();
```

- [ ] **Step 4: Add the `buildNavigationTree(ScreenDesignModel, String)` overload**

Add immediately after the existing `static DefaultMutableTreeNode buildNavigationTree(ScreenDesignModel design)` method:

```java
static DefaultMutableTreeNode buildNavigationTree(ScreenDesignModel design, String filter) {
    if (filter == null || filter.isBlank()) {
        return buildNavigationTree(design);
    }
    String lower = filter.strip().toLowerCase();

    // Build the full tree first, then prune
    DefaultMutableTreeNode fullRoot = buildNavigationTree(design);

    // Prune block children that have no matching items or sub-blocks (recursively)
    pruneNonMatchingChildren(fullRoot, lower);
    return fullRoot;
}

private static boolean pruneNonMatchingChildren(DefaultMutableTreeNode node, String filter) {
    NavigationNode nav = navigationNodeFor(node).orElse(null);
    if (nav != null && nav.type() == NodeType.SCREEN) {
        // Always keep screen root; prune its block children
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (!pruneNonMatchingChildren(child, filter)) {
                node.remove(i);
            }
        }
        return true;
    }
    if (nav != null && (nav.type() == NodeType.ITEM || nav.type() == NodeType.TEMPORARY_ITEM)) {
        return nav.id().toLowerCase().contains(filter);
    }
    if (nav != null && nav.type() == NodeType.BLOCK) {
        boolean blockMatches = nav.id().toLowerCase().contains(filter);
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (!pruneNonMatchingChildren(child, filter)) {
                node.remove(i);
            }
        }
        return blockMatches || node.getChildCount() > 0;
    }
    return false;
}
```

- [ ] **Step 5: Run the tests to confirm they pass**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.navigationTreeFilterExcludesNonMatchingNodesButKeepsParentsWithMatchingChildren" --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.navigationTreeFilterIsBlankReturnsSameTreeAsNoFilter"
```

Expected: PASS.

- [ ] **Step 6: Write a test confirming the filter row is present in the navigation panel**

Add to `ScreenDesignerApplicationTest`:

```java
@Test
void navigationPanelIncludesFilterFieldAboveTree() throws Exception {
    ScreenDesignerApplication application = new ScreenDesignerApplication();
    invokePrivateMethod(application, "refreshAll");

    JPanel navPanel = (JPanel) invokePrivateMethod(application, "navigation");
    BorderLayout navLayout = (BorderLayout) navPanel.getLayout();
    JPanel northPanel = assertInstanceOf(JPanel.class, navLayout.getLayoutComponent(BorderLayout.NORTH));
    // After the change, northPanel wraps workingDirectoryPanel (NORTH) and filter row (SOUTH)
    BorderLayout northLayout = assertInstanceOf(BorderLayout.class, northPanel.getLayout());
    assertNotNull(northLayout.getLayoutComponent(BorderLayout.SOUTH),
            "Filter row should be present as SOUTH child of the north container");
}
```

- [ ] **Step 7: Run the new layout test to confirm it fails**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest.navigationPanelIncludesFilterFieldAboveTree"
```

Expected: FAIL — before Step 8, `navigation()` places `workingDirectoryPanel()` directly at NORTH, which uses a `BorderLayout` with only NORTH and CENTER children (no SOUTH), so `getLayoutComponent(BorderLayout.SOUTH)` returns null.

- [ ] **Step 8: Update `navigation()` to embed the filter field**

Replace the existing `navigation()` method body:

```java
private JPanel navigation() {
    objectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    objectTree.setRootVisible(true);
    objectTree.setCellRenderer(new ValidationTreeCellRenderer());
    objectTree.addTreeSelectionListener(event -> updateSelectedNavigationState());
    installTreeContextMenu();
    installTreeDragAndDrop();

    treeFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyTreeFilter(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyTreeFilter(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyTreeFilter(); }
    });

    JPanel filterPanel = new JPanel(new BorderLayout(4, 4));
    filterPanel.add(new JLabel("Filter"), BorderLayout.WEST);
    filterPanel.add(treeFilterField, BorderLayout.CENTER);

    JPanel northPanel = new JPanel(new BorderLayout(4, 8));
    northPanel.add(workingDirectoryPanel(), BorderLayout.NORTH);
    northPanel.add(filterPanel, BorderLayout.SOUTH);

    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.add(northPanel, BorderLayout.NORTH);
    panel.add(new JScrollPane(objectTree), BorderLayout.CENTER);
    panel.add(navigationActions(), BorderLayout.SOUTH);
    return panel;
}
```

- [ ] **Step 9: Add the `applyTreeFilter()` private method**

Add after `navigation()`:

```java
private void applyTreeFilter() {
    String filter = treeFilterField.getText();
    objectTreeModel.setRoot(buildNavigationTree(design, filter));
    for (int row = 0; row < objectTree.getRowCount(); row++) {
        objectTree.expandRow(row);
    }
}
```

- [ ] **Step 10: Run the full test class**

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest"
```

Expected: all tests PASS.

- [ ] **Step 11: Compile-check**

```
./gradlew --no-daemon compileJava testClasses
```

- [ ] **Step 12: Commit**

```
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplication.java
git add src/test/java/com/eb/javafx/testscreen/ScreenDesignerApplicationTest.java
git commit -m "Add filter field above navigation tree in Screen Designer"
```

---

## Final verification

- [ ] Run the full test class one last time to confirm all four tasks are clean together:

```
./gradlew --no-daemon test --tests "com.eb.javafx.testscreen.ScreenDesignerApplicationTest"
```

- [ ] Write change summary per project convention:

```
git mv change_summary.md change_summary_claude_awesome_hofstadter_db0681.md   # if it exists
# otherwise create it
```

The summary should note: dead code removal, null-safe error display, clickable validation list, tree filter field.
