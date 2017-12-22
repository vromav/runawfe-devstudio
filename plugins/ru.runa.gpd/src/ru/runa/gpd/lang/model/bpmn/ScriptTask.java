package ru.runa.gpd.lang.model.bpmn;

import java.util.List;

import ru.runa.gpd.extension.HandlerArtifact;
import ru.runa.gpd.lang.model.Delegable;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.Transition;

public class ScriptTask extends Node implements Delegable, IBoundaryEventContainer {

    public ScriptTask() {
        super();
        this.font = P_BPMN_SCRIPTTASK_FONT;
        this.fontColor = P_BPMN_SCRIPTTASK_FONT_COLOR;
        this.backgroundColor = P_BPMN_SCRIPTTASK_BACKGROUND_COLOR;
        this.baseColor = P_BPMN_SCRIPTTASK_BASE_COLOR;
    }

    @Override
    public String getDelegationType() {
        return HandlerArtifact.ACTION;
    }

    @Override
    protected boolean allowLeavingTransition(List<Transition> transitions) {
        return super.allowLeavingTransition(transitions) && transitions.size() == 0;
    }

}
