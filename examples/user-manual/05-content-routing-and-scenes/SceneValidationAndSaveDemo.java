import com.eb.javafx.save.SaveSnapshotDocument;
import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneFlowSnapshotDocuments;
import com.eb.javafx.scene.SceneFlowState;
import com.eb.javafx.scene.SceneGraphSummary;
import com.eb.javafx.scene.SceneReferenceValidators;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.scene.SceneReturnPoint;
import com.eb.javafx.scene.SceneStep;
import com.eb.javafx.scene.SceneTransition;
import com.eb.javafx.scene.SceneValidationReport;

import java.util.List;
import java.util.Set;

public final class SceneValidationAndSaveDemo {
    public static void main(String[] args) {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(
                SceneStep.dialogue("line", "speaker.demo", "intro.line", "display.demo"),
                SceneStep.transition("done", SceneTransition.complete()),
                SceneStep.narration("unused", "intro.unused"))));

        SceneValidationReport report = registry.validationReport(List.of(
                SceneReferenceValidators.knownSpeakers(Set.of("speaker.demo")),
                SceneReferenceValidators.knownDisplayReferences(Set.of("display.demo"))));
        SceneGraphSummary summary = report.summaries().get(0);
        System.out.println("Scene: " + summary.sceneId());
        System.out.println("Reachable steps: " + summary.reachableStepIds());
        System.out.println("Warnings: " + report.problems().size());

        SceneFlowState state = new SceneFlowState("intro", 1,
                List.of(new SceneReturnPoint("menu", 0)), List.of(), null);
        SaveSnapshotDocument document = SceneFlowSnapshotDocuments.compose(state, List.of(
                new SaveSnapshotSection("appPreview", 1, "{\"label\":\"Demo\"}")));
        System.out.println("Sections: " + document.sections().size());
        System.out.println("Restored scene: " + SceneFlowSnapshotDocuments.restore(document).activeSceneId());
    }
}
