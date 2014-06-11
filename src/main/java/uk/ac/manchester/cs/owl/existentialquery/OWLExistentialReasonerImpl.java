package uk.ac.manchester.cs.owl.existentialquery;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;

import java.util.*;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 14-Apr-2010
 */
/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Information Management Group<br>
 * Date: 26-Nov-2009
 */
public class OWLExistentialReasonerImpl implements OWLExistentialReasoner {

    private OWLOntology ontology;

    private OWLReasoner reasoner;

    private FillerTreatment fillerTreatment = FillerTreatment.MOST_SPECIFIC;

    public OWLExistentialReasonerImpl(OWLOntology ontology, OWLReasoner reasoner, FillerTreatment fillerTreatment) {
        this.ontology = ontology;
        this.reasoner = reasoner;
        this.fillerTreatment = fillerTreatment;
    }

    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, OWLObjectPropertyExpression property) {
        Set<Node<OWLClass>> result = new HashSet<Node<OWLClass>>();
        computeExistentialFillers(ce, Arrays.asList(property), ontology.getOWLOntologyManager().getOWLDataFactory().getOWLThing(), result);
        return new OWLClassNodeSet(result);
    }

    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, List<OWLObjectPropertyExpression> propertyList) {
        Set<Node<OWLClass>> result = new HashSet<Node<OWLClass>>();
        computeExistentialFillers(ce, propertyList, ontology.getOWLOntologyManager().getOWLDataFactory().getOWLThing(), result);
        return new OWLClassNodeSet(result);
    }

    private void computeExistentialFillers(final OWLClassExpression ce, final List<OWLObjectPropertyExpression> properties, final OWLClass curFiller, final Set<Node<OWLClass>> result) {
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLClassExpression chain = null;
        OWLClassExpression filler = curFiller;
        for(int i = properties.size() - 1; i > -1; i--) {
            OWLObjectPropertyExpression prop = properties.get(i);
            chain = filler = df.getOWLObjectSomeValuesFrom(prop, filler);
        }
        OWLClassExpression testCls = df.getOWLObjectIntersectionOf(ce, chain.getObjectComplementOf());
        if(!reasoner.isSatisfiable(testCls)) {
            // Remove any supers
            if (fillerTreatment.equals(FillerTreatment.MOST_SPECIFIC)) {
                for(Node<OWLClass> resultCls : new ArrayList<Node<OWLClass>>(result)) {
                        OWLClass resultClsRep = resultCls.iterator().next();
                        OWLSubClassOfAxiom ax = df.getOWLSubClassOfAxiom(curFiller, resultClsRep);
                        if(reasoner.isEntailed(ax)) {
                            result.remove(resultCls);
                        }
                }
            }
            // More specific
            Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(curFiller);
            result.add(equivalentClasses);
            for(Node<OWLClass> sub : reasoner.getSubClasses(curFiller, true)) {
                OWLClass representative = sub.getRepresentativeElement();
                computeExistentialFillers(ce, properties, representative, result);
            }
        }
    }
}
