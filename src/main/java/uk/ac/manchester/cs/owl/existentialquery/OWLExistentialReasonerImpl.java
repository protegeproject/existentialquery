package uk.ac.manchester.cs.owl.existentialquery;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

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

    private OWLReasoner reasoner;

    private FillerTreatment fillerTreatment = FillerTreatment.MOST_SPECIFIC;

    /**
     * Constructs an existential reasoner.
     * @param reasoner The delegate reasoner.  Not {@code null}.
     * @param fillerTreatment The {@link FillerTreatment}. Not {@code null}.
     * @throws  NullPointerException if any parameters are {@code null}.
     */
    public OWLExistentialReasonerImpl(OWLReasoner reasoner, FillerTreatment fillerTreatment) {
        this.reasoner = checkNotNull(reasoner);
        this.fillerTreatment = checkNotNull(fillerTreatment);
    }

    /**
     * Gets the fillers of the existential restrictions that are entailed to be superclasses the specified class
     * expression and act along the specified property.  In essence, this finds bindings for ?x with respect to
     * the following template: <code>SubClassOf(ce ObjectSomeValuesFrom(property, ?x))</code>
     * @param ce The class expression.  Not {@code null}.
     * @param property The property expression.  Not {@code null}.
     * @return A set of class expressions that are the entailed fillers of entailed existential restriction superclasses.
     */
    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, OWLObjectPropertyExpression property) {
        Set<Node<OWLClass>> result = new HashSet<Node<OWLClass>>();
        computeExistentialFillers(ce, Arrays.asList(property), getDataFactory().getOWLThing(), result);
        return new OWLClassNodeSet(result);
    }

    /**
     * Gets the fillers of the existential restrictions that are entailed to be superclasses the specified class
     * expression and act along the specified property chain.  In essence, this finds bindings for ?x with respect to
     * the following template: <code>SubClassOf(ce ObjectSomeValuesFrom(p1, (ObjectSomeValuesFrom(p2 ?x)))</code> for
     * arbitrary chains of properties.
     * @param ce The class expression.  Not {@code null}.
     * @param propertyList A list of property expressions that constitute a property chain.
     * @return A set of class expressions that are the entailed fillers of entailed existential restriction superclasses.
     */
    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, List<OWLObjectPropertyExpression> propertyList) {
        Set<Node<OWLClass>> result = new HashSet<Node<OWLClass>>();
        computeExistentialFillers(ce, propertyList, getDataFactory().getOWLThing(), result);
        return new OWLClassNodeSet(result);
    }

    private void computeExistentialFillers(final OWLClassExpression ce, final List<OWLObjectPropertyExpression> properties, final OWLClass curFiller, final Set<Node<OWLClass>> result) {
        if(properties.isEmpty()) {
            throw new IllegalArgumentException("properties must not be empty");
        }
        final OWLDataFactory df = getDataFactory();
        OWLClassExpression chain = null;
        OWLClassExpression filler = curFiller;
        for(int i = properties.size() - 1; i > -1; i--) {
            OWLObjectPropertyExpression prop = properties.get(i);
            chain = filler = df.getOWLObjectSomeValuesFrom(prop, filler);
        }
        final OWLClassExpression testCls = df.getOWLObjectIntersectionOf(ce, chain.getObjectComplementOf());
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


    private OWLDataFactory getDataFactory() {
        return reasoner.getRootOntology().getOWLOntologyManager().getOWLDataFactory();
    }
}
