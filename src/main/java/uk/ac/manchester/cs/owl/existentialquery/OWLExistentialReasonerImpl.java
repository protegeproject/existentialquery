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

    private int entailmentCheckCount = 0;

    /**
     * Constructs an existential reasoner.
     *
     * @param reasoner        The delegate reasoner.  Not {@code null}.
     * @param fillerTreatment The {@link FillerTreatment}. Not {@code null}.
     * @throws NullPointerException if any parameters are {@code null}.
     */
    public OWLExistentialReasonerImpl(OWLReasoner reasoner, FillerTreatment fillerTreatment) {
        this.reasoner = checkNotNull(reasoner);
        this.fillerTreatment = checkNotNull(fillerTreatment);
    }

    /**
     * Gets the fillers of the existential restrictions that are entailed to be superclasses the specified class
     * expression and act along the specified property.  In essence, this finds bindings for ?x with respect to
     * the following template: <code>SubClassOf(ce ObjectSomeValuesFrom(property, ?x))</code>
     *
     * @param ce       The class expression.  Not {@code null}.
     * @param property The property expression.  Not {@code null}.
     * @return A set of class expressions that are the entailed fillers of entailed existential restriction superclasses.
     */
    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, OWLObjectPropertyExpression property) {
        return getFillers(ce, Arrays.asList(property));
    }

    /**
     * Gets the fillers of the existential restrictions that are entailed to be superclasses the specified class
     * expression and act along the specified property chain.  In essence, this finds bindings for ?x with respect to
     * the following template: <code>SubClassOf(ce ObjectSomeValuesFrom(p1, (ObjectSomeValuesFrom(p2 ?x)))</code> for
     * arbitrary chains of properties.
     *
     * @param ce           The class expression.  Not {@code null}.
     * @param propertyList A list of property expressions that constitute a property chain.
     * @return A set of class expressions that are the entailed fillers of entailed existential restriction superclasses.
     */
    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, List<OWLObjectPropertyExpression> propertyList) {
        Set<Node<OWLClass>> result = new HashSet<Node<OWLClass>>();
        entailmentCheckCount = 0;
        computeExistentialFillers(ce, propertyList, getDataFactory().getOWLThing(), result, new HashSet<OWLClass>());
        NodeSet<OWLClass> nodeSetResult = new OWLClassNodeSet(result);
        if (fillerTreatment == FillerTreatment.ALL) {
            return nodeSetResult;
        }
        Set<Node<OWLClass>> removed = new HashSet<Node<OWLClass>>();
        Set<Node<OWLClass>> finalResult = new HashSet<Node<OWLClass>>(nodeSetResult.getNodes());
        for (Node<OWLClass> resultCls : result) {
            OWLClass resultClsRep = resultCls.getRepresentativeElement();
            if (!removed.contains(resultCls)) {
                removed.add(resultCls);
                NodeSet<OWLClass> supers = reasoner.getSuperClasses(resultClsRep, false);
                removed.addAll(supers.getNodes());
                finalResult.removeAll(supers.getNodes());
            }
        }
        System.out.printf("For %s and %s, there were %d entailment checks.\n", ce, propertyList, entailmentCheckCount);
        return new OWLClassNodeSet(finalResult);

    }

    private void computeExistentialFillers(final OWLClassExpression ce, final List<OWLObjectPropertyExpression> properties, final OWLClass curFiller, final Set<Node<OWLClass>> result, Set<OWLClass> visited) {
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("properties must not be empty");
        }
        if (visited.contains(curFiller)) {
            return;
        }
        visited.add(curFiller);
        final OWLDataFactory df = getDataFactory();
        OWLClassExpression chain = null;
        OWLClassExpression filler = curFiller;
        for (int i = properties.size() - 1; i > -1; i--) {
            OWLObjectPropertyExpression prop = properties.get(i);
            chain = filler = df.getOWLObjectSomeValuesFrom(prop, filler);
        }
        if (isEntailed(ce, chain)) {
            // More specific
            Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(curFiller);
            result.add(equivalentClasses);
            for (Node<OWLClass> sub : reasoner.getSubClasses(curFiller, true)) {
                OWLClass representative = sub.getRepresentativeElement();
                computeExistentialFillers(ce, properties, representative, result, visited);
            }
        }
    }

    private boolean isEntailed(OWLClassExpression ce, OWLClassExpression chain) {
        entailmentCheckCount++;
        OWLSubClassOfAxiom ax = getDataFactory().getOWLSubClassOfAxiom(ce, chain);
        return reasoner.isEntailed(ax);
    }


    private OWLDataFactory getDataFactory() {
        return reasoner.getRootOntology().getOWLOntologyManager().getOWLDataFactory();
    }
}
