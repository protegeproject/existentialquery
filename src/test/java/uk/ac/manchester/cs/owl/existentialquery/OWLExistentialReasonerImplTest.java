package uk.ac.manchester.cs.owl.existentialquery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OWLExistentialReasonerImplTest {
    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private OWLDataFactory df;
    private OWLReasoner reasoner;
    private OWLClass subject, filler, equivalentFiller, parent, intermediate, chainFiller;
    private OWLObjectProperty r, s;
    private OWLAxiom restriction;

    @Before
    public void setUp() throws Exception {
        manager = OWLManager.createOWLOntologyManager();
        ontology = manager.createOntology();
        df = manager.getOWLDataFactory();
        subject = cls("Subject");
        filler = cls("Filler");
        equivalentFiller = cls("EquivalentFiller");
        parent = cls("Parent");
        intermediate = cls("Intermediate");
        chainFiller = cls("ChainFiller");
        r = df.getOWLObjectProperty(IRI.create("urn:test#r"));
        s = df.getOWLObjectProperty(IRI.create("urn:test#s"));
        restriction = df.getOWLSubClassOfAxiom(subject, df.getOWLObjectSomeValuesFrom(r, filler));
        manager.addAxioms(ontology, new HashSet<>(Arrays.asList(
                restriction,
                df.getOWLSubClassOfAxiom(filler, parent),
                df.getOWLEquivalentClassesAxiom(filler, equivalentFiller),
                df.getOWLSubClassOfAxiom(subject, df.getOWLObjectSomeValuesFrom(s, intermediate)),
                df.getOWLSubClassOfAxiom(intermediate, df.getOWLObjectSomeValuesFrom(r, chainFiller)))));
        reasoner = new ReasonerFactory().createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    }

    @After
    public void tearDown() {
        if (reasoner != null) {
            reasoner.dispose();
        }
    }

    @Test
    public void returnsAllEntailedFillersIncludingEquivalentClasses() {
        assertEquals(set(filler, equivalentFiller, parent, df.getOWLThing()),
                query(FillerTreatment.ALL).getFillers(subject, r).getFlattened());
    }

    @Test
    public void returnsOnlyMostSpecificFillers() {
        assertEquals(set(filler, equivalentFiller),
                query(FillerTreatment.MOST_SPECIFIC).getFillers(subject, r).getFlattened());
    }

    @Test
    public void acceptsAnonymousSubclassExpressions() {
        OWLClassExpression expression = df.getOWLObjectIntersectionOf(subject, parent);
        assertEquals(set(filler, equivalentFiller),
                query(FillerTreatment.MOST_SPECIFIC).getFillers(expression, r).getFlattened());
    }

    @Test
    public void respectsPropertyChainOrder() {
        assertEquals(set(chainFiller), query(FillerTreatment.MOST_SPECIFIC)
                .getFillers(subject, Arrays.asList(s, r)).getFlattened());
        assertTrue(query(FillerTreatment.ALL)
                .getFillers(subject, Arrays.asList(r, s)).isEmpty());
    }

    @Test
    public void reflectsOntologyChangesAfterReasonerSynchronization() {
        OWLExistentialReasoner query = query(FillerTreatment.MOST_SPECIFIC);
        assertEquals(set(filler, equivalentFiller), query.getFillers(subject, r).getFlattened());
        manager.removeAxiom(ontology, restriction);
        reasoner.flush();
        assertTrue(query.getFillers(subject, r).isEmpty());
        manager.addAxiom(ontology, restriction);
        reasoner.flush();
        assertEquals(set(filler, equivalentFiller), query.getFillers(subject, r).getFlattened());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyPropertyChains() {
        query(FillerTreatment.ALL).getFillers(subject, Collections.emptyList());
    }

    private OWLExistentialReasoner query(FillerTreatment treatment) {
        return new OWLExistentialReasonerImpl(reasoner, treatment);
    }

    private OWLClass cls(String name) {
        return df.getOWLClass(IRI.create("urn:test#" + name));
    }

    private Set<OWLClass> set(OWLClass... classes) {
        return new HashSet<>(Arrays.asList(classes));
    }
}
