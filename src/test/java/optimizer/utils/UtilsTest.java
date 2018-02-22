package optimizer.utils;

import optimizer.objective.Relation;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Test
    public void complyTest(){
        assertTrue(Utils.comply(Relation.GREATER_THAN,15,10,14));
        assertFalse(Utils.comply(Relation.LESS_THAN,15,10,14));
        assertTrue(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,15,2,14));
        assertFalse(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,17,2,14));

        assertTrue(Utils.comply(Relation.GREATER_THAN,15f,10f,14f));
        assertFalse(Utils.comply(Relation.LESS_THAN,15f,10f,14f));
        assertTrue(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,15f,2f,14f));
        assertFalse(Utils.comply(Relation.MAXIMIZE_TO_CONVERGENCE,17f,2f,14f));

        assertFalse(Utils.comply(Relation.MAXIMIZE,15f,10f,14f));
        assertFalse(Utils.comply(Relation.MAXIMIZE,15f,10f,14f));
        assertFalse(Utils.comply(Relation.MINIMIZE,15f,2f,14f));
        assertFalse(Utils.comply(Relation.MINIMIZE,17f,2f,14f));    
    }
}
