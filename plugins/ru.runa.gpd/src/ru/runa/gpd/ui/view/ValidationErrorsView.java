package ru.runa.gpd.ui.view;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginConstants;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.editor.ProcessEditorBase;
import ru.runa.gpd.lang.model.Action;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.NamedGraphElement;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.jpdl.ActionContainer;
import ru.runa.gpd.util.UiUtil;
import ru.runa.gpd.util.WorkspaceOperations;

public class ValidationErrorsView extends ViewPart implements ISelectionChangedListener {
    public static final String ID = "ru.runa.gpd.validationErrors";
    static final String[] COLUMN_NAMES = { Localization.getString("ValidationErrorsView.Source"),
            Localization.getString("ValidationErrorsView.Message"), Localization.getString("ValidationErrorsView.ProcessName") };
    private TableViewer viewer;

    @Override
    public void createPartControl(Composite parent) {
        UiUtil.hideToolBar(getViewSite());
        viewer = multiColumnViewer(parent);
        viewer.setContentProvider(new MarkerContentProvider());
        viewer.setLabelProvider(new MarkerLabelProvider());
        viewer.setSorter(new ViewerSorter());
        viewer.setInput(ResourcesPlugin.getWorkspace());
        viewer.addSelectionChangedListener(this);
    }

    private TableViewer multiColumnViewer(Composite parent) {
        Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        TableLayout layout = new TableLayout();
        table.setLayout(layout);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        layout.addColumnData(new ColumnWeightData(1, 20, true));
        new TableColumn(table, SWT.LEFT);
        layout.addColumnData(new ColumnWeightData(5, 40, true));
        TableColumn tc1 = new TableColumn(table, SWT.LEFT);
        tc1.setText(COLUMN_NAMES[0]);
        layout.addColumnData(new ColumnWeightData(10, true));
        TableColumn tc2 = new TableColumn(table, SWT.LEFT);
        tc2.setText(COLUMN_NAMES[1]);
        layout.addColumnData(new ColumnWeightData(10, true));
        TableColumn tc3 = new TableColumn(table, SWT.LEFT);
        tc3.setText(COLUMN_NAMES[2]);
        return new TableViewer(table);
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        IMarker marker = (IMarker) ((StructuredSelection) viewer.getSelection()).getFirstElement();
        if (marker == null) {
            return;
        }
        try {
            IFile definitionFile = (IFile) marker.getResource();
            ProcessEditorBase editor = WorkspaceOperations.openProcessDefinition(definitionFile);
            GraphElement graphElement = null;
            String elementId = marker.getAttribute(PluginConstants.SELECTION_LINK_KEY, null);
            if (elementId != null) {
                graphElement = editor.getDefinition().getGraphElementById(elementId);
            }
            String swimlaneName = marker.getAttribute(PluginConstants.SWIMLANE_LINK_KEY, null);
            if (swimlaneName != null) {
                graphElement = findElement(editor.getDefinition(), Swimlane.class, swimlaneName);
            }
            int actionIndex = marker.getAttribute(PluginConstants.ACTION_INDEX_KEY, -1);
            if (actionIndex != -1) {
                String parentTreePath = marker.getAttribute(PluginConstants.PARENT_NODE_KEY, null);
                String[] paths = parentTreePath.split("\\|", -1);
                ActionContainer actionContainer;
                if (paths.length == 1) {
                    actionContainer = (ActionContainer) findElement(editor.getDefinition(), NamedGraphElement.class, paths[0]);
                } else if (paths.length == 2) {
                    Node node = (Node) findElement(editor.getDefinition(), Node.class, paths[0]);
                    actionContainer = node.getTransitionByName(paths[1]);
                } else {
                    throw new RuntimeException("Invalid tree path: " + parentTreePath);
                }
                List<? extends Action> activeActions = actionContainer.getActions();
                graphElement = activeActions.get(actionIndex);
            }
            if (graphElement != null) {
                editor.select(graphElement);
            }
        } catch (Exception e) {
            // don't display error to user
            PluginLogger.logErrorWithoutDialog("Unable select element", e);
        }
    }

    private GraphElement findElement(ProcessDefinition definition, Class<? extends NamedGraphElement> clazz, String elementName) {
        List<? extends NamedGraphElement> elements = definition.getChildrenRecursive(clazz);
        for (NamedGraphElement element : elements) {
            if (elementName.equals(element.getName())) {
                return element;
            }
        }
        return null;
    }

    static class MarkerLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object obj, int index) {
            IMarker marker = (IMarker) obj;
            switch (index) {
            case 3:
                return marker.getAttribute(PluginConstants.PROCESS_NAME_KEY, "Undefined");
            case 2:
                return marker.getAttribute(IMarker.MESSAGE, "Undefined");
            case 1:
                return marker.getAttribute(IMarker.LOCATION, "Undefined");
            default:
                return "";
            }
        }

        @Override
        public Image getColumnImage(Object obj, int columnIndex) {
            if (columnIndex == 0) {
                IMarker marker = (IMarker) obj;
                int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                if (severity == IMarker.SEVERITY_ERROR) {
                    return SharedImages.getImage("icons/column_error.gif");
                }
                if (severity == IMarker.SEVERITY_WARNING) {
                    return SharedImages.getImage("icons/column_warning.gif");
                }
            }
            return null;
        }
    }

    static class MarkerContentProvider implements IStructuredContentProvider, IResourceChangeListener {
        private StructuredViewer viewer;
        private IWorkspace input = null;

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            if (viewer == null) {
                this.viewer = (StructuredViewer) v;
            }
            if (input == null && newInput != null) {
                input = (IWorkspace) newInput;
                input.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
            }
            if (newInput == null && input != null) {
                input.removeResourceChangeListener(this);
                input = null;
            }
        }

        @Override
        public void dispose() {
            if (input != null) {
                input.removeResourceChangeListener(this);
                input = null;
            }
        }

        @Override
        public Object[] getElements(Object parent) {
            try {
                return input.getRoot().findMarkers(ValidationErrorsView.ID, false, IResource.DEPTH_INFINITE);
            } catch (CoreException e) {
                return null;
            }
        }

        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            final Control ctrl = viewer.getControl();
            IMarkerDelta[] mDeltas = event.findMarkerDeltas(ValidationErrorsView.ID, false);
            if (mDeltas.length != 0) {
                try {
                    ctrl.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                viewer.refresh();
                            } catch (Exception e) {
                                // widget is disposed
                            }
                        }
                    });
                } catch (Exception e) {
                    // widget is disposed
                }
            }
        }
    }
}
