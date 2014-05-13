/**
Copyright (C) SYSTAP, LLC 2006-2014.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.bigdata.blueprints;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.bigdata.rdf.axioms.NoAxioms;
import com.bigdata.rdf.sail.AbstractBigdataSailTestCase;
import com.bigdata.rdf.sail.BigdataSail;
import com.bigdata.rdf.sail.BigdataSailRepository;
import com.bigdata.rdf.vocab.NoVocabulary;
import com.tinkerpop.blueprints.Contains;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.EdgeTestSuite;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.GraphQueryTestSuite;
import com.tinkerpop.blueprints.GraphTestSuite;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.TestSuite;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQueryTestSuite;
import com.tinkerpop.blueprints.VertexTestSuite;
import com.tinkerpop.blueprints.impls.GraphTest;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;

/**
 */
public abstract class AbstractTestBigdataGraph extends AbstractBigdataSailTestCase {

    protected static final transient Logger log = Logger.getLogger(AbstractTestBigdataGraph.class);
    
    /**
     * 
     */
    public AbstractTestBigdataGraph() {
    }

    /**
     * @param name
     */
    public AbstractTestBigdataGraph(String name) {
        super(name);
    }
    
    protected BigdataSail getSail() {
        
        return getSail(getProperties());
        
    }
    
