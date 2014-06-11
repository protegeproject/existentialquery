package uk.ac.manchester.cs.owl.existentialquery;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.util.List;

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
public interface OWLExistentialReasoner {

    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, OWLObjectPropertyExpression property);

    public NodeSet<OWLClass> getFillers(OWLClassExpression ce, List<OWLObjectPropertyExpression> propertyList);
}


