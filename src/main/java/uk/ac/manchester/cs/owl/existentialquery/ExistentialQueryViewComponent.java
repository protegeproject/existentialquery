package uk.ac.manchester.cs.owl.existentialquery;

import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.owl.model.classexpression.OWLExpressionParserException;
import org.protege.editor.owl.model.inference.OWLReasonerManager;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.protege.editor.owl.ui.clsdescriptioneditor.OWLAutoCompleter;
import org.protege.editor.owl.ui.clsdescriptioneditor.OWLExpressionCheckerFactory;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.protege.editor.owl.ui.view.AbstractActiveOntologyViewComponent;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 14-Apr-2010
 */
public class ExistentialQueryViewComponent extends AbstractActiveOntologyViewComponent {

    public static final String PROPERTY_DELIM = ",";

    private JList resultsList;

    private JTextField classExpressionField;

    private JTextField propertyField;

    private JCheckBox fillerTreatmentCheckbox = new JCheckBox("Return most specific fillers");

    @Override
    protected void initialiseOntologyView() throws Exception {
        setLayout(new BorderLayout(5, 5));
        resultsList = new JList();
        classExpressionField = new AugmentedJTextField("Enter subclass class expression");

        final OWLExpressionCheckerFactory owlExpressionCheckerFactory = getOWLEditorKit().getOWLModelManager().getOWLExpressionCheckerFactory();
        OWLAutoCompleter autoCompleter = new OWLAutoCompleter(getOWLEditorKit(), classExpressionField, owlExpressionCheckerFactory.getOWLClassExpressionChecker());

        propertyField = new AugmentedJTextField("Enter property expressions (use " + PROPERTY_DELIM + " to separate properties in a chain)");
        OWLAutoCompleter propertyAutoCompleter = new OWLAutoCompleter(getOWLEditorKit(), propertyField, owlExpressionCheckerFactory.getPropertySetChecker());

        JPanel inputPanel = new JPanel(new BorderLayout(3, 3));
        inputPanel.add(classExpressionField, BorderLayout.NORTH);
        inputPanel.add(propertyField, BorderLayout.SOUTH);
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.add(inputPanel, BorderLayout.NORTH);
        JButton execButton = new JButton(new AbstractAction("Execute") {
            public void actionPerformed(ActionEvent e) {
                doQuery();
            }
        });
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(execButton, BorderLayout.EAST);
        buttonPanel.add(fillerTreatmentCheckbox, BorderLayout.WEST);
        queryPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(queryPanel, BorderLayout.NORTH);


        add(new JScrollPane(resultsList));
        resultsList.setCellRenderer(new OWLCellRenderer(getOWLEditorKit()));

        fillerTreatmentCheckbox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                doQuery();
            }
        });


    }

    private void doQuery() {



        resultsList.setListData(new Object[0]);
        String className = classExpressionField.getText().trim();
        if (className.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a class expression");
            return;
        }
        String propName = propertyField.getText().trim();
        if (propName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an object property expression");
            return;
        }
        OWLClassExpression ce = null;
        try {
            ce = getOWLModelManager().getOWLExpressionCheckerFactory().getOWLClassExpressionChecker().createObject(className);
        }
        catch (OWLExpressionParserException e) {
            JOptionPane.showMessageDialog(this, "Error in class expression", "Error in class expression", JOptionPane.ERROR_MESSAGE);
            return;
        }
        StringTokenizer tokenizer = new StringTokenizer(propName, PROPERTY_DELIM, false);
        ArrayList<OWLObjectPropertyExpression> props = new ArrayList<OWLObjectPropertyExpression>();
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken().trim();
            OWLObjectProperty prop = getOWLModelManager().getOWLEntityFinder().getOWLObjectProperty(tok);
            if (prop != null) {
                props.add(prop);
            }
        }
        if (ce == null) {
            System.out.println("Couldn't find class");
            return;
        }

        final OWLReasonerManager owlReasonerManager = getOWLModelManager().getOWLReasonerManager();
        ReasonerStatus reasonerStatus = owlReasonerManager.getReasonerStatus();
        if(reasonerStatus == ReasonerStatus.NO_REASONER_FACTORY_CHOSEN) {
            JOptionPane.showMessageDialog(this, "Please select a reasoner from the Reasoner menu", "No reasoner selected", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(reasonerStatus == ReasonerStatus.REASONER_NOT_INITIALIZED) {
            JOptionPane.showMessageDialog(this, "Please select Start Reasoner from the Reasoner menu", "Reasoner not started", JOptionPane.ERROR_MESSAGE);
            return;
        }

        OWLOntology ont = getOWLModelManager().getActiveOntology();

        OWLReasoner owlReasoner = owlReasonerManager.getCurrentReasoner();
        final FillerTreatment fillerTreatment = fillerTreatmentCheckbox.isSelected() ? FillerTreatment.MOST_SPECIFIC : FillerTreatment.ALL;
        OWLExistentialReasonerImpl reasoner = new OWLExistentialReasonerImpl(ont, owlReasoner, fillerTreatment);
        NodeSet<OWLClass> clses = reasoner.getFillers(ce, props);
        resultsList.setListData(clses.getFlattened().toArray());

    }

    @Override
    protected void disposeOntologyView() {
    }

    @Override
    protected void updateView(OWLOntology ontology) throws Exception {
    }
}