    public Properties getProperties() {
        
        Properties props = super.getProperties();

        //no inference
        props.setProperty(BigdataSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        props.setProperty(BigdataSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
        props.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
        props.setProperty(BigdataSail.Options.JUSTIFY, "false");
        
        // no text index
        props.setProperty(BigdataSail.Options.TEXT_INDEX, "false");
        
        // triples mode
        props.setProperty(BigdataSail.Options.QUADS, "false");
        props.setProperty(BigdataSail.Options.STATEMENT_IDENTIFIERS, "false");
        
        return props;
        
    }
    
    @Override
    protected BigdataSail getSail(final Properties properties) {
        
        return new BigdataSail(properties);
        
    }

    @Override
    protected BigdataSail reopenSail(final BigdataSail sail) {

        final Properties properties = sail.getDatabase().getProperties();

        if (sail.isOpen()) {

            try {

                sail.shutDown();

            } catch (Exception ex) {

                throw new RuntimeException(ex);

            }

        }
        
        return getSail(properties);
        
    }

    protected abstract GraphTest newBigdataGraphTest() throws Exception;
    

    public void testVertexTestSuite() throws Exception {
    	final GraphTest test = newBigdataGraphTest();
    	test.stopWatch();
        test.doTestSuite(new VertexTestSuite(test));
        GraphTest.printTestPerformance("VertexTestSuite", test.stopWatch());
    }

    public void testEdgeSuite() throws Exception {
    	final GraphTest test = newBigdataGraphTest();
    	test.stopWatch();
        test.doTestSuite(new EdgeTestSuite(test));
        GraphTest.printTestPerformance("EdgeTestSuite", test.stopWatch());
    }

    public void testGraphSuite() throws Exception {
    	final GraphTest test = newBigdataGraphTest();
    	test.stopWatch();
        test.doTestSuite(new GraphTestSuite(test));
        GraphTest.printTestPerformance("GraphTestSuite", test.stopWatch());
    }

    public void testVertexQueryTestSuite() throws Exception {
    	final GraphTest test = newBigdataGraphTest();
    	test.stopWatch();
        test.doTestSuite(new VertexQueryTestSuite(test));
        GraphTest.printTestPerformance("VertexQueryTestSuite", test.stopWatch());
    }

    public void testGraphQueryTestSuite() throws Exception {
    	final GraphTest test = newBigdataGraphTest();
    	test.stopWatch();
        test.doTestSuite(new GraphQueryTestSuite(test));
        GraphTest.printTestPerformance("GraphQueryTestSuite", test.stopWatch());
    }
//
//    public void testTransactionalGraphTestSuite() throws Exception {
//    	final GraphTest test = newBigdataGraphTest();
//    	test.stopWatch();
//        test.doTestSuite(new TransactionalGraphTestSuite(test));
//        GraphTest.printTestPerformance("TransactionalGraphTestSuite", test.stopWatch());
//    }
//
//    public void testGraphQueryForHasOR() throws Exception {
//        final BigdataGraphTest test = newBigdataGraphTest();
//        test.stopWatch();
//        final BigdataTestSuite testSuite = new BigdataTestSuite(test);
//        try {
//            testSuite.testGraphQueryForHasOR();
//        } finally {
//            test.shutdown();
//        }
//        
//    }
    
//    private static class BigdataTestSuite extends TestSuite {
//        
//        public BigdataTestSuite(final BigdataGraphTest graphTest) {
//            super(graphTest);
//        }
//        
//        public void testGraphQueryForHasOR() {
//            Graph graph = graphTest.generateGraph();
//            if (graph.getFeatures().supportsEdgeIndex && graph instanceof KeyIndexableGraph) {
//                ((KeyIndexableGraph) graph).createKeyIndex("type", Edge.class);
//            }
//            if (graph.getFeatures().supportsEdgeIteration  && graph.getFeatures().supportsEdgeProperties && graph.getFeatures().supportsVertexProperties) {
//                Vertex marko = graph.addVertex(null);
//                marko.setProperty("name", "marko");
//                Vertex matthias = graph.addVertex(null);
//                matthias.setProperty("name", "matthias");
//                Vertex stephen = graph.addVertex(null);
//                stephen.setProperty("name", "stephen");
//
//                Edge edge = marko.addEdge("knows", stephen);
//                edge.setProperty("type", "tinkerpop");
//                edge.setProperty("weight", 1.0);
//                edge = marko.addEdge("knows", matthias);
//                edge.setProperty("type", "aurelius");
//
//                assertEquals(count(graph.query().has("type", Contains.IN, Arrays.asList("tinkerpop", "aurelius")).edges()), 2);
//                assertEquals(count(graph.query().has("type", Contains.IN, Arrays.asList("tinkerpop", "aurelius")).has("type", "tinkerpop").edges()), 1);
//                assertEquals(count(graph.query().has("type", Contains.IN, Arrays.asList("tinkerpop", "aurelius")).has("type", "tinkerpop").has("type", "aurelius").edges()), 0);
//                assertEquals(graph.query().has("weight").edges().iterator().next().getProperty("type"), "tinkerpop");
//                assertEquals(graph.query().has("weight").edges().iterator().next().getProperty("weight"), 1.0);
//                assertEquals(graph.query().hasNot("weight").edges().iterator().next().getProperty("type"), "aurelius");
//                assertNull(graph.query().hasNot("weight").edges().iterator().next().getProperty("weight"));
//
//                List result = asList(graph.query().has("name", Contains.IN, Arrays.asList("marko", "stephen")).vertices());
//                for (Object o : result) {
//                    final Vertex v = (Vertex) o;
//                    log.trace(v.getProperty("name"));
//                }
//                assertEquals(result.size(), 2);
//                assertTrue(result.contains(marko));
//                assertTrue(result.contains(stephen));
//                result = asList(graph.query().has("name", Contains.IN, Arrays.asList("marko", "stephen", "matthias", "josh", "peter")).vertices());
//                assertEquals(result.size(), 3);
//                assertTrue(result.contains(marko));
//                assertTrue(result.contains(stephen));
//                assertTrue(result.contains(matthias));
//                result = asList(graph.query().has("name").vertices());
//                assertEquals(result.size(), 3);
//                assertTrue(result.contains(marko));
//                assertTrue(result.contains(stephen));
//                assertTrue(result.contains(matthias));
//                result = asList(graph.query().hasNot("name").vertices());
//                assertEquals(result.size(), 0);
//                result = asList(graph.query().hasNot("blah").vertices());
//                assertEquals(result.size(), 3);
//                assertTrue(result.contains(marko));
//                assertTrue(result.contains(stephen));
//                assertTrue(result.contains(matthias));
//                result = asList(graph.query().has("name", Contains.NOT_IN, Arrays.asList("bill", "sam")).vertices());
//                assertEquals(result.size(), 3);
//                assertTrue(result.contains(marko));
//                assertTrue(result.contains(stephen));
//                assertTrue(result.contains(matthias));
//                result = asList(graph.query().has("name", Contains.IN, Arrays.asList("bill", "matthias", "stephen", "marko")).vertices());
//                assertEquals(result.size(), 3);
//                assertTrue(result.contains(marko));
//                assertTrue(result.contains(stephen));
//                assertTrue(result.contains(matthias));
//            }
//            graph.shutdown();
//        }        
//    }
//    
//    
//    private class BigdataGraphTest extends GraphTest {
//
//		@Override
//		public void doTestSuite(TestSuite testSuite) throws Exception {
//	        for (Method method : testSuite.getClass().getDeclaredMethods()) {
//	            if (method.getName().startsWith("test")) {
//	                System.out.println("Testing " + method.getName() + "...");
//	                try {
//		                method.invoke(testSuite);
//	                } catch (Exception ex) {
//	                	ex.getCause().printStackTrace();
//	                	throw ex;
//	                } finally {
//		                shutdown();
//	                }
//	            }
//	        }
//		}
//		
//		private Map<String,BigdataSail> testSails = new LinkedHashMap<String, BigdataSail>();
//
//		@Override
//		public Graph generateGraph(final String key) {
//			
//			try {
//	            if (testSails.containsKey(key) == false) {
//	                final BigdataSail testSail = getSail();
//	                testSail.initialize();
//	                testSails.put(key, testSail);
//	            }
//	            
//				final BigdataSail sail = testSails.get(key); //testSail; //getSail();
//				final BigdataSailRepository repo = new BigdataSailRepository(sail);
//				final BigdataGraph graph = new BigdataGraphEmbedded(repo) {
//	
//				    /**
//				     * Test cases have weird semantics for shutdown.
//				     */
//					@Override
//					public void shutdown() {
//					    try {
//				            if (cxn != null) {
//    					        cxn.commit();
//    					        cxn.close();
//    					        cxn = null;
//				            }
//					    } catch (Exception ex) {
//					        throw new RuntimeException(ex);
//					    }
//					}
//					
//				};
//				return graph;
//			} catch (Exception ex) {
//				throw new RuntimeException(ex);
//			}
//			
//		}
//
//		@Override
//		public Graph generateGraph() {
//			
//			return generateGraph(null);
//		}
//		
//		public void shutdown() {
//		    for (BigdataSail sail : testSails.values()) {
//		        sail.__tearDownUnitTest();
//		    }
//		    testSails.clear();
//		}
//		
//    	
//    }

}
